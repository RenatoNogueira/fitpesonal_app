package br.com.fitpersonal.app.domain.rules

/** Regras clínicas portadas de gui/avaliacoes_view.py e reports/avaliacao_pdf.py. Sem dependência de Android. */

enum class ClassePressao(val rotulo: String, val alerta: Boolean) {
    HIPOTENSAO("Hipotensão (Pressão Baixa)", true),
    NORMAL("Normotensão (Normal)", false),
    ELEVADA("Elevada/Limítrofe", true),
    HIPERTENSAO_1("Hipertensão Estágio 1", true),
    HIPERTENSAO_2("Hipertensão Estágio 2", true),
    CRISE("Crise Hipertensiva", true),
}

enum class ClasseImc(val rotulo: String) {
    BAIXO_PESO("Abaixo do peso"), NORMAL("Peso normal"), SOBREPESO("Sobrepeso"),
    OBESIDADE_1("Obesidade grau I"), OBESIDADE_2("Obesidade grau II"), OBESIDADE_3("Obesidade grau III")
}

data class ComposicaoCorporal(val massaGordaKg: Double?, val massaMagraKg: Double?)

object AvaliacaoRules {

    const val ALTURA_MIN = 0.5
    const val ALTURA_MAX = 2.5
    const val PESO_MIN = 10.0
    const val PESO_MAX = 400.0

    fun imc(pesoKg: Double?, alturaM: Double?): Double? {
        if (pesoKg == null || alturaM == null || pesoKg <= 0 || alturaM <= 0) return null
        return pesoKg / (alturaM * alturaM)
    }

    /** Classificação OMS — melhoria sobre o desktop, que só mostrava o número. */
    fun classificarImc(imc: Double): ClasseImc = when {
        imc < 18.5 -> ClasseImc.BAIXO_PESO
        imc < 25.0 -> ClasseImc.NORMAL
        imc < 30.0 -> ClasseImc.SOBREPESO
        imc < 35.0 -> ClasseImc.OBESIDADE_1
        imc < 40.0 -> ClasseImc.OBESIDADE_2
        else -> ClasseImc.OBESIDADE_3
    }

    /**
     * Preenche massa gorda/magra somente quando ausentes (mesmo comportamento do desktop).
     */
    fun composicao(
        pesoKg: Double?, percentualGordura: Double?,
        massaGordaInformada: Double?, massaMagraInformada: Double?,
    ): ComposicaoCorporal {
        if (pesoKg == null || percentualGordura == null) {
            return ComposicaoCorporal(massaGordaInformada, massaMagraInformada)
        }
        val gorda = pesoKg * percentualGordura / 100.0
        return ComposicaoCorporal(
            massaGordaKg = massaGordaInformada ?: gorda,
            massaMagraKg = massaMagraInformada ?: (pesoKg - gorda),
        )
    }

    /** A ordem das condições replica o desktop: hipotensão → crise → estágio 2 → 1 → elevada → normal. */
    fun classificarPressao(sistolica: Int?, diastolica: Int?): ClassePressao? {
        if (sistolica == null && diastolica == null) return null
        val ps = sistolica ?: 0
        val pd = diastolica ?: 0
        return when {
            (ps in 1..89) || (pd in 1..59) -> ClassePressao.HIPOTENSAO
            ps >= 180 || pd >= 120 -> ClassePressao.CRISE
            ps >= 160 || pd >= 100 -> ClassePressao.HIPERTENSAO_2
            ps >= 140 || pd >= 90 -> ClassePressao.HIPERTENSAO_1
            ps > 120 || pd > 80 -> ClassePressao.ELEVADA
            else -> ClassePressao.NORMAL
        }
    }

    /** "12 por 8" usado no PDF. */
    fun pressaoPopular(sistolica: Int?, diastolica: Int?): String? {
        if (sistolica == null || diastolica == null || sistolica < 10 || diastolica < 10) return null
        return "${sistolica / 10} por ${diastolica / 10}"
    }

    fun alertasLaboratoriais(percentualGordura: Double?, ldl: Double?, hdl: Double?): List<String> = buildList {
        if (percentualGordura != null && percentualGordura >= 30) add("Gordura corporal elevada")
        if (ldl != null) {
            if (ldl >= 160) add("LDL alto") else if (ldl >= 130) add("LDL limítrofe")
        }
        if (hdl != null && hdl < 40) add("HDL baixo")
    }

    fun alturaValida(m: Double) = m in ALTURA_MIN..ALTURA_MAX
    fun pesoValido(kg: Double) = kg in PESO_MIN..PESO_MAX
}
