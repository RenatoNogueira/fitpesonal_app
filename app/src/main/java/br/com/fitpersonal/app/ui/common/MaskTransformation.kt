package br.com.fitpersonal.app.ui.common

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Máscara visual sobre texto que guarda só dígitos. '#' = dígito; demais caracteres são literais.
 * Ex.: "###.###.###-##" para CPF, "##/##/####" para data.
 */
class MaskTransformation(private val mask: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val out = StringBuilder()
        val origToTrans = IntArray(digits.length + 1)
        var di = 0
        for (m in mask) {
            if (di >= digits.length) break
            if (m == '#') { origToTrans[di] = out.length; out.append(digits[di]); di++ } else out.append(m)
        }
        origToTrans[digits.length] = out.length
        val transToOrig = IntArray(out.length + 1)
        var o = 0
        for (t in 0..out.length) {
            while (o < digits.length && origToTrans[o] < t) o++
            transToOrig[t] = o.coerceAtMost(digits.length)
        }
        return TransformedText(
            AnnotatedString(out.toString()),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int) = origToTrans[offset.coerceIn(0, digits.length)]
                override fun transformedToOriginal(offset: Int) = transToOrig[offset.coerceIn(0, out.length)]
            },
        )
    }
}

object Masks {
    val DATA = MaskTransformation("##/##/####")
    val CPF = MaskTransformation("###.###.###-##")
    val CEP = MaskTransformation("#####-###")
    /** Telefone BR: 10 ou 11 dígitos; a máscara de 11 serve para ambos. */
    val TELEFONE = MaskTransformation("(##) #####-####")
}
