package br.com.fitpersonal.app.ui.protecao

import android.os.SystemClock
import br.com.fitpersonal.app.domain.rules.BloqueioRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Estado de sessão do bloqueio do app: só vive em memória (processo morto = trava de novo).
 * A MainActivity avisa quando o app sai e volta ao primeiro plano.
 */
@Singleton
class BloqueioApp @Inject constructor() {
    private val _desbloqueado = MutableStateFlow(false)
    val desbloqueado: StateFlow<Boolean> = _desbloqueado

    private var saiuEm: Long? = null

    fun desbloquear() {
        _desbloqueado.value = true
        saiuEm = null
    }

    fun aoIrParaSegundoPlano() {
        saiuEm = SystemClock.elapsedRealtime()
    }

    fun aoVoltarAoPrimeiroPlano() {
        if (BloqueioRules.deveTravar(saiuEm, SystemClock.elapsedRealtime())) _desbloqueado.value = false
        saiuEm = null
    }
}
