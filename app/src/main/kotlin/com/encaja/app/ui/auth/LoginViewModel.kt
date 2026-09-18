package com.encaja.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.encaja.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val cargando: Boolean = false,
    val error: String? = null
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

    /** Traduce los mensajes de error en inglés del SDK de Firebase a algo legible. */
    private fun mensajeDeError(error: Throwable): String = when {
        error.message?.contains("badly formatted", ignoreCase = true) == true -> "El correo no tiene un formato válido"
        error.message?.contains("password is invalid", ignoreCase = true) == true -> "Contraseña incorrecta"
        error.message?.contains("no user record", ignoreCase = true) == true -> "No existe ninguna cuenta con ese correo"
        error.message?.contains("email address is already in use", ignoreCase = true) == true -> "Ya existe una cuenta con ese correo"
        error.message?.contains("at least 6 characters", ignoreCase = true) == true -> "La contraseña debe tener al menos 6 caracteres"
        else -> "Algo ha fallado, inténtalo de nuevo"
    }
}
