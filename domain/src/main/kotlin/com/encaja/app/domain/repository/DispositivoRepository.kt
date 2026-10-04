package com.encaja.app.domain.repository

/**
 * Los dispositivos (tokens de notificaciones push) de la cuenta abierta. Se guardan en
 * Firestore para que el servidor sepa a qué móviles avisar (tablón, cambios en actividades).
 */
interface DispositivoRepository {
    /** Registra este móvil para la cuenta con sesión abierta (si la hay). */
    suspend fun registrarDispositivoActual()

    /** Guarda un token nuevo que el sistema acaba de dar a este móvil. */
    suspend fun registrarToken(token: String)

    /** Quita este móvil de la cuenta: tras cerrar sesión ya no debe recibir sus notificaciones. */
    suspend fun olvidarDispositivoActual()
}
