package com.encaja.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
    // Permiso de notificaciones (Android 13+), para los avisos de actividades. Se pide
    // una vez al abrir; si se deniega, los avisos simplemente no se muestran.
    private val pedirNotificaciones = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PreferenciaTema.cargar(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            pedirNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
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
