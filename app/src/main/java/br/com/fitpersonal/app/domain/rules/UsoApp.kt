package br.com.fitpersonal.app.domain.rules

/** Como a pessoa usa o app: define qual metade do aplicativo é mostrada. */
enum class UsoApp(val rotulo: String) {
    PROFESSOR("Professor"),
    ALUNO("Aluno");

    companion object {
        /** Nome gravado nas preferências; desconhecido ou vazio = ainda não escolheu. */
        fun deNome(nome: String?): UsoApp? = entries.firstOrNull { it.name == nome }
    }
}
