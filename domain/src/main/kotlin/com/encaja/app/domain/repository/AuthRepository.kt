package com.encaja.app.domain.repository

import com.encaja.app.domain.model.UserSession

interface AuthRepository {
    suspend fun registrarse(email: String, password: String): Result<UserSession>
    suspend fun iniciarSesion(email: String, password: String): Result<UserSession>
    suspend fun iniciarSesionConGoogle(idToken: String): Result<UserSession>
    fun cerrarSesion()
    fun sesionActual(): UserSession?

    /** Manda al correo de la cuenta actual el enlace para verificarlo. */
    suspend fun enviarVerificacion(): Result<Unit>

    /** Vuelve a consultar a Firebase si la cuenta actual ya verificó su correo (Google: siempre sí). */
    suspend fun correoVerificado(): Boolean

    /** Manda un enlace para elegir una contraseña nueva. */
    suspend fun recuperarPassword(email: String): Result<Unit>
}
