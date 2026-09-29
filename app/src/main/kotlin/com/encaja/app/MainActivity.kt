package com.encaja.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.encaja.app.ui.EncajaApp
import com.encaja.app.ui.auth.AuthViewModel
import com.encaja.app.ui.auth.LoginScreen
import com.encaja.app.ui.theme.EncajaTheme
import com.encaja.app.ui.theme.PreferenciaTema
import com.encaja.app.ui.theme.TemaPreferido
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PreferenciaTema.cargar(this)
        setContent {
            // Sigue el tema elegido en Ajustes (Sistema/Claro/Oscuro); por defecto,
            // el modo claro ("Cálido") u oscuro ("Nocturno") del teléfono.
            val temaPreferido by PreferenciaTema.actual
            val sistemaOscuro = isSystemInDarkTheme()
            val esOscuro = when (temaPreferido) {
                TemaPreferido.SISTEMA -> sistemaOscuro
                TemaPreferido.CLARO -> false
                TemaPreferido.OSCURO -> true
            }
            EncajaTheme(darkTheme = esOscuro) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val authViewModel: AuthViewModel = hiltViewModel()
                    val autenticado by authViewModel.autenticado.collectAsState()

                    if (autenticado) {
                        EncajaApp(onCerrarSesion = { authViewModel.cerrarSesion() })
                    } else {
                        LoginScreen(onLoginExitoso = { authViewModel.marcarAutenticado() })
                    }
                }
            }
        }
    }
}
