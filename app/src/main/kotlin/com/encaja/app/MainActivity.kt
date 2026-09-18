package com.encaja.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.encaja.app.ui.EncajaApp
import com.encaja.app.ui.auth.AuthViewModel
import com.encaja.app.ui.auth.LoginScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
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
