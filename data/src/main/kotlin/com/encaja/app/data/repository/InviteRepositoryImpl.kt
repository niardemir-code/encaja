package com.encaja.app.data.repository

import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.FamilyId
import com.encaja.app.domain.model.FamilyMembership
import com.encaja.app.domain.repository.InviteRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class InviteRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : InviteRepository {

    // Sin 0/O ni 1/I: se confunden fácilmente al leerlos en voz alta o a mano.
    private companion object {
        const val DIAS_VALIDEZ = 14
    }

    private val caracteresPermitidos = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"

    private fun generarCodigo(): String = (1..6).map { caracteresPermitidos.random() }.joinToString("")

    private val funciones: FirebaseFunctions by lazy { FirebaseFunctions.getInstance("europe-west1") }

    /**
     * Pide al servidor que compruebe si el enlace es huérfano (cuenta borrada a mano) y lo limpie.
     * Devuelve true solo si estaba huérfano y se ha limpiado; ante cualquier fallo, false.
     */
    private suspend fun enlaceHuerfano(caregiverId: CaregiverId): Boolean = try {
        val r = funciones.getHttpsCallable("repararVinculo")
            .call(mapOf("caregiverId" to caregiverId.value)).await()
        (r.data as? Map<*, *>)?.get("huerfano") == true
    } catch (e: Exception) {
        false
    }

    override suspend fun generarInvitacion(familyId: FamilyId, caregiverId: CaregiverId): Result<String> {
        return try {
            // Si ese cuidador ya tiene a alguien vinculado, no generamos un código nuevo:
            // evita que dos personas distintas acaben siendo "la misma" Sílvia.
            //
            // Se comprueba con un documento único (families/{familyId}/caregiverLinks/{caregiverId})
            // en vez de una consulta sobre toda la colección "users": las reglas de seguridad de
            // Firestore normales son por documento (cada uno solo puede leer su propio users/{uid}),
            // y esas reglas bloquean cualquier consulta que recorra la colección entera buscando por
            // campos (de ahí el PERMISSION_DENIED). Un get() a un documento concreto sí es compatible.
            val yaVinculado = firestore.collection("families").document(familyId.value)
                .collection("caregiverLinks").document(caregiverId.value)
                .get()
                .await()
            if (yaVinculado.exists() && !enlaceHuerfano(caregiverId)) {
                return Result.failure(IllegalStateException("Ese cuidador ya tiene una cuenta vinculada"))
            }

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
                            "usado" to false,
                            // Firestore borra solo el documento al pasar esta fecha (política TTL
                            // sobre "caducaEn" en la colección "invites") y canjear lo rechaza.
                            "caducaEn" to Timestamp(java.util.Date(System.currentTimeMillis() + DIAS_VALIDEZ * 86_400_000L))
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
                // Los códigos antiguos no tienen fecha de caducidad: siguen valiendo.
                val caducaEn = snapshot.getTimestamp("caducaEn")
                if (caducaEn != null && caducaEn.toDate().time < System.currentTimeMillis()) {
                    throw IllegalStateException("Ese código ha caducado, pide uno nuevo")
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
