package com.encaja.app.domain.repository

import com.encaja.app.domain.model.UserSession

interface AuthRepository {
    suspend fun registrarse(email: String, password: String): Result<UserSession>
    suspend fun iniciarSesion(email: String, password: String): Result<UserSession>
    suspend fun iniciarSesionConGoogle(idToken: String): Result<UserSession>
    fun cerrarSesion()
    fun sesionActual(): UserSession?
}
