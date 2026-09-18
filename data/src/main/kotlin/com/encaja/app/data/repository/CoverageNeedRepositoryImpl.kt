package com.encaja.app.data.repository

import com.encaja.app.data.firestore.CoverageNeedFirestoreMapper
import com.encaja.app.data.local.CoverageNeedDao
import com.encaja.app.data.local.CoverageNeedEntity
import com.encaja.app.domain.model.CoverageNeed
import com.encaja.app.domain.model.FamilyId
import com.encaja.app.domain.repository.CoverageNeedRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import javax.inject.Inject

class CoverageNeedRepositoryImpl @Inject constructor(
    private val dao: CoverageNeedDao,
    private val firestore: FirebaseFirestore
) : CoverageNeedRepository {

    override suspend fun obtenerNeeds(familyId: FamilyId, desde: LocalDate, hasta: LocalDate): List<CoverageNeed> {
        return try {
            val snapshot = firestore
                .collection("families").document(familyId.value)
                .collection("coverageNeeds")
                .whereGreaterThanOrEqualTo("fecha", desde.toString())
                .whereLessThanOrEqualTo("fecha", hasta.toString())
                .get()
                .await()

            val needs = snapshot.documents.mapNotNull { doc ->
                CoverageNeedFirestoreMapper.desdeDocumento(doc.id, doc.data ?: emptyMap())
            }

            dao.guardarTodos(needs.map { CoverageNeedEntity.desdeDominio(familyId.value, it) })
            needs
        } catch (e: Exception) {
            dao.obtener(familyId.value, desde.toString(), hasta.toString()).map { it.aDominio() }
        }
    }

    override suspend fun guardarNeeds(familyId: FamilyId, needs: List<CoverageNeed>) {
        val coleccion = firestore.collection("families").document(familyId.value).collection("coverageNeeds")
        val batch = firestore.batch()
        needs.forEach { need ->
            batch.set(coleccion.document(need.id.value), CoverageNeedFirestoreMapper.aDocumento(need))
        }
        batch.commit().await()
        dao.guardarTodos(needs.map { CoverageNeedEntity.desdeDominio(familyId.value, it) })
    }
}