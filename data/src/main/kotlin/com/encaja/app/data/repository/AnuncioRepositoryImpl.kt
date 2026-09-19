package com.encaja.app.data.repository

import com.encaja.app.data.firestore.AnuncioFirestoreMapper
import com.encaja.app.data.local.AnuncioDao
import com.encaja.app.data.local.AnuncioEntity
import com.encaja.app.domain.model.Anuncio
import com.encaja.app.domain.model.AnuncioId
import com.encaja.app.domain.model.FamilyId
import com.encaja.app.domain.repository.AnuncioRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDateTime
import javax.inject.Inject

class AnuncioRepositoryImpl @Inject constructor(
    private val dao: AnuncioDao,
    private val firestore: FirebaseFirestore
) : AnuncioRepository {

    private fun coleccion(familyId: FamilyId) =
        firestore.collection("families").document(familyId.value).collection("anuncios")

    override suspend fun obtenerAnuncios(familyId: FamilyId): List<Anuncio> {
        return try {
            val snapshot = coleccion(familyId).get().await()
            val anuncios = snapshot.documents.mapNotNull { doc ->
                AnuncioFirestoreMapper.desdeDocumento(doc.id, doc.data ?: emptyMap())
            }
            dao.guardarTodos(anuncios.map { AnuncioEntity.desdeDominio(familyId.value, it) })
            anuncios.sortedByDescending { it.publicadoEn }
        } catch (e: Exception) {
            dao.obtener(familyId.value).map { it.aDominio() }.sortedByDescending { it.publicadoEn }
        }
    }

    override suspend fun publicarAnuncio(familyId: FamilyId, autorNombre: String, texto: String): Anuncio {
        val documento = coleccion(familyId).document()
        val anuncio = Anuncio(AnuncioId(documento.id), autorNombre, texto, LocalDateTime.now())
        documento.set(AnuncioFirestoreMapper.aDocumento(anuncio)).await()
        dao.guardar(AnuncioEntity.desdeDominio(familyId.value, anuncio))
        return anuncio
    }

    override suspend fun eliminarAnuncio(familyId: FamilyId, anuncioId: AnuncioId) {
        coleccion(familyId).document(anuncioId.value).delete().await()
        dao.eliminar(familyId.value, anuncioId.value)
    }
}
