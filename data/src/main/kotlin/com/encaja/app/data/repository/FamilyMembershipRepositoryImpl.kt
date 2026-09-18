package com.encaja.app.data.repository

import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.FamilyId
import com.encaja.app.domain.model.FamilyMembership
import com.encaja.app.domain.repository.FamilyMembershipRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FamilyMembershipRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : FamilyMembershipRepository {

    override suspend fun obtenerMembresia(uid: String): FamilyMembership? {
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            val familyId = doc.getString("familyId") ?: return null
            val caregiverId = doc.getString("caregiverId") ?: return null
            FamilyMembership(FamilyId(familyId), CaregiverId(caregiverId))
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun vincularAFamilia(uid: String, membership: FamilyMembership) {
        firestore.collection("users").document(uid).set(
            mapOf(
                "familyId" to membership.familyId.value,
                "caregiverId" to membership.caregiverId.value
            )
        ).await()
    }
}
