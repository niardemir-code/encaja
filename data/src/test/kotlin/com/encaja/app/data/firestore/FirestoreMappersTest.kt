package com.encaja.app.data.firestore

import com.encaja.app.domain.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class FirestoreMappersTest {

    @Test
    fun `Caregiver hace el viaje completo sin perder datos`() {
        val original = Caregiver(CaregiverId("dolors"), "Dolors Vila", CaregiverRole.CUIDADOR, puedeDesplazarse = false)

        val documento = CaregiverFirestoreMapper.aDocumento(original)
        val reconstruido = CaregiverFirestoreMapper.desdeDocumento(original.id.value, documento)

        assertEquals(original, reconstruido)
    }

    @Test
    fun `Caregiver sin puedeDesplazarse en el documento asume true por defecto`() {
        val documentoViejo = mapOf("nombre" to "Víctor", "rol" to "ADMIN")
        val reconstruido = CaregiverFirestoreMapper.desdeDocumento("victor", documentoViejo)

        assertEquals(true, reconstruido?.puedeDesplazarse)
    }

    @Test
    fun `Caregiver con rol desconocido en el documento no revienta, devuelve null`() {
        val documentoCorrupto = mapOf("nombre" to "X", "rol" to "ROL_QUE_NO_EXISTE")
        assertNull(CaregiverFirestoreMapper.desdeDocumento("x", documentoCorrupto))
    }

    @Test
    fun `CoverageNeed hace el viaje completo sin perder datos`() {
        val original = CoverageNeed(
            CoverageNeedId("futbol"), ChildId("etna"), LocalDate.of(2026, 9, 15),
            LocalTime.of(18, 30), LocalTime.of(20, 0), "Fútbol de Etna", requiereDesplazamiento = true
        )

        val documento = CoverageNeedFirestoreMapper.aDocumento(original)
        val reconstruido = CoverageNeedFirestoreMapper.desdeDocumento(original.id.value, documento)

        assertEquals(original, reconstruido)
    }

    @Test
    fun `AvailabilityBlock hace el viaje completo, incluida la etiqueta libre`() {
        val original = AvailabilityBlock(
            CaregiverId("josefa"), LocalDate.of(2026, 9, 15),
            LocalTime.of(18, 0), LocalTime.of(19, 0),
            MotivoNoDisponibilidad.MEDICO, etiqueta = "Revisión rodilla"
        )

        val documento = AvailabilityBlockFirestoreMapper.aDocumento(original)
        val reconstruido = AvailabilityBlockFirestoreMapper.desdeDocumento(documento)

        assertEquals(original, reconstruido)
    }

    @Test
    fun `AvailabilityBlock sin etiqueta viaja bien con etiqueta null`() {
        val original = AvailabilityBlock(
            CaregiverId("victor"), LocalDate.of(2026, 9, 15),
            LocalTime.of(14, 0), LocalTime.of(22, 0), MotivoNoDisponibilidad.TRABAJO
        )

        val documento = AvailabilityBlockFirestoreMapper.aDocumento(original)
        val reconstruido = AvailabilityBlockFirestoreMapper.desdeDocumento(documento)

        assertEquals(original, reconstruido)
        assertNull(reconstruido?.etiqueta)
    }

    @Test
    fun `PatronCuidado hace el viaje completo sin perder datos`() {
        val original = PatronCuidado(DayOfWeek.TUESDAY, CaregiverId("josefa"))

        val documento = PatronCuidadoFirestoreMapper.aDocumento(original)
        val reconstruido = PatronCuidadoFirestoreMapper.desdeDocumento(documento)

        assertEquals(original, reconstruido)
    }

    @Test
    fun `documento con un campo que falta devuelve null en vez de reventar`() {
        val documentoIncompleto = mapOf("childId" to "etna")
        assertNull(CoverageNeedFirestoreMapper.desdeDocumento("x", documentoIncompleto))
    }
}