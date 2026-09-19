package com.encaja.app.data.firestore

import com.encaja.app.domain.model.ComidaDelDia
import java.time.LocalDate

/**
 * El id del documento en Firestore es la propia fecha en formato ISO-8601
 * ("2026-09-21"), igual que se hace con las anulaciones. Así no hace falta
 * guardar la fecha dentro del documento.
 */
object ComidaDelDiaFirestoreMapper {

    fun aDocumento(comida: ComidaDelDia): Map<String, Any?> = mapOf(
        "comida" to comida.comida,
        "cena" to comida.cena
    )

    fun desdeDocumento(fecha: LocalDate, datos: Map<String, Any?>): ComidaDelDia {
        return ComidaDelDia(
            fecha = fecha,
            comida = datos["comida"] as? String,
            cena = datos["cena"] as? String
        )
    }
}
