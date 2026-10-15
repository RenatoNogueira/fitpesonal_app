package br.com.fitpersonal.contrato

import org.junit.Assert.*
import org.junit.Test

class ContratoTest {

    private fun recurso(nome: String): String =
        javaClass.getResourceAsStream("/exemplos/$nome")!!.readBytes().toString(Charsets.UTF_8).trim()

    private val pacote = PacoteTreino(
        treinoId = "t-1", revisao = 2, emitidoEm = "2026-05-20", validoAte = null,
        profissional = ProfissionalRef("Renato", "RJE", "123-G/SP", "(11) 9"),
        aluno = AlunoRef("Maria", "ref-1"),
        treino = TreinoRef(
            "Treino A/B", "Ganhar massa",
            mapOf(
                "A" to listOf(ExercicioRef("Supino", "3", "12", "20 kg", "60 s", "devagar", null)),
                "C" to listOf(ExercicioRef("Remada")),
            ),
        ),
    )

    private val resumo = Resumo(
        resumoId = "r-1", alunoRef = "ref-1", de = "2026-05-01", ate = "2026-05-31",
        sessoes = listOf(
            SessaoRef(
                "e-1", "2026-05-03", "t-1", "A", 7,
                listOf(ItemSessao("Supino", 22.5, "12", true), ItemSessao("Remada", null, null, false)),
            ),
        ),
        mensagem = "Dor no ombro",
    )

    @Test fun pacoteRoundTrip() {
        val json = ContratoJson.serializar(pacote)
        val lido = (ContratoJson.lerPacote(json) as Leitura.Ok).valor
        assertEquals(pacote, lido)
        assertEquals("treino", ContratoJson.tipoDe(json))
    }

    @Test fun resumoRoundTrip() {
        val json = ContratoJson.serializar(resumo)
        val lido = (ContratoJson.lerResumo(json) as Leitura.Ok).valor
        assertEquals(resumo, lido)
        assertEquals("resumo", ContratoJson.tipoDe(json))
    }

    @Test fun serializacaoEhDeterministica() {
        assertEquals(ContratoJson.serializar(pacote), ContratoJson.serializar(pacote))
        assertTrue(ContratoJson.serializar(pacote).contains("\"integridade\":\"sha256:"))
    }

    @Test fun adulteracaoQuebraAIntegridade() {
        val json = ContratoJson.serializar(pacote).replace("Supino", "Agachamento")
        val r = ContratoJson.lerPacote(json) as Leitura.Erro
        assertEquals(MotivoErro.INTEGRIDADE, r.motivo)
    }

    @Test fun semIntegridadeAindaLe() {
        val base = ContratoJson.canonicoTreino(pacote)
        assertTrue(ContratoJson.lerPacote(base) is Leitura.Ok)
    }

    @Test fun versaoFuturaEhRecusada() {
        val json = ContratoJson.serializar(pacote).replace("\"formato\":1", "\"formato\":99")
        val r = ContratoJson.lerPacote(json) as Leitura.Erro
        assertEquals(MotivoErro.VERSAO_FUTURA, r.motivo)
    }

    @Test fun tipoErradoEhRecusado() {
        val r = ContratoJson.lerResumo(ContratoJson.serializar(pacote)) as Leitura.Erro
        assertEquals(MotivoErro.TIPO_INESPERADO, r.motivo)
    }

    @Test fun jsonInvalidoECamposInvalidos() {
        assertEquals(MotivoErro.JSON_INVALIDO, (ContratoJson.lerPacote("não é json") as Leitura.Erro).motivo)
        val ruim = ContratoJson.canonicoTreino(pacote.copy(emitidoEm = "20/05/2026"))
        assertEquals(MotivoErro.CAMPO_INVALIDO, (ContratoJson.lerPacote(ruim) as Leitura.Erro).motivo)
        val idRuim = ContratoJson.canonicoTreino(pacote.copy(treinoId = "tem espaço"))
        assertEquals(MotivoErro.CAMPO_INVALIDO, (ContratoJson.lerPacote(idRuim) as Leitura.Erro).motivo)
    }

    @Test fun textoComCaracteresEspeciais() {
        val p = pacote.copy(
            treino = pacote.treino.copy(
                divisoes = mapOf("A" to listOf(ExercicioRef("Rosca \"direta\"", obs = "linha1\nlinha2 \\ barra é ç"))),
            ),
        )
        val lido = (ContratoJson.lerPacote(ContratoJson.serializar(p)) as Leitura.Ok).valor
        assertEquals(p, lido)
    }

