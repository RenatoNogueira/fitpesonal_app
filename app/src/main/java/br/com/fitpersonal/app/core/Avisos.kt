package br.com.fitpersonal.app.core

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

enum class TipoAviso { SUCESSO, INFO, ERRO }

data class Aviso(val texto: String, val tipo: TipoAviso)

/**
 * Feedback rápido para o usuário ("Aluno salvo", "Treino excluído"…). Qualquer ViewModel ou regra pode
 * avisar sem conhecer a tela: o aviso é mostrado pelo host global (HostAvisos), que sobrevive à navegação
 * — então aparece mesmo quando a tela de edição já fechou logo após salvar.
 */
object Avisos {
    private val _fluxo = MutableSharedFlow<Aviso>(extraBufferCapacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val fluxo: SharedFlow<Aviso> = _fluxo.asSharedFlow()

    fun sucesso(texto: String) { _fluxo.tryEmit(Aviso(texto, TipoAviso.SUCESSO)) }
    fun info(texto: String) { _fluxo.tryEmit(Aviso(texto, TipoAviso.INFO)) }
    fun erro(texto: String) { _fluxo.tryEmit(Aviso(texto, TipoAviso.ERRO)) }

    /** "criado" ao salvar algo novo, "atualizado" ao editar: "Aluno criado" / "Aluno atualizado". */
    fun salvou(coisa: String, novo: Boolean) = sucesso("$coisa " + if (novo) "criado" else "atualizado")
}
