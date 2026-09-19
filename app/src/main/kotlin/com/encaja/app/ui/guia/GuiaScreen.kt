package com.encaja.app.ui.guia

// NOTA: depende de Jetpack Compose y Hilt, no compilado en este entorno.
// Reproduce la idea original de las maquetas: cada niño es como un canal
// de TDT, con sus actividades del día colocadas en una franja horaria
// (verde si están cubiertas, rojo si son un hueco).

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// La franja horaria que se representa en la guía: de 7:00 a 22:00.
private val INICIO_FRANJA: LocalTime = LocalTime.of(7, 0)
private val FIN_FRANJA: LocalTime = LocalTime.of(22, 0)
private val MINUTOS_FRANJA = java.time.Duration.between(INICIO_FRANJA, FIN_FRANJA).toMinutes().toInt()

private val VERDE = Color(0xFFC0DD97)
private val ROJO = Color(0xFFF7C1C1)

@Composable
fun GuiaScreen(viewModel: GuiaViewModel = hiltViewModel()) {
    val pantalla by viewModel.pantalla.collectAsState()

    when (val estadoActual = pantalla) {
        is GuiaPantallaEstado.Cargando -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is GuiaPantallaEstado.SinFamilia -> {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Vincúlate a una familia desde la pestaña Semana para ver esta pantalla.",
                    textAlign = TextAlign.Center
                )
            }
        }

        is GuiaPantallaEstado.ConDatos -> {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                CabeceraDeDia(
                    fecha = estadoActual.estado.fecha,
                    onDiaAnterior = { viewModel.diaAnterior() },
                    onDiaSiguiente = { viewModel.diaSiguiente() }
                )
                Spacer(Modifier.height(16.dp))

                if (estadoActual.estado.filas.isEmpty()) {
                    Text(
                        "Todavía no hay niños dados de alta en la familia.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        estadoActual.estado.filas.forEach { fila ->
                            FilaGuiaDelNino(fila)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CabeceraDeDia(fecha: LocalDate, onDiaAnterior: () -> Unit, onDiaSiguiente: () -> Unit) {
    val formatter = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale("es"))
    val etiquetaDia = fecha.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("es"))
        .replaceFirstChar { it.uppercase() }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onDiaAnterior) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Día anterior")
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(etiquetaDia, style = MaterialTheme.typography.titleMedium)
            Text(fecha.format(formatter), style = MaterialTheme.typography.labelMedium)
        }
        IconButton(onClick = onDiaSiguiente) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Día siguiente")
        }
    }
}

@Composable
private fun FilaGuiaDelNino(fila: FilaGuia) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(fila.child.nombre, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))

        if (fila.bloques.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Sin actividades hoy",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                val anchoTotal = maxWidth
                fila.bloques.forEach { bloque ->
                    val inicioMin = minutosDesdeInicioFranja(bloque.need.horaInicio)
                    val finMin = minutosDesdeInicioFranja(bloque.need.horaFin).coerceAtLeast(inicioMin + 20)
                    val offsetX = anchoTotal * (inicioMin.toFloat() / MINUTOS_FRANJA)
                    val ancho = anchoTotal * ((finMin - inicioMin).toFloat() / MINUTOS_FRANJA)

                    Box(
                        modifier = Modifier
                            .offset(x = offsetX)
                            .width(ancho)
                            .fillMaxHeight()
                            .padding(horizontal = 1.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (bloque.cubierto) VERDE else ROJO)
                            .padding(6.dp)
                    ) {
                        Text(
                            bloque.need.descripcion,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}

/** Minutos transcurridos desde INICIO_FRANJA, recortados a la propia franja (7:00–22:00). */
private fun minutosDesdeInicioFranja(hora: LocalTime): Int {
    val minutos = java.time.Duration.between(INICIO_FRANJA, hora).toMinutes().toInt()
    return minutos.coerceIn(0, MINUTOS_FRANJA)
}
