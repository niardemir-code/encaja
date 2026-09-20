package com.encaja.app.ui.familia

import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.MotivoNoDisponibilidad
import com.encaja.app.domain.model.PatronCuidado
import com.encaja.app.ui.semana.DatosEjemploFamilia
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.DayOfWeek

class FamiliaUiStateMapperTest {

    private val d = DatosEjemploFamilia
    private val mapper = FamiliaUiStateMapper(d.caregivers, d.unidades, d.patrones, d.anulaciones, d.disponibilidad)
    private val estado = mapper.construir(d.lunes)

    private fun personaIdDe(responsable: Responsable?): CaregiverId? =
        (responsable as? Responsable.Persona)?.caregiver?.id

    @Test
    fun `la fila de asignacion tiene los 7 dias de la semana, empezando el lunes`() {
        assertEquals(7, estado.diasAsignacion.size)
        assertEquals(d.lunes, estado.diasAsignacion.first().fecha)
    }

    @Test
    fun `el lunes esta asignado a Silvia segun el patron`() {
        assertEquals(CaregiverId("silvia"), personaIdDe(estado.diasAsignacion[0].responsable))
        assertFalse(estado.diasAsignacion[0].esCambioPuntual)
    }

    @Test
    fun `el miercoles esta asignado a Silvia por la anulacion, no por el patron`() {
        assertEquals(CaregiverId("silvia"), personaIdDe(estado.diasAsignacion[2].responsable))
        assertTrue(estado.diasAsignacion[2].esCambioPuntual)
    }

    @Test
    fun `el sabado no tiene patron ni anulacion, queda sin asignar`() {
        assertNull(estado.diasAsignacion[5].responsable)
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

    @Test
    fun `el patron semanal refleja quien es responsable cada dia de la semana`() {
        assertEquals(CaregiverId("silvia"), personaIdDe(estado.patronSemanal[DayOfWeek.MONDAY]))
        assertEquals(CaregiverId("josefa"), personaIdDe(estado.patronSemanal[DayOfWeek.TUESDAY]))
        assertNull(estado.patronSemanal[DayOfWeek.SATURDAY])
    }

    @Test
    fun `una unidad familiar asignada a un dia se resuelve como Responsable Unidad`() {
        val patronesConUnidad = listOf(PatronCuidado(DayOfWeek.SATURDAY, d.abuelosMaternos.id.let { CaregiverId(it.value) }))
        val mapperConUnidad = FamiliaUiStateMapper(d.caregivers, d.unidades, patronesConUnidad, emptyMap(), d.disponibilidad)
        val estadoConUnidad = mapperConUnidad.construir(d.lunes)

        val sabado = estadoConUnidad.diasAsignacion[5].responsable
        assertTrue(sabado is Responsable.Unidad)
        assertEquals("GF", (sabado as Responsable.Unidad).unidad.codigo)
        assertEquals(d.abuelosMaternos, estadoConUnidad.patronSemanal[DayOfWeek.SATURDAY]?.let { (it as Responsable.Unidad).unidad })
    }

    @Test
    fun `opcionesAsignables incluye tanto cuidadores como unidades`() {
        assertEquals(d.caregivers.size + d.unidades.size, estado.opcionesAsignables.size)
    }
}
