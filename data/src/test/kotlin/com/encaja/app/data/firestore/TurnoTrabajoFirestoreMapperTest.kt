package com.encaja.app.data.firestore

import com.encaja.app.domain.model.TurnoId
import com.encaja.app.domain.model.TurnoTrabajo
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalTime

class TurnoTrabajoFirestoreMapperTest {

    @Test
    fun `TurnoTrabajo hace el viaje completo sin perder datos`() {
        val original = TurnoTrabajo(TurnoId("t1"), "Noche", LocalTime.of(22, 0), LocalTime.of(6, 0))
        assertEquals(original, TurnoTrabajoFirestoreMapper.desdeDocumento("t1", TurnoTrabajoFirestoreMapper.aDocumento(original)))
    }

    @Test
    fun `documento con hora mal formada no revienta, devuelve null`() {
        val corrupto = mapOf("nombre" to "X", "horaInicio" to "25:99", "horaFin" to "06:00")
        assertNull(TurnoTrabajoFirestoreMapper.desdeDocumento("x", corrupto))
    }
}
