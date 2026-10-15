package br.com.fitpersonal.app.domain.rules

object Validators {

    private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun somenteDigitos(s: String?): String = s.orEmpty().filter { it.isDigit() }

    fun emailValido(email: String) = EMAIL.matches(email.trim())

    /** DDD + número (10 ou 11 dígitos), DDD não começa com 0. */
    fun telefoneValido(telefone: String): Boolean {
        val d = somenteDigitos(telefone)
        return d.length in 10..11 && d.first() != '0'
    }

    fun cepValido(cep: String) = somenteDigitos(cep).length == 8

    fun cpfValido(cpf: String): Boolean {
        val d = somenteDigitos(cpf)
        if (d.length != 11 || d.all { it == d[0] }) return false
        fun dv(base: String, pesoInicial: Int): Int {
            val soma = base.mapIndexed { i, c -> (c - '0') * (pesoInicial - i) }.sum()
            val r = (soma * 10) % 11
            return if (r == 10) 0 else r
        }
        return dv(d.substring(0, 9), 10) == d[9] - '0' && dv(d.substring(0, 10), 11) == d[10] - '0'
    }

    fun formatarCpf(cpf: String): String {
        val d = somenteDigitos(cpf)
        return if (d.length != 11) d else "${d.substring(0, 3)}.${d.substring(3, 6)}.${d.substring(6, 9)}-${d.substring(9)}"
    }
}
