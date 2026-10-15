package br.com.fitpersonal.app.data.repo

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import br.com.fitpersonal.app.domain.rules.LembreteAluno
import br.com.fitpersonal.app.domain.rules.TemaApp
import br.com.fitpersonal.app.domain.rules.UsoApp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.configStore by preferencesDataStore(name = "configuracoes")

data class Configuracoes(
    val backupAtivo: Boolean = true,
    /** Uri (árvore SAF) da pasta escolhida; null = armazenamento interno do app. */
    val pastaBackup: String? = null,
    val pastaBackupNome: String = "",
    val ultimoBackupEm: Long = 0L,
    val ultimoBackupMsg: String = "",
    val lembretesAtivo: Boolean = true,
    val antecedenciaMin: Int = 60,
    /** Versão do termo de privacidade aceita pelo profissional (0 = nunca aceitou). */
    val consentimentoVersao: Int = 0,
    val consentimentoEm: Long = 0L,
    /** Exige a tela de bloqueio do aparelho (biometria/PIN/padrão) ao abrir o app. */
    val bloqueioAtivo: Boolean = false,
    /** Impede capturas de tela e esconde o app na lista de recentes. */
    val bloquearCapturaTela: Boolean = false,
    val tema: TemaApp = TemaApp.SISTEMA,
    val onboardingConcluido: Boolean = false,
    /** Notificação diária (~9h) com alunos de reavaliação atrasada ou vencendo. */
    val reavaliacaoAvisos: Boolean = true,
    /** Metas do negócio por mês: sessões concluídas e receita em centavos (0 = sem meta). */
    val metaSessoesMes: Int = 0,
    val metaReceitaMesCentavos: Long = 0L,
    /** Professor ou aluno; null = a pessoa ainda não escolheu (mostra a tela de escolha). */
    val usoApp: UsoApp? = null,
    /** Lembretes de treino do modo aluno (dias, horário e liga/desliga). */
    val lembreteAluno: LembreteAluno = LembreteAluno(),
    /** Sincronização por pasta: última versão aplicada/enviada, quando e identificação deste aparelho. */
    val syncVersao: Long = 0L,
    val syncUltimoEm: Long = 0L,
    val syncMensagem: String = "",
    val syncDispositivo: String = "",
)

@Singleton
class ConfiguracoesRepository @Inject constructor(@ApplicationContext private val ctx: Context) {

    private object K {
        val backupAtivo = booleanPreferencesKey("backup_ativo")
        val pasta = stringPreferencesKey("backup_pasta")
        val pastaNome = stringPreferencesKey("backup_pasta_nome")
        val ultimoEm = longPreferencesKey("backup_ultimo_em")
        val ultimoMsg = stringPreferencesKey("backup_ultimo_msg")
        val lembretes = booleanPreferencesKey("lembretes_ativo")
        val antecedencia = intPreferencesKey("lembretes_antecedencia_min")
        val consentVersao = intPreferencesKey("consentimento_versao")
        val consentEm = longPreferencesKey("consentimento_em")
        val bloqueio = booleanPreferencesKey("bloqueio_ativo")
        val capturaTela = booleanPreferencesKey("bloquear_captura_tela")
        val tema = stringPreferencesKey("tema")
        val onboarding = booleanPreferencesKey("onboarding_concluido")
        val reavaliacaoAvisos = booleanPreferencesKey("reavaliacao_avisos")
        val metaSessoes = intPreferencesKey("meta_sessoes_mes")
        val metaReceita = longPreferencesKey("meta_receita_mes_centavos")
        val usoApp = stringPreferencesKey("uso_app")
        val alunoLembreteAtivo = booleanPreferencesKey("aluno_lembrete_ativo")
        val alunoLembreteDias = intPreferencesKey("aluno_lembrete_dias")
        val alunoLembreteMin = intPreferencesKey("aluno_lembrete_min")
        val syncVersao = longPreferencesKey("sync_versao")
        val syncUltimoEm = longPreferencesKey("sync_ultimo_em")
        val syncMensagem = stringPreferencesKey("sync_mensagem")
        val syncDispositivo = stringPreferencesKey("sync_dispositivo")
    }

