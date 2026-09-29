@file:OptIn(ExperimentalMaterial3Api::class)

package com.encaja.app.ui.semana

// NOTA: igual que el ViewModel, este archivo depende de Jetpack Compose
// y no ha podido compilarse en este entorno (sin acceso al repositorio
// de Google). Reproduce fielmente la maqueta de "Esta semana": círculos
// de día y tarjeta de hueco con acciones, con los tres estados: cargando,
// sin familia, con datos. El botón de invitar vive en la barra superior
// (EncajaApp.kt), no aquí.

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.encaja.app.domain.model.Anuncio
import com.encaja.app.domain.model.AnuncioId
import com.encaja.app.domain.usecase.AvisoConflicto
import com.encaja.app.domain.usecase.RolResponsable
import com.encaja.app.ui.familia.etiquetaMotivo
import com.encaja.app.ui.familia.fechaAMillisUtc
import com.encaja.app.ui.familia.millisUtcAFecha
import com.encaja.app.ui.theme.LocalEncajaExtraColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun SemanaScreen(viewModel: SemaforoViewModel = hiltViewModel()) {
    val pantalla by viewModel.pantalla.collectAsState()

    // Igual que en Guía: el ViewModel se conserva al cambiar de pestaña (saveState/
    // restoreState), así que sin esto se seguiría viendo lo que había la última vez
    // que se estuvo aquí — p.ej. una actividad recién creada en Guía no aparecería
    // en Semana hasta cambiar de semana y volver. Al reentrar en la pestaña se
    // vuelve a componer desde cero, así que esto se ejecuta cada vez.
    LaunchedEffect(Unit) { viewModel.recargar() }

    when (val estadoActual = pantalla) {
        is SemaforoPantallaEstado.Cargando -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is SemaforoPantallaEstado.SinFamilia -> {
            var codigo by remember { mutableStateOf("") }
            var error by remember { mutableStateOf<String?>(null) }

            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("Todavía no perteneces a ninguna familia", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = codigo,
                    onValueChange = { codigo = it.uppercase(); error = null },
                    label = { Text("Código de invitación") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.canjearCodigo(codigo) { mensaje -> error = mensaje } },
                    enabled = codigo.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Unirme con este código")
                }
            }
        }

        is SemaforoPantallaEstado.ConDatos -> {
            val uiState = estadoActual.estado
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    CabeceraDeSemana(
                        lunes = uiState.dias.firstOrNull()?.fecha,
                        domingo = uiState.dias.lastOrNull()?.fecha,
                        onSemanaAnterior = { viewModel.cambiarSemana(-1) },
                        onSemanaSiguiente = { viewModel.cambiarSemana(1) },
                        onElegirFecha = { viewModel.irASemanaDe(it) }
                    )
                }

                item { FilaDeDias(uiState.dias) }

                item {
                    TablonDeAnuncios(
                        anuncios = uiState.anuncios,
                        onPublicar = { texto -> viewModel.publicarAnuncio(texto) },
                        onEliminar = { anuncioId -> viewModel.eliminarAnuncio(anuncioId) }
                    )
                }

                items(uiState.huecosDeLaSemana) { hueco ->
                    TarjetaHueco(hueco = hueco)
                }

                items(uiState.avisos) { aviso ->
                    TarjetaAvisoConflicto(aviso = aviso)
                }
            }
        }
    }
}

/** Cabecera de la pantalla Semana: flechas para retroceder/avanzar una semana completa,
 * el rango de fechas (lunes-domingo) en el centro, y un icono de calendario para saltar
 * directamente a la semana que contiene una fecha cualquiera. Igual que la cabecera de
 * la Guía, pero navegando semana a semana en vez de día a día. */
