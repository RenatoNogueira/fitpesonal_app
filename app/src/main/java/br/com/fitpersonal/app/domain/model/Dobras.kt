package br.com.fitpersonal.app.domain.model

/** Pontos de dobra cutânea (mm) usados pelos protocolos de Jackson & Pollock. */
enum class Dobra(val rotulo: String) {
    PEITORAL("Peitoral"),
    AXILAR_MEDIA("Axilar média"),
    TRICEPS("Tríceps"),
    SUBESCAPULAR("Subescapular"),
    ABDOMINAL("Abdominal"),
    SUPRAILIACA("Supra-ilíaca"),
    COXA("Coxa"),
}

enum class ProtocoloDobras(val rotulo: String) {
    POLLOCK_3("Pollock 3 dobras"),
    POLLOCK_7("Pollock 7 dobras"),
}

/** Sexo usado na fórmula (os protocolos de Pollock são definidos para dois grupos). */
enum class SexoBiologico(val rotulo: String) {
    MASCULINO("Masculino"), FEMININO("Feminino");

    companion object {
        fun deTexto(s: String?): SexoBiologico? = when (s?.trim()?.lowercase()) {
            "masculino", "m" -> MASCULINO
            "feminino", "f" -> FEMININO
            else -> null
        }
    }
}
