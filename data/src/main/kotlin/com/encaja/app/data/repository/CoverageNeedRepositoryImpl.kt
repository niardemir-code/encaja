package com.encaja.app.data.repository

import com.encaja.app.data.firestore.CoverageNeedFirestoreMapper
import com.encaja.app.data.local.CoverageNeedDao
import com.encaja.app.data.local.CoverageNeedEntity
import com.encaja.app.domain.model.CoverageNeed
import com.encaja.app.domain.model.CoverageNeedId
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

    override suspend fun eliminarNeed(familyId: FamilyId, needId: CoverageNeedId) {
        firestore.collection("families").document(familyId.value)
            .collection("coverageNeeds").document(needId.value)
            .delete()
            .await()
        dao.eliminar(familyId.value, needId.value)
    }

    override suspend fun eliminarNeeds(familyId: FamilyId, needIds: List<CoverageNeedId>) {
        if (needIds.isEmpty()) return
        val coleccion = firestore.collection("families").document(familyId.value).collection("coverageNeeds")
        // Firestore limita cada batch a 500 escrituras; con una selección masiva (p.ej.
        // "Marcar todo" en "Todas las actividades" tras años de actividades acumuladas) se
        // podría superar, así que se trocea en tandas de como mucho 500 borrados cada una.
        needIds.chunked(500).forEach { tanda ->
            val batch = firestore.batch()
            tanda.forEach { needId -> batch.delete(coleccion.document(needId.value)) }
            batch.commit().await()
        }
        dao.eliminarVarios(familyId.value, needIds.map { it.value })
    }

    override suspend fun obtenerTodosLosNeeds(familyId: FamilyId): List<CoverageNeed> {
        return try {
            val snapshot = firestore
                .collection("families").document(familyId.value)
                .collection("coverageNeeds")
                .get()
                .await()

            val needs = snapshot.documents.mapNotNull { doc ->
                CoverageNeedFirestoreMapper.desdeDocumento(doc.id, doc.data ?: emptyMap())
            }

            dao.guardarTodos(needs.map { CoverageNeedEntity.desdeDominio(familyId.value, it) })
            needs
        } catch (e: Exception) {
            dao.obtenerTodos(familyId.value).map { it.aDominio() }
        }
    }

    override suspend fun obtenerNeedsDelGrupo(familyId: FamilyId, grupoRepeticionId: String): List<CoverageNeed> {
        return try {
            val snapshot = firestore
                .collection("families").document(familyId.value)
                .collection("coverageNeeds")
                .whereEqualTo("grupoRepeticionId", grupoRepeticionId)
                .get()
                .await()

            val needs = snapshot.documents.mapNotNull { doc ->
                CoverageNeedFirestoreMapper.desdeDocumento(doc.id, doc.data ?: emptyMap())
            }
            dao.guardarTodos(needs.map { CoverageNeedEntity.desdeDominio(familyId.value, it) })
            needs
        } catch (e: Exception) {
            dao.obtenerPorGrupo(familyId.value, grupoRepeticionId).map { it.aDominio() }
        }
    }
}