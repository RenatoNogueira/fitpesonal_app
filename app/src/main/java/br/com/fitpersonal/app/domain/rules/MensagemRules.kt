package br.com.fitpersonal.app.domain.rules

import java.net.URLEncoder
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Mensagens prontas (pt-BR) para enviar ao aluno por WhatsApp. */
object MensagemRules {
    private val PT_BR = Locale("pt", "BR")
    private val DIA_MES = DateTimeFormatter.ofPattern("dd/MM")
    private val HORA = DateTimeFormatter.ofPattern("HH:mm")

    private fun assinatura(profissional: String) =
        if (profissional.isBlank()) "" else "\n\n$profissional"

    fun lembreteAgendamento(
        nomeAluno: String,
        data: LocalDate,
        horario: LocalTime,
        tipo: String,
        profissional: String,
    ): String {
        val dia = data.dayOfWeek.getDisplayName(TextStyle.FULL, PT_BR)
        return "Olá, ${Saudacao.primeiroNome(nomeAluno)}! Passando para lembrar do seu(sua) " +
            "${tipo.lowercase(PT_BR)} em ${data.format(DIA_MES)} ($dia) às ${horario.format(HORA)}. " +
            "Confirma presença?" + assinatura(profissional)
    }

    fun cobranca(
        nomeAluno: String,
        competencia: YearMonth,
        valorCentavos: Long,
        profissional: String,
        pix: PixCobranca? = null,
    ): String {
        val base = "Olá, " + Saudacao.primeiroNome(nomeAluno) + "! Identificamos que a mensalidade de " +
            FinanceiroRules.rotuloCompetencia(competencia) + " (" + FinanceiroRules.formatarReais(valorCentavos) + ") " +
            "está em aberto."
        val pagamento = if (pix == null) "" else buildString {
            append("\n\nPara pagar por Pix:\nChave: ").append(pix.chave)
            append("\nValor: ").append(FinanceiroRules.formatarReais(valorCentavos))
            pix.copiaECola?.let { append("\n\nPix copia e cola:\n").append(it) }
        }
        return base + pagamento + "\n\nSe já pagou, desconsidere. Qualquer dúvida, é só chamar." + assinatura(profissional)
    }

    fun treinoPronto(nomeAluno: String, nomeTreino: String, profissional: String): String =
        "Olá, ${Saudacao.primeiroNome(nomeAluno)}! Seu treino \"$nomeTreino\" está atualizado. " +
            "Qualquer dúvida na execução, me chame." + assinatura(profissional)

    fun pacoteAcabando(nomeAluno: String, saldo: Int, profissional: String): String {
        val situacao = if (saldo <= 0) "seu pacote de sessões chegou ao fim" else "você está na última sessão do pacote"
        return "Olá, ${Saudacao.primeiroNome(nomeAluno)}! Passando para avisar que $situacao. " +
            "Vamos renovar para não perder o ritmo?" + assinatura(profissional)
    }

    fun reavaliacao(nomeAluno: String, profissional: String): String =
        "Olá, ${Saudacao.primeiroNome(nomeAluno)}! Está na hora da sua reavaliação. " +
            "Vamos marcar um horário?" + assinatura(profissional)
}

object WhatsAppRules {
    /**
     * Link `wa.me` com a mensagem. [telefone] aceita máscara; 10/11 dígitos ganham o DDI 55.
     * Devolve null se não houver telefone utilizável.
     */
    fun link(telefone: String?, mensagem: String): String? {
        var digitos = telefone.orEmpty().filter { it.isDigit() }
        if (digitos.length !in 10..13) return null
        if (digitos.length <= 11) digitos = "55$digitos"
        val texto = URLEncoder.encode(mensagem, "UTF-8").replace("+", "%20")
        return "https://wa.me/$digitos?text=$texto"
    }
}

/** CSV para Excel pt-BR: ponto e vírgula, UTF-8 com BOM, aspas escapadas e proteção contra "fórmulas". */
object CsvRules {
    const val SEPARADOR = ';'
    const val BOM = "﻿"

    /** Texto livre: escapa aspas/separadores e neutraliza início com = + - @ (injeção de fórmula). */
    fun texto(valor: String?): String {
        var v = valor.orEmpty().replace("\r\n", "\n").replace('\r', '\n')
        if (v.isNotEmpty() && v.first() in "=+-@\t") v = "'$v"
        val precisaAspas = v.any { it == SEPARADOR || it == '"' || it == '\n' }
        return if (precisaAspas) "\"" + v.replace("\"", "\"\"") + "\"" else v
    }

    /** Número já formatado (vírgula decimal); não passa pela neutralização do sinal de menos. */
    fun numero(valor: Double?, casas: Int = 1): String =
        valor?.let { String.format(Locale("pt", "BR"), "%.${casas}f", it) }.orEmpty()

    fun linha(vararg campos: String): String = campos.joinToString(SEPARADOR.toString())
}
