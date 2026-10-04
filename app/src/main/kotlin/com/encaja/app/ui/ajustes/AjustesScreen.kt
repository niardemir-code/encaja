package com.encaja.app.ui.ajustes

// NOTA: depende de Jetpack Compose y Hilt, no compilado en este entorno.

import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.encaja.app.ui.theme.PreferenciaTema
import com.encaja.app.ui.theme.TemaPreferido

/**
 * Menú raíz de Ajustes: solo enlaces a cada bloque (Niños, Cuidadores —que incluye
 * las Unidades familiares—, Actividades), para no amontonar aquí todos los
 * formularios. Cada bloque es su propia pantalla, con flecha de "atrás" hacia aquí.
 */
@Composable
fun AjustesScreen(
    onCerrarSesion: () -> Unit,
    onAbrirNinos: () -> Unit,
    onAbrirCuidadores: () -> Unit,
    onAbrirTodasActividades: () -> Unit,
    onAbrirOcupaciones: () -> Unit = {},
    viewModel: AjustesViewModel = hiltViewModel()
) {
    // Con scroll: con las tarjetas de avisos (alarma exacta, batería) ya no cabe todo en
    // una pantalla y, si no, el tema y "Cerrar sesión" quedaban cortados por abajo.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Ajustes", style = MaterialTheme.typography.headlineSmall)
        viewModel.emailUsuarioActual?.let { email ->
            Spacer(Modifier.height(4.dp))
            Text(
                "Sesión iniciada como: $email",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(24.dp))

        FilaAjuste(
            icono = Icons.Default.ChildCare,
            titulo = "Niños",
            subtitulo = "Añadir o quitar niños de la familia",
            onClick = onAbrirNinos
        )
        Spacer(Modifier.height(12.dp))
        FilaAjuste(
            icono = Icons.Default.Group,
            titulo = "Cuidadores",
            subtitulo = "Personas y unidades familiares",
            onClick = onAbrirCuidadores
        )
        Spacer(Modifier.height(12.dp))
        FilaAjuste(
            icono = Icons.Default.CalendarViewWeek,
            titulo = "Actividades",
            subtitulo = "Ver y editar todas las actividades",
            onClick = onAbrirTodasActividades
        )
        Spacer(Modifier.height(12.dp))
        FilaAjuste(
            icono = Icons.Default.EventBusy,
            titulo = "Ocupaciones",
            subtitulo = "Todo lo apuntado en Familia, para repasar y borrar",
            onClick = onAbrirOcupaciones
        )

        Spacer(Modifier.height(12.dp))
        val contexto = LocalContext.current
        var ayudasReiniciadas by remember { mutableStateOf(false) }
        FilaAjuste(
            icono = Icons.Default.Info,
            titulo = "Ver las ayudas otra vez",
            subtitulo = if (ayudasReiniciadas) "Hecho: volverán a mostrarse al entrar en cada pantalla"
            else "Repite la guía de la primera vez en cada pantalla",
            onClick = {
                com.encaja.app.ui.ayuda.PreferenciaAyuda.reiniciar(contexto)
                ayudasReiniciadas = true
            }
        )

        Spacer(Modifier.height(24.dp))
        SeccionAlarmasExactas()

        Spacer(Modifier.height(12.dp))
        SeccionAhorroDeBateria()

        Spacer(Modifier.height(24.dp))
        SeccionTema()

        Spacer(Modifier.height(32.dp))
        OutlinedButton(onClick = onCerrarSesion, modifier = Modifier.fillMaxWidth()) {
            Text("Cerrar sesión")
        }

        // Darse de baja: desvincula la cuenta de la familia y elimina el acceso con Google.
        var confirmandoBaja by remember { mutableStateOf(false) }
        var borrando by remember { mutableStateOf(false) }
        var errorBaja by remember { mutableStateOf<String?>(null) }
        var codigoParaVolver by remember { mutableStateOf<String?>(null) }
        var cuentaBorrada by remember { mutableStateOf(false) }
        Spacer(Modifier.height(12.dp))
        androidx.compose.material3.TextButton(
            onClick = { errorBaja = null; confirmandoBaja = true },
            enabled = !borrando,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Borrar mi cuenta", color = MaterialTheme.colorScheme.error)
        }
        errorBaja?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        // Cuenta ya borrada: si era la última de la familia, se enseña el código para que quien
        // llegue después pueda retomar a este cuidador con todo su historial.
        if (cuentaBorrada) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = {},
                title = { Text("Cuenta borrada") },
                text = {
                    if (codigoParaVolver != null) {
                        Text(
                            "Eras la última persona con cuenta en esta familia. Su información sigue " +
                                "guardada. Apunta este código: quien lo introduzca al crear su cuenta se " +
                                "vinculará a tu persona y seguirá con todo.\n\n${codigoParaVolver}"
                        )
                    } else {
                        Text("Tu cuenta se ha eliminado. La información de la familia sigue guardada.")
                    }
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = { onCerrarSesion() }) { Text("Entendido") }
                }
            )
        }
        if (confirmandoBaja) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { confirmandoBaja = false },
                title = { Text("¿Borrar tu cuenta?") },
                text = {
                    Text(
                        "Se eliminará tu cuenta de la app y dejarás de tener acceso a esta familia y de " +
                            "recibir avisos. Tu persona y todo su historial se quedan en la familia, y podrán " +
                            "volver a vincular a alguien con ella. No se puede deshacer."
                    )
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        confirmandoBaja = false
                        borrando = true
                        viewModel.borrarMiCuenta(
                            alTerminar = { codigo ->
                                borrando = false
                                codigoParaVolver = codigo
                                cuentaBorrada = true
                            },
                            alFallar = { borrando = false; errorBaja = it }
                        )
                    }) { Text("Borrar", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { confirmandoBaja = false }) { Text("Cancelar") }
                }
            )
        }
    }
}