@Composable
private fun CabeceraDeSemana(
    lunes: LocalDate?,
    domingo: LocalDate?,
    onSemanaAnterior: () -> Unit,
    onSemanaSiguiente: () -> Unit,
    onElegirFecha: (LocalDate) -> Unit
) {
    var calendarioAbierto by remember { mutableStateOf(false) }
    val formatter = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale("es"))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onSemanaAnterior) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Semana anterior")
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f).clickable { calendarioAbierto = true }
        ) {
            Text("Esta semana", style = MaterialTheme.typography.titleMedium)
            if (lunes != null && domingo != null) {
                Text(
                    "${lunes.format(formatter)} – ${domingo.format(formatter)}",
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
        IconButton(onClick = { calendarioAbierto = true }) {
            Icon(Icons.Default.CalendarMonth, contentDescription = "Ir a una semana")
        }
        IconButton(onClick = onSemanaSiguiente) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Semana siguiente")
        }
    }

    if (calendarioAbierto) {
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = fechaAMillisUtc(lunes ?: LocalDate.now())
        )
        DatePickerDialog(
            onDismissRequest = { calendarioAbierto = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { onElegirFecha(millisUtcAFecha(it)) }
                    calendarioAbierto = false
                }) { Text("Ir") }
            },
            dismissButton = { TextButton(onClick = { calendarioAbierto = false }) { Text("Cancelar") } }
        ) {
            DatePicker(state = estado)
        }
    }
}

/**
 * Botón de "invitar a alguien": ahora vive en la barra superior (a la izquierda del
 * avatar), no en la lista de Semana, así que se puede usar desde cualquier pestaña.
 * Se le pasa el mismo [SemaforoViewModel] de la pestaña Semana (obtenido en
 * EncajaApp a partir de su propia entrada del back stack), de donde saca la lista
 * de cuidadores y genera el código.
 */
