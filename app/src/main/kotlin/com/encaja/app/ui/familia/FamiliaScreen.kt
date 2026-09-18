package com.encaja.app.ui.familia

// NOTA: depende de Jetpack Compose y Hilt, no compilado en este entorno.
// Reutiliza la misma paleta y patrón de estados (Cargando/SinFamilia/
// ConDatos) que la pantalla del Semáforo.

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

private val VERDE = Color(0xFFC0DD97)
private val ROJO = Color(0xFFF7C1C1)

@Composable
fun FamiliaScreen(viewModel: FamiliaViewModel = hiltViewModel()) {
    val pantalla by viewModel.pantalla.collectAsState()

    when (val estadoActual = pantalla) {
        is FamiliaPantallaEstado.Cargando -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is FamiliaPantallaEstado.SinFamilia -> {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Vincúlate a una familia desde la pestaña Semana para ver esta pantalla.",
                    textAlign = TextAlign.Center
                )
            }
        }

        is FamiliaPantallaEstado.ConDatos -> {
            val estado = estadoActual.estado
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Text("Familia", style = MaterialTheme.typography.headlineSmall) }

                item {
                    Column {
                        Text("Con quién están las niñas", style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.height(6.dp))
                        FilaDeAsignacion(estado.diasAsignacion, estado.cuidadores)
                    }
                }

                items(estado.cuidadores) { cuidadorSemana ->
                    FilaDeCuidador(cuidadorSemana)
                }
            }
        }
    }
}

@Composable
private fun FilaDeAsignacion(dias: List<DiaAsignado>, cuidadores: List<CuidadorDisponibilidadSemana>) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        dias.forEach { dia ->
            val nombre = cuidadores.firstOrNull { it.caregiver.id == dia.caregiverId }?.caregiver?.nombre
            val iniciales = nombre?.split(" ")?.take(2)?.mapNotNull { it.firstOrNull() }?.joinToString("")?.uppercase()

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    dia.fecha.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("es")).take(1).uppercase(),
                    style = MaterialTheme.typography.labelSmall
                )
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (iniciales != null) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(iniciales ?: "–", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun FilaDeCuidador(cuidadorSemana: CuidadorDisponibilidadSemana) {
    Column {
        Text(cuidadorSemana.caregiver.nombre, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            cuidadorSemana.dias.forEach { dia ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (dia.libre) VERDE else ROJO),
                    contentAlignment = Alignment.Center
                ) {
                    if (!dia.libre) {
                        Text(
                            dia.bloqueos.first().motivo.name.take(3).lowercase(),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}
