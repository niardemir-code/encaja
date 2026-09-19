package com.encaja.app.domain.repository

import com.encaja.app.domain.model.Anuncio
import com.encaja.app.domain.model.AnuncioId
import com.encaja.app.domain.model.FamilyId

interface AnuncioRepository {
    /** Del más reciente al más antiguo. */
    suspend fun obtenerAnuncios(familyId: FamilyId): List<Anuncio>

    /** Genera el id y la fecha de publicación; devuelve el anuncio ya creado. */
    suspend fun publicarAnuncio(familyId: FamilyId, autorNombre: String, texto: String): Anuncio

    /** Cualquier miembro de la familia puede borrar un anuncio, lo haya escrito quien lo haya escrito. */
    suspend fun eliminarAnuncio(familyId: FamilyId, anuncioId: AnuncioId)
}