@Composable
fun BotonInvitar(viewModel: SemaforoViewModel) {
    var mostrarSelector by remember { mutableStateOf(false) }
    var codigoGenerado by remember { mutableStateOf<String?>(null) }
    var errorInvitacion by remember { mutableStateOf<String?>(null) }
    var copiado by remember { mutableStateOf(false) }
    val cuidadores by viewModel.cuidadores.collectAsState()
    val clipboard = LocalClipboardManager.current

    IconButton(onClick = { mostrarSelector = true }) {
        Icon(Icons.Default.PersonAdd, contentDescription = "Invitar a alguien")
    }

    if (mostrarSelector) {
        AlertDialog(
            onDismissRequest = {
                mostrarSelector = false
                codigoGenerado = null
                errorInvitacion = null
                copiado = false
            },
            title = { Text(if (codigoGenerado != null) "Código generado" else "¿Para quién es la invitación?") },
            text = {
                Column {
                    when {
                        codigoGenerado != null -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    codigoGenerado!!,
                                    style = MaterialTheme.typography.headlineMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = {
                                    clipboard.setText(AnnotatedString(codigoGenerado!!))
                                    copiado = true
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copiar código")
                                }
                            }
                            if (copiado) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Copiado al portapapeles",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Compártelo con esa persona. Deja de funcionar en cuanto se use una vez.")
                        }
                        errorInvitacion != null -> {
                            Text(errorInvitacion!!, color = MaterialTheme.colorScheme.error)
                        }
                        else -> {
                            cuidadores.forEach { caregiver ->
                                TextButton(onClick = {
                                    viewModel.generarInvitacion(
                                        caregiver.id,
                                        alConseguirlo = { codigo -> codigoGenerado = codigo },
                                        alFallar = { mensaje -> errorInvitacion = mensaje }
                                    )
                                }) {
                                    Text(caregiver.nombreCompleto)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    mostrarSelector = false
                    codigoGenerado = null
                    errorInvitacion = null
                    copiado = false
                }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

@Composable
private fun FilaDeDias(dias: List<DiaSemaforo>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        dias.forEach { dia ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = dia.fecha.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("es")).take(1).uppercase(),
                    style = MaterialTheme.typography.labelSmall
                )
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(colorParaEstado(dia.estado)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        dia.fecha.dayOfMonth.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = colorTextoParaEstado(dia.estado),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun TablonDeAnuncios(
    anuncios: List<Anuncio>,
    onPublicar: (String) -> Unit,
    onEliminar: (AnuncioId) -> Unit
) {
    var textoNuevo by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(12.dp)
    ) {
        Text("Tablón de anuncios", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        if (anuncios.isEmpty()) {
            Text(
                "Todavía no hay ningún anuncio.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.height(8.dp))
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                anuncios.forEach { anuncio ->
                    FilaAnuncio(anuncio = anuncio, onEliminar = { onEliminar(anuncio.id) })
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = textoNuevo,
                onValueChange = { textoNuevo = it },
                label = { Text("Nuevo anuncio") },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(
                enabled = textoNuevo.isNotBlank(),
                onClick = {
                    onPublicar(textoNuevo)
                    textoNuevo = ""
                }
            ) {
                Text("Publicar")
            }
        }
    }
}

@Composable
private fun FilaAnuncio(anuncio: Anuncio, onEliminar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${anuncio.autorNombre} · ${formatearFechaAnuncio(anuncio.publicadoEn)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Text(anuncio.texto, style = MaterialTheme.typography.bodyMedium)
        }
        IconButton(onClick = onEliminar) {
            Icon(Icons.Default.Delete, contentDescription = "Eliminar anuncio")
        }
    }
}

@Composable
private fun colorParaEstado(estado: EstadoDia): Color = when (estado) {
    EstadoDia.VERDE -> LocalEncajaExtraColors.current.verdeContainer
    EstadoDia.AMBAR -> MaterialTheme.colorScheme.tertiaryContainer
    EstadoDia.ROJO -> MaterialTheme.colorScheme.errorContainer
    EstadoDia.SIN_DATOS -> MaterialTheme.colorScheme.surfaceVariant
}

/** Color del número dentro de cada círculo del semáforo: mismo criterio que las
 * tarjetas de hueco y aviso de conflicto — fondo pálido del estado + texto en el
 * tono fuerte de ese mismo color, en vez de un texto neutro encima. */
@Composable
private fun colorTextoParaEstado(estado: EstadoDia): Color = when (estado) {
    EstadoDia.VERDE -> LocalEncajaExtraColors.current.onVerdeContainer
    EstadoDia.AMBAR -> MaterialTheme.colorScheme.onTertiaryContainer
    EstadoDia.ROJO -> MaterialTheme.colorScheme.onErrorContainer
    EstadoDia.SIN_DATOS -> MaterialTheme.colorScheme.onSurfaceVariant
}

/** "Lunes 21" en vez de la fecha ISO en bruto (2026-09-21). */
private fun formatearFechaHueco(fecha: java.time.LocalDate): String {
    val nombreDia = fecha.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("es")).replaceFirstChar { it.uppercase() }
    return "$nombreDia ${fecha.dayOfMonth}"
}

/** "18:30" en vez del LocalTime en bruto (18:30:00 o 18:30). */
private fun formatearHoraHueco(hora: java.time.LocalTime): String =
    hora.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))

@Composable
private fun TarjetaHueco(hueco: com.encaja.app.domain.model.Hueco) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(12.dp)
    ) {
        Text(
            text = "${formatearFechaHueco(hueco.need.fecha)} · falta cubrir",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "${formatearHoraHueco(hueco.need.horaInicio)} — ${hueco.need.descripcion}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

/** "Ojo, Sílvia tiene asignado llevar y recoger a Etna el sábado en "Fútbol de
 * Etna", pero tiene asignada "Trabajo" a esa hora." Un único aviso aunque la
 * misma persona esté asignada a llevar y a recoger (no cambia el semáforo). */
private fun textoAviso(aviso: AvisoConflicto): String {
    val accion = when {
        RolResponsable.LLEVA in aviso.roles && RolResponsable.RECOGE in aviso.roles -> "llevar y recoger"
        RolResponsable.LLEVA in aviso.roles -> "llevar"
        else -> "recoger"
    }
    val dia = aviso.need.fecha.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("es"))
    val tarea = aviso.bloque.etiqueta ?: etiquetaMotivo(aviso.bloque.motivo)
    return "Ojo, ${aviso.caregiver.nombreCompleto} tiene asignado $accion a ${aviso.child.nombre} " +
        "el $dia en \"${aviso.need.descripcion}\", pero tiene asignada \"$tarea\" a esa hora."
}

@Composable
private fun TarjetaAvisoConflicto(aviso: AvisoConflicto) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(12.dp)
    ) {
        Text(
            text = aviso.need.descripcion,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = textoAviso(aviso),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}

