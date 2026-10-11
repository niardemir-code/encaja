package com.encaja.app.ui.informacion

// NOTA: depende de Android (PowerManager, Settings) y Compose; no compilado en este entorno.

import androidx.compose.material.icons.filled.BatteryAlert
import com.encaja.app.ui.theme.EstiloAccionAviso
import com.encaja.app.ui.theme.AccionAvisoHoja
import com.encaja.app.ui.theme.AvisoHoja
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.encaja.app.ui.theme.LocalEncajaExtraColors

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

/**
 * Aviso de primera vez: ofrece quitar las restricciones de batería. Diseño de tarjeta grande con
 * campana, panel con la ilustración del móvil, enlace de «Más información» y dos botones.
 */
@Composable
fun DialogoAvisosBateria(onQuitar: () -> Unit, onAhoraNo: () -> Unit, onMasInfo: () -> Unit) {
    val extra = LocalEncajaExtraColors.current
    val lavanda = MaterialTheme.colorScheme.primaryContainer
    val indigo = MaterialTheme.colorScheme.primary
    val suave = lavanda.copy(alpha = 0.55f)

    Dialog(
        onDismissRequest = onAhoraNo,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(36.dp),
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth()
        ) {
            Box {
                // Mancha decorativa de fondo, a la derecha, detrás del texto.
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = 36.dp, y = (-20).dp)
                        .size(210.dp)
                        .clip(CircleShape)
                        .background(lavanda.copy(alpha = 0.35f))
                )
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Cabecera: campana con chispas y título; a la derecha, la X.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier.size(84.dp).clip(CircleShape).background(lavanda),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = null, tint = indigo, modifier = Modifier.size(44.dp))
                            }
                            Canvas(modifier = Modifier.matchParentSize()) {
                                val color = extra.ambar
                                val trazo = 5.dp.toPx()
                                drawLine(color, Offset(size.width * 0.02f, size.height * 0.28f), Offset(size.width * 0.17f, size.height * 0.35f), trazo, StrokeCap.Round)
                                drawLine(color, Offset(size.width * 0.20f, size.height * 0.02f), Offset(size.width * 0.27f, size.height * 0.15f), trazo, StrokeCap.Round)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Avisos de tu familia",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f).padding(end = 36.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "¿Quieres recibir los avisos de los integrantes de tu familia aunque la app esté cerrada?",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "Quita las restricciones de batería para Encaja.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = indigo
                        )
                    }

                    // Panel con la ilustración del móvil y las instrucciones.
                    Surface(shape = RoundedCornerShape(24.dp), color = suave, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            IlustracionMovilBateria()
                            Text(
                                "Se abrirá la ficha de Encaja en los ajustes: entra en «Batería» y elige " +
                                    "«Sin restricciones» (según el móvil, puede llamarse «No optimizar» o " +
                                    "«Permitir actividad en segundo plano»).",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Enlace a la ayuda por marca.
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = suave,
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onMasInfo)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = indigo, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(14.dp))
                            Text(
                                "Más información (Xiaomi, Oppo, Huawei…)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = indigo)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = onAhoraNo,
                            shape = RoundedCornerShape(28.dp),
                            border = BorderStroke(1.5.dp, lavanda),
                            modifier = Modifier.weight(1f).height(56.dp)
                        ) {
                            Text("Ahora no", fontWeight = FontWeight.ExtraBold, color = indigo)
                        }
                        Button(
                            onClick = onQuitar,
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = extra.acento, contentColor = extra.onAcento),
                            modifier = Modifier.weight(1.3f).height(56.dp)
                        ) {
                            Text("Quitar restricciones", fontWeight = FontWeight.ExtraBold, maxLines = 1)
                        }
                    }
                }

                // La X de arriba a la derecha equivale a «Ahora no».
                IconButton(
                    onClick = onAhoraNo,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(lavanda)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = indigo, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

/** Móvil inclinado con un engranaje y una batería verde, con unas chispas al lado. */
@Composable
private fun IlustracionMovilBateria() {
    val extra = LocalEncajaExtraColors.current
    val indigo = MaterialTheme.colorScheme.primary
    val marco = extra.acento.copy(alpha = 0.85f)
    Box(modifier = Modifier.size(width = 92.dp, height = 112.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(width = 64.dp, height = 104.dp)
                .rotate(-8f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(5.dp, marco, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = indigo, modifier = Modifier.size(32.dp))
                Box(
                    modifier = Modifier
                        .size(width = 24.dp, height = 32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF3FBF6B)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(width = 12.dp, height = 3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.85f)))
                        Box(Modifier.size(width = 12.dp, height = 3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.85f)))
                    }
                }
            }
        }
        Canvas(modifier = Modifier.matchParentSize()) {
            val trazo = 3.dp.toPx()
            drawLine(marco, Offset(size.width * 0.80f, size.height * 0.20f), Offset(size.width * 0.93f, size.height * 0.27f), trazo, StrokeCap.Round)
            drawLine(marco, Offset(size.width * 0.84f, size.height * 0.34f), Offset(size.width * 0.98f, size.height * 0.37f), trazo, StrokeCap.Round)
            drawLine(marco, Offset(size.width * 0.80f, size.height * 0.08f), Offset(size.width * 0.85f, size.height * 0.15f), trazo, StrokeCap.Round)
        }
    }
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
    AvisoHoja(
        icono = Icons.Default.BatteryAlert,
        titulo = "Avisos con la app cerrada",
        texto = null,
        peligro = false,
        conX = true,
        acciones = listOf(
            AccionAvisoHoja("Cerrar", EstiloAccionAviso.BORDE) { onCerrar() }
        ),
        onCerrar = onCerrar,
        contenido = {
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
        
        }
    )
}
