package br.com.fitpersonal.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import br.com.fitpersonal.app.core.Falhas
import br.com.fitpersonal.app.data.local.CatalogoSeeder
import br.com.fitpersonal.app.data.repo.ConfiguracoesRepository
import br.com.fitpersonal.app.work.LembreteAgendador
import br.com.fitpersonal.app.work.Notificador
import br.com.fitpersonal.app.work.TrabalhosAgendados
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class FitApp : Application(), Configuration.Provider {

    @Inject lateinit var seeder: CatalogoSeeder
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var trabalhos: TrabalhosAgendados
    @Inject lateinit var lembretes: LembreteAgendador
    @Inject lateinit var config: ConfiguracoesRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** WorkManager inicializado sob demanda com a fábrica do Hilt (o initializer padrão foi removido no manifest). */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        Falhas.instalar(this)
        br.com.fitpersonal.app.core.Analitica.iniciar(this)
        runCatching { Notificador.criarCanais(this) }.onFailure { Falhas.registrar(it, contexto = "canais") }
        // A coleta de estatísticas segue exatamente a escolha do usuário (desligada por padrão).
        scope.launch {
            try {
                config.config.map { it.analiticaAtiva }.distinctUntilChanged().collect { br.com.fitpersonal.app.core.Analitica.definirAtiva(it) }
            } catch (e: Exception) { Falhas.registrar(e, contexto = "analitica") }
        }
        scope.launch {
            // Nada aqui pode derrubar o app: cada etapa é isolada e a falha é registrada.
            try { seeder.seedSeNecessario() } catch (e: Exception) { Falhas.registrar(e, contexto = "catalogo") }
            try {
                // Reconcilia o agendamento com as preferências (cobre primeira execução, restauração e importação).
                val cfg = config.config.first()
                trabalhos.sincronizarBackupDiario(cfg.backupAtivo)
                trabalhos.sincronizarAvisoReavaliacao(cfg.reavaliacaoAvisos)
            } catch (e: Exception) { Falhas.registrar(e, contexto = "agendamentos") }
            try { lembretes.reagendarTodos() } catch (e: Exception) { Falhas.registrar(e, contexto = "lembretes") }
        }
    }
}
