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
    val error: TextoUi? = null
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
                    _uiState.value = LoginUiState(cargando = false)
                    alConseguirlo()
                },
                onFailure = { error ->
                    _uiState.value = LoginUiState(cargando = false, error = mensajeDeError(error))
                }
            )
        }
    }

    fun registrarse(email: String, password: String, alConseguirlo: () -> Unit) {
        _uiState.value = LoginUiState(cargando = true)
        viewModelScope.launch {
            authRepository.registrarse(email, password).fold(
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
