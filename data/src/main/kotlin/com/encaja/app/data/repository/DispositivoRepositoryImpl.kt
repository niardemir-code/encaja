package com.encaja.app.data.repository

import com.encaja.app.domain.repository.DispositivoRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

// NOTA: depende de Firebase Cloud Messaging; no compilado en este entorno.
class DispositivoRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : DispositivoRepository {

    private fun tokens(uid: String) = firestore.collection("users").document(uid).collection("tokens")

    override suspend fun registrarDispositivoActual() {
        val uid = auth.currentUser?.uid ?: return
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            tokens(uid).document(token).set(mapOf("actualizado" to FieldValue.serverTimestamp())).await()
        } catch (e: Exception) {
            // Sin red o sin Play Services: se reintenta en la próxima apertura.
        }
    }

    override suspend fun registrarToken(token: String) {
        val uid = auth.currentUser?.uid ?: return
        try {
            tokens(uid).document(token).set(mapOf("actualizado" to FieldValue.serverTimestamp())).await()
        } catch (e: Exception) {
        }
    }

    override suspend fun olvidarDispositivoActual() {
        val uid = auth.currentUser?.uid ?: return
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            tokens(uid).document(token).delete().await()
        } catch (e: Exception) {
        }
    }
}
