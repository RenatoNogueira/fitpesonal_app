package br.com.fitpersonal.app.domain.rules

import br.com.fitpersonal.app.domain.model.Dobra
import br.com.fitpersonal.app.domain.model.ProtocoloDobras
import br.com.fitpersonal.app.domain.model.SexoBiologico
import java.time.LocalDate
import java.time.Period

data class ResultadoPollock(
    val protocolo: ProtocoloDobras,
    val sexo: SexoBiologico,
    val idade: Int,
    val somaDobrasMm: Double,
    val densidadeCorporal: Double,
    /** Percentual de gordura pela equação de Siri. */
    val percentualGordura: Double,
)

sealed interface AnalisePollock {
    /** Ainda não é possível calcular; [motivo] é mostrado ao usuário. */
    data class Pendente(val motivo: String) : AnalisePollock
    data class Calculada(val resultado: ResultadoPollock, val avisos: List<String>) : AnalisePollock
}

/**
 * Protocolos de Jackson & Pollock (densidade corporal) + equação de Siri (% gordura):
 *   %G = 495 / DC − 450
 *
 * 3 dobras — homens: peitoral, abdominal, coxa · mulheres: tríceps, supra-ilíaca, coxa
 * 7 dobras — peitoral, axilar média, tríceps, subescapular, abdominal, supra-ilíaca, coxa
 *
 * As equações foram validadas para adultos de ~18 a 61 anos; fora disso o resultado sai com aviso.
 */
object Pollock {

    const val DOBRA_MIN_MM = 1.0
    const val DOBRA_MAX_MM = 80.0
    private const val IDADE_MIN = 18
    private const val IDADE_MAX = 61

    fun dobrasNecessarias(protocolo: ProtocoloDobras, sexo: SexoBiologico): List<Dobra> = when (protocolo) {
        ProtocoloDobras.POLLOCK_7 -> listOf(
            Dobra.PEITORAL, Dobra.AXILAR_MEDIA, Dobra.TRICEPS, Dobra.SUBESCAPULAR,
            Dobra.ABDOMINAL, Dobra.SUPRAILIACA, Dobra.COXA,
        )
        ProtocoloDobras.POLLOCK_3 ->
            if (sexo == SexoBiologico.MASCULINO) listOf(Dobra.PEITORAL, Dobra.ABDOMINAL, Dobra.COXA)
            else listOf(Dobra.TRICEPS, Dobra.SUPRAILIACA, Dobra.COXA)
    }

    fun densidade(protocolo: ProtocoloDobras, sexo: SexoBiologico, somaMm: Double, idade: Int): Double {
        val s = somaMm
        val i = idade.toDouble()
        return when (protocolo) {
            ProtocoloDobras.POLLOCK_3 ->
                if (sexo == SexoBiologico.MASCULINO) 1.10938 - 0.0008267 * s + 0.0000016 * s * s - 0.0002574 * i
                else 1.0994921 - 0.0009929 * s + 0.0000023 * s * s - 0.0001392 * i
            ProtocoloDobras.POLLOCK_7 ->
                if (sexo == SexoBiologico.MASCULINO) 1.112 - 0.00043499 * s + 0.00000055 * s * s - 0.00028826 * i
                else 1.097 - 0.00046971 * s + 0.00000056 * s * s - 0.00012828 * i
        }
    }

    fun siri(densidade: Double): Double = 495.0 / densidade - 450.0

    fun idadeEm(nascimento: LocalDate, data: LocalDate): Int = Period.between(nascimento, data).years

    fun analisar(
        protocolo: ProtocoloDobras,
        dobras: Map<Dobra, Double>,
        sexo: SexoBiologico?,
        nascimento: LocalDate?,
        dataAvaliacao: LocalDate,
    ): AnalisePollock {
        if (sexo == null) return AnalisePollock.Pendente("Informe o sexo para escolher a equação.")
        if (nascimento == null) return AnalisePollock.Pendente("Cadastre a data de nascimento do aluno para calcular a idade.")
        val idade = idadeEm(nascimento, dataAvaliacao)
        if (idade <= 0) return AnalisePollock.Pendente("Data de nascimento inconsistente com a data da avaliação.")

        val necessarias = dobrasNecessarias(protocolo, sexo)
        val faltando = necessarias.filter { dobras[it] == null }
        if (faltando.isNotEmpty()) {
            return AnalisePollock.Pendente("Faltam: ${faltando.joinToString(", ") { it.rotulo }}.")
        }

        val soma = necessarias.sumOf { dobras.getValue(it) }
        val dc = densidade(protocolo, sexo, soma, idade)
        val pct = siri(dc)

        val avisos = buildList {
            if (idade < IDADE_MIN || idade > IDADE_MAX) {
                add("Idade ($idade anos) fora da faixa de validação das equações (18–61 anos).")
            }
            if (pct < 2.0 || pct > 60.0) add("Resultado improvável (%.1f%%). Confira as medidas.".format(pct))
        }
        return AnalisePollock.Calculada(
            ResultadoPollock(protocolo, sexo, idade, soma, dc, pct),
            avisos,
        )
    }
}
