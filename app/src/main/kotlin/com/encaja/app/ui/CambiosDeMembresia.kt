package com.encaja.app.ui

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Avisa al resto de la app de que la cuenta acaba de quedar vinculada a una familia y a un
 * cuidador (al canjear una invitación, crear una familia o vincularse a mano). Sin esto, lo que
 * se calcula una sola vez al abrir la app (las iniciales del avatar, los avisos de este móvil)
 * seguía con los datos de antes de vincularse hasta reiniciar la aplicación.
 */
@Singleton
class CambiosDeMembresia @Inject constructor() {
    private val _eventos = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val eventos: SharedFlow<Unit> = _eventos.asSharedFlow()

    fun avisar() {
        _eventos.tryEmit(Unit)
    }
}
