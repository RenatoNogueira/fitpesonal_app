package br.com.fitpersonal.app.domain

import br.com.fitpersonal.app.domain.model.AvaliacaoFisica
import br.com.fitpersonal.app.domain.model.SexoBiologico
import br.com.fitpersonal.app.domain.rules.ClasseRcq
import br.com.fitpersonal.app.domain.rules.EvolucaoRules
import br.com.fitpersonal.app.domain.rules.Metrica
import br.com.fitpersonal.app.domain.rules.SentidoVariacao
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class EvolucaoRulesTest {

    private fun av(id: Long, d: LocalDate, peso: Double? = null, gordura: Double? = null, cintura: Double? = null, quadril: Double? = null) =
        AvaliacaoFisica(
            id = id, idAluno = 1, data = d, pesoKg = peso, alturaM = 1.70,
            percentualGordura = gordura, cinturaCm = cintura, quadrilCm = quadril,
        )

    private val jan = LocalDate.of(2026, 1, 10)
    private val fev = LocalDate.of(2026, 2, 10)
    private val mar = LocalDate.of(2026, 3, 10)

    @Test fun serieEmOrdemCronologicaSoComValores() {
        val lista = listOf(av(3, mar, peso = 78.0), av(1, jan, peso = 82.0), av(2, fev, peso = null, gordura = 20.0))
        val s = EvolucaoRules.serie(lista, Metrica.PESO)
        assertEquals(listOf(jan, mar), s.map { it.data })
        assertEquals(listOf(82.0, 78.0), s.map { it.valor })
        assertEquals(1, EvolucaoRules.serie(lista, Metrica.PERCENTUAL_GORDURA).size)
    }

    @Test fun imcVemDePesoEAltura() {
        val s = EvolucaoRules.serie(listOf(av(1, jan, peso = 72.25)), Metrica.IMC)
        assertEquals(25.0, s.single().valor, 0.01)   // 72,25 / 1,70²
    }

    @Test fun variacaoComSentido() {
        val gordura = EvolucaoRules.variacao(Metrica.PERCENTUAL_GORDURA, 25.0, 22.0)
        assertEquals(-3.0, gordura.absoluta, 0.0001)
        assertEquals(-12.0, gordura.percentual!!, 0.0001)
        assertEquals(SentidoVariacao.MELHOROU, gordura.sentido)

        assertEquals(SentidoVariacao.PIOROU, EvolucaoRules.variacao(Metrica.CINTURA, 80.0, 84.0).sentido)
        assertEquals(SentidoVariacao.MELHOROU, EvolucaoRules.variacao(Metrica.MASSA_MAGRA, 55.0, 56.0).sentido)
        // peso não recebe juízo: depende do objetivo
        assertEquals(SentidoVariacao.NEUTRO, EvolucaoRules.variacao(Metrica.PESO, 80.0, 75.0).sentido)
        // diferença abaixo da precisão exibida
        assertEquals(SentidoVariacao.IGUAL, EvolucaoRules.variacao(Metrica.PESO, 80.00, 80.01).sentido)
    }

    @Test fun comparativoCobreSoMetricasPresentes() {
        val antes = av(1, jan, peso = 82.0, gordura = 25.0)
        val depois = av(2, mar, peso = 78.0, gordura = 22.0, cintura = 80.0)
        val linhas = EvolucaoRules.comparar(antes, depois).associateBy { it.metrica }

        assertEquals(-4.0, linhas.getValue(Metrica.PESO).variacao!!.absoluta, 0.0001)
        assertNotNull(linhas[Metrica.IMC])
        // cintura só existe na segunda: aparece sem variação
        assertNull(linhas.getValue(Metrica.CINTURA).antes)
        assertNull(linhas.getValue(Metrica.CINTURA).variacao)
        assertNull(linhas[Metrica.MASSA_MAGRA])
    }

    @Test fun rcqEClassificacaoPorSexo() {
        assertNull(EvolucaoRules.rcq(null, 100.0))
        assertNull(EvolucaoRules.rcq(80.0, 0.0))
        val r = EvolucaoRules.rcq(90.0, 100.0)!!
        assertEquals(0.90, r, 0.0001)
        assertEquals(ClasseRcq.ELEVADO, EvolucaoRules.classificarRcq(r, SexoBiologico.MASCULINO))
        assertEquals(ClasseRcq.ELEVADO, EvolucaoRules.classificarRcq(0.86, SexoBiologico.FEMININO))
        assertEquals(ClasseRcq.ADEQUADO, EvolucaoRules.classificarRcq(0.84, SexoBiologico.FEMININO))
        assertNull(EvolucaoRules.classificarRcq(0.95, null))
    }

    @Test fun deltaFormatadoEmPortugues() {
        val v = EvolucaoRules.variacao(Metrica.PESO, 80.0, 78.8)
        assertEquals("-1,2 kg", EvolucaoRules.formatarDelta(v, Metrica.PESO))
        val g = EvolucaoRules.variacao(Metrica.PERCENTUAL_GORDURA, 20.0, 21.5)
        assertEquals("+1,5 %", EvolucaoRules.formatarDelta(g, Metrica.PERCENTUAL_GORDURA))
    }
}
