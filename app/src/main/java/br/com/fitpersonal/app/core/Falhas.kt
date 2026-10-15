package br.com.fitpersonal.app.core

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.app.Application
import android.content.Context
import android.os.Build
import br.com.fitpersonal.app.BuildConfig
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Registro local de falhas: o app grava o que derrubou (ou quase derrubou) a execução em
 * `filesDir/falhas.log`, para você ver em Perfil → Dados → Relatório de falhas e enviar para análise.
 * Nada sai do aparelho sem você compartilhar.
 */
object Falhas {
    private const val ARQUIVO = "falhas.log"
    private const val MAX_BYTES = 96 * 1024
    private const val MAX_STACK = 7_000

    @Volatile private var pasta: File? = null

    /** Chamar uma vez, em Application.onCreate: passa a gravar também as falhas fatais não tratadas. */
    fun instalar(app: Application) {
        pasta = app.filesDir
        val anterior = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            registrar(e, fatal = true, thread = t.name)
            anterior?.uncaughtException(t, e)
        }
    }

    /** Anota uma falha. [fatal] = derrubou o app; false = foi contida (a tela segue funcionando). */
    fun registrar(e: Throwable, fatal: Boolean = false, thread: String? = null, contexto: String? = null) {
        runCatching {
            val dir = pasta ?: return
            val quando = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            val pilha = e.stackTraceToString().take(MAX_STACK)
            val bloco = buildString {
                append("=== ").append(quando).append(if (fatal) "  FATAL" else "  contida").append('\n')
                append("app ").append(BuildConfig.VERSION_NAME)
                append(" • Android ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(')')
                append(" • ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append('\n')
                thread?.let { append("thread: ").append(it).append('\n') }
                contexto?.let { append("contexto: ").append(it).append('\n') }
                append(pilha).append("\n\n")
            }
            synchronized(this) {
                val arq = File(dir, ARQUIVO)
                val atual = if (arq.exists()) arq.readText() else ""
                val novo = (atual + bloco).let { if (it.length > MAX_BYTES) it.takeLast(MAX_BYTES) else it }
                arq.writeText(novo)
            }
        }
    }

    fun lerLog(ctx: Context): String =
        runCatching { File(ctx.filesDir, ARQUIVO).takeIf { it.exists() }?.readText().orEmpty() }.getOrDefault("")

    fun limpar(ctx: Context) {
        runCatching { File(ctx.filesDir, ARQUIVO).delete() }
    }

    /** Por que o sistema encerrou o processo nas últimas vezes (Android 11+): falha, memória, ANR… */
    fun saidasDoProcesso(ctx: Context): List<String> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return emptyList()
        return runCatching {
            val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            am.getHistoricalProcessExitReasons(ctx.packageName, 0, 10).map { i ->
                fmt.format(Date(i.timestamp)) + "  " + motivo(i.reason) +
                    (i.description?.takeIf { it.isNotBlank() }?.let { " — $it" } ?: "")
            }
        }.getOrDefault(emptyList())
    }

    private fun motivo(r: Int): String = when (r) {
        ApplicationExitInfo.REASON_CRASH -> "FALHA (exceção do app)"
        ApplicationExitInfo.REASON_CRASH_NATIVE -> "FALHA NATIVA"
        ApplicationExitInfo.REASON_ANR -> "ANR (app não respondeu)"
        ApplicationExitInfo.REASON_LOW_MEMORY -> "pouca memória no aparelho"
        ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "uso excessivo de recursos"
        ApplicationExitInfo.REASON_USER_REQUESTED -> "encerrado pelo usuário"
        ApplicationExitInfo.REASON_USER_STOPPED -> "forçado a parar"
        ApplicationExitInfo.REASON_PERMISSION_CHANGE -> "permissão alterada"
        ApplicationExitInfo.REASON_DEPENDENCY_DIED -> "dependência encerrou"
        ApplicationExitInfo.REASON_SIGNALED -> "sinal do sistema"
        ApplicationExitInfo.REASON_EXIT_SELF -> "encerrou sozinho"
        ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "falha ao iniciar"
        else -> "motivo $r"
    }

    /** Texto único (cabeçalho + saídas do sistema + log) para ver na tela ou compartilhar. */
    fun relatorio(ctx: Context): String = buildString {
        append("FitPersonal ").append(BuildConfig.VERSION_NAME).append('\n')
        append("Android ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(") • ")
        append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append("\n\n")
        val saidas = saidasDoProcesso(ctx)
        append("Últimos encerramentos do app:\n")
        if (saidas.isEmpty()) append("(sem registro do sistema)\n") else saidas.forEach { append("• ").append(it).append('\n') }
        append("\nFalhas registradas pelo app:\n")
        val log = lerLog(ctx)
        append(if (log.isBlank()) "(nenhuma)\n" else log)
    }
}

/** Evita que uma exceção solta numa corrotina de tela derrube o app: registra e segue. */
val TratadorDeErros = CoroutineExceptionHandler { _, e -> Falhas.registrar(e, fatal = false, contexto = "corrotina") }

/** Como `stateIn`, mas uma exceção no fluxo é registrada e o último valor é mantido (em vez de derrubar o app). */
fun <T> Flow<T>.estadoSeguro(escopo: CoroutineScope, inicial: T): StateFlow<T> =
    catch { Falhas.registrar(it, fatal = false, contexto = "fluxo") }
        .stateIn(escopo, SharingStarted.WhileSubscribed(5_000), inicial)
