package com.encaja.app.ui.semana

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SemaforoUiStateMapperTest {

    private val d = DatosEjemploFamilia
    private val mapper = SemaforoUiStateMapper(d.ninos, d.caregivers, d.disponibilidad)
    private val estado = mapper.construir(d.lunes, d.needsDeLaSemana)

    @Test
    fun `lunes queda en verde porque Silvia esta asignada a llevar y recoger`() {
        assertEquals(EstadoDia.VERDE, estado.dias[0].estado)
    }

    @Test
    fun `martes queda en rojo por el futbol de Etna sin quien la lleve`() {
        assertEquals(EstadoDia.ROJO, estado.dias[1].estado)
        assertEquals(1, estado.dias[1].huecos.size)
        assertEquals("Fútbol de Etna", estado.dias[1].huecos.first().need.descripcion)
    }

    @Test
    fun `miercoles queda en verde porque Silvia esta asignada a llevar y recoger`() {
        assertEquals(EstadoDia.VERDE, estado.dias[2].estado)
    }

    @Test
    fun `jueves queda en verde, cubierto por Josefa`() {
        assertEquals(EstadoDia.VERDE, estado.dias[3].estado)
    }

    @Test
    fun `los dias sin necesidades registradas quedan SIN_DATOS`() {
        assertEquals(EstadoDia.SIN_DATOS, estado.dias[4].estado) // viernes, sin needs de ejemplo
        assertEquals(EstadoDia.SIN_DATOS, estado.dias[5].estado) // sábado
        assertEquals(EstadoDia.SIN_DATOS, estado.dias[6].estado) // domingo
    }

    @Test
    fun `hay exactamente un hueco en toda la semana`() {
        assertEquals(1, estado.huecosDeLaSemana.size)
    }
}
