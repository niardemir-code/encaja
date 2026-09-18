package com.encaja.app.data.repository

import com.encaja.app.domain.model.UserSession
import com.encaja.app.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    override suspend fun registrarse(email: String, password: String): Result<UserSession> {
        return try {
            val resultado = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val uid = resultado.user?.uid ?: return Result.failure(IllegalStateException("No se pudo crear la cuenta"))
            Result.success(UserSession(uid, resultado.user?.email))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun iniciarSesion(email: String, password: String): Result<UserSession> {
        return try {
            val resultado = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val uid = resultado.user?.uid ?: return Result.failure(IllegalStateException("No se pudo iniciar sesión"))
            Result.success(UserSession(uid, resultado.user?.email))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun cerrarSesion() {
        firebaseAuth.signOut()
    }

    override suspend fun iniciarSesionConGoogle(idToken: String): Result<UserSession> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val resultado = firebaseAuth.signInWithCredential(credential).await()
            val uid = resultado.user?.uid ?: return Result.failure(IllegalStateException("No se pudo iniciar sesión con Google"))
            Result.success(UserSession(uid, resultado.user?.email))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun sesionActual(): UserSession? {
        val user = firebaseAuth.currentUser ?: return null
        return UserSession(user.uid, user.email)
    }
}
