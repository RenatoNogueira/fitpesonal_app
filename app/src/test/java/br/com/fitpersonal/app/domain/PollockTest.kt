package br.com.fitpersonal.app.domain

import br.com.fitpersonal.app.domain.model.Dobra
import br.com.fitpersonal.app.domain.model.ProtocoloDobras
import br.com.fitpersonal.app.domain.model.SexoBiologico
import br.com.fitpersonal.app.domain.rules.AnalisePollock
import br.com.fitpersonal.app.domain.rules.Pollock
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class PollockTest {
    private val data = LocalDate.of(2026, 10, 14)
    private val nasc30 = LocalDate.of(1996, 10, 14)   // 30 anos

    @Test fun dobrasPorProtocoloESexo() {
        assertEquals(
            listOf(Dobra.PEITORAL, Dobra.ABDOMINAL, Dobra.COXA),
            Pollock.dobrasNecessarias(ProtocoloDobras.POLLOCK_3, SexoBiologico.MASCULINO),
        )
        assertEquals(
            listOf(Dobra.TRICEPS, Dobra.SUPRAILIACA, Dobra.COXA),
            Pollock.dobrasNecessarias(ProtocoloDobras.POLLOCK_3, SexoBiologico.FEMININO),
        )
        assertEquals(7, Pollock.dobrasNecessarias(ProtocoloDobras.POLLOCK_7, SexoBiologico.FEMININO).size)
    }

    @Test fun tresDobrasHomem() {
        // soma 60 mm, 30 anos: DC ≈ 1,0578 → %G ≈ 17,9
        val r = Pollock.analisar(
            ProtocoloDobras.POLLOCK_3,
            mapOf(Dobra.PEITORAL to 15.0, Dobra.ABDOMINAL to 25.0, Dobra.COXA to 20.0),
            SexoBiologico.MASCULINO, nasc30, data,
        ) as AnalisePollock.Calculada
        assertEquals(1.0578, r.resultado.densidadeCorporal, 0.0005)
        assertEquals(17.9, r.resultado.percentualGordura, 0.1)
        assertEquals(60.0, r.resultado.somaDobrasMm, 0.0)
        assertEquals(30, r.resultado.idade)
        assertTrue(r.avisos.isEmpty())
    }

    @Test fun seteDobrasMulher() {
        // soma 100 mm, 25 anos: DC ≈ 1,0524 → %G ≈ 20,3
        val nasc25 = LocalDate.of(2001, 10, 14)
        val dobras = mapOf(
            Dobra.PEITORAL to 10.0, Dobra.AXILAR_MEDIA to 12.0, Dobra.TRICEPS to 18.0, Dobra.SUBESCAPULAR to 15.0,
            Dobra.ABDOMINAL to 20.0, Dobra.SUPRAILIACA to 12.0, Dobra.COXA to 13.0,
        )
        val r = Pollock.analisar(ProtocoloDobras.POLLOCK_7, dobras, SexoBiologico.FEMININO, nasc25, data)
            as AnalisePollock.Calculada
        assertEquals(1.0524, r.resultado.densidadeCorporal, 0.0005)
        assertEquals(20.3, r.resultado.percentualGordura, 0.1)
    }

    @Test fun pendenteQuandoFaltaDado() {
        val semSexo = Pollock.analisar(ProtocoloDobras.POLLOCK_3, emptyMap(), null, nasc30, data)
        assertTrue(semSexo is AnalisePollock.Pendente)
        val semNasc = Pollock.analisar(ProtocoloDobras.POLLOCK_3, emptyMap(), SexoBiologico.MASCULINO, null, data)
        assertTrue(semNasc is AnalisePollock.Pendente)
        val faltando = Pollock.analisar(
            ProtocoloDobras.POLLOCK_3, mapOf(Dobra.PEITORAL to 10.0), SexoBiologico.MASCULINO, nasc30, data,
        ) as AnalisePollock.Pendente
        assertTrue("Abdominal" in faltando.motivo && "Coxa" in faltando.motivo)
    }

    @Test fun avisaIdadeForaDaFaixa() {
        val nasc70 = LocalDate.of(1956, 1, 1)
        val r = Pollock.analisar(
            ProtocoloDobras.POLLOCK_3,
            mapOf(Dobra.PEITORAL to 15.0, Dobra.ABDOMINAL to 25.0, Dobra.COXA to 20.0),
            SexoBiologico.MASCULINO, nasc70, data,
        ) as AnalisePollock.Calculada
        assertTrue(r.avisos.any { it.contains("faixa") })
    }

    @Test fun sexoDeTexto() {
        assertEquals(SexoBiologico.FEMININO, SexoBiologico.deTexto("Feminino"))
        assertNull(SexoBiologico.deTexto("Outro"))
        assertNull(SexoBiologico.deTexto(null))
    }
}
