package com.encaja.app.data.firestore

import com.encaja.app.domain.model.AvailabilityBlock
import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.CategoriaId
import com.encaja.app.domain.model.MotivoNoDisponibilidad
import java.time.LocalDate
import java.time.LocalTime

object AvailabilityBlockFirestoreMapper {

    fun aDocumento(bloque: AvailabilityBlock): Map<String, Any?> = mapOf(
        "caregiverId" to bloque.caregiverId.value,
        "fecha" to bloque.fecha.toString(),
        "horaInicio" to bloque.horaInicio.toString(),
        "horaFin" to bloque.horaFin.toString(),
        "motivo" to bloque.motivo.name,
        "etiqueta" to bloque.etiqueta,
        "categoriaId" to bloque.categoriaId?.value
    )

    fun desdeDocumento(datos: Map<String, Any?>): AvailabilityBlock? {
        val caregiverId = (datos["caregiverId"] as? String)?.let { CaregiverId(it) } ?: return null
        val fecha = (datos["fecha"] as? String)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
        val horaInicio = (datos["horaInicio"] as? String)?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: return null
        val horaFin = (datos["horaFin"] as? String)?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: return null
        val motivoTexto = datos["motivo"] as? String ?: return null
        val motivo = runCatching { MotivoNoDisponibilidad.valueOf(motivoTexto) }.getOrNull() ?: return null
        val etiqueta = datos["etiqueta"] as? String
        val categoriaId = (datos["categoriaId"] as? String)?.let { CategoriaId(it) }

        return AvailabilityBlock(caregiverId, fecha, horaInicio, horaFin, motivo, etiqueta, categoriaId)
    }
}