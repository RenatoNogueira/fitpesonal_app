package br.com.fitpersonal.contrato

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MetodoContratoTest {

    private fun pacote(metodo: String?) = PacoteTreino(
        treinoId = "t-m", revisao = 1, emitidoEm = "2026-05-20", validoAte = null,
        profissional = ProfissionalRef("Renato", "FitPersonal", "123-G/SP", "(11) 9"),
        aluno = AlunoRef("Maria", "ref-1"),
        treino = TreinoRef(
            "Treino A", null,
            mapOf("A" to listOf(ExercicioRef(nome = "Supino", series = "3", repeticoes = "12", metodo = metodo))),
        ),
    )

    @Test fun metodoViajaNoArquivoELeDeVolta() {
        val p = pacote("BISET")
        val json = ContratoJson.serializar(p)
        assertTrue(json.contains("\"metodo\":\"BISET\""))
        assertEquals(p, (ContratoJson.lerPacote(json) as Leitura.Ok).valor)
    }

    @Test fun semMetodoOCampoNaoApareceEOChecksumNaoMuda() {
        val json = ContratoJson.serializar(pacote(null))
        assertFalse(json.contains("\"metodo\""))
        assertNull((ContratoJson.lerPacote(json) as Leitura.Ok).valor.treino.divisoes["A"]!!.first().metodo)
    }

    @Test fun metodoDeVersaoFuturaPassaIntactoEMantemAIntegridade() {
        val p = pacote("TABATA")
        val lido = ContratoJson.lerPacote(ContratoJson.serializar(p))
        assertTrue(lido is Leitura.Ok)
        assertEquals("TABATA", (lido as Leitura.Ok).valor.treino.divisoes["A"]!!.first().metodo)
    }
}
