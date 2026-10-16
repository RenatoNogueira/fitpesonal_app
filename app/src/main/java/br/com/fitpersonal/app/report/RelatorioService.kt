package br.com.fitpersonal.app.report

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import br.com.fitpersonal.app.data.files.FotoStorage
import br.com.fitpersonal.app.data.repo.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.text.Normalizer
import br.com.fitpersonal.app.domain.model.AvaliacaoFisica
import br.com.fitpersonal.app.domain.model.StatusAgendamento
import br.com.fitpersonal.app.domain.model.TipoAgendamento
import br.com.fitpersonal.app.domain.rules.LinhaAgendaMes
import br.com.fitpersonal.app.domain.rules.RelatorioMensalRules
import br.com.fitpersonal.app.domain.rules.TreinoEntregaRules
import br.com.fitpersonal.app.domain.rules.TreinoRules
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/** Monta os dados, gera o PDF em cache/pdf e devolve o arquivo (pronto para compartilhar). */
@Singleton
class RelatorioService @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val avaliacoes: AvaliacaoRepository,
    private val alunos: AlunoRepository,
    private val treinos: TreinoRepository,
    private val agenda: AgendaRepository,
    private val pagamentos: PagamentoRepository,
    private val contrato: br.com.fitpersonal.app.data.contrato.ContratoService,
    private val perfilRepo: PerfilRepository,
    private val fotos: FotoStorage,
) {

    suspend fun avaliacao(idAvaliacao: Long): File = withContext(Dispatchers.IO) {
        val a = avaliacoes.buscar(idAvaliacao) ?: error("Avaliação não encontrada")
        val aluno = alunos.buscar(a.idAluno) ?: error("Aluno não encontrado")
        val perfil = perfilRepo.perfil.first()
        val arquivo = saida("avaliacao_${slug(aluno.nome)}_${a.data}.pdf")
        arquivo.outputStream().use { out ->
            AvaliacaoPdf.gerar(a, aluno, perfil, logo(perfil.logo), { carregar(it, 900) }, out)
        }
        arquivo
    }

    /** Comparativo entre duas avaliações do mesmo aluno (a mais antiga vem primeiro, independentemente da ordem passada). */
    suspend fun comparativo(idAvaliacaoA: Long, idAvaliacaoB: Long): File = withContext(Dispatchers.IO) {
        val a = avaliacoes.buscar(idAvaliacaoA) ?: error("Avaliação não encontrada")
        val b = avaliacoes.buscar(idAvaliacaoB) ?: error("Avaliação não encontrada")
        require(a.idAluno == b.idAluno) { "As avaliações precisam ser do mesmo aluno" }
        val (antes, depois) = if (a.data <= b.data) a to b else b to a
        val aluno = alunos.buscar(antes.idAluno) ?: error("Aluno não encontrado")
        val perfil = perfilRepo.perfil.first()
        val arquivo = saida("comparativo_${slug(aluno.nome)}_${antes.data}_${depois.data}.pdf")
        arquivo.outputStream().use { out ->
            ComparativoPdf.gerar(aluno, antes, depois, perfil, logo(perfil.logo), out, fotosLado(antes, depois))
        }
        arquivo
    }

    /** Só entram posições com foto nas duas avaliações, para o par ficar lado a lado. */
    private fun fotosLado(antes: AvaliacaoFisica, depois: AvaliacaoFisica): List<Pair<String, Bitmap>> {
        val posicoes = listOf(
            Triple("Frente", antes.fotoFrente, depois.fotoFrente),
            Triple("Costas", antes.fotoCostas, depois.fotoCostas),
            Triple("Lateral dir.", antes.fotoLateralDir, depois.fotoLateralDir),
            Triple("Lateral esq.", antes.fotoLateralEsq, depois.fotoLateralEsq),
        )
        return posicoes.flatMap { (nome, a, d) ->
            val ba = a?.let { carregar(it, 600) }
            val bd = d?.let { carregar(it, 600) }
            if (ba != null && bd != null) {
                listOf("$nome — ${antes.data.format(DATA_BR)}" to ba, "$nome — ${depois.data.format(DATA_BR)}" to bd)
            } else emptyList()
        }
    }

    suspend fun treino(idTreino: Long): File = withContext(Dispatchers.IO) {
        val t = treinos.buscar(idTreino) ?: error("Treino não encontrado")
        val aluno = alunos.buscar(t.idAluno) ?: error("Aluno não encontrado")
        val exercicios = treinos.exerciciosDe(idTreino)
        val perfil = perfilRepo.perfil.first()
        val arquivo = saida("treino_${slug(aluno.nome)}_${slug(t.nome)}.pdf")
        arquivo.outputStream().use { out ->
            TreinoPdf.gerar(t, exercicios, aluno, perfil, logo(perfil.logo), out)
        }
        arquivo
    }

    suspend fun agenda(dia: LocalDate): File = withContext(Dispatchers.IO) {
        val itens = agenda.doDia(dia).first()
        val perfil = perfilRepo.perfil.first()
        val arquivo = saida("agenda_$dia.pdf")
        arquivo.outputStream().use { out -> AgendaPdf.gerar(dia, itens, perfil, logo(perfil.logo), out) }
        arquivo
    }

    /** Treino em texto formatado (WhatsApp), pronto para colar ou compartilhar. */
    suspend fun treinoTexto(idTreino: Long): String = withContext(Dispatchers.IO) {
        val t = treinos.buscar(idTreino) ?: error("Treino não encontrado")
        val aluno = alunos.buscar(t.idAluno) ?: error("Aluno não encontrado")
        val perfil = perfilRepo.perfil.first()
        TreinoEntregaRules.texto(
            aluno.nome, t.nome, t.objetivo,
            TreinoRules.agruparPorDivisao(treinos.exerciciosDe(idTreino)),
            perfil.nome,
        )
    }

    /** Treino como página HTML única (abre no navegador do celular do aluno, sem instalar nada). */
    suspend fun treinoHtml(idTreino: Long): File = withContext(Dispatchers.IO) {
        val t = treinos.buscar(idTreino) ?: error("Treino não encontrado")
        val aluno = alunos.buscar(t.idAluno) ?: error("Aluno não encontrado")
        val perfil = perfilRepo.perfil.first()
        // Embute o contrato: a página passa a registrar sessões e devolver o resumo por código.
        val pacoteJson = runCatching { contrato.gerar(idTreino).arquivo.readText(Charsets.UTF_8) }.getOrNull()
        val html = TreinoEntregaRules.html(
            nomeAluno = aluno.nome,
            nomeTreino = t.nome,
            objetivo = t.objetivo,
            dataTexto = t.dataCriacao.format(DATA_BR),
            profissional = perfil.nome,
            marca = perfil.marca,
            grupos = TreinoRules.agruparPorDivisao(treinos.exerciciosDe(idTreino)),
            pacoteJson = pacoteJson,
        )
        val arquivo = saida("treino_${slug(aluno.nome)}_${slug(t.nome)}.html")
        arquivo.writeText(html, Charsets.UTF_8)
        arquivo
    }

    /** Recibo de um pagamento de mensalidade. */
    suspend fun recibo(idPagamento: Long): File = withContext(Dispatchers.IO) {
        val p = pagamentos.buscar(idPagamento) ?: error("Pagamento não encontrado")
        val aluno = alunos.buscar(p.idAluno) ?: error("Aluno não encontrado")
        val perfil = perfilRepo.perfil.first()
        val arquivo = saida("recibo_${slug(aluno.nome)}_${p.competencia}.pdf")
        arquivo.outputStream().use { out -> ReciboPdf.gerar(p, aluno, perfil, logo(perfil.logo), out) }
        arquivo
    }

    /** Relatório mensal de agenda e avaliações (todos os alunos). */
    suspend fun mensal(mes: YearMonth): File = withContext(Dispatchers.IO) {
        val linhas = agenda.doMes(mes).map {
            LinhaAgendaMes(it.nomeAluno, (TipoAgendamento.entries.firstOrNull { e -> e.name == it.tipo } ?: TipoAgendamento.TREINO), (StatusAgendamento.entries.firstOrNull { e -> e.name == it.status } ?: StatusAgendamento.PENDENTE))
        }
        val resumo = RelatorioMensalRules.resumir(linhas)
        val avaliacoesNoMes = avaliacoes.contarNoMes(mes)
        val perfil = perfilRepo.perfil.first()
        val dataBr = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val recebimentos = pagamentos.daCompetencia(mes).map {
            Triple(it.nomeAluno, it.valorCentavos, LocalDate.parse(it.pagoEm).format(dataBr))
        }
        val arquivo = saida("relatorio_$mes.pdf")
        arquivo.outputStream().use { out ->
            RelatorioMensalPdf.gerar(mes, resumo, avaliacoesNoMes, perfil, logo(perfil.logo), out, recebimentos)
        }
        arquivo
    }

    // ───────────── helpers ─────────────

    private val DATA_BR: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    private fun saida(nome: String): File = File(ctx.cacheDir, "pdf").apply { mkdirs() }.resolve(nome)

    private fun logo(relativo: String?): Bitmap? = relativo?.let { carregar(it, 300) }

    /** Decodifica reduzindo (lado maior ≈ [maxLado]) para manter o PDF leve. */
    private fun carregar(relativo: String, maxLado: Int): Bitmap? {
        val f = fotos.arquivo(relativo)
        if (!f.exists()) return null
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, limites)
        var amostra = 1
        while (maxOf(limites.outWidth, limites.outHeight) / amostra > maxLado * 2) amostra *= 2
        return BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = amostra })
    }

    private fun slug(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^A-Za-z0-9]+"), "_")
            .trim('_')
            .lowercase()
            .take(40)
            .ifEmpty { "sem_nome" }
}
