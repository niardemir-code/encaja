package com.encaja.app.ui.ajustes

// NOTA: depende de Jetpack Compose y Hilt, no compilado en este entorno.

import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
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
import com.encaja.app.ui.informacion.AvisosBateria
import com.encaja.app.ui.informacion.DialogoMasInformacionBateria
import com.encaja.app.ui.theme.PreferenciaTema
import com.encaja.app.ui.theme.TemaPreferido
import com.encaja.app.ui.theme.AccionAvisoHoja
import com.encaja.app.ui.theme.AvisoHoja
import com.encaja.app.ui.theme.BotonPildoraBordeHoja
import com.encaja.app.ui.theme.BotonPildoraHoja
import com.encaja.app.ui.theme.CabeceraPantalla
import com.encaja.app.ui.theme.EstiloAccionAviso
import com.encaja.app.ui.theme.FondoPantalla
import com.encaja.app.ui.theme.IconoEnCirculoHoja
import com.encaja.app.ui.theme.RotuloSeccionPantalla
import com.encaja.app.ui.theme.TarjetaFilaHoja
import com.encaja.app.ui.theme.acentoHoja
import com.encaja.app.ui.theme.bordeHoja
import com.encaja.app.ui.theme.casillaHoja
import com.encaja.app.ui.theme.lavandaHoja
import com.encaja.app.ui.theme.indigoHoja
import com.encaja.app.ui.theme.onAcentoHoja
import com.encaja.app.ui.theme.tintaHoja
import com.encaja.app.ui.theme.tintaSuaveHoja
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.Logout

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
    onAbrirAyuda: () -> Unit = {},
    onAbrirContacto: () -> Unit = {},
    onAbrirLegal: () -> Unit = {},
    viewModel: AjustesViewModel = hiltViewModel()
) {
    // Con scroll: con las tarjetas de avisos (alarma exacta, batería) ya no cabe todo en
    // una pantalla y, si no, el tema y "Cerrar sesión" quedaban cortados por abajo.
    FondoPantalla {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
        CabeceraPantalla(
            "Ajustes",
            viewModel.emailUsuarioActual?.let { "Sesión iniciada como: $it" }
        )

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
        FilaAjuste(
            icono = Icons.Default.QuestionMark,
            titulo = "Ayuda",
            subtitulo = "Cómo funciona la app, pantalla por pantalla",
            onClick = onAbrirAyuda
        )
        Spacer(Modifier.height(12.dp))
        FilaAjuste(
            icono = Icons.Default.Mail,
            titulo = "Contacto",
            subtitulo = "Escribe al desarrollador",
            onClick = onAbrirContacto
        )
        Spacer(Modifier.height(12.dp))
        FilaAjuste(
            icono = Icons.Default.Lock,
            titulo = "Privacidad y legal",
            subtitulo = "Tus datos, condiciones de uso y aviso legal",
            onClick = onAbrirLegal
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
        BotonPildoraBordeHoja("Cerrar sesión", onClick = onCerrarSesion, icono = Icons.AutoMirrored.Filled.Logout)

        // Darse de baja: desvincula la cuenta de la familia y elimina el acceso con Google.
        var confirmandoBaja by remember { mutableStateOf(false) }
        var borrando by remember { mutableStateOf(false) }
        var errorBaja by remember { mutableStateOf<String?>(null) }
        var codigoParaVolver by remember { mutableStateOf<String?>(null) }
        var cuentaBorrada by remember { mutableStateOf(false) }
        Spacer(Modifier.height(12.dp))
        TextButton(
            onClick = { errorBaja = null; confirmandoBaja = true },
            enabled = !borrando,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Borrar mi cuenta", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
        }
        errorBaja?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        // Cuenta ya borrada: si era la última de la familia, se enseña el código para que quien
        // llegue después pueda retomar a este cuidador con todo su historial.
        if (cuentaBorrada) {
            AvisoHoja(
                icono = Icons.Default.CheckCircle,
                titulo = "Cuenta borrada",
                texto = if (codigoParaVolver != null) {
                    "Eras la última persona con cuenta en esta familia. Su información sigue " +
                        "guardada. Apunta este código: quien lo introduzca al crear su cuenta se " +
                        "vinculará a tu persona y seguirá con todo.\n\n${codigoParaVolver}"
                } else {
                    "Tu cuenta se ha eliminado. La información de la familia sigue guardada."
                },
                peligro = false,
                acciones = listOf(AccionAvisoHoja("Entendido", EstiloAccionAviso.PRIMARIO) { onCerrarSesion() }),
                onCerrar = {}
            )
        }
        if (confirmandoBaja) {
            AvisoHoja(
                icono = Icons.Default.Delete,
                titulo = "¿Borrar tu cuenta?",
                texto = "Se eliminará tu cuenta de la app y dejarás de tener acceso a esta familia y de " +
                    "recibir avisos. Tu persona y todo su historial se quedan en la familia, y podrán " +
                    "volver a vincular a alguien con ella. No se puede deshacer.",
                acciones = listOf(
                    AccionAvisoHoja("Borrar", EstiloAccionAviso.PELIGRO) {
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
                    },
                    AccionAvisoHoja("Cancelar", EstiloAccionAviso.BORDE) { confirmandoBaja = false }
                ),
                onCerrar = { confirmandoBaja = false }
            )
        }
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

    TarjetaAviso {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconoEnCirculoHoja(Icons.Default.Alarm, MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error)
                Text(
                    "Los avisos pueden llegar tarde",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = tintaHoja,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Encaja no tiene permiso para programar alarmas a una hora exacta, así que el móvil " +
                    "puede retrasar los avisos de las actividades bastantes minutos. Para que salten justo " +
                    "a la hora elegida, activa \"Alarmas y recordatorios\" para Encaja.",
                style = MaterialTheme.typography.bodySmall,
                color = tintaSuaveHoja
            )
            Spacer(Modifier.height(14.dp))
            BotonPildoraHoja("Activar", onClick = {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.fromParts("package", contexto.packageName, null)
                }
                contexto.startActivity(intent)
            })
        }
    }
}

