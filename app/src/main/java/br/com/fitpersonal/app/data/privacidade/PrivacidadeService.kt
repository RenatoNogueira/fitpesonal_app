package br.com.fitpersonal.app.data.privacidade

import android.content.Context
import br.com.fitpersonal.app.data.files.FotoStorage
import br.com.fitpersonal.app.data.local.AgendamentoDao
import br.com.fitpersonal.app.data.local.AlunoDao
import br.com.fitpersonal.app.data.local.AvaliacaoDao
import br.com.fitpersonal.app.data.local.HistoricoCargaDao
import br.com.fitpersonal.app.data.local.ListaEsperaDao
import br.com.fitpersonal.app.data.local.PagamentoDao
import br.com.fitpersonal.app.data.local.TesteAptidaoDao
import br.com.fitpersonal.app.data.local.TreinoDao
import br.com.fitpersonal.app.data.local.toDomain
import br.com.fitpersonal.app.work.LembreteAgendador
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton

/** Direitos do titular (LGPD): cópia de todos os dados de um aluno e eliminação definitiva. */
@Singleton
class PrivacidadeService @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val alunos: AlunoDao,
    private val avaliacoes: AvaliacaoDao,
    private val treinos: TreinoDao,
    private val agenda: AgendamentoDao,
    private val pagamentos: PagamentoDao,
    private val historicoCarga: HistoricoCargaDao,
    private val testes: TesteAptidaoDao,
    private val espera: ListaEsperaDao,
    private val fotos: FotoStorage,
    private val lembretes: LembreteAgendador,
) {

    /** Gera `cache/export/dados_<aluno>.json` com cadastro, avaliações, treinos e agendamentos. */
    suspend fun exportar(idAluno: Long): File = withContext(Dispatchers.IO) {
        val aluno = alunos.buscar(idAluno)?.toDomain() ?: error("Aluno não encontrado")
        val raiz = JSONObject()
        raiz.put("exportadoEm", java.time.LocalDateTime.now().toString())
        raiz.put("aluno", JSONObject().apply {
            put("nome", aluno.nome)
            put("dataNascimento", aluno.dataNascimento?.toString() ?: JSONObject.NULL)
            put("sexo", aluno.sexo ?: JSONObject.NULL)
            put("telefone", aluno.telefone ?: JSONObject.NULL)
            put("email", aluno.email ?: JSONObject.NULL)
            put("cpf", aluno.cpf ?: JSONObject.NULL)
            put("cep", aluno.cep ?: JSONObject.NULL)
            put("instagram", aluno.instagram ?: JSONObject.NULL)
            put("objetivo", aluno.objetivo ?: JSONObject.NULL)
            put("observacoesMedicas", aluno.observacoesMedicas ?: JSONObject.NULL)
            put("dataCadastro", aluno.dataCadastro.toString())
            put("consentimentoLgpdEm", aluno.consentimentoLgpdEm?.toString() ?: JSONObject.NULL)
            put("metaMetrica", aluno.metaMetrica ?: JSONObject.NULL)
            put("metaValor", aluno.metaValor ?: JSONObject.NULL)
            put("reavaliacaoDias", aluno.reavaliacaoDias ?: JSONObject.NULL)
            put("mensalidadeCentavos", aluno.mensalidadeCentavos ?: JSONObject.NULL)
            put("diaVencimento", aluno.diaVencimento ?: JSONObject.NULL)
            put("pacoteSessoes", aluno.pacoteSessoes ?: JSONObject.NULL)
            put("pacoteInicio", aluno.pacoteInicio?.toString() ?: JSONObject.NULL)
        })

        val av = JSONArray()
        avaliacoes.doAlunoSnapshot(idAluno).map { it.toDomain() }.sortedBy { it.data }.forEach { a ->
            av.put(JSONObject().apply {
                put("data", a.data.toString())
                put("pesoKg", a.pesoKg ?: JSONObject.NULL)
                put("alturaM", a.alturaM ?: JSONObject.NULL)
                put("percentualGordura", a.percentualGordura ?: JSONObject.NULL)
                put("massaMagraKg", a.massaMagraKg ?: JSONObject.NULL)
                put("massaGordaKg", a.massaGordaKg ?: JSONObject.NULL)
                put("cinturaCm", a.cinturaCm ?: JSONObject.NULL)
                put("quadrilCm", a.quadrilCm ?: JSONObject.NULL)
                put("ldl", a.ldl ?: JSONObject.NULL)
                put("hdl", a.hdl ?: JSONObject.NULL)
                put("pressaoSistolica", a.pressaoSistolica ?: JSONObject.NULL)
                put("pressaoDiastolica", a.pressaoDiastolica ?: JSONObject.NULL)
                put("condicoes", JSONArray(a.condicoes.map { it.name }))
                put("historicoSaude", a.historicoSaude ?: JSONObject.NULL)
                put("anamnese", a.anamnese ?: JSONObject.NULL)
                put("estiloVida", a.estiloVida ?: JSONObject.NULL)
                put("metas", a.metas ?: JSONObject.NULL)
                put("postura", a.postura ?: JSONObject.NULL)
                put("funcionalMobilidade", a.funcionalMobilidade ?: JSONObject.NULL)
                put("cardio", a.cardio ?: JSONObject.NULL)
                put("forcaResistencia", a.forcaResistencia ?: JSONObject.NULL)
                put("parq", a.parq ?: JSONObject.NULL)
                put("assinaturaColetada", a.assinatura != null)
                put("protocoloDobras", a.protocoloDobras?.name ?: JSONObject.NULL)
                put("dobrasMm", JSONObject().apply { a.dobrasMm.forEach { (d, v) -> put(d.name, v) } })
            })
        }
        raiz.put("avaliacoes", av)

        val tr = JSONArray()
        treinos.doAlunoSnapshot(idAluno).map { it.toDomain() }.forEach { t ->
            val ex = JSONArray()
            treinos.exerciciosSnapshot(t.id).map { it.toDomain() }.forEach { e ->
                ex.put(JSONObject().apply {
                    put("nome", e.nome)
                    put("divisao", e.divisao.name)
                    put("series", e.series ?: JSONObject.NULL)
                    put("repeticoes", e.repeticoes ?: JSONObject.NULL)
                    put("carga", e.carga ?: JSONObject.NULL)
                    put("descanso", e.descanso ?: JSONObject.NULL)
                    put("observacoes", e.observacoes ?: JSONObject.NULL)
                })
            }
            tr.put(JSONObject().apply {
                put("nome", t.nome)
                put("objetivo", t.objetivo ?: JSONObject.NULL)
                put("dataCriacao", t.dataCriacao.toString())
                put("exercicios", ex)
            })
        }
        raiz.put("treinos", tr)

        val ag = JSONArray()
        agenda.doAlunoSnapshot(idAluno).map { it.toDomain() }.sortedBy { it.data }.forEach { g ->
            ag.put(JSONObject().apply {
                put("data", g.data.toString())
                put("horario", g.horario.toString())
                put("tipo", g.tipo.name)
                put("status", g.status.name)
                put("anotacao", g.anotacao ?: JSONObject.NULL)
            })
        }
        raiz.put("agendamentos", ag)

        val pg = JSONArray()
        pagamentos.doAlunoSnapshot(idAluno).sortedBy { it.competencia }.forEach { p ->
            pg.put(JSONObject().apply {
                put("competencia", p.competencia)
                put("valorCentavos", p.valorCentavos)
                put("pagoEm", p.pagoEm)
            })
        }
        raiz.put("pagamentos", pg)

        val cg = JSONArray()
        historicoCarga.doAlunoSnapshot(idAluno).sortedBy { it.data }.forEach { c ->
            cg.put(JSONObject().apply {
                put("exercicio", c.exercicio)
                put("data", c.data)
                put("cargaKg", c.cargaKg)
            })
        }
        raiz.put("historicoCarga", cg)

        val ta = JSONArray()
        testes.doAlunoSnapshot(idAluno).sortedBy { it.data }.forEach { t ->
            ta.put(JSONObject().apply {
                put("tipo", t.tipo)
                put("data", t.data)
                put("valor", t.valor)
                put("observacao", t.observacao ?: JSONObject.NULL)
            })
        }
        raiz.put("testesAptidao", ta)

        val le = JSONArray()
        espera.doAlunoSnapshot(idAluno).forEach { e ->
            le.put(JSONObject().apply {
                put("diaSemana", e.diaSemana)
                put("horario", e.horario ?: JSONObject.NULL)
                put("observacao", e.observacao ?: JSONObject.NULL)
                put("criadoEm", e.criadoEm)
            })
        }
        raiz.put("listaEspera", le)

        val arquivo = File(ctx.cacheDir, "export").apply { mkdirs() }
            .resolve("dados_${slug(aluno.nome)}.json")
        arquivo.writeText(raiz.toString(2), Charsets.UTF_8)
        arquivo
    }

    /**
     * Elimina o aluno e tudo que é dele: registros (cascata do banco), fotos de perfil e de avaliações,
     * lembretes agendados e cópias em cache (PDFs e exportações). Não há como desfazer.
     */
    suspend fun excluirDefinitivamente(idAluno: Long) = withContext(Dispatchers.IO) {
        val aluno = alunos.buscar(idAluno) ?: return@withContext
        val arquivosFoto: List<String> =
            listOfNotNull(aluno.fotoPerfil) + avaliacoes.doAlunoSnapshot(idAluno).flatMap { a ->
                listOfNotNull(a.fotoFrente, a.fotoCostas, a.fotoLateralDir, a.fotoLateralEsq, a.assinatura)
            }
        val idsAgenda = agenda.doAlunoSnapshot(idAluno).map { it.id }

        alunos.excluir(aluno)

        idsAgenda.forEach(lembretes::cancelar)
        arquivosFoto.forEach(fotos::apagar)
        // PDFs e exportações em cache podem conter dados do aluno
        listOf("pdf", "export").forEach { File(ctx.cacheDir, it).deleteRecursively() }
    }

    private fun slug(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^A-Za-z0-9]+"), "_")
            .trim('_')
            .lowercase()
            .take(40)
            .ifEmpty { "aluno" }
}
