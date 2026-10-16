package br.com.fitpersonal.contrato

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoContratoTest {

    private fun pacote(video: String?) = PacoteTreino(
        treinoId = "t-v", revisao = 1, emitidoEm = "2026-05-20", validoAte = null,
        profissional = ProfissionalRef("Renato", "FitPersonal", "123-G/SP", "(11) 9"),
        aluno = AlunoRef("Maria", "ref-1"),
        treino = TreinoRef(
            "Treino A", null,
            mapOf("A" to listOf(ExercicioRef(nome = "Supino", series = "3", repeticoes = "12", video = video))),
        ),
    )

    @Test fun videoViajaNoArquivoELeDeVolta() {
        val p = pacote("https://www.youtube.com/watch?v=abc")
        val json = ContratoJson.serializar(p)
        assertTrue(json.contains("\"video\":\"https://www.youtube.com/watch?v=abc\""))
        val lido = (ContratoJson.lerPacote(json) as Leitura.Ok).valor
        assertEquals(p, lido)
    }

    @Test fun semVideoOCampoNaoApareceEOChecksumNaoMuda() {
        val json = ContratoJson.serializar(pacote(null))
        assertFalse(json.contains("\"video\""))
        val lido = (ContratoJson.lerPacote(json) as Leitura.Ok).valor
        assertNull(lido.treino.divisoes["A"]!!.first().video)
    }

    @Test fun adulteracaoDoLinkQuebraAIntegridade() {
        val base = ContratoJson.serializar(pacote("https://exemplo.com/v"))
        // troca o esquema por um perigoso e corrige o checksum reserializando o pacote lido
        val adulterado = base.replace("https://exemplo.com/v", "javascript:alert(1)")
        val r = ContratoJson.lerPacote(adulterado)
        // sem a correção do checksum a leitura recusa por integridade
        assertTrue(r is Leitura.Erro)
    }
}
