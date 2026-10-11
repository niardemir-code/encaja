package com.encaja.app.ui.informacion

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.encaja.app.ui.theme.BotonPildoraBordeHoja
import com.encaja.app.ui.theme.BotonPildoraHoja
import com.encaja.app.ui.theme.CabeceraPantalla
import com.encaja.app.ui.theme.FondoPantalla
import com.encaja.app.ui.theme.RotuloSeccionPantalla
import com.encaja.app.ui.theme.TarjetaFilaHoja
import com.encaja.app.ui.theme.tintaHoja
import com.encaja.app.ui.theme.tintaSuaveHoja

/** Ajustes > Contacto: dirección del desarrollador y botón para escribirle con el correo del móvil. */
@Composable
fun ContactoScreen() {
    val contexto = LocalContext.current
    val portapapeles = LocalClipboardManager.current
    var mensaje by remember { mutableStateOf<String?>(null) }

    FondoPantalla {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            CabeceraPantalla(
                "Contacto",
                "¿Tienes una duda, has encontrado un fallo o quieres proponer una mejora? Escríbenos."
            )

            TarjetaFilaHoja {
                Column(Modifier.weight(1f)) {
                    Text("Correo del desarrollador", style = MaterialTheme.typography.labelMedium, color = tintaSuaveHoja)
                    Text(
                        DatosLegales.EMAIL_CONTACTO,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = tintaHoja
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            BotonPildoraHoja(
                "Escribir un correo",
                onClick = {
                    mensaje = if (abrirCorreo(contexto)) null
                    else "No hay ninguna app de correo en este móvil. Copia la dirección y escríbenos desde donde prefieras."
                },
                icono = Icons.Default.Mail
            )
            Spacer(Modifier.height(10.dp))
            BotonPildoraBordeHoja("Copiar dirección", onClick = {
                portapapeles.setText(AnnotatedString(DatosLegales.EMAIL_CONTACTO))
                mensaje = "Dirección copiada."
            })

            mensaje?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "Al escribir desde la app, se añade al mensaje la versión de Encaja y de Android para ayudarnos a entender el problema. Puedes borrarlo antes de enviar.",
                style = MaterialTheme.typography.bodySmall,
                color = tintaSuaveHoja
            )
        }
    }
}

/** Abre la app de correo con un mensaje nuevo para el desarrollador. Devuelve false si no hay ninguna. */
private fun abrirCorreo(contexto: Context): Boolean {
    val version = versionDeLaApp(contexto)
    val cuerpo = "\n\n—\nVersión de Encaja: $version\nAndroid: ${Build.VERSION.RELEASE} (${Build.MODEL})"
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(DatosLegales.EMAIL_CONTACTO))
        putExtra(Intent.EXTRA_SUBJECT, "Encaja: consulta")
        putExtra(Intent.EXTRA_TEXT, cuerpo)
    }
    return try {
        contexto.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}

private fun versionDeLaApp(contexto: Context): String = runCatching {
    val paquete = contexto.packageName
    val info = if (Build.VERSION.SDK_INT >= 33) {
        contexto.packageManager.getPackageInfo(paquete, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        contexto.packageManager.getPackageInfo(paquete, 0)
    }
    info.versionName ?: "?"
}.getOrDefault("?")
