package com.encaja.app.data.firestore

import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.PatronCuidado
import java.time.DayOfWeek

object PatronCuidadoFirestoreMapper {

    fun aDocumento(patron: PatronCuidado): Map<String, Any?> = mapOf(
        "diaSemana" to patron.diaSemana.name, // "MONDAY", "TUESDAY"...
        "caregiverId" to patron.caregiverId.value
    )

    fun desdeDocumento(datos: Map<String, Any?>): PatronCuidado? {
        val diaTexto = datos["diaSemana"] as? String ?: return null
        val dia = runCatching { DayOfWeek.valueOf(diaTexto) }.getOrNull() ?: return null
        val caregiverId = (datos["caregiverId"] as? String)?.let { CaregiverId(it) } ?: return null

        return PatronCuidado(dia, caregiverId)
    }
}