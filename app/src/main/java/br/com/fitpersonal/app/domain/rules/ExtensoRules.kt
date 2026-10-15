package br.com.fitpersonal.app.domain.rules

/** Valores em reais por extenso (pt-BR), para recibos. Cobre de zero até 999.999.999 reais. */
object ExtensoRules {
    private val UNIDADES = listOf(
        "zero", "um", "dois", "três", "quatro", "cinco", "seis", "sete", "oito", "nove", "dez", "onze", "doze",
        "treze", "quatorze", "quinze", "dezesseis", "dezessete", "dezoito", "dezenove",
    )
    private val DEZENAS = listOf("", "", "vinte", "trinta", "quarenta", "cinquenta", "sessenta", "setenta", "oitenta", "noventa")
    private val CENTENAS = listOf(
        "", "cento", "duzentos", "trezentos", "quatrocentos", "quinhentos", "seiscentos", "setecentos",
        "oitocentos", "novecentos",
    )

    private fun ate999(n: Int): String {
        if (n == 0) return ""
        if (n == 100) return "cem"
        val centena = n / 100
        val resto = n % 100
        val partes = mutableListOf<String>()
        if (centena > 0) partes += CENTENAS[centena]
        if (resto > 0) {
            partes += if (resto < 20) UNIDADES[resto]
            else DEZENAS[resto / 10] + if (resto % 10 > 0) " e " + UNIDADES[resto % 10] else ""
        }
        return partes.joinToString(" e ")
    }

    /** "e" só antes de resto menor que 100 ou centena exata; senão, espaço ("mil duzentos e cinquenta"). */
    private fun separador(proximo: Int) = if (proximo < 100 || proximo % 100 == 0) " e " else " "

    fun inteiro(n: Long): String {
        require(n in 0..999_999_999) { "valor fora do intervalo" }
        if (n == 0L) return "zero"
        val milhoes = (n / 1_000_000).toInt()
        val milhares = ((n / 1_000) % 1_000).toInt()
        val resto = (n % 1_000).toInt()

        val saida = StringBuilder()
        if (milhoes > 0) saida.append(if (milhoes == 1) "um milhão" else ate999(milhoes) + " milhões")
        if (milhares > 0) {
            if (saida.isNotEmpty()) saida.append(separador(milhares))
            saida.append(if (milhares == 1) "mil" else ate999(milhares) + " mil")
        }
        if (resto > 0) {
            if (saida.isNotEmpty()) saida.append(separador(resto))
            saida.append(ate999(resto))
        }
        return saida.toString()
    }

    /** 125090 centavos → "mil duzentos e cinquenta reais e noventa centavos". */
    fun reais(centavos: Long): String {
        require(centavos >= 0) { "valor negativo" }
        val reais = centavos / 100
        val cent = (centavos % 100).toInt()
        val partes = mutableListOf<String>()
        if (reais > 0) {
            val sufixo = when {
                reais == 1L -> " real"
                reais >= 1_000_000 && reais % 1_000_000 == 0L -> " de reais"
                else -> " reais"
            }
            partes += inteiro(reais) + sufixo
        }
        if (cent > 0) partes += inteiro(cent.toLong()) + if (cent == 1) " centavo" else " centavos"
        return if (partes.isEmpty()) "zero real" else partes.joinToString(" e ")
    }
}
