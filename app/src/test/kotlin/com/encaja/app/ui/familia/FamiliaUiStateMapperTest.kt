package com.encaja.app.ui.familia

import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.MotivoNoDisponibilidad
import com.encaja.app.ui.semana.DatosEjemploFamilia
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FamiliaUiStateMapperTest {

    private val d = DatosEjemploFamilia
    private val mapper = FamiliaUiStateMapper(d.caregivers, d.disponibilidad)
    private val estado = mapper.construir(d.lunes)

    @Test
    fun `hay una fila por cada cuidador de la familia`() {
        assertEquals(d.caregivers.size, estado.cuidadores.size)
    }

    @Test
    fun `cada cuidador tiene los 7 dias de la semana, empezando el lunes`() {
        val primero = estado.cuidadores.first()
        assertEquals(7, primero.dias.size)
        assertEquals(d.lunes, primero.dias.first().fecha)
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
