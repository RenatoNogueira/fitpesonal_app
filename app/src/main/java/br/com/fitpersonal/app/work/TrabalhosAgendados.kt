package br.com.fitpersonal.app.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import br.com.fitpersonal.app.data.repo.AgendaRepository
import br.com.fitpersonal.app.data.repo.ConfiguracoesRepository
import br.com.fitpersonal.app.domain.model.Agendamento
import br.com.fitpersonal.app.domain.rules.LembreteRules
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Camada fina sobre o WorkManager: backup noturno periódico e lembretes pontuais por agendamento. */
@Singleton
class TrabalhosAgendados @Inject constructor(@ApplicationContext private val ctx: Context) {

    private val wm get() = WorkManager.getInstance(ctx)

    private companion object {
        const val BACKUP = "backup_diario"
        const val TAG_LEMBRETE = "lembrete"
        const val HORA_BACKUP = 3
        const val REAVALIACAO = "reavaliacao_diaria"
        const val HORA_REAVALIACAO = 9
        const val ALUNO_LEMBRETE = "aluno_lembrete_diario"
    }

    /** Liga/desliga o backup diário (~03:00). KEEP: reabrir o app não "reinicia" o relógio do trabalho. */
    fun sincronizarBackupDiario(ativo: Boolean) {
        if (!ativo) { wm.cancelUniqueWork(BACKUP); return }
        val atraso = LembreteRules.atrasoAteHorario(LocalDateTime.now(), HORA_BACKUP)
        val req = PeriodicWorkRequestBuilder<BackupWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(atraso.toMillis(), TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder().setRequiresBatteryNotLow(true).setRequiresStorageNotLow(true).build()
            )
            .build()
        wm.enqueueUniquePeriodicWork(BACKUP, ExistingPeriodicWorkPolicy.KEEP, req)
    }

    /** Liga/desliga o aviso diário de reavaliações (~09:00). */
    fun sincronizarAvisoReavaliacao(ativo: Boolean) {
        if (!ativo) { wm.cancelUniqueWork(REAVALIACAO); return }
        val atraso = LembreteRules.atrasoAteHorario(LocalDateTime.now(), HORA_REAVALIACAO)
        val req = PeriodicWorkRequestBuilder<ReavaliacaoWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(atraso.toMillis(), TimeUnit.MILLISECONDS)
            .build()
        wm.enqueueUniquePeriodicWork(REAVALIACAO, ExistingPeriodicWorkPolicy.KEEP, req)
    }

    /** Liga/desliga o aviso diário do modo aluno, no horário escolhido (reagenda ao mudar o horário). */
    fun sincronizarLembreteAluno(l: br.com.fitpersonal.app.domain.rules.LembreteAluno) {
        if (!l.ativo) { wm.cancelUniqueWork(ALUNO_LEMBRETE); return }
        val agora = LocalDateTime.now()
        var alvo = agora.toLocalDate().atTime(l.minutos / 60, l.minutos % 60)
        if (!alvo.isAfter(agora)) alvo = alvo.plusDays(1)
        val atraso = java.time.Duration.between(agora, alvo).toMillis()
        val req = PeriodicWorkRequestBuilder<AlunoLembreteWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(atraso, TimeUnit.MILLISECONDS)
            .build()
        wm.enqueueUniquePeriodicWork(ALUNO_LEMBRETE, ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    /** Agenda (ou reagenda) o lembrete; se o momento já passou, apenas cancela o anterior. */
    fun agendarLembrete(idAgendamento: Long, momento: LocalDateTime) {
        val atraso = LembreteRules.atrasoMillis(LocalDateTime.now(), momento)
        if (atraso == null) { cancelarLembrete(idAgendamento); return }
        val req = OneTimeWorkRequestBuilder<LembreteWorker>()
            .setInitialDelay(atraso, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(LembreteWorker.CHAVE_ID to idAgendamento))
            .addTag(TAG_LEMBRETE)
            .build()
        wm.enqueueUniqueWork("lembrete_$idAgendamento", ExistingWorkPolicy.REPLACE, req)
    }

    fun cancelarLembrete(idAgendamento: Long) { wm.cancelUniqueWork("lembrete_$idAgendamento") }
    fun cancelarTodosLembretes() { wm.cancelAllWorkByTag(TAG_LEMBRETE) }
}

/** Liga regras de negócio (configuração + agenda) ao agendamento dos lembretes. */
@Singleton
class LembreteAgendador @Inject constructor(
    private val trabalhos: TrabalhosAgendados,
    private val agenda: AgendaRepository,
    private val config: ConfiguracoesRepository,
) {
    suspend fun agendar(a: Agendamento) {
        val cfg = config.config.first()
        if (!cfg.lembretesAtivo || !LembreteRules.deveNotificar(a.status)) {
            trabalhos.cancelarLembrete(a.id)
            return
        }
        trabalhos.agendarLembrete(a.id, LembreteRules.momentoLembrete(a.data, a.horario, cfg.antecedenciaMin))
    }

    fun cancelar(idAgendamento: Long) = trabalhos.cancelarLembrete(idAgendamento)

    /** Reconstrói os lembretes de todos os agendamentos pendentes de hoje em diante. */
    suspend fun reagendarTodos() {
        val cfg = config.config.first()
        if (!cfg.lembretesAtivo) { trabalhos.cancelarTodosLembretes(); return }
        agenda.pendentesAPartir(LocalDate.now()).forEach { agendar(it) }
    }
}
