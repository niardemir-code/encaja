package com.encaja.app.data.repository

import com.encaja.app.data.firestore.getConFallback
import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.FamilyId
import com.encaja.app.domain.model.FamilyMembership
import com.encaja.app.domain.repository.FamilyMembershipRepository
import com.encaja.app.domain.repository.ResultadoMembresia
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FamilyMembershipRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : FamilyMembershipRepository {

    override suspend fun consultarMembresia(uid: String): ResultadoMembresia {
        return try {
            val doc = firestore.collection("users").document(uid).getConFallback()
            val familyId = doc.getString("familyId")
            val caregiverId = doc.getString("caregiverId")
            if (familyId == null || caregiverId == null) {
                ResultadoMembresia.NoTiene
            } else {
                ResultadoMembresia.Tiene(FamilyMembership(FamilyId(familyId), CaregiverId(caregiverId)))
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            ResultadoMembresia.Error
        }
    }

    override fun escucharMembresia(uid: String, alCambiar: (ResultadoMembresia) -> Unit): () -> Unit {
        val registro = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val familyId = snapshot.getString("familyId")
                val caregiverId = snapshot.getString("caregiverId")
                when {
                    snapshot.exists() && familyId != null && caregiverId != null ->
                        alCambiar(ResultadoMembresia.Tiene(FamilyMembership(FamilyId(familyId), CaregiverId(caregiverId))))
                    // Que no exista solo cuenta si lo confirma el servidor, no la caché local.
                    !snapshot.metadata.isFromCache -> alCambiar(ResultadoMembresia.NoTiene)
                }
            }
        return { registro.remove() }
    }

    override suspend fun vincularAFamilia(uid: String, membership: FamilyMembership) {
        firestore.collection("users").document(uid).set(
            mapOf(
                "familyId" to membership.familyId.value,
                "caregiverId" to membership.caregiverId.value
            )
        ).await()

        // Documento "espejo" que permite comprobar, con un simple get() por id (en vez de
        // una consulta sobre toda la colección "users"), si un cuidador concreto ya tiene
        // cuenta vinculada. Ver el comentario en InviteRepositoryImpl.generarInvitacion.
        firestore.collection("families").document(membership.familyId.value)
            .collection("caregiverLinks").document(membership.caregiverId.value)
            .set(mapOf("uid" to uid))
            .await()
    }
}
