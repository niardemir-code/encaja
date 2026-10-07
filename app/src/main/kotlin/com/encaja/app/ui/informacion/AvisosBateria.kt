package com.encaja.app.ui.informacion

// NOTA: depende de Android (PowerManager, Settings) y Compose; no compilado en este entorno.

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Ayudas para que los avisos lleguen con la app cerrada: quitar la restricción de batería de
 * Android y, en móviles con ahorro propio del fabricante (Xiaomi, Oppo, Huawei...), los
 * ajustes extra que hay que tocar.
 */
object AvisosBateria {
    private const val PREFS = "avisos_bateria"
    private const val CLAVE_PREGUNTADO = "preguntado"

    fun estaExenta(contexto: Context): Boolean =
        contexto.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(contexto.packageName) ?: true

    /** True si ya se le mostró el aviso de primera vez (se muestra una sola vez). */
    fun yaPreguntado(contexto: Context): Boolean =
        contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(CLAVE_PREGUNTADO, false)

    fun marcarPreguntado(contexto: Context) {
        contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(CLAVE_PREGUNTADO, true).apply()
    }

    /**
     * Abre la ficha de Encaja en los ajustes del sistema, donde el usuario entra en «Batería» y
     * elige «Sin restricciones» (o «No optimizar»). La lista general de optimización de batería
     * (ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS) no sirve de primera opción: en muchos móviles
     * solo muestra las apps ya exentas y Encaja no aparece. Tampoco se usa la petición directa
     * (ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS), que exige un permiso que Google Play restringe.
     */
    fun abrirQuitarRestricciones(contexto: Context) {
        try {
            contexto.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", contexto.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (e: Exception) {
            try {
                contexto.startActivity(
                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (e2: Exception) {
                // Sin pantalla de ajustes disponible: no se puede hacer nada más.
            }
        }
    }
}

/** Aviso de primera vez: ofrece quitar las restricciones de batería. */
@Composable
fun DialogoAvisosBateria(onQuitar: () -> Unit, onAhoraNo: () -> Unit, onMasInfo: () -> Unit) {
    AlertDialog(
        onDismissRequest = onAhoraNo,
        title = { Text("Avisos de tu familia") },
        text = {
            Column {
                Text(
                    "¿Quieres recibir los avisos de los integrantes de tu familia aunque la app esté cerrada? " +
                        "Quita las restricciones de batería para Encaja."
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Se abrirá la ficha de Encaja en los ajustes: entra en «Batería» y elige «Sin restricciones» " +
                        "(según el móvil, puede llamarse «No optimizar» o «Permitir actividad en segundo plano»).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onMasInfo) { Text("Más información (Xiaomi, Oppo, Huawei…)") }
            }
        },
        confirmButton = { TextButton(onClick = onQuitar) { Text("Quitar restricciones") } },
        dismissButton = { TextButton(onClick = onAhoraNo) { Text("Ahora no") } }
    )
}

private data class PasosMarca(val marca: String, val pasos: String)

private val PASOS_POR_MARCA = listOf(
    PasosMarca(
        "Xiaomi, Redmi, POCO",
        "Ajustes → Aplicaciones → Administrar aplicaciones → Encaja: activa «Inicio automático» y, en " +
            "«Ahorro de batería», elige «Sin restricciones». Además, en la lista de apps recientes, mantén " +
            "pulsada la tarjeta de Encaja y toca el candado."
    ),
    PasosMarca(
        "Oppo, Realme, OnePlus",
        "Ajustes → Aplicaciones → Encaja: permite el «Inicio automático» y, en el uso de batería, " +
            "«Permitir actividad en segundo plano»."
    ),
    PasosMarca(
        "Samsung",
        "Ajustes → Batería → Límites de uso en segundo plano: quita Encaja de las apps en suspensión " +
            "y añádela a «Apps que nunca se suspenden»."
    ),
    PasosMarca(
        "Huawei, Honor",
        "Ajustes → Aplicaciones → Inicio de aplicaciones → Encaja: desactiva la gestión automática y " +
            "activa «Inicio automático», «Inicio secundario» y «Ejecución en segundo plano»."
    ),
    PasosMarca(
        "Vivo",
        "Ajustes → Batería → Consumo en segundo plano → Encaja: permite la actividad en segundo plano " +
            "y el inicio automático."
    )
)

/** Ayuda para móviles con ahorro de batería extra del fabricante. */
@Composable
fun DialogoMasInformacionBateria(onCerrar: () -> Unit) {
    val contexto = LocalContext.current
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Avisos con la app cerrada") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Además de quitar la restricción de batería de Android, muchos fabricantes añaden su " +
                        "propio ahorro de energía, que puede cerrar la app y evitar que lleguen los avisos. " +
                        "Los nombres de los menús cambian según el modelo y la versión.",
                    style = MaterialTheme.typography.bodyMedium
                )
                PASOS_POR_MARCA.forEach { item ->
                    Spacer(Modifier.height(12.dp))
                    Text(item.marca, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(item.pasos, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Si cierras Encaja a la fuerza desde los ajustes del móvil («Forzar detención»), no " +
                        "llegará ningún aviso hasta que la vuelvas a abrir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = {
                    try {
                        contexto.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://dontkillmyapp.com")))
                    } catch (e: Exception) { /* sin navegador */ }
                }) { Text("Guía por marca (dontkillmyapp.com)") }
            }
        },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Cerrar") } }
    )
}
