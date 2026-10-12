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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.encaja.app.R
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
    var mensaje by remember { mutableStateOf<Int?>(null) }

    FondoPantalla {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            CabeceraPantalla(
                stringResource(R.string.contacto_titulo),
                stringResource(R.string.contacto_subtitulo)
            )

            TarjetaFilaHoja {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.contacto_correo_desarrollador), style = MaterialTheme.typography.labelMedium, color = tintaSuaveHoja)
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
                stringResource(R.string.contacto_escribir),
                onClick = {
                    mensaje = if (abrirCorreo(contexto)) null
                    else R.string.contacto_sin_app_correo
                },
                icono = Icons.Default.Mail
            )
            Spacer(Modifier.height(10.dp))
            BotonPildoraBordeHoja(stringResource(R.string.contacto_copiar), onClick = {
                portapapeles.setText(AnnotatedString(DatosLegales.EMAIL_CONTACTO))
                mensaje = R.string.contacto_copiada
            })

            mensaje?.let {
                Spacer(Modifier.height(12.dp))
                Text(stringResource(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.contacto_nota),
                style = MaterialTheme.typography.bodySmall,
                color = tintaSuaveHoja
            )
        }
    }
}

/** Abre la app de correo con un mensaje nuevo para el desarrollador. Devuelve false si no hay ninguna. */
private fun abrirCorreo(contexto: Context): Boolean {
    val version = versionDeLaApp(contexto)
    val cuerpo = "\n\n—\n${contexto.getString(R.string.contacto_version, version)}\nAndroid: ${Build.VERSION.RELEASE} (${Build.MODEL})"
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(DatosLegales.EMAIL_CONTACTO))
        putExtra(Intent.EXTRA_SUBJECT, contexto.getString(R.string.contacto_asunto))
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
