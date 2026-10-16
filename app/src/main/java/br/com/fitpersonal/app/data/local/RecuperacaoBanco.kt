package br.com.fitpersonal.app.data.local

import java.io.File

/** O que fazer com o banco na abertura do app. */
enum class AcaoBanco {
    /** Tudo certo (ou primeira execução): abrir normalmente. */
    NADA,

    /** Banco antigo sem criptografia: cifrar no lugar com a chave atual. */
    CRIPTOGRAFAR,

    /** Não há banco, mas o segredo local está ilegível: descartar segredos e gerar chave nova. */
    REDEFINIR_CHAVE,

    /** Banco antigo em texto puro e segredo ilegível: gerar chave nova e cifrar (nada se perde). */
    REDEFINIR_E_CRIPTOGRAFAR,

    /** Banco cifrado que não abre com a chave deste aparelho: guardar de lado e recomeçar. */
    MOVER_E_RECOMECAR,

    /** Chave do Keystore perdida e banco cifrado ilegível: nova chave, guardar o banco de lado e recomeçar. */
    REDEFINIR_E_MOVER_E_RECOMECAR,
}

data class EstadoBanco(
    val existe: Boolean,
    /** A senha do banco pôde ser obtida (lida do cofre ou recém-gerada). */
    val chaveDisponivel: Boolean,
    val textoPuro: Boolean,
    /** Só faz sentido para banco cifrado com chave disponível: a abertura de teste funcionou. */
    val abreComChave: Boolean,
)

/** Decisão pura, testada na JVM; a execução (arquivos/Keystore) fica em [BancoCriptografado.preparar]. */
object PoliticaRecuperacao {
    fun decidir(e: EstadoBanco): AcaoBanco = when {
        !e.existe -> if (e.chaveDisponivel) AcaoBanco.NADA else AcaoBanco.REDEFINIR_CHAVE
        !e.chaveDisponivel -> if (e.textoPuro) AcaoBanco.REDEFINIR_E_CRIPTOGRAFAR else AcaoBanco.REDEFINIR_E_MOVER_E_RECOMECAR
        e.textoPuro -> AcaoBanco.CRIPTOGRAFAR
        e.abreComChave -> AcaoBanco.NADA
        else -> AcaoBanco.MOVER_E_RECOMECAR
    }
}

/** Aviso persistente de que o banco foi recolocado: a UI mostra a tela de recuperação até o usuário resolver. */
data class InfoRecuperacao(val emMillis: Long, val motivo: String)

object RegistroRecuperacao {
    private const val ARQUIVO = "recuperacao_pendente.txt"

    fun gravar(dir: File, info: InfoRecuperacao) {
        dir.mkdirs()
        File(dir, ARQUIVO).writeText("${info.emMillis}\n${info.motivo}")
    }

    fun ler(dir: File): InfoRecuperacao? {
        val f = File(dir, ARQUIVO)
        if (!f.isFile) return null
        return runCatching {
            val linhas = f.readText().lines()
            InfoRecuperacao(linhas[0].trim().toLong(), linhas.drop(1).joinToString("\n"))
        }.getOrElse { InfoRecuperacao(0L, "Dados anteriores indisponíveis.") }
    }

    fun limpar(dir: File) { File(dir, ARQUIVO).delete() }
}
