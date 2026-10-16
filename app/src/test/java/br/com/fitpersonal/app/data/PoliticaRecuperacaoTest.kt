package br.com.fitpersonal.app.data

import br.com.fitpersonal.app.data.local.AcaoBanco
import br.com.fitpersonal.app.data.local.EstadoBanco
import br.com.fitpersonal.app.data.local.InfoRecuperacao
import br.com.fitpersonal.app.data.local.PoliticaRecuperacao
import br.com.fitpersonal.app.data.local.RegistroRecuperacao
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class PoliticaRecuperacaoTest {

    private fun decidir(existe: Boolean, chave: Boolean, puro: Boolean = false, abre: Boolean = false) =
        PoliticaRecuperacao.decidir(EstadoBanco(existe, chave, puro, abre))

    @Test fun primeiraExecucao() {
        assertEquals(AcaoBanco.NADA, decidir(existe = false, chave = true))
    }

    @Test fun semBancoMasSegredoIlegivelGeraChaveNova() {
        assertEquals(AcaoBanco.REDEFINIR_CHAVE, decidir(existe = false, chave = false))
    }

    @Test fun bancoNormalAbre() {
        assertEquals(AcaoBanco.NADA, decidir(existe = true, chave = true, abre = true))
    }

    @Test fun bancoAntigoSemCriptografiaEMigrado() {
        assertEquals(AcaoBanco.CRIPTOGRAFAR, decidir(existe = true, chave = true, puro = true))
    }

    @Test fun chavePerdidaComBancoCifradoMoveEdRecomeca() {
        assertEquals(AcaoBanco.REDEFINIR_E_MOVER_E_RECOMECAR, decidir(existe = true, chave = false))
    }

    @Test fun chavePerdidaMasBancoEraTextoPuroNaoPerdeDados() {
        assertEquals(AcaoBanco.REDEFINIR_E_CRIPTOGRAFAR, decidir(existe = true, chave = false, puro = true))
    }

    @Test fun bancoCifradoQueNaoAbreComAChaveAtual() {
        assertEquals(AcaoBanco.MOVER_E_RECOMECAR, decidir(existe = true, chave = true, abre = false))
    }

    @Test fun registroIdaEVolta() {
        val dir = Files.createTempDirectory("rje-rec").toFile()
        try {
            assertNull(RegistroRecuperacao.ler(dir))
            RegistroRecuperacao.gravar(dir, InfoRecuperacao(1234L, "chave perdida\nlinha 2"))
            val lido = RegistroRecuperacao.ler(dir)!!
            assertEquals(1234L, lido.emMillis)
            assertEquals("chave perdida\nlinha 2", lido.motivo)
            RegistroRecuperacao.limpar(dir)
            assertNull(RegistroRecuperacao.ler(dir))
        } finally {
            dir.deleteRecursively()
        }
    }
}
