package br.com.fitpersonal.app.domain.rules

/** Preferência de tema escolhida pelo usuário. */
enum class TemaApp(val rotulo: String) {
    SISTEMA("Automático"), CLARO("Claro"), ESCURO("Escuro");

    companion object {
        /** Valor salvo desconhecido ou ausente volta ao automático. */
        fun deNome(nome: String?): TemaApp = entries.firstOrNull { it.name == nome } ?: SISTEMA
    }
}

object BloqueioRules {
    /** Tempo em segundo plano tolerado antes de pedir o desbloqueio de novo. */
    const val TOLERANCIA_MS = 30_000L

    /**
     * Deve travar de novo? [saiuEm] = instante em que o app foi para segundo plano (null = nunca saiu).
     * Relógio que anda para trás (diferença negativa) é tratado como "travar", por segurança.
     */
    fun deveTravar(saiuEm: Long?, agora: Long, toleranciaMs: Long = TOLERANCIA_MS): Boolean {
        if (saiuEm == null) return false
        val ausente = agora - saiuEm
        return ausente < 0 || ausente > toleranciaMs
    }
}
