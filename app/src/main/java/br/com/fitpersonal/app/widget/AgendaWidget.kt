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
    /** Pendentes cujo horário já passou (pedem atenção: o widget fica coral). */
    val atrasados: Int = 0,
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
            atrasados = pendentes.count { p ->
                runCatching { java.time.LocalTime.parse(p.horario).isBefore(java.time.LocalTime.now()) }.getOrDefault(false)
            },
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

    // Fundo "pasta" desenhado no tamanho real do widget (aba alta à esquerda, degradê lima).
    val contexto = LocalContext.current
    val tamanho = androidx.glance.LocalSize.current
    val paleta = when {
        d.erro -> PaletaWidget.Lima
        d.total == 0 -> PaletaWidget.Verde        // dia livre
        d.atrasados > 0 -> PaletaWidget.Coral     // há atendimento que já passou do horário
        d.pendentes == 0 -> PaletaWidget.Turquesa // tudo concluído
        else -> PaletaWidget.Lima
    }
    val fundoPasta = androidx.compose.runtime.remember(tamanho, paleta) {
        desenharFundoPasta(contexto, tamanho.width.value, tamanho.height.value, paleta)
    }

    // Quantas linhas cabem na altura atual do widget (cada linha ~32 dp; cabeçalho e margens ~82 dp).
    val altura = tamanho.height
    val linhasMax = ((altura - 82.dp) / 32.dp).toInt().coerceIn(1, 6)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(fundoPasta))
            .padding(start = 14.dp, end = 14.dp, top = 28.dp, bottom = 14.dp)
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
                        if (d.atrasados > 0) d.atrasados.toString() + " atrasado(s)" else d.pendentes.toString() + "/" + d.total,
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

/**
 * Desenha a "pasta" do fundo (mesma geometria da FolderShape do app) com degradê lima, em pixels reais:
 * o Glance não tem formas livres, então o fundo é uma imagem gerada no tamanho exato do widget.
 */
private fun desenharFundoPasta(contexto: Context, larguraDp: Float, alturaDp: Float, paleta: PaletaWidget): android.graphics.Bitmap {
    val d = contexto.resources.displayMetrics.density
    val w = Math.round(larguraDp * d).coerceIn(2, 1600)
    val h = Math.round(alturaDp * d).coerceIn(2, 1600)
    val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)

    val wf = w.toFloat()
    val hf = h.toFloat()
    val r = minOf(28f * d, hf / 3f)
    val n = minOf(18f * d, hf / 3f)
    val xAba = (wf * 0.6f).coerceIn(r * 2f, wf - r * 3f)

    val caminho = android.graphics.Path().apply {
        moveTo(0f, r)
        arcTo(android.graphics.RectF(0f, 0f, 2f * r, 2f * r), 180f, 90f, false)
        lineTo(xAba - r, 0f)
        cubicTo(xAba, 0f, xAba, n, xAba + r, n)
        lineTo(wf - r, n)
        arcTo(android.graphics.RectF(wf - 2f * r, n, wf, n + 2f * r), 270f, 90f, false)
        lineTo(wf, hf - r)
        arcTo(android.graphics.RectF(wf - 2f * r, hf - 2f * r, wf, hf), 0f, 90f, false)
        lineTo(r, hf)
        arcTo(android.graphics.RectF(0f, hf - 2f * r, 2f * r, hf), 90f, 90f, false)
        close()
    }
    val tinta = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        shader = android.graphics.LinearGradient(
            0f, 0f, wf, hf,
            intArrayOf(paleta.claro, paleta.meio, paleta.escuro),
            floatArrayOf(0f, 0.5f, 1f),
            android.graphics.Shader.TileMode.CLAMP,
        )
    }
    canvas.drawPath(caminho, tinta)
    return bmp
}

/** Cores do widget por situação do dia: claro → meio → escuro do degradê. */
private enum class PaletaWidget(val claro: Int, val meio: Int, val escuro: Int) {
    Lima(0xFFEAFA9A.toInt(), 0xFFD9F24F.toInt(), 0xFFBCD43F.toInt()),
    Coral(0xFFFFB9A8.toInt(), 0xFFFF7A5C.toInt(), 0xFFE0603F.toInt()),
    Turquesa(0xFF9BEEE3.toInt(), 0xFF34D1BF.toInt(), 0xFF27AE9E.toInt()),
    Verde(0xFFC8F9B0.toInt(), 0xFF8CEB5E.toInt(), 0xFF6FC947.toInt()),
}
