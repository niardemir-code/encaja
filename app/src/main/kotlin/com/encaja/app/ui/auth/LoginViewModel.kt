package com.encaja.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.encaja.app.R
import com.encaja.app.domain.repository.AuthRepository
import com.encaja.app.ui.common.TextoUi
import com.encaja.app.ui.common.textoUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val cargando: Boolean = false,
    val error: TextoUi? = null,
    /** Correo de la cuenta que falta por verificar; si no es null se muestra esa pantalla. */
    val pendienteVerificar: String? = null,
    /** Mensaje informativo (correo reenviado, enlace de contraseña enviado...). */
    val info: TextoUi? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun iniciarSesion(email: String, password: String, alConseguirlo: () -> Unit) {
        _uiState.value = LoginUiState(cargando = true)
        viewModelScope.launch {
            authRepository.iniciarSesion(email, password).fold(
                onSuccess = {
                    if (authRepository.correoVerificado()) {
                        _uiState.value = LoginUiState(cargando = false)
                        alConseguirlo()
                    } else {
                        _uiState.value = LoginUiState(pendienteVerificar = email.trim())
                    }
                },
                onFailure = { error ->
                    _uiState.value = LoginUiState(cargando = false, error = mensajeDeError(error))
                }
            )
        }
    }

    fun registrarse(email: String, password: String) {
        // Red de seguridad: la pantalla ya no deja pulsar el botón sin cumplir los requisitos.
        if (!evaluarPassword(password).cumple) {
            _uiState.value = LoginUiState(error = textoUi(R.string.login_error_password_debil))
            return
        }
        _uiState.value = LoginUiState(cargando = true)
        viewModelScope.launch {
            authRepository.registrarse(email, password).fold(
                onSuccess = {
                    // La cuenta no entra en la app hasta verificar el correo.
                    authRepository.enviarVerificacion()
                    _uiState.value = LoginUiState(pendienteVerificar = email.trim())
                },
                onFailure = { error ->
                    _uiState.value = LoginUiState(cargando = false, error = mensajeDeError(error))
                }
            )
        }
    }

    /** "Ya lo he verificado": consulta a Firebase y, si consta, entra. */
    fun comprobarVerificado(alConseguirlo: () -> Unit) {
        val pendiente = _uiState.value.pendienteVerificar ?: return
        _uiState.value = LoginUiState(cargando = true, pendienteVerificar = pendiente)
        viewModelScope.launch {
            if (authRepository.correoVerificado()) {
                _uiState.value = LoginUiState()
                alConseguirlo()
            } else {
                _uiState.value = LoginUiState(pendienteVerificar = pendiente, error = textoUi(R.string.login_verifica_aun_no))
            }
        }
    }

    fun reenviarVerificacion() {
        val pendiente = _uiState.value.pendienteVerificar ?: return
        _uiState.value = LoginUiState(cargando = true, pendienteVerificar = pendiente)
        viewModelScope.launch {
            authRepository.enviarVerificacion().fold(
                onSuccess = { _uiState.value = LoginUiState(pendienteVerificar = pendiente, info = textoUi(R.string.login_verifica_reenviado)) },
                onFailure = { _uiState.value = LoginUiState(pendienteVerificar = pendiente, error = textoUi(R.string.login_error_generico)) }
            )
        }
    }

    /** "Usar otra cuenta": cierra la sesión pendiente y vuelve al formulario. */
    fun cancelarVerificacion() {
        authRepository.cerrarSesion()
        _uiState.value = LoginUiState()
    }

    fun recuperarPassword(email: String) {
        if (email.isBlank()) {
            _uiState.value = LoginUiState(error = textoUi(R.string.login_recuperar_pide_correo))
            return
        }
        _uiState.value = LoginUiState(cargando = true)
        viewModelScope.launch {
            authRepository.recuperarPassword(email).fold(
                // Mismo mensaje exista o no la cuenta, para no revelar qué correos están registrados.
                onSuccess = { _uiState.value = LoginUiState(info = textoUi(R.string.login_recuperar_enviado)) },
                onFailure = { error -> _uiState.value = LoginUiState(error = mensajeDeError(error)) }
            )
        }
    }

    fun iniciarSesionConGoogle(idToken: String, alConseguirlo: () -> Unit) {
        _uiState.value = LoginUiState(cargando = true)
        viewModelScope.launch {
            authRepository.iniciarSesionConGoogle(idToken).fold(
                onSuccess = {
                    _uiState.value = LoginUiState(cargando = false)
                    alConseguirlo()
                },
                onFailure = { error ->
                    _uiState.value = LoginUiState(cargando = false, error = mensajeDeError(error))
                }
            )
        }
    }

    /** Traduce los mensajes de error en inglés del SDK de Firebase a un texto que cada idioma puede mostrar. */
    private fun mensajeDeError(error: Throwable): TextoUi = when {
        error.message?.contains("badly formatted", ignoreCase = true) == true -> textoUi(R.string.login_error_correo_invalido)
        error.message?.contains("password is invalid", ignoreCase = true) == true -> textoUi(R.string.login_error_password_incorrecta)
        error.message?.contains("no user record", ignoreCase = true) == true -> textoUi(R.string.login_error_sin_cuenta)
        error.message?.contains("email address is already in use", ignoreCase = true) == true -> textoUi(R.string.login_error_cuenta_existe)
        error.message?.contains("at least 6 characters", ignoreCase = true) == true -> textoUi(R.string.login_error_password_corta)
        else -> textoUi(R.string.login_error_generico)
    }
}
