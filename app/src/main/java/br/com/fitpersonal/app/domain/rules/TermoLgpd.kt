package br.com.fitpersonal.app.domain.rules

data class SecaoTermo(val titulo: String, val texto: String)

/**
 * Termo de uso e privacidade exibido ao profissional no primeiro uso (e a cada nova [VERSAO]).
 *
 * ATENÇÃO: é um texto-modelo escrito para o funcionamento atual do app. Antes de distribuir,
 * deve ser revisado por assessoria jurídica (LGPD — Lei 13.709/2018).
 */
object TermoLgpd {
    /** Incrementar quando o texto mudar de forma relevante: força novo aceite. */
    const val VERSAO = 2   // v2: consulta de clima (OpenWeather) passou a usar a rede

    const val TITULO = "Privacidade e proteção de dados"

    val SECOES: List<SecaoTermo> = listOf(
        SecaoTermo(
            "Seu papel e o do aplicativo",
            "Você, profissional, é o controlador dos dados dos seus alunos (LGPD, art. 5º, VI). " +
                "O FitPersonal é apenas a ferramenta que organiza esses dados no seu aparelho. " +
                "O aplicativo não envia dados de alunos para servidores do desenvolvedor. A única comunicação de rede é " +
                "a consulta de clima ao serviço OpenWeather, que recebe apenas o nome da cidade informada no seu perfil " +
                "(e a chave de acesso do aplicativo) — nenhum dado de aluno.",
        ),
        SecaoTermo(
            "Quais dados são tratados",
            "Identificação e contato (nome, nascimento, CPF, CEP, telefone, e-mail, Instagram) e dados " +
                "pessoais sensíveis de saúde (medidas corporais, pressão arterial, perfil lipídico, histórico de " +
                "saúde, anamnese, avaliação postural e fotos). Dados de saúde exigem cuidado reforçado (art. 11).",
        ),
        SecaoTermo(
            "Finalidade e base legal",
            "Os dados servem para avaliação física, prescrição de treinos e agenda. Para dados de saúde, a base " +
                "usual é o consentimento específico e destacado do aluno (art. 11, I). O aplicativo permite " +
                "registrar, no cadastro de cada aluno, que esse consentimento foi obtido e a data. Obter e guardar o " +
                "consentimento (idealmente por escrito) é responsabilidade sua.",
        ),
        SecaoTermo(
            "Como os dados são protegidos",
            "O banco de dados é criptografado (SQLCipher, AES-256) com uma chave guardada no cofre do Android " +
                "(Keystore). Backups são sempre criptografados com uma senha definida por você. " +
                "As fotos ficam no armazenamento privado do aplicativo, protegido pelo sistema, mas não são " +
                "criptografadas pelo aplicativo: mantenha o bloqueio de tela ativado.",
        ),
        SecaoTermo(
            "Compartilhamento",
            "PDFs, mensagens de WhatsApp e arquivos de backup só saem do aparelho por uma ação sua. Ao enviar " +
                "uma avaliação ou ficha, você decide o destinatário — confirme que ele pode recebê-la. " +
                "Guarde os arquivos de backup em local seguro e não os compartilhe.",
        ),
        SecaoTermo(
            "Direitos do titular (aluno)",
            "O aluno pode pedir confirmação do tratamento, acesso, correção e eliminação dos seus dados " +
                "(art. 18). Você pode corrigir pelo cadastro do aluno e excluí-lo, o que remove suas avaliações, " +
                "treinos e agendamentos do aplicativo. Cópias já exportadas (PDFs, backups) devem ser tratadas por você.",
        ),
        SecaoTermo(
            "Senha do backup",
            "A senha do backup não pode ser recuperada. Se você a perder, os backups não poderão ser abertos. " +
                "Anote-a em local seguro.",
        ),
        SecaoTermo(
            "Incidentes",
            "Em caso de perda ou roubo do aparelho, ou de acesso indevido aos dados, avalie a comunicação aos " +
                "alunos afetados e à ANPD, conforme o art. 48 da LGPD.",
        ),
    )
}

/** Validação da senha de backup (a única que o usuário digita; não há recuperação). */
object SenhaBackupRules {
    const val MIN = 8

    /** @return mensagem de erro ou null se válida. */
    fun validar(senha: String, confirmacao: String): String? = when {
        senha.length < MIN -> "Use pelo menos $MIN caracteres."
        senha.all { it == senha[0] } -> "A senha não pode repetir o mesmo caractere."
        senha.none { it.isLetter() } || senha.none { it.isDigit() } -> "Misture letras e números."
        senha != confirmacao -> "A confirmação não confere."
        else -> null
    }
}
