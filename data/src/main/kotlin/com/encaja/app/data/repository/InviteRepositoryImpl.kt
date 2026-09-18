package com.encaja.app.data.repository

import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.FamilyId
import com.encaja.app.domain.model.FamilyMembership
import com.encaja.app.domain.repository.InviteRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class InviteRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : InviteRepository {

    // Sin 0/O ni 1/I: se confunden fácilmente al leerlos en voz alta o a mano.
    private val caracteresPermitidos = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"

    private fun generarCodigo(): String = (1..6).map { caracteresPermitidos.random() }.joinToString("")

    override suspend fun generarInvitacion(familyId: FamilyId, caregiverId: CaregiverId): Result<String> {
        return try {
            // Hasta 5 intentos por si el código generado ya existiera (muy improbable con 6 caracteres).
            repeat(5) {
                val codigo = generarCodigo()
                val doc = firestore.collection("invites").document(codigo)
                val existente = doc.get().await()
                if (!existente.exists()) {
                    doc.set(
                        mapOf(
                            "familyId" to familyId.value,
                            "caregiverId" to caregiverId.value,
                            "usado" to false
                        )
                    ).await()
                    return Result.success(codigo)
                }
            }
            Result.failure(IllegalStateException("No se pudo generar un código único, inténtalo de nuevo"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun canjearInvitacion(codigo: String): Result<FamilyMembership> {
        return try {
            val doc = firestore.collection("invites").document(codigo.uppercase())

            val membership = firestore.runTransaction { transaccion ->
                val snapshot = transaccion.get(doc)
                if (!snapshot.exists()) {
                    throw IllegalArgumentException("Ese código no existe")
                }
                val usado = snapshot.getBoolean("usado") ?: false
                if (usado) {
                    throw IllegalStateException("Ese código ya se ha usado")
                }
                val familyId = snapshot.getString("familyId") ?: throw IllegalStateException("Código incompleto")
                val caregiverId = snapshot.getString("caregiverId") ?: throw IllegalStateException("Código incompleto")

                transaccion.update(doc, "usado", true)
                FamilyMembership(FamilyId(familyId), CaregiverId(caregiverId))
            }.await()

            Result.success(membership)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
