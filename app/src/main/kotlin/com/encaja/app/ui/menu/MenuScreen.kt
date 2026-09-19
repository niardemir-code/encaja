package com.encaja.app.ui.menu

// NOTA: depende de Jetpack Compose y Hilt, no compilado en este entorno.
// El estado de los 7 días vive en este composable (no en cada tarjeta) para
// poder guardar toda la semana con un único botón, en vez de un botón de
// guardar por cada día.

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.encaja.app.domain.model.ComidaDelDia
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun MenuScreen(viewModel: MenuViewModel = hiltViewModel()) {
    val pantalla by viewModel.pantalla.collectAsState()

    when (val estadoActual = pantalla) {
        is MenuPantallaEstado.Cargando -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is MenuPantallaEstado.SinFamilia -> {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Vincúlate a una familia desde la pestaña Semana para ver esta pantalla.",
                    textAlign = TextAlign.Center
                )
            }
        }

        is MenuPantallaEstado.ConDatos -> {
            ContenidoMenuSemanal(
                dias = estadoActual.dias,
                onGuardarSemana = { dias -> viewModel.guardarSemana(dias) }
            )
        }
    }
}

@Composable
private fun ContenidoMenuSemanal(
    dias: List<ComidaDelDia>,
    onGuardarSemana: (List<ComidaDelDia>) -> Unit
) {
    // Un par de campos (comida, cena) por cada día de la semana, hospedados aquí
    // arriba para poder juntarlos todos al pulsar el único botón de guardar.
    val estadosPorDia = dias.map { dia ->
        remember(dia.fecha) { mutableStateOf(dia.comida ?: "") } to
            remember(dia.fecha) { mutableStateOf(dia.cena ?: "") }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Text("Menú semanal", style = MaterialTheme.typography.headlineSmall) }

        items(dias.size) { index ->
            val dia = dias[index]
            val (comida, cena) = estadosPorDia[index]
            TarjetaDia(fecha = dia.fecha, comida = comida, cena = cena)
        }

        item {
            Button(
                onClick = {
                    val diasActualizados = dias.mapIndexed { index, dia ->
                        val (comida, cena) = estadosPorDia[index]
                        dia.copy(comida = comida.value, cena = cena.value)
                    }
                    onGuardarSemana(diasActualizados)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar semana")
            }
        }
    }
}

@Composable
private fun TarjetaDia(
    fecha: LocalDate,
    comida: MutableState<String>,
    cena: MutableState<String>
) {
    val etiquetaDia = fecha.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("es"))
        .replaceFirstChar { it.uppercase() }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "$etiquetaDia ${fecha.dayOfMonth}",
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = comida.value,
                onValueChange = { comida.value = it },
                label = { Text("Comida") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = cena.value,
                onValueChange = { cena.value = it },
                label = { Text("Cena") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
