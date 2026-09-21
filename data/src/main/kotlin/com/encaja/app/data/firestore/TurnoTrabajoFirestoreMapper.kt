package com.encaja.app.data.firestore

import com.encaja.app.domain.model.TurnoId
import com.encaja.app.domain.model.TurnoTrabajo
import java.time.LocalTime

/** Convierte entre TurnoTrabajo y el mapa de campos de Firestore. Kotlin puro. */
object TurnoTrabajoFirestoreMapper {

    fun aDocumento(turno: TurnoTrabajo): Map<String, Any?> = mapOf(
        "nombre" to turno.nombre,
        "horaInicio" to turno.horaInicio.toString(),
        "horaFin" to turno.horaFin.toString()
    )

    fun desdeDocumento(id: String, datos: Map<String, Any?>): TurnoTrabajo? {
        val nombre = datos["nombre"] as? String ?: return null
        val inicio = (datos["horaInicio"] as? String)?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: return null
        val fin = (datos["horaFin"] as? String)?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: return null
        return TurnoTrabajo(TurnoId(id), nombre, inicio, fin)
    }
}