    val config: Flow<Configuracoes> = ctx.configStore.data.map { p ->
        Configuracoes(
            backupAtivo = p[K.backupAtivo] ?: true,
            pastaBackup = p[K.pasta]?.takeIf { it.isNotBlank() },
            pastaBackupNome = p[K.pastaNome].orEmpty(),
            ultimoBackupEm = p[K.ultimoEm] ?: 0L,
            ultimoBackupMsg = p[K.ultimoMsg].orEmpty(),
            lembretesAtivo = p[K.lembretes] ?: true,
            antecedenciaMin = p[K.antecedencia] ?: 60,
            consentimentoVersao = p[K.consentVersao] ?: 0,
            consentimentoEm = p[K.consentEm] ?: 0L,
            bloqueioAtivo = p[K.bloqueio] ?: false,
            bloquearCapturaTela = p[K.capturaTela] ?: false,
            tema = TemaApp.deNome(p[K.tema]),
            onboardingConcluido = p[K.onboarding] ?: false,
            reavaliacaoAvisos = p[K.reavaliacaoAvisos] ?: true,
            metaSessoesMes = p[K.metaSessoes] ?: 0,
            metaReceitaMesCentavos = p[K.metaReceita] ?: 0L,
            usoApp = UsoApp.deNome(p[K.usoApp]),
            lembreteAluno = LembreteAluno(
                ativo = p[K.alunoLembreteAtivo] ?: false,
                dias = p[K.alunoLembreteDias] ?: LembreteAluno.DIAS_PADRAO,
                minutos = p[K.alunoLembreteMin] ?: (18 * 60),
            ),
            syncVersao = p[K.syncVersao] ?: 0L,
            syncUltimoEm = p[K.syncUltimoEm] ?: 0L,
            syncMensagem = p[K.syncMensagem].orEmpty(),
            syncDispositivo = p[K.syncDispositivo].orEmpty(),
        )
    }

    suspend fun definirBackupAtivo(v: Boolean) = ctx.configStore.edit { it[K.backupAtivo] = v }

    suspend fun definirPasta(uri: String?, nome: String) = ctx.configStore.edit {
        it[K.pasta] = uri.orEmpty()
        it[K.pastaNome] = nome
    }

    suspend fun registrarBackup(emMillis: Long, mensagem: String) = ctx.configStore.edit {
        it[K.ultimoEm] = emMillis
        it[K.ultimoMsg] = mensagem
    }

    suspend fun definirLembretes(v: Boolean) = ctx.configStore.edit { it[K.lembretes] = v }
    suspend fun definirAntecedencia(min: Int) = ctx.configStore.edit { it[K.antecedencia] = min }

    suspend fun registrarConsentimento(versao: Int, emMillis: Long) = ctx.configStore.edit {
        it[K.consentVersao] = versao
        it[K.consentEm] = emMillis
    }

    suspend fun definirBloqueio(v: Boolean) = ctx.configStore.edit { it[K.bloqueio] = v }
    suspend fun definirBloquearCaptura(v: Boolean) = ctx.configStore.edit { it[K.capturaTela] = v }
    suspend fun definirTema(t: TemaApp) = ctx.configStore.edit { it[K.tema] = t.name }
    suspend fun concluirOnboarding() = ctx.configStore.edit { it[K.onboarding] = true }
    suspend fun definirReavaliacaoAvisos(v: Boolean) = ctx.configStore.edit { it[K.reavaliacaoAvisos] = v }

    suspend fun registrarSincronizacao(versao: Long, emMillis: Long, mensagem: String) = ctx.configStore.edit {
        it[K.syncVersao] = versao
        it[K.syncUltimoEm] = emMillis
        it[K.syncMensagem] = mensagem
    }

    suspend fun definirDispositivoSync(id: String) = ctx.configStore.edit { it[K.syncDispositivo] = id }

    suspend fun definirLembreteAluno(l: LembreteAluno) = ctx.configStore.edit {
        it[K.alunoLembreteAtivo] = l.ativo
        it[K.alunoLembreteDias] = l.dias
        it[K.alunoLembreteMin] = l.minutos.coerceIn(0, 24 * 60 - 1)
    }

    suspend fun definirUsoApp(uso: UsoApp) = ctx.configStore.edit { it[K.usoApp] = uso.name }

    suspend fun definirMetasNegocio(sessoes: Int, receitaCentavos: Long) = ctx.configStore.edit {
        it[K.metaSessoes] = sessoes.coerceAtLeast(0)
        it[K.metaReceita] = receitaCentavos.coerceAtLeast(0L)
    }
}
