package com.encaja.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.encaja.app.ui.EncajaApp
import com.encaja.app.ui.auth.AuthViewModel
import com.encaja.app.ui.auth.LoginScreen
import com.encaja.app.ui.intro.IntroScreen
import com.encaja.app.ui.intro.PreferenciaIntro
import com.encaja.app.ui.theme.EncajaTheme
import com.encaja.app.ui.theme.PreferenciaTema
import com.encaja.app.ui.theme.TemaPreferido
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // Permiso de notificaciones (Android 13+), para los avisos de actividades. Se pide
    // una vez al abrir; si se deniega, los avisos simplemente no se muestran.
    private val pedirNotificaciones = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    // Actividad a la que hay que saltar en la Guía (fecha, id) cuando se abre la app desde el
    // aviso de una actividad. Se consume una vez navegado.
    private val abrirGuia = mutableStateOf<Pair<String, String>?>(null)

    // True cuando se abre desde la notificación de un anuncio: hay que mostrar el tablón.
    private val abrirTablon = mutableStateOf(false)

    private fun leerDestino(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_ABRIR_TABLON, false) == true) {
            abrirTablon.value = true
            intent.removeExtra(EXTRA_ABRIR_TABLON)
        }
        val fecha = intent?.getStringExtra(EXTRA_ABRIR_FECHA)
        val id = intent?.getStringExtra(EXTRA_ABRIR_NECESIDAD)
        if (fecha != null && id != null) {
            abrirGuia.value = fecha to id
            // Se limpian para que girar la pantalla o reabrir no repita la navegación.
            intent.removeExtra(EXTRA_ABRIR_FECHA)
            intent.removeExtra(EXTRA_ABRIR_NECESIDAD)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        leerDestino(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Debe ir antes de super.onCreate: aplica el splash con el logo de Encaja.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        PreferenciaTema.cargar(this)
        leerDestino(intent)
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

                    // La intro se ve una sola vez, antes del login, en la primera apertura.
                    var introVista by remember { mutableStateOf(PreferenciaIntro.vista(this@MainActivity)) }
                    var registroInicial by remember { mutableStateOf(false) }

                    if (autenticado) {
                        EncajaApp(
                            onCerrarSesion = { authViewModel.cerrarSesion() },
                            abrirGuia = abrirGuia.value,
                            alAbrirGuia = { abrirGuia.value = null },
                            abrirTablon = abrirTablon.value,
                            alAbrirTablon = { abrirTablon.value = false }
                        )
                    } else if (!introVista) {
                        IntroScreen(
                            alTerminar = { registrarse, codigo ->
                                if (codigo != null) PreferenciaIntro.guardarCodigo(this@MainActivity, codigo)
                                PreferenciaIntro.marcarVista(this@MainActivity)
                                registroInicial = registrarse
                                introVista = true
                            },
                            alCrearFamilia = {
                                PreferenciaIntro.guardarIntencionCrear(this@MainActivity)
                                PreferenciaIntro.marcarVista(this@MainActivity)
                                registroInicial = true
                                introVista = true
                            }
                        )
                    } else {
                        LoginScreen(
                            onLoginExitoso = { authViewModel.marcarAutenticado() },
                            modoRegistroInicial = registroInicial
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_ABRIR_FECHA = "abrir_fecha"
        const val EXTRA_ABRIR_NECESIDAD = "abrir_necesidad"
        const val EXTRA_ABRIR_TABLON = "abrir_tablon"
    }
}
