package br.com.fitpersonal.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import br.com.fitpersonal.app.data.backup.BackupAutomatico
import br.com.fitpersonal.app.data.repo.AgendaRepository
import br.com.fitpersonal.app.data.repo.AlunoRepository
import br.com.fitpersonal.app.data.repo.ConfiguracoesRepository
import br.com.fitpersonal.app.data.local.AgendamentoDao
import br.com.fitpersonal.app.data.local.AlunoDao
import br.com.fitpersonal.app.data.local.toDomain
import br.com.fitpersonal.app.domain.rules.PacoteRules
import br.com.fitpersonal.app.domain.rules.SituacaoPacote
import br.com.fitpersonal.app.data.local.AvaliacaoDao
import br.com.fitpersonal.app.data.local.PagamentoDao
import br.com.fitpersonal.app.domain.rules.FinanceiroRules
import br.com.fitpersonal.app.domain.rules.ResumoDiarioRules
import br.com.fitpersonal.app.domain.rules.SituacaoPagamento
import br.com.fitpersonal.app.domain.rules.LembreteRules
import br.com.fitpersonal.app.domain.rules.ReavaliacaoRules
import br.com.fitpersonal.app.domain.rules.SituacaoReavaliacao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate

@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val automatico: BackupAutomatico,
    private val manager: br.com.fitpersonal.app.data.backup.BackupManager,
    private val config: ConfiguracoesRepository,
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        // O usuário pode ter desligado o backup depois que o trabalho foi enfileirado.
        if (!config.config.first().backupAtivo) return Result.success()
        // Sem senha não há backup (não é erro transitório: repetir não adianta). O status aparece em Perfil.
        if (!manager.temSenha()) {
            automatico.executar()   // registra a mensagem "Defina uma senha..." como último resultado
            return Result.success()
        }
        val r = automatico.executar()
        return when {
            r.sucesso -> Result.success()
            runAttemptCount < 3 -> Result.retry()
            else -> Result.failure()
        }
    }
}

/** Aviso diário: quantos alunos estão com a reavaliação atrasada/vencendo (só notifica se houver). */
@HiltWorker
class ReavaliacaoWorker @AssistedInject constructor(
    @Assisted private val ctx: Context,
    @Assisted params: WorkerParameters,
    private val alunos: AlunoDao,
    private val avaliacoes: AvaliacaoDao,
    private val agenda: AgendamentoDao,
    private val pagamentos: PagamentoDao,
    private val config: ConfiguracoesRepository,
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        if (!config.config.first().reavaliacaoAvisos) return Result.success()
        val hoje = LocalDate.now()

        // Agenda do dia (sessões não canceladas)
        val doDia = agenda.doDia(hoje.toString()).first().filter { it.status != "CANCELADO" }
        ResumoDiarioRules.agendaDoDia(doDia.size, doDia.firstOrNull()?.horario)
            ?.let { Notificador.agendaDoDia(ctx, it) }

        // Mensalidades em atraso (mesma regra do cartão da Home)
        val todos = alunos.observar("").first().map { it.toDomain() }
        val pagasPorAluno = pagamentos.todosSnapshot().groupBy { it.idAluno }
        var devedores = 0
        var emAberto = 0L
        var aVencer = 0
        var menorPrazo = Int.MAX_VALUE
        todos.forEach { a ->
            val valor = a.mensalidadeCentavos ?: return@forEach
            val pagas = pagasPorAluno[a.id].orEmpty().map { it.toDomain().competencia }.toSet()
            val r = FinanceiroRules.resumir(valor, a.diaVencimento, a.dataCadastro, pagas, hoje)
            if (r.situacao == SituacaoPagamento.ATRASADO) {
                devedores++
                emAberto += valor * r.atrasadas.size
            }
            // vencendo nos próximos 3 dias: dá tempo de mandar o Pix antes do atraso
            r.proximoVencimento?.let { venc ->
                val dias = java.time.temporal.ChronoUnit.DAYS.between(hoje, venc).toInt()
                if (r.situacao == SituacaoPagamento.A_VENCER && dias in 0..3) {
                    aVencer++
                    if (dias < menorPrazo) menorPrazo = dias
                }
            }
        }
        ResumoDiarioRules.mensalidades(devedores, emAberto)?.let { Notificador.mensalidades(ctx, it) }
        ResumoDiarioRules.vencendo(aVencer, if (aVencer == 0) -1 else menorPrazo)?.let { Notificador.vencimentos(ctx, it) }

        // Aniversariantes de hoje
        val aniversariantes = todos.filter { a ->
            a.dataNascimento?.let { it.monthValue == hoje.monthValue && it.dayOfMonth == hoje.dayOfMonth } == true
        }.map { it.nome }
        ResumoDiarioRules.aniversarios(aniversariantes)?.let { Notificador.aniversarios(ctx, it) }

        // Pacotes de sessões na última sessão ou esgotados
        val concluidos = agenda.concluidos().first().map { it.toDomain() }.groupBy { it.idAluno }
        val pacotes = alunos.observar("").first().map { a ->
            val d = a.toDomain()
            PacoteRules.resumir(d.pacoteSessoes, d.pacoteInicio, concluidos[a.id].orEmpty())
        }
        val esgotados = pacotes.count { it.situacao == SituacaoPacote.ESGOTADO }
        val naUltima = pacotes.count { it.situacao == SituacaoPacote.ULTIMA_SESSAO }
        PacoteRules.textoAviso(esgotados, naUltima)?.let { Notificador.pacotes(ctx, it) }

        val ultimas = avaliacoes.ultimasPorAluno().first().associate { it.idAluno to LocalDate.parse(it.ultima) }
        val situacoes = alunos.observar("").first().mapNotNull { a ->
            ReavaliacaoRules.situacao(ultimas[a.id], a.reavaliacaoDias, hoje)
        }
        val atrasadas = situacoes.count {
            it is SituacaoReavaliacao.Atrasada || it is SituacaoReavaliacao.SemAvaliacao
        }
        val vencendo = situacoes.count { it is SituacaoReavaliacao.Proxima }

        ReavaliacaoRules.textoAviso(atrasadas, vencendo)?.let { Notificador.reavaliacoes(ctx, it) }
        return Result.success()
    }
}

