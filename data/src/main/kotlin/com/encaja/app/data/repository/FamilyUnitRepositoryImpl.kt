package com.encaja.app.data.repository

import com.encaja.app.data.firestore.FamilyUnitFirestoreMapper
import com.encaja.app.data.local.FamilyUnitDao
import com.encaja.app.data.local.FamilyUnitEntity
import com.encaja.app.domain.model.FamilyId
import com.encaja.app.domain.model.FamilyUnit
import com.encaja.app.domain.model.FamilyUnitId
import com.encaja.app.domain.repository.FamilyUnitRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FamilyUnitRepositoryImpl @Inject constructor(
    private val dao: FamilyUnitDao,
    private val firestore: FirebaseFirestore
) : FamilyUnitRepository {

    override suspend fun obtenerUnidades(familyId: FamilyId): List<FamilyUnit> {
        return try {
            val snapshot = firestore
                .collection("families").document(familyId.value)
                .collection("familyUnits")
                .get()
                .await()

            val unidades = snapshot.documents.mapNotNull { doc ->
                FamilyUnitFirestoreMapper.desdeDocumento(doc.id, doc.data ?: emptyMap())
            }

            dao.guardarTodos(unidades.map { FamilyUnitEntity.desdeDominio(familyId.value, it) })
            unidades
        } catch (e: Exception) {
            dao.obtener(familyId.value).map { it.aDominio() }
        }
    }

    override suspend fun guardarUnidades(familyId: FamilyId, unidades: List<FamilyUnit>) {
        val coleccion = firestore.collection("families").document(familyId.value).collection("familyUnits")
        val batch = firestore.batch()
        unidades.forEach { unidad ->
            batch.set(coleccion.document(unidad.id.value), FamilyUnitFirestoreMapper.aDocumento(unidad))
        }
        batch.commit().await()
        dao.guardarTodos(unidades.map { FamilyUnitEntity.desdeDominio(familyId.value, it) })
    }

    override suspend fun eliminarUnidad(familyId: FamilyId, unidadId: FamilyUnitId) {
        firestore.collection("families").document(familyId.value)
            .collection("familyUnits").document(unidadId.value)
            .delete()
            .await()
        dao.eliminar(familyId.value, unidadId.value)
    }
}
