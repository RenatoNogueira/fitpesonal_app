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
import androidx.glance.ImageProvider
import androidx.glance.appwidget.cornerRadius
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.width
import br.com.fitpersonal.app.R

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

    override val sizeMode: androidx.glance.appwidget.SizeMode = androidx.glance.appwidget.SizeMode.Exact

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
    val preto = androidx.glance.unit.ColorProvider(Color(0xFF0A0D08))
    val suave = androidx.glance.unit.ColorProvider(Color(0xB30A0D08))
    val lima = androidx.glance.unit.ColorProvider(Color(0xFFD9F24F))
    // Intent explícito: funciona em qualquer versão do Glance (a variante com tipo reificado mudou de pacote).
    val abrirApp = Intent(LocalContext.current, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

    // Quantas linhas cabem na altura atual do widget (cada linha ~32 dp; cabeçalho e margens ~74 dp).
    val altura = androidx.glance.LocalSize.current.height
    val linhasMax = ((altura - 74.dp) / 32.dp).toInt().coerceIn(1, 6)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_fundo))
            .padding(14.dp)
            .clickable(actionStartActivity(abrirApp)),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Agenda de hoje",
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(color = preto, fontSize = 16.sp, fontWeight = FontWeight.Bold),
            )
            if (!d.erro && d.total > 0) {
                Box(
                    modifier = GlanceModifier
                        .background(preto)
                        .cornerRadius(12.dp)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        d.pendentes.toString() + "/" + d.total,
                        style = TextStyle(color = lima, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    )
                }
            }
        }
        Spacer(GlanceModifier.height(8.dp))
        when {
            d.erro -> Text("Abra o app para atualizar.", style = TextStyle(color = suave, fontSize = 13.sp))
            d.total == 0 -> Text("Dia livre 🎉", style = TextStyle(color = preto, fontSize = 15.sp, fontWeight = FontWeight.Medium))
            d.oculto -> Text(
                d.pendentes.toString() + " pendente(s) de " + d.total,
                style = TextStyle(color = preto, fontSize = 14.sp, fontWeight = FontWeight.Medium),
            )
            d.linhas.isEmpty() -> Text(
                "Tudo concluído hoje ✅",
                style = TextStyle(color = preto, fontSize = 15.sp, fontWeight = FontWeight.Medium),
            )
            else -> {
                d.linhas.take(linhasMax).forEach { (hora, nome) ->
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .background(preto)
                                .cornerRadius(10.dp)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text(hora, style = TextStyle(color = lima, fontSize = 13.sp, fontWeight = FontWeight.Bold))
                        }
                        Spacer(GlanceModifier.width(10.dp))
                        Text(
                            nome,
                            modifier = GlanceModifier.defaultWeight(),
                            style = TextStyle(color = preto, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                            maxLines = 1,
                        )
                    }
                }
                if (d.linhas.size > linhasMax) {
                    Text(
                        "+" + (d.linhas.size - linhasMax) + " mais",
                        style = TextStyle(color = suave, fontSize = 12.sp),
                    )
                }
            }
        }
    }
}