@HiltWorker
class LembreteWorker @AssistedInject constructor(
    @Assisted private val ctx: Context,
    @Assisted params: WorkerParameters,
    private val agenda: AgendaRepository,
    private val alunos: AlunoRepository,
    private val config: ConfiguracoesRepository,
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val id = inputData.getLong(CHAVE_ID, 0L)
        if (id == 0L || !config.config.first().lembretesAtivo) return Result.success()

        // Relê do banco: o agendamento pode ter sido editado, concluído, cancelado ou excluído.
        val ag = agenda.buscar(id) ?: return Result.success()
        if (!LembreteRules.deveNotificar(ag.status)) return Result.success()
        val aluno = alunos.buscar(ag.idAluno) ?: return Result.success()

        Notificador.lembrete(
            ctx, id,
            titulo = LembreteRules.titulo(ag.tipo),
            texto = LembreteRules.texto(aluno.nome, ag.data, ag.horario, LocalDate.now()),
        )
        return Result.success()
    }

    companion object { const val CHAVE_ID = "id" }
}

/**
 * Aviso diário do modo aluno: "hoje é dia de treinar" nos dias marcados e, fora deles, um toque de
 * retenção quando o aluno passa de 3 a 14 dias sem treinar. Só roda no modo aluno.
 */
@HiltWorker
class AlunoLembreteWorker @AssistedInject constructor(
    @Assisted private val ctx: Context,
    @Assisted params: WorkerParameters,
    private val config: ConfiguracoesRepository,
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val cfg = config.config.first()
        if (cfg.usoApp != br.com.fitpersonal.app.domain.rules.UsoApp.ALUNO || !cfg.lembreteAluno.ativo) {
            return Result.success()
        }
        val repo = br.com.fitpersonal.app.aluno.Repositorio(ctx)
        val treino = repo.treinos().firstOrNull()
        val sessoesDoTreino = treino?.let { repo.sessoesDe(it.treinoId) }.orEmpty()
        val ultimaData = repo.sessoes().maxOfOrNull { it.data }
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val divisoes = treino?.treino?.divisoes?.filterValues { it.isNotEmpty() }?.keys?.toList().orEmpty()
        val proxima = br.com.fitpersonal.app.domain.rules.LembreteAlunoRules
            .proximaDivisao(divisoes, sessoesDoTreino.firstOrNull()?.divisao)

        br.com.fitpersonal.app.domain.rules.LembreteAlunoRules
            .avisoDeHoje(LocalDate.now(), cfg.lembreteAluno, ultimaData, treino?.treino?.nome, proxima)
            ?.let { Notificador.alunoAviso(ctx, it.titulo, it.texto) }
        return Result.success()
    }
}
