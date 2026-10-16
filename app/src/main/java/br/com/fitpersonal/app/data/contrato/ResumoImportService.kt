package br.com.fitpersonal.app.data.contrato

import androidx.room.withTransaction
import br.com.fitpersonal.app.data.local.AlunoDao
import br.com.fitpersonal.app.data.local.ExecucaoImportadaEntity
import br.com.fitpersonal.app.data.local.HistoricoCargaDao
import br.com.fitpersonal.app.data.local.HistoricoCargaEntity
import br.com.fitpersonal.app.data.local.RetornoAlunoEntity
import br.com.fitpersonal.app.data.local.RetornoDao
import br.com.fitpersonal.app.data.local.FitDatabase
import br.com.fitpersonal.app.domain.rules.CargaRules
import br.com.fitpersonal.contrato.CodigoTexto
import br.com.fitpersonal.contrato.ContratoJson
import br.com.fitpersonal.contrato.Leitura
import br.com.fitpersonal.contrato.MotivoErro
import br.com.fitpersonal.contrato.Resumo
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Resultado da análise (sem gravar nada): o que o profissional vê antes de confirmar. */
sealed interface PreviaResumo {
    data class Ok(
        val resumo: Resumo,
        /** O token do aluno no resumo bate com o do aluno desta ficha. */
        val confere: Boolean,
        val sessoesNovas: Int,
        val sessoesJaImportadas: Int,
        /** O resumo inteiro já foi importado antes. */
        val resumoJaImportado: Boolean,
        val nomeCargasNovas: Int,
    ) : PreviaResumo

    data class Erro(val mensagem: String) : PreviaResumo
}

data class ResultadoImportacao(val sessoes: Int, val cargasRegistradas: Int)

/** Lê o resumo devolvido pelo aluno (arquivo ou código) e grava o que for novo, sem duplicar. */
@Singleton
class ResumoImportService @Inject constructor(
    private val db: FitDatabase,
    private val alunos: AlunoDao,
    private val retornos: RetornoDao,
    private val historico: HistoricoCargaDao,
) {

    private fun jsonDe(texto: String): Leitura<String> {
        val t = texto.trim()
        return if (t.startsWith("{")) Leitura.Ok(t) else CodigoTexto.decodificar(t)
    }

    suspend fun analisar(texto: String, idAluno: Long): PreviaResumo {
        val json = when (val j = jsonDe(texto)) {
            is Leitura.Ok -> j.valor
            is Leitura.Erro -> return PreviaResumo.Erro("Código não reconhecido: ${j.detalhe}.")
        }
        if (ContratoJson.tipoDe(json) == "treino") {
            return PreviaResumo.Erro("Isto é um treino, não um resumo do aluno.")
        }
        val resumo = when (val r = ContratoJson.lerResumo(json)) {
            is Leitura.Ok -> r.valor
            is Leitura.Erro -> return PreviaResumo.Erro(
                when (r.motivo) {
                    MotivoErro.VERSAO_FUTURA -> "Este resumo vem de uma versão mais nova do app. Atualize o aplicativo."
                    MotivoErro.INTEGRIDADE -> "O resumo parece alterado ou corrompido."
                    else -> "Resumo inválido: ${r.detalhe}."
                }
            )
        }
        val aluno = alunos.buscar(idAluno) ?: return PreviaResumo.Erro("Aluno não encontrado.")
        val ids = resumo.sessoes.map { it.execucaoId }
        val jaTem = if (ids.isEmpty()) emptyList() else retornos.jaImportadas(ids)
        val novas = resumo.sessoes.filter { it.execucaoId !in jaTem }
        return PreviaResumo.Ok(
            resumo = resumo,
            confere = aluno.refContrato != null && aluno.refContrato == resumo.alunoRef,
            sessoesNovas = novas.size,
            sessoesJaImportadas = ids.size - novas.size,
            resumoJaImportado = retornos.contarResumo(resumo.resumoId) > 0,
            nomeCargasNovas = novas.sumOf { s -> s.itens.count { it.cargaKg != null && it.feito } },
        )
    }

    /** Grava as sessões novas (cargas no histórico) e o retorno do aluno. Idempotente. */
    suspend fun importar(resumo: Resumo, idAluno: Long, hoje: LocalDate = LocalDate.now()): ResultadoImportacao =
        db.withTransaction {
            val ids = resumo.sessoes.map { it.execucaoId }
            val jaTem = if (ids.isEmpty()) emptySet() else retornos.jaImportadas(ids).toSet()
            val novas = resumo.sessoes.filter { it.execucaoId !in jaTem }.sortedBy { it.data }

            var cargas = 0
            novas.forEach { s ->
                s.itens.forEach { item ->
                    // (propriedade de outro módulo: sem smart cast, por isso a variável local)
                    val kg = item.cargaKg ?: return@forEach
                    if (!item.feito || kg <= 0) return@forEach
                    val chave = CargaRules.chaveExercicio(item.exercicio)
                    if (chave.isEmpty()) return@forEach
                    val ultimo = historico.ultimo(idAluno, chave)
                    if (ultimo == null || ultimo.cargaKg != kg) {
                        historico.inserir(
                            HistoricoCargaEntity(
                                idAluno = idAluno, exercicio = item.exercicio, chave = chave,
                                data = s.data, cargaKg = kg,
                            )
                        )
                        cargas++
                    }
                }
            }
            retornos.registrarExecucoes(novas.map { ExecucaoImportadaEntity(it.execucaoId, idAluno, hoje.toString()) })

            if (retornos.contarResumo(resumo.resumoId) == 0) {
                val esforcos = resumo.sessoes.mapNotNull { it.esforco }
                retornos.inserir(
                    RetornoAlunoEntity(
                        idAluno = idAluno, resumoId = resumo.resumoId, importadoEm = hoje.toString(),
                        de = resumo.de, ate = resumo.ate, sessoes = resumo.sessoes.size,
                        esforcoMedio = esforcos.takeIf { it.isNotEmpty() }?.average(),
                        mensagem = resumo.mensagem,
                    )
                )
            }
            ResultadoImportacao(sessoes = novas.size, cargasRegistradas = cargas)
        }
}
