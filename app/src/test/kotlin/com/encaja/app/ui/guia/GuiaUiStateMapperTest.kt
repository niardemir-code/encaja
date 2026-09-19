package com.encaja.app.ui.guia

import com.encaja.app.ui.semana.DatosEjemploFamilia
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GuiaUiStateMapperTest {

    private val d = DatosEjemploFamilia
    private val mapper = GuiaUiStateMapper(d.ninos, d.caregivers, d.patrones, d.anulaciones, d.disponibilidad)

    @Test
    fun `el martes el bloque de futbol de Etna sale sin cubrir`() {
        val martes = d.lunes.plusDays(1)
        val needsDelDia = d.needsDeLaSemana.filter { it.fecha == martes }

        val estado = mapper.construir(martes, needsDelDia)
        val filaEtna = estado.filas.first { it.child.id == d.etna }

        assertEquals(1, filaEtna.bloques.size)
        assertFalse(filaEtna.bloques.first().cubierto)
    }

    @Test
    fun `el lunes el bloque de Julia sale cubierto por Silvia`() {
        val lunes = d.lunes
        val needsDelDia = d.needsDeLaSemana.filter { it.fecha == lunes }

        val estado = mapper.construir(lunes, needsDelDia)
        val filaJulia = estado.filas.first { it.child.id == d.julia }

        assertEquals(1, filaJulia.bloques.size)
        assertTrue(filaJulia.bloques.first().cubierto)
        assertEquals("Sílvia Izquierdo", filaJulia.bloques.first().cuidadorAsignado)
    }

    @Test
    fun `un dia sin needs deja a cada nino con la fila vacia, no se omite`() {
        val viernes = d.lunes.plusDays(4)
        val estado = mapper.construir(viernes, emptyList())

        assertEquals(d.ninos.size, estado.filas.size)
        assertTrue(estado.filas.all { it.bloques.isEmpty() })
    }

    @Test
    fun `los bloques del dia salen ordenados por hora de inicio`() {
        // Miércoles solo tiene un need de ejemplo, pero comprobamos el orden
        // igualmente por si en el futuro hay más de uno el mismo día.
        val miercoles = d.lunes.plusDays(2)
        val needsDelDia = d.needsDeLaSemana.filter { it.fecha == miercoles }
        val estado = mapper.construir(miercoles, needsDelDia)

        val filaEtna = estado.filas.first { it.child.id == d.etna }
        val horas = filaEtna.bloques.map { it.need.horaInicio }
        assertEquals(horas.sorted(), horas)
    }
}
