package br.com.fitpersonal.app.widget

import android.content.Context
import android.content.Intent
import androidx.glance.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import br.com.fitpersonal.app.MainActivity
import br.com.fitpersonal.app.data.repo.AgendaRepository
import br.com.fitpersonal.app.data.repo.ConfiguracoesRepository
import br.com.fitpersonal.app.domain.model.StatusAgendamento
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.time.LocalDate

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun agenda(): AgendaRepository
    fun configuracoes(): ConfiguracoesRepository
}

/** Dados já prontos para desenhar (sem acesso ao banco dentro da composição). */
data class DadosWidget(
    /** Com o bloqueio do app ligado, o widget mostra só a contagem (sem nomes na tela inicial). */
    val oculto: Boolean,
    val total: Int,
    val pendentes: Int,
    /** (horário, nome do aluno) em ordem. */
    val linhas: List<Pair<String, String>>,
    val erro: Boolean = false,
)

/** Próximos agendamentos de hoje na tela inicial; tocar abre o app. */
class AgendaWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dados = carregar(context)
        provideContent { Conteudo(dados) }
    }

    private suspend fun carregar(context: Context): DadosWidget = runCatching {
        val ep = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        val oculto = ep.configuracoes().config.first().bloqueioAtivo
        val itens = ep.agenda().doDia(LocalDate.now()).first()
            .filter { it.status != StatusAgendamento.CANCELADO.name }
        val pendentes = itens.filter { it.status == StatusAgendamento.PENDENTE.name }
        DadosWidget(
            oculto = oculto,
            total = itens.size,
            pendentes = pendentes.size,
            linhas = pendentes.map { it.horario to it.nomeAluno },
        )
    }.getOrElse { DadosWidget(oculto = true, total = 0, pendentes = 0, linhas = emptyList(), erro = true) }
}

class AgendaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AgendaWidget()
}

@androidx.compose.runtime.Composable
private fun Conteudo(d: DadosWidget) {
    val fundo = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF1B1B1F))
    val texto = ColorProvider(day = Color(0xFF1B1B1F), night = Color(0xFFE6E1E5))
    val apoio = ColorProvider(day = Color(0xFF5F6368), night = Color(0xFFC4C7C5))
    // Intent explícito: funciona em qualquer versão do Glance (a variante com tipo reificado mudou de pacote).
    val abrirApp = Intent(LocalContext.current, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(fundo)
            .padding(12.dp)
            .clickable(actionStartActivity(abrirApp)),
    ) {
        Text(
            "Agenda de hoje",
            style = TextStyle(color = texto, fontSize = 14.sp, fontWeight = FontWeight.Bold),
        )
        when {
            d.erro -> Text("Abra o app para atualizar.", style = TextStyle(color = apoio, fontSize = 12.sp))
            d.total == 0 -> Text("Dia livre", style = TextStyle(color = apoio, fontSize = 12.sp))
            d.oculto -> Text(
                "${d.pendentes} pendente(s) de ${d.total}",
                style = TextStyle(color = texto, fontSize = 13.sp),
            )
            d.linhas.isEmpty() -> Text("Tudo concluído hoje", style = TextStyle(color = apoio, fontSize = 12.sp))
            else -> {
                d.linhas.take(4).forEach { (hora, nome) ->
                    Text("$hora  $nome", style = TextStyle(color = texto, fontSize = 13.sp), maxLines = 1)
                }
                if (d.linhas.size > 4) {
                    Text("+${d.linhas.size - 4} mais", style = TextStyle(color = apoio, fontSize = 12.sp))
                }
            }
        }
    }
}
