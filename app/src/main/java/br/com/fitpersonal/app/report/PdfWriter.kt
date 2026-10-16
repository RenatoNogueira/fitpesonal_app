package br.com.fitpersonal.app.report

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.OutputStream

/** Cabeçalho repetido no topo de cada página. */
data class Cabecalho(
    val titulo: String,
    val marca: String = "",
    val contato: String = "",
    val logo: Bitmap? = null,
)

/**
 * Escritor de PDF minimalista sobre android.graphics.pdf.PdfDocument: quebra de página automática,
 * parágrafos com quebra de linha, tabelas com cabeçalho repetido e imagens.
 * Unidades em pontos (A4 = 595 x 842).
 */
class PdfWriter(paisagem: Boolean = false, private val cabecalho: Cabecalho) {

    private val largura = if (paisagem) 842 else 595
    private val altura = if (paisagem) 595 else 842
    private val margem = 36f
    private val margemInferior = 40f
    val larguraUtil = largura - 2 * margem

    private val doc = PdfDocument()
    private var pagina: PdfDocument.Page? = null
    private var canvas: Canvas? = null
    private var y = margem
    private var numero = 0

    private fun paint(tamanho: Float, bold: Boolean = false, cor: Int = Color.BLACK) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = tamanho
        color = cor
        typeface = if (bold) Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) else Typeface.SANS_SERIF
    }

    private val normal = paint(10f)
    private val negrito = paint(10f, bold = true)
    private val paintSecao = paint(12f, bold = true)
    private val pequeno = paint(8f, cor = Color.GRAY)
    private val linhaPaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 0.8f }
    private val fundoCabecalho = Paint().apply { color = Color.rgb(235, 238, 242) }

    init { novaPagina() }

    // ───────────── páginas ─────────────

    private fun fecharPagina() {
        val p = pagina ?: return
        val c = canvas!!
        c.drawText("Página $numero", largura / 2f - 24f, altura - 20f, pequeno)
        doc.finishPage(p)
        pagina = null
    }

    private fun novaPagina() {
        fecharPagina()
        numero++
        pagina = doc.startPage(PdfDocument.PageInfo.Builder(largura, altura, numero).create())
        canvas = pagina!!.canvas
        y = margem
        desenharCabecalho()
    }

    private fun garantir(h: Float) { if (y + h > altura - margemInferior) novaPagina() }

    private fun desenharCabecalho() {
        val c = canvas!!
        var xTexto = margem
        cabecalho.logo?.let { logo ->
            val lado = 40f
            val escala = lado / maxOf(logo.width, logo.height)
            val dst = android.graphics.RectF(margem, y, margem + logo.width * escala, y + logo.height * escala)
            c.drawBitmap(logo, null, dst, null)
            xTexto = dst.right + 10f
        }
        var ty = y + 14f
        if (cabecalho.marca.isNotBlank()) {
            c.drawText(cabecalho.marca, xTexto, ty, paint(14f, true)); ty += 16f
        }
        c.drawText(cabecalho.titulo, xTexto, ty, paint(12f, true, Color.DKGRAY)); ty += 12f
        if (cabecalho.contato.isNotBlank()) {
            c.drawText(cabecalho.contato, xTexto, ty, pequeno); ty += 10f
        }
        y = maxOf(ty, y + if (cabecalho.logo != null) 44f else 0f) + 6f
        c.drawLine(margem, y, margem + larguraUtil, y, linhaPaint)
        y += 12f
    }

    // ───────────── conteúdo ─────────────

    fun espaco(h: Float = 8f) { y += h }

    fun secao(titulo: String) {
        garantir(34f)
        espaco(6f)
        canvas!!.drawText(titulo, margem, y + 12f, paintSecao)
        y += 18f
        canvas!!.drawLine(margem, y, margem + larguraUtil, y, linhaPaint)
        y += 8f
    }

    /** Parágrafo com quebra automática; linhas são colocadas uma a uma, então pode atravessar páginas. */
    fun texto(conteudo: String, enfatizar: Boolean = false, tamanho: Float? = null) {
        if (conteudo.isBlank()) return
        val p = (if (enfatizar) negrito else normal).let { base -> tamanho?.let { paint(it, enfatizar) } ?: base }
        val layout = StaticLayout.Builder.obtain(conteudo, 0, conteudo.length, p, larguraUtil.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(2f, 1f).build()
        for (i in 0 until layout.lineCount) {
            val h = layout.getLineBottom(i) - layout.getLineTop(i)
            garantir(h.toFloat())
            val baseline = y + (layout.getLineBaseline(i) - layout.getLineTop(i))
            canvas!!.drawText(conteudo, layout.getLineStart(i), layout.getLineEnd(i), margem, baseline, p)
            y += h
        }
        y += 3f
    }

    fun campo(rotulo: String, valor: String) = texto("$rotulo: $valor")

    /** Linhas em branco com traço, para o profissional preencher à mão (ex.: cardio). */
    fun linhasEmBranco(rotulo: String, quantidade: Int) {
        texto(rotulo, enfatizar = true)
        repeat(quantidade) {
            garantir(20f)
            y += 16f
            canvas!!.drawLine(margem, y, margem + larguraUtil, y, linhaPaint)
        }
        y += 6f
    }

    /**
     * Tabela com larguras proporcionais (soma livre). O cabeçalho é repetido ao quebrar de página.
     */
    fun tabela(colunas: List<Pair<String, Float>>, linhas: List<List<String>>) {
        val soma = colunas.sumOf { it.second.toDouble() }.toFloat()
        val larguras = colunas.map { it.second / soma * larguraUtil }
        val pad = 4f

        fun layouts(cells: List<String>, p: TextPaint) = cells.mapIndexed { i, t ->
            val txt = t.ifBlank { " " }
            StaticLayout.Builder.obtain(txt, 0, txt.length, p, (larguras[i] - 2 * pad).toInt().coerceAtLeast(10)).build()
        }

        fun desenharLinha(cells: List<String>, p: TextPaint, cabecalhoTabela: Boolean) {
            val ls = layouts(cells, p)
            val h = ls.maxOf { it.height } + 2 * pad
            if (y + h > altura - margemInferior) {
                novaPagina()
                if (!cabecalhoTabela) desenharLinha(colunas.map { it.first }, negrito, true)
            }
            val c = canvas!!
            var x = margem
            if (cabecalhoTabela) c.drawRect(margem, y, margem + larguraUtil, y + h, fundoCabecalho)
            ls.forEachIndexed { i, l ->
                c.save(); c.translate(x + pad, y + pad); l.draw(c); c.restore()
                x += larguras[i]
            }
            c.drawLine(margem, y + h, margem + larguraUtil, y + h, linhaPaint)
            y += h
        }

        desenharLinha(colunas.map { it.first }, negrito, true)
        linhas.forEach { desenharLinha(it, normal, false) }
        y += 6f
    }

    /** Imagens com legenda, [porLinha] por linha, largura proporcional. */
    fun imagens(itens: List<Pair<String, Bitmap>>, porLinha: Int = 2) {
        val gap = 12f
        val w = (larguraUtil - gap * (porLinha - 1)) / porLinha
        itens.chunked(porLinha).forEach { linha ->
            val alturas = linha.map { (_, b) -> w * b.height / b.width }
            val h = alturas.max().coerceAtMost((altura - 2 * margem - 60f))
            garantir(h + 18f)
            linha.forEachIndexed { i, (legenda, bmp) ->
                val x = margem + i * (w + gap)
                canvas!!.drawText(legenda, x, y + 10f, pequeno)
                val dst = android.graphics.RectF(x, y + 14f, x + w, y + 14f + alturas[i].coerceAtMost(h))
                canvas!!.drawBitmap(bmp, null, dst, null)
            }
            y += h + 22f
        }
    }

    fun finalizar(saida: OutputStream) {
        fecharPagina()
        doc.writeTo(saida)
        doc.close()
    }
}
