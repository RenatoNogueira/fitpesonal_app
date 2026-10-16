package br.com.fitpersonal.app.data.privacidade

import android.content.Context
import br.com.fitpersonal.app.data.local.AgendamentoDao
import br.com.fitpersonal.app.data.local.AlunoDao
import br.com.fitpersonal.app.data.local.AvaliacaoDao
import br.com.fitpersonal.app.data.local.PagamentoDao
import br.com.fitpersonal.app.data.local.toDomain
import br.com.fitpersonal.app.domain.rules.CsvRules
import br.com.fitpersonal.app.domain.rules.EvolucaoRules
import br.com.fitpersonal.app.domain.rules.FinanceiroRules
import br.com.fitpersonal.app.domain.rules.Metrica
import br.com.fitpersonal.app.domain.rules.ParqRules
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Planilhas CSV (Excel pt-BR: UTF-8 com BOM, separador ";") dentro de um zip em cache/export.
 * Não inclui CPF nem observações médicas; avaliações trazem só medidas e a contagem de "sim" do PAR-Q.
 */
@Singleton
class ExportacaoService @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val alunos: AlunoDao,
    private val avaliacoes: AvaliacaoDao,
    private val agenda: AgendamentoDao,
    private val pagamentos: PagamentoDao,
) {

    suspend fun exportarPlanilhas(hoje: LocalDate = LocalDate.now()): File = withContext(Dispatchers.IO) {
        val listaAlunos = alunos.todosSnapshot().map { it.toDomain() }.sortedBy { it.nome.lowercase() }
        val nomes = listaAlunos.associate { it.id to it.nome }
        fun nome(id: Long) = nomes[id].orEmpty()

        val csvAlunos = montar(
            listOf("Nome", "Nascimento", "Sexo", "Telefone", "E-mail", "Objetivo", "Cadastro", "Consentimento LGPD",
                "Mensalidade", "Dia do vencimento"),
            listaAlunos.map { a ->
                listOf(
                    CsvRules.texto(a.nome), a.dataNascimento?.toString().orEmpty(), CsvRules.texto(a.sexo),
                    CsvRules.texto(a.telefone), CsvRules.texto(a.email), CsvRules.texto(a.objetivo),
                    a.dataCadastro.toString(), a.consentimentoLgpdEm?.toString().orEmpty(),
                    a.mensalidadeCentavos?.let(FinanceiroRules::formatarReais).orEmpty(),
                    a.diaVencimento?.toString().orEmpty(),
                )
            },
        )

        val csvAvaliacoes = montar(
            listOf("Aluno", "Data", "Peso (kg)", "Altura (m)", "IMC", "% gordura", "Massa magra (kg)",
                "Massa gorda (kg)", "Cintura (cm)", "Quadril (cm)", "RCQ", "PA sistólica", "PA diastólica", "PAR-Q (sim)"),
            avaliacoes.todosSnapshot().map { it.toDomain() }.map { a ->
                listOf(
                    CsvRules.texto(nome(a.idAluno)), a.data.toString(),
                    CsvRules.numero(a.pesoKg), CsvRules.numero(a.alturaM, 2),
                    CsvRules.numero(EvolucaoRules.valor(a, Metrica.IMC)),
                    CsvRules.numero(a.percentualGordura), CsvRules.numero(a.massaMagraKg), CsvRules.numero(a.massaGordaKg),
                    CsvRules.numero(a.cinturaCm), CsvRules.numero(a.quadrilCm),
                    CsvRules.numero(EvolucaoRules.valor(a, Metrica.RCQ), 2),
                    a.pressaoSistolica?.toString().orEmpty(), a.pressaoDiastolica?.toString().orEmpty(),
                    if (ParqRules.aplicado(a.parq)) ParqRules.respostasSim(a.parq).size.toString() else "",
                )
            },
        )

        val csvAgenda = montar(
            listOf("Aluno", "Data", "Horário", "Tipo", "Status"),
            agenda.todosSnapshot().map { it.toDomain() }.map { g ->
                listOf(CsvRules.texto(nome(g.idAluno)), g.data.toString(), g.horario.toString(), g.tipo.rotulo, g.status.rotulo)
            },
        )

        val csvPagamentos = montar(
            listOf("Aluno", "Competência", "Valor", "Pago em"),
            pagamentos.todosSnapshot().map { it.toDomain() }.map { p ->
                listOf(
                    CsvRules.texto(nome(p.idAluno)), p.competencia.toString(),
                    FinanceiroRules.formatarReais(p.valorCentavos), p.pagoEm.toString(),
                )
            },
        )

        val zip = File(ctx.cacheDir, "export").apply { mkdirs() }.resolve("fitpersonal_planilhas_$hoje.zip")
        ZipOutputStream(zip.outputStream().buffered()).use { z ->
            mapOf(
                "alunos.csv" to csvAlunos,
                "avaliacoes.csv" to csvAvaliacoes,
                "agenda.csv" to csvAgenda,
                "pagamentos.csv" to csvPagamentos,
            ).forEach { (nomeArquivo, conteudo) ->
                z.putNextEntry(ZipEntry(nomeArquivo))
                z.write(conteudo.toByteArray(Charsets.UTF_8))
                z.closeEntry()
            }
        }
        zip
    }

    private fun montar(cabecalho: List<String>, linhas: List<List<String>>): String =
        CsvRules.BOM + (listOf(cabecalho.joinToString(CsvRules.SEPARADOR.toString())) +
            linhas.map { it.joinToString(CsvRules.SEPARADOR.toString()) }).joinToString("\r\n") + "\r\n"
}
