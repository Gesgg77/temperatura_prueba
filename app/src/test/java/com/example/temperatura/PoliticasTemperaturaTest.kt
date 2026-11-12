package com.example.temperatura

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PoliticasTemperaturaTest {
    @Test
    fun bloqueaAlQuintoFalloYAumentaHastaUnaHora() {
        var estado = IntentosAcceso()
        repeat(4) { indice ->
            estado = estado.registrarFallo()
            assertEquals(4 - indice, estado.restantes)
            assertEquals(0L, estado.esperaNueva)
        }
        estado = estado.registrarFallo()
        assertEquals(60_000L, estado.esperaNueva)
        assertEquals(5, estado.restantes)
        repeat(5) { estado = estado.registrarFallo() }
        assertEquals(120_000L, estado.esperaNueva)
        repeat(100) { estado = estado.registrarFallo() }
        assertEquals(3_600_000L, estado.esperaNueva)
    }

    @Test
    fun calculaEsperaConTiempoMonotonoYConReinicio() {
        assertEquals(30_000L, IntentosAcceso.restante(60_000L, 100_000L, 130_000L, true))
        assertEquals(0L, IntentosAcceso.restante(60_000L, 100_000L, 160_000L, true))
        assertEquals(60_000L, IntentosAcceso.restante(60_000L, 100_000L, 500_000L, false))
    }

    @Test
    fun rechazaMedicionesFueraDeRangoYValoresNoFinitos() {
        assertTrue(Medicion(22.4, 53.0).esValida)
        assertFalse(Medicion(Double.NaN, 53.0).esValida)
        assertFalse(Medicion(Double.POSITIVE_INFINITY, 53.0).esValida)
        assertFalse(Medicion(-300.0, 53.0).esValida)
        assertFalse(Medicion(23.0, -1.0).esValida)
        assertFalse(Medicion(23.0, 101.0).esValida)
    }
}
