package com.encaja.app.ui.semana

// NOTA: igual que el ViewModel, este archivo depende de Jetpack Compose
// y no ha podido compilarse en este entorno (sin acceso al repositorio
// de Google). Reproduce fielmente la maqueta de "Esta semana": círculos
// de día, tarjeta de hueco con acciones y barra de reparto — ahora
// además con los tres estados: cargando, sin familia, con datos.

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun SemanaScreen(viewModel: SemaforoViewModel = hiltViewModel()) {
    val pantalla by viewModel.pantalla.collectAsState()

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
                    val formatter = java.time.format.DateTimeFormatter.ofPattern("dd-MM")
                    val rango = if (uiState.dias.isNotEmpty()) {
                        " (${uiState.dias.first().fecha.format(formatter)} al ${uiState.dias.last().fecha.format(formatter)})"
                    } else ""
                    Text("Esta semana$rango", style = MaterialTheme.typography.headlineSmall)
                }

                item { FilaDeDias(uiState.dias) }

                items(uiState.huecosDeLaSemana) { hueco ->
                    TarjetaHueco(hueco = hueco)
                }

                item { BarraDeReparto(uiState.reparto, uiState.totalTramos) }

                item { InvitarSeccion(viewModel) }
            }
        }
    }
}

@Composable
private fun InvitarSeccion(viewModel: SemaforoViewModel) {
    var mostrarSelector by remember { mutableStateOf(false) }
    var codigoGenerado by remember { mutableStateOf<String?>(null) }
    var errorInvitacion by remember { mutableStateOf<String?>(null) }
    val cuidadores by viewModel.cuidadores.collectAsState()

    OutlinedButton(onClick = { mostrarSelector = true }) {
        Text("Invitar a alguien")
    }

    if (mostrarSelector) {
        AlertDialog(
            onDismissRequest = {
                mostrarSelector = false
                codigoGenerado = null
                errorInvitacion = null
            },
            title = { Text(if (codigoGenerado != null) "Código generado" else "¿Para quién es la invitación?") },
            text = {
                Column {
                    when {
                        codigoGenerado != null -> {
                            Text(codigoGenerado!!, style = MaterialTheme.typography.headlineMedium)
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
                                    Text(caregiver.nombre)
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
                    Text(dia.fecha.dayOfMonth.toString(), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun colorParaEstado(estado: EstadoDia): Color = when (estado) {
    EstadoDia.VERDE -> Color(0xFFC0DD97)
    EstadoDia.ROJO -> Color(0xFFF7C1C1)
    EstadoDia.SIN_DATOS -> MaterialTheme.colorScheme.surfaceVariant
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

private val paletaCuidadores = listOf(
    Color(0xFFB5D4F4), // azul
    Color(0xFFF4C0D1), // rosa
    Color(0xFF9FE1CB), // verde
    Color(0xFFDDD3F7), // lila
    Color(0xFFFAC775), // ámbar
    Color(0xFFF0997B)  // coral
)

@Composable
private fun BarraDeReparto(reparto: List<TramoReparto>, total: Int) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Reparto de la semana", style = MaterialTheme.typography.labelSmall)
            Text("$total tramos", style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
        ) {
            reparto.forEachIndexed { index, tramo ->
                Box(
                    modifier = Modifier
                        .weight(tramo.tramos.toFloat().coerceAtLeast(0.01f))
                        .fillMaxHeight()
                        .background(paletaCuidadores[index % paletaCuidadores.size])
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        reparto.forEachIndexed { index, tramo ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(paletaCuidadores[index % paletaCuidadores.size])
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "${tramo.nombre}  ${tramo.tramos}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
