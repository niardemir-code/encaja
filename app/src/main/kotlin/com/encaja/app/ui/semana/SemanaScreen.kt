package com.encaja.app.ui.semana

// NOTA: igual que el ViewModel, este archivo depende de Jetpack Compose
// y no ha podido compilarse en este entorno (sin acceso al repositorio
// de Google). Reproduce fielmente la maqueta de "Esta semana": círculos
// de día, tarjeta de hueco con acciones y barra de reparto.

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    val uiState by viewModel.uiState.collectAsState()

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
            text = "${hueco.need.fecha} · falta cubrir",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "${hueco.need.horaInicio} — ${hueco.need.descripcion}",
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
