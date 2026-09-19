package com.encaja.app.data.repository

import com.encaja.app.data.firestore.ChildFirestoreMapper
import com.encaja.app.data.local.ChildDao
import com.encaja.app.data.local.ChildEntity
import com.encaja.app.domain.model.Child
import com.encaja.app.domain.model.ChildId
import com.encaja.app.domain.model.FamilyId
import com.encaja.app.domain.repository.ChildRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ChildRepositoryImpl @Inject constructor(
    private val dao: ChildDao,
    private val firestore: FirebaseFirestore
) : ChildRepository {

    private fun coleccion(familyId: FamilyId) =
        firestore.collection("families").document(familyId.value).collection("children")

    override suspend fun obtenerNinos(familyId: FamilyId): List<Child> {
        return try {
            val snapshot = coleccion(familyId).get().await()
            val ninos = snapshot.documents.mapNotNull { doc ->
                ChildFirestoreMapper.desdeDocumento(doc.id, doc.data ?: emptyMap())
            }
            dao.guardarTodos(ninos.map { ChildEntity.desdeDominio(familyId.value, it) })
            ninos
        } catch (e: Exception) {
            dao.obtener(familyId.value).map { it.aDominio() }
        }
    }

    override suspend fun guardarNinos(familyId: FamilyId, ninos: List<Child>) {
        val coleccion = coleccion(familyId)
        val batch = firestore.batch()
        ninos.forEach { nino ->
            batch.set(coleccion.document(nino.id.value), ChildFirestoreMapper.aDocumento(nino))
        }
        batch.commit().await()
        dao.guardarTodos(ninos.map { ChildEntity.desdeDominio(familyId.value, it) })
    }

    override suspend fun eliminarNino(familyId: FamilyId, childId: ChildId) {
        coleccion(familyId).document(childId.value).delete().await()
        dao.eliminar(familyId.value, childId.value)
    }
}