    // ── código de texto ──

    @Test fun codigoRoundTrip() {
        val json = ContratoJson.serializar(pacote)
        val codigo = CodigoTexto.codificar(json)
        assertTrue(codigo.startsWith("RJE1:"))
        assertEquals(json, (CodigoTexto.decodificar(codigo) as Leitura.Ok).valor)
    }

    @Test fun codigoDentroDeMensagemMaior() {
        val json = ContratoJson.serializar(resumo)
        val msg = "Oi prof! Segue meu resumo:\n${CodigoTexto.codificar(json)}\nObrigada!"
        assertEquals(json, (CodigoTexto.decodificar(msg) as Leitura.Ok).valor)
    }

    @Test fun codigoSemCompressao() {
        val json = ContratoJson.serializar(resumo)
        val b64 = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(json.toByteArray())
        assertEquals(json, (CodigoTexto.decodificar("RJE0:$b64") as Leitura.Ok).valor)
    }

    @Test fun codigoInvalido() {
        assertEquals(MotivoErro.CODIGO_INVALIDO, (CodigoTexto.decodificar("oi") as Leitura.Erro).motivo)
        assertTrue(CodigoTexto.decodificar("RJE1:AAAA") is Leitura.Erro)
    }

    @Test fun bombaDeDescompressaoEhBarrada() {
        val gigante = "a".repeat(2 * 1024 * 1024)
        val d = java.util.zip.Deflater(9, true)
        d.setInput(gigante.toByteArray()); d.finish()
        val buf = ByteArray(8192)
        val out = java.io.ByteArrayOutputStream()
        while (!d.finished()) out.write(buf, 0, d.deflate(buf))
        val codigo = "RJE1:" + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray())
        assertEquals(MotivoErro.TAMANHO, (CodigoTexto.decodificar(codigo) as Leitura.Erro).motivo)
    }

    // ── arquivos de exemplo compartilhados com o iOS ──

    @Test fun exemploDeTreinoEhValido() {
        val r = ContratoJson.lerPacote(recurso("treino.rjetreino"))
        assertTrue("falhou: $r", r is Leitura.Ok)
        val p = (r as Leitura.Ok).valor
        assertEquals("Hipertrofia A/B", p.treino.nome)
        assertEquals(2, p.treino.divisoes["A"]!!.size)
        assertEquals("açúcar \"extra\" & atenção", p.treino.divisoes["B"]!![0].obs)
        // re-serializar reproduz exatamente o arquivo (mesmo algoritmo canônico em todas as plataformas)
        assertEquals(recurso("treino.rjetreino"), ContratoJson.serializar(p))
    }

    @Test fun exemploDeResumoEhValido() {
        val r = ContratoJson.lerResumo(recurso("resumo.rjeresumo"))
        assertTrue("falhou: $r", r is Leitura.Ok)
        val v = (r as Leitura.Ok).valor
        assertEquals(22.5, v.sessoes[0].itens[0].cargaKg!!, 0.0)
        assertEquals(recurso("resumo.rjeresumo"), ContratoJson.serializar(v))
    }

    @Test fun codigosDeExemploDecodificamParaOsArquivos() {
        assertEquals(
            recurso("treino.rjetreino"),
            (CodigoTexto.decodificar(recurso("treino.codigo.txt")) as Leitura.Ok).valor,
        )
        assertEquals(
            recurso("resumo.rjeresumo"),
            (CodigoTexto.decodificar(recurso("resumo.codigo.txt")) as Leitura.Ok).valor,
        )
    }
}

class DescansoRulesTest {
    @Test fun formatosComuns() {
        assertEquals(60, DescansoRules.segundos("60 s"))
        assertEquals(60, DescansoRules.segundos("60s"))
        assertEquals(60, DescansoRules.segundos("1 min"))
        assertEquals(90, DescansoRules.segundos("1:30"))
        assertEquals(90, DescansoRules.segundos("90"))
        assertEquals(120, DescansoRules.segundos("2 minutos"))
    }

    @Test fun textoVagoOuAbsurdo() {
        assertNull(DescansoRules.segundos(null))
        assertNull(DescansoRules.segundos(""))
        assertNull(DescansoRules.segundos("até recuperar"))
        assertNull(DescansoRules.segundos("0"))
        assertNull(DescansoRules.segundos("99999"))
    }
}
