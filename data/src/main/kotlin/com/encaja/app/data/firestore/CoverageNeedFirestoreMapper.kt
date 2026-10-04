package com.encaja.app.data.firestore

import com.encaja.app.domain.model.ChildId
import com.encaja.app.domain.model.CoverageNeed
import com.encaja.app.domain.model.CoverageNeedId
import java.time.LocalDate
import java.time.LocalTime

object CoverageNeedFirestoreMapper {

    fun aDocumento(need: CoverageNeed): Map<String, Any?> = mapOf(
        "childId" to need.childId.value,
        "fecha" to need.fecha.toString(),          // ISO-8601: "2026-09-15"
        "horaInicio" to need.horaInicio.toString(), // "16:30"
        "horaFin" to need.horaFin.toString(),
        "descripcion" to need.descripcion,
        "requiereDesplazamiento" to need.requiereDesplazamiento,
        "quienLlevaId" to need.quienLlevaId,
        "quienRecogeId" to need.quienRecogeId,
        "grupoRepeticionId" to need.grupoRepeticionId,
        "avisoLlevarMin" to need.avisoLlevarMin,
        "avisoRecogerMin" to need.avisoRecogerMin,
        "icono" to need.icono,
        "creadoPorId" to need.creadoPorId
    )

    fun desdeDocumento(id: String, datos: Map<String, Any?>): CoverageNeed? {
        val childId = (datos["childId"] as? String)?.let { ChildId(it) } ?: return null
        val fecha = (datos["fecha"] as? String)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
        val horaInicio = (datos["horaInicio"] as? String)?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: return null
        val horaFin = (datos["horaFin"] as? String)?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: return null
        val descripcion = datos["descripcion"] as? String ?: return null
        val requiereDesplazamiento = datos["requiereDesplazamiento"] as? Boolean ?: true
        val quienLlevaId = datos["quienLlevaId"] as? String
        val quienRecogeId = datos["quienRecogeId"] as? String
        val grupoRepeticionId = datos["grupoRepeticionId"] as? String
        // Firestore devuelve los números enteros como Long.
        val avisoLlevarMin = (datos["avisoLlevarMin"] as? Number)?.toInt()
        val avisoRecogerMin = (datos["avisoRecogerMin"] as? Number)?.toInt()
        val icono = datos["icono"] as? String
        val creadoPorId = datos["creadoPorId"] as? String

        return CoverageNeed(
            CoverageNeedId(id), childId, fecha, horaInicio, horaFin, descripcion, requiereDesplazamiento,
            quienLlevaId, quienRecogeId, grupoRepeticionId, avisoLlevarMin, avisoRecogerMin, icono, creadoPorId
        )
    }
}