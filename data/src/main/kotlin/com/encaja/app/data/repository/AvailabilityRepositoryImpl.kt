package com.encaja.app.data.repository

import com.encaja.app.data.firestore.AvailabilityBlockFirestoreMapper
import com.encaja.app.data.local.AvailabilityBlockEntity
import com.encaja.app.data.local.AvailabilityDao
import com.encaja.app.domain.model.AvailabilityBlock
import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.FamilyId
import com.encaja.app.domain.model.conBloqueoDeCategorias
import com.encaja.app.domain.repository.AvailabilityRepository
import com.encaja.app.domain.repository.CategoriaRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

class AvailabilityRepositoryImpl @Inject constructor(
    private val dao: AvailabilityDao,
    private val firestore: FirebaseFirestore,
    private val categoriaRepository: CategoriaRepository
) : AvailabilityRepository {

    private fun idDocumento(caregiverId: CaregiverId, fecha: LocalDate, horaInicio: LocalTime) =
        "${caregiverId.value}_${fecha}_$horaInicio"

    private fun coleccion(familyId: FamilyId) =
        firestore.collection("families").document(familyId.value).collection("availabilityBlocks")

    /**
     * Lo que ven todas las pantallas (y el cálculo de huecos): los bloques con "bloquea"
     * ya ajustado según su categoría. Solo se consultan las categorías si hay algún
     * bloque de una categoría propia.
     */
    override suspend fun obtenerDisponibilidad(familyId: FamilyId, desde: LocalDate, hasta: LocalDate): List<AvailabilityBlock> {
        val bloques = leerBloques(familyId, desde, hasta)
        if (bloques.none { it.categoriaId != null }) return bloques
        return bloques.conBloqueoDeCategorias(categoriaRepository.obtenerCategorias(familyId))
    }

    private suspend fun leerBloques(familyId: FamilyId, desde: LocalDate, hasta: LocalDate): List<AvailabilityBlock> {
        return try {
            val snapshot = coleccion(familyId)
                .whereGreaterThanOrEqualTo("fecha", desde.toString())
                .whereLessThanOrEqualTo("fecha", hasta.toString())
                .get()
                .await()

            val bloques = snapshot.documents.mapNotNull { doc ->
                AvailabilityBlockFirestoreMapper.desdeDocumento(doc.data ?: emptyMap())
            }

            dao.guardarTodos(bloques.map { AvailabilityBlockEntity.desdeDominio(familyId.value, it) })
            bloques
        } catch (e: Exception) {
            dao.obtener(familyId.value, desde.toString(), hasta.toString()).map { it.aDominio() }
        }
    }

    override suspend fun guardarBloque(familyId: FamilyId, bloque: AvailabilityBlock) {
        val id = idDocumento(bloque.caregiverId, bloque.fecha, bloque.horaInicio)
        coleccion(familyId).document(id).set(AvailabilityBlockFirestoreMapper.aDocumento(bloque)).await()
        dao.guardar(AvailabilityBlockEntity.desdeDominio(familyId.value, bloque))
    }

    override suspend fun eliminarBloque(familyId: FamilyId, caregiverId: CaregiverId, fecha: LocalDate, horaInicio: LocalTime) {
        val id = idDocumento(caregiverId, fecha, horaInicio)
        coleccion(familyId).document(id).delete().await()
        dao.eliminar(familyId.value, caregiverId.value, fecha.toString(), horaInicio.toString())
    }
}