/**
 * Si el sistema (sobre todo en Xiaomi, Huawei, Samsung... con su propio ahorro de
 * batería además del de Android) aplica restricciones de fondo a la app, puede matarla
 * mientras no esté en pantalla, y entonces el aviso de una actividad no llega aunque la
 * alarma esté bien programada. Pedir que no se le aplique ahorro de batería reduce (que
 * no elimina del todo, según el fabricante) ese riesgo. Esta sección deja verlo y, si
 * falta, pedirlo directamente. Siempre se muestra, porque la ayuda para los ajustes extra
 * de cada fabricante hace falta aunque la restricción de Android ya esté quitada.
 */
@Composable
private fun SeccionAhorroDeBateria() {
    val contexto = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var exenta by remember { mutableStateOf(AvisosBateria.estaExenta(contexto)) }
    var masInfoAbierta by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) exenta = AvisosBateria.estaExenta(contexto)
        }
        lifecycleOwner.lifecycle.addObserver(observador)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observador) }
    }

    if (masInfoAbierta) DialogoMasInformacionBateria(onCerrar = { masInfoAbierta = false })

    TarjetaAviso {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconoEnCirculoHoja(
                    if (exenta) Icons.Default.CheckCircle else Icons.Default.BatteryAlert,
                    if (exenta) lavandaHoja else MaterialTheme.colorScheme.errorContainer,
                    if (exenta) indigoHoja else MaterialTheme.colorScheme.error
                )
                Text(
                    "Avisos con la app cerrada",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = tintaHoja,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (exenta) {
                    "Encaja no tiene restricciones de batería. Si aun así no te llegan los avisos con la app cerrada, " +
                        "tu móvil puede tener un ahorro de energía propio del fabricante."
                } else {
                    "El ahorro de batería del móvil puede impedir que lleguen los avisos cuando " +
                        "Encaja no está abierta en pantalla. Para evitarlo, quita las restricciones de batería: " +
                        "se abrirá la ficha de Encaja en los ajustes: entra en «Batería» y elige «Sin restricciones» " +
                        "(según el móvil, puede llamarse «No optimizar»)."
                },
                style = MaterialTheme.typography.bodySmall,
                color = tintaSuaveHoja
            )
            Spacer(Modifier.height(14.dp))
            if (!exenta) {
                BotonPildoraHoja("Quitar restricciones", onClick = { AvisosBateria.abrirQuitarRestricciones(contexto) })
                Spacer(Modifier.height(4.dp))
            }
            TextButton(onClick = { masInfoAbierta = true }) {
                Text("Más información (Xiaomi, Oppo, Huawei…)", color = acentoHoja, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Tarjeta blanca redondeada para los avisos (alarmas exactas, batería). */
@Composable
private fun TarjetaAviso(contenido: @Composable () -> Unit) {
    val forma = RoundedCornerShape(22.dp)
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(casillaHoja)
            .border(1.dp, bordeHoja.copy(alpha = 0.5f), forma)
            .padding(16.dp)
    ) { contenido() }
}

/** Elegir entre seguir el tema del teléfono o forzar el claro/oscuro de la app. */
@Composable
private fun SeccionTema() {
    val contexto = LocalContext.current
    val temaActual by PreferenciaTema.actual

    RotuloSeccionPantalla("Tema")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OpcionTema("Sistema", TemaPreferido.SISTEMA, temaActual, contexto, Modifier.weight(1f))
        OpcionTema("Claro", TemaPreferido.CLARO, temaActual, contexto, Modifier.weight(1f))
        OpcionTema("Oscuro", TemaPreferido.OSCURO, temaActual, contexto, Modifier.weight(1f))
    }
}

@Composable
private fun OpcionTema(
    etiqueta: String,
    valor: TemaPreferido,
    actual: TemaPreferido,
    contexto: android.content.Context,
    modifier: Modifier = Modifier
) {
    val elegido = actual == valor
    val forma = RoundedCornerShape(24.dp)
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .height(48.dp)
            .clip(forma)
            .background(if (elegido) acentoHoja else casillaHoja)
            .border(1.dp, if (elegido) acentoHoja else bordeHoja, forma)
            .clickable { PreferenciaTema.elegir(contexto, valor) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            etiqueta,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (elegido) onAcentoHoja else tintaHoja
        )
    }
}

@Composable
private fun FilaAjuste(
    icono: ImageVector,
    titulo: String,
    subtitulo: String,
    onClick: () -> Unit
) {
    TarjetaFilaHoja(onClick = onClick) {
        IconoEnCirculoHoja(icono, lavandaHoja, indigoHoja, tamano = 46.dp)
        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = tintaHoja)
            Text(subtitulo, style = MaterialTheme.typography.bodySmall, color = tintaSuaveHoja)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = tintaSuaveHoja)
    }
}
