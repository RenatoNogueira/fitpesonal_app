package br.com.fitpersonal.app.core

import android.content.Context
import android.os.Bundle
import br.com.fitpersonal.app.domain.rules.AnaliticaRules
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Estatísticas de uso anônimas (Firebase Analytics) — para entender quais telas as pessoas usam e onde desistem.
 *
 * Regras de privacidade (o app guarda dados de saúde):
 *  - **Opt-in**: nada é coletado até o usuário aceitar ([definirAtiva]); a coleta já nasce desligada no manifesto.
 *  - **Nenhum dado pessoal**: só nomes fixos de telas e de ações ("aluno_criado"). Nunca nomes, textos digitados,
 *    ids, medidas, fotos ou mensagens de erro.
 *  - **Inerte sem configuração**: sem app/google-services.json o Firebase não inicializa e esta classe não faz nada.
 *
 * Eventos: `screen_view` (tela), `acao_concluida` (salvou/excluiu algo) e `tela_abandonada` (saiu de um formulário
 * sem concluir, com o tempo em segundos). Dicionário completo em docs/ANALYTICS.md.
 */
object Analitica {
    @Volatile private var firebase: FirebaseAnalytics? = null
    @Volatile private var ativa = false

    // Estado do formulário aberto, para medir abandono
    private var formularioAtual: String? = null
    private var formularioDesde = 0L
    private var formularioConcluiu = false

    /** Chamar uma vez (FitApp.onCreate). Sem configuração do Firebase, fica desligada para sempre. */
    fun iniciar(ctx: Context) {
        firebase = runCatching {
            if (FirebaseApp.getApps(ctx).isEmpty()) null else FirebaseAnalytics.getInstance(ctx)
        }.getOrNull()
    }

    /** O Firebase está configurado neste build (há como oferecer a opção ao usuário). */
    val disponivel: Boolean get() = firebase != null

    /** Liga/desliga a coleta (reflete a escolha do usuário). */
    fun definirAtiva(ativo: Boolean) {
        ativa = ativo
        runCatching { firebase?.setAnalyticsCollectionEnabled(ativo) }
    }

    private fun registrar(nome: String, params: Bundle? = null) {
        if (!ativa) return
        runCatching { firebase?.logEvent(nome, params) }
    }

    /** Tela mostrada no app do aluno (que não usa o NavHost). */
    fun tela(nome: String) {
        registrar(FirebaseAnalytics.Event.SCREEN_VIEW, Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, nome)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, "FitPersonal")
        })
    }

    /** Chamar a cada mudança de destino da navegação: registra a tela e fecha a medição de abandono do formulário anterior. */
    fun navegou(rota: String?) {
        val nome = AnaliticaRules.nomeDaTela(rota) ?: return

        // O formulário que estava aberto foi deixado sem concluir nada?
        formularioAtual?.let { anterior ->
            if (!formularioConcluiu) {
                val segundos = ((System.currentTimeMillis() - formularioDesde) / 1000L).coerceIn(0L, 3600L)
                registrar("tela_abandonada", Bundle().apply {
                    putString("tela", anterior)
                    putLong("segundos", segundos)
                })
            }
        }
        formularioAtual = if (AnaliticaRules.ehFormulario(rota)) nome else null
        formularioDesde = System.currentTimeMillis()
        formularioConcluiu = false

        tela(nome)
    }

    /** Uma ação foi concluída com sucesso (o texto é um aviso fixo do app, como "Aluno criado"). */
    fun acaoConcluida(aviso: String) {
        formularioConcluiu = true
        registrar("acao_concluida", Bundle().apply { putString("acao", AnaliticaRules.chaveSegura(aviso)) })
    }
}