/**
 * A partir de Android 13, el permiso "Alarmas y recordatorios" (alarma exacta) no se
 * concede solo con declararlo en el manifest: hay que activarlo a mano en los ajustes
 * del sistema. Si no está activo, los avisos de las actividades se programan como
 * alarmas "aproximadas", que Android puede retrasar bastantes minutos (sobre todo con
 * ahorro de batería), en vez de saltar justo a la hora elegida. Esta sección deja
 * verlo y, si falta, abrir directamente esos ajustes.
 */
@Composable
private fun SeccionAlarmasExactas() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val contexto = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    fun estaConcedido(): Boolean =
        contexto.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() ?: true

    var concedido by remember { mutableStateOf(estaConcedido()) }

    // Al volver de los ajustes del sistema (donde el usuario puede haberlo activado o
    // desactivado) se vuelve a comprobar, porque la pantalla no se recrea.
    DisposableEffect(lifecycleOwner) {
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) concedido = estaConcedido()
        }
        lifecycleOwner.lifecycle.addObserver(observador)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observador) }
    }

    if (concedido) return

    OutlinedCard(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Alarm, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text(
                    "Los avisos pueden llegar tarde",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Encaja no tiene permiso para programar alarmas a una hora exacta, así que el móvil " +
                    "puede retrasar los avisos de las actividades bastantes minutos. Para que salten justo " +
                    "a la hora elegida, activa \"Alarmas y recordatorios\" para Encaja.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.fromParts("package", contexto.packageName, null)
                }
                contexto.startActivity(intent)
            }) {
                Text("Activar")
            }
        }
    }
}

/**
 * Si el sistema (sobre todo en Xiaomi, Huawei, Samsung... con su propio ahorro de
 * batería además del de Android) aplica restricciones de fondo a la app, puede matarla
 * mientras no esté en pantalla, y entonces el aviso de una actividad no llega aunque la
 * alarma esté bien programada. Pedir que no se le aplique ahorro de batería reduce (que
 * no elimina del todo, según el fabricante) ese riesgo. Esta sección deja verlo y, si
 * falta, pedirlo directamente.
 */
@Composable
private fun SeccionAhorroDeBateria() {
    val contexto = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    fun estaExenta(): Boolean =
        contexto.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(contexto.packageName) ?: true

    var exenta by remember { mutableStateOf(estaExenta()) }

    DisposableEffect(lifecycleOwner) {
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) exenta = estaExenta()
        }
        lifecycleOwner.lifecycle.addObserver(observador)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observador) }
    }

    if (exenta) return

    OutlinedCard(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BatteryAlert, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text(
                    "Avisos con la app cerrada",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "El ahorro de batería del móvil puede impedir que lleguen los avisos cuando " +
                    "Encaja no está abierta en pantalla. Para evitarlo, quita las restricciones de batería.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.fromParts("package", contexto.packageName, null)
                }
                contexto.startActivity(intent)
            }) {
                Text("Quitar restricciones")
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "En algunos móviles (Xiaomi, Huawei, Samsung...) hay además un ajuste propio del " +
                    "fabricante (\"Inicio automático\", \"Apps protegidas\"...) que conviene activar " +
                    "también para Encaja, desde los ajustes de batería del propio teléfono.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Elegir entre seguir el tema del teléfono o forzar el claro/oscuro de la app. */
@Composable
private fun SeccionTema() {
    val contexto = LocalContext.current
    val temaActual by PreferenciaTema.actual

    Text("Tema", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OpcionTema("Sistema", TemaPreferido.SISTEMA, temaActual, contexto)
        OpcionTema("Claro", TemaPreferido.CLARO, temaActual, contexto)
        OpcionTema("Oscuro", TemaPreferido.OSCURO, temaActual, contexto)
    }
}

@Composable
private fun OpcionTema(etiqueta: String, valor: TemaPreferido, actual: TemaPreferido, contexto: android.content.Context) {
    FilterChip(
        selected = actual == valor,
        onClick = { PreferenciaTema.elegir(contexto, valor) },
        label = { Text(etiqueta) }
    )
}

@Composable
private fun FilaAjuste(
    icono: ImageVector,
    titulo: String,
    subtitulo: String,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text(titulo, style = MaterialTheme.typography.titleMedium)
                    Text(
                        subtitulo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}
