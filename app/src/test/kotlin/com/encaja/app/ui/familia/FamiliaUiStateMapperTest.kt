package com.encaja.app.ui.familia

import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.MotivoNoDisponibilidad
import com.encaja.app.ui.semana.DatosEjemploFamilia
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FamiliaUiStateMapperTest {

    private val d = DatosEjemploFamilia
    private val mapper = FamiliaUiStateMapper(d.caregivers, d.patrones, d.anulaciones, d.disponibilidad)
    private val estado = mapper.construir(d.lunes)

    @Test
    fun `la fila de asignacion tiene los 7 dias de la semana, empezando el lunes`() {
        assertEquals(7, estado.diasAsignacion.size)
        assertEquals(d.lunes, estado.diasAsignacion.first().fecha)
    }

    @Test
    fun `el lunes esta asignado a Silvia segun el patron`() {
        assertEquals(CaregiverId("silvia"), estado.diasAsignacion[0].caregiverId)
    }

    @Test
    fun `el miercoles esta asignado a Silvia por la anulacion, no por el patron`() {
        assertEquals(CaregiverId("silvia"), estado.diasAsignacion[2].caregiverId)
    }

    @Test
    fun `el sabado no tiene patron ni anulacion, queda sin asignar`() {
        assertNull(estado.diasAsignacion[5].caregiverId)
    }

    @Test
    fun `hay una fila por cada cuidador de la familia`() {
        assertEquals(d.caregivers.size, estado.cuidadores.size)
    }

    @Test
    fun `Victor esta ocupado de lunes a jueves por trabajo, libre el resto de la semana`() {
        val victor = estado.cuidadores.first { it.caregiver.id == CaregiverId("victor") }
        assertFalse(victor.dias[0].libre) // lunes
        assertFalse(victor.dias[3].libre) // jueves
        assertTrue(victor.dias[4].libre)  // viernes
        assertTrue(victor.dias[6].libre)  // domingo
    }

    @Test
    fun `Josefa tiene un bloqueo de motivo MEDICO justo el martes`() {
        val josefa = estado.cuidadores.first { it.caregiver.id == CaregiverId("josefa") }
        val martes = josefa.dias[1]
        assertFalse(martes.libre)
        assertEquals(MotivoNoDisponibilidad.MEDICO, martes.bloqueos.first().motivo)
        assertTrue(josefa.dias[0].libre) // lunes, sin bloqueo
    }

    @Test
    fun `Dolors no tiene ningun bloqueo, esta libre toda la semana`() {
        val dolors = estado.cuidadores.first { it.caregiver.id == CaregiverId("dolors") }
        assertTrue(dolors.dias.all { it.libre })
    }
}
