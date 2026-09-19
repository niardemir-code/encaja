package com.encaja.app.data.firestore

import com.encaja.app.domain.model.Anuncio
import com.encaja.app.domain.model.AnuncioId
import java.time.LocalDateTime

object AnuncioFirestoreMapper {

    fun aDocumento(anuncio: Anuncio): Map<String, Any?> = mapOf(
        "autorNombre" to anuncio.autorNombre,
        "texto" to anuncio.texto,
        "publicadoEn" to anuncio.publicadoEn.toString()
    )

    fun desdeDocumento(id: String, datos: Map<String, Any?>): Anuncio? {
        val autorNombre = datos["autorNombre"] as? String ?: return null
        val texto = datos["texto"] as? String ?: return null
        val publicadoEnTexto = datos["publicadoEn"] as? String ?: return null
        val publicadoEn = try {
            LocalDateTime.parse(publicadoEnTexto)
        } catch (e: Exception) {
            return null
        }
        return Anuncio(AnuncioId(id), autorNombre, texto, publicadoEn)
    }
}
