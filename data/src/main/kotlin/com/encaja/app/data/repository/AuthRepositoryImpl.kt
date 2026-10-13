package com.encaja.app.data.repository

import com.encaja.app.domain.model.UserSession
import com.encaja.app.domain.repository.AuthRepository
import com.google.firebase.auth.EmailAuthProvider
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
        // Una cuenta de correo y contraseña sin verificar no entra en la app (así no sirve
        // inventarse un correo). Las de Google ya vienen verificadas.
        if (!verificada(user)) return null
        return UserSession(user.uid, user.email)
    }

    private fun verificada(user: com.google.firebase.auth.FirebaseUser): Boolean =
        user.isEmailVerified || user.providerData.none { it.providerId == EmailAuthProvider.PROVIDER_ID }

    override suspend fun enviarVerificacion(): Result<Unit> = try {
        val user = firebaseAuth.currentUser ?: return Result.failure(IllegalStateException("Sin sesión"))
        firebaseAuth.useAppLanguage()   // el correo sale en el idioma de la app
        user.sendEmailVerification().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun correoVerificado(): Boolean = try {
        val user = firebaseAuth.currentUser
        if (user == null) false else {
            user.reload().await()
            verificada(user)
        }
    } catch (e: Exception) {
        false
    }

    override suspend fun recuperarPassword(email: String): Result<Unit> = try {
        firebaseAuth.useAppLanguage()
        firebaseAuth.sendPasswordResetEmail(email.trim()).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
