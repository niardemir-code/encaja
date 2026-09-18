package com.encaja.app.data.repository

import com.encaja.app.data.firestore.CaregiverFirestoreMapper
import com.encaja.app.data.local.CaregiverDao
import com.encaja.app.data.local.CaregiverEntity
import com.encaja.app.domain.model.Caregiver
import com.encaja.app.domain.model.FamilyId
import com.encaja.app.domain.repository.CaregiverRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class CaregiverRepositoryImpl @Inject constructor(
    private val dao: CaregiverDao,
    private val firestore: FirebaseFirestore
) : CaregiverRepository {

    override suspend fun obtenerCuidadores(familyId: FamilyId): List<Caregiver> {
        return try {
            val snapshot = firestore
                .collection("families").document(familyId.value)
                .collection("caregivers")
                .get()
                .await()

            val caregivers = snapshot.documents.mapNotNull { doc ->
                CaregiverFirestoreMapper.desdeDocumento(doc.id, doc.data ?: emptyMap())
            }

            dao.guardarTodos(caregivers.map { CaregiverEntity.desdeDominio(familyId.value, it) })
            caregivers
        } catch (e: Exception) {
            dao.obtener(familyId.value).map { it.aDominio() }
        }
    }

    override suspend fun guardarCuidadores(familyId: FamilyId, caregivers: List<Caregiver>) {
        val coleccion = firestore.collection("families").document(familyId.value).collection("caregivers")
        val batch = firestore.batch()
        caregivers.forEach { caregiver ->
            batch.set(coleccion.document(caregiver.id.value), CaregiverFirestoreMapper.aDocumento(caregiver))
        }
        batch.commit().await()
        dao.guardarTodos(caregivers.map { CaregiverEntity.desdeDominio(familyId.value, it) })
    }
}