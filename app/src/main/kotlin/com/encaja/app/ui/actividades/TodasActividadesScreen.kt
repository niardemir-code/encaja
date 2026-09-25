package com.encaja.app.ui.actividades

// NOTA: depende de Jetpack Compose y Hilt, no compilado en este entorno.
// Pantalla "Todas las actividades" (Ajustes → Todas las actividades): el listado completo
// de actividades de cada niño, con una casilla por actividad y un botón para borrar de
// golpe la selección — para las antiguas que ya no ofrecen "esta y las siguientes" en el
// diálogo de la Guía (p.ej. porque no pertenecen a ningún grupo de repetición). Un buscador
// por texto ayuda a marcar rápido todas las de un mismo nombre (p.ej. "Hello English").

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.encaja.app.domain.model.CoverageNeed
import com.encaja.app.domain.model.CoverageNeedId
import com.encaja.app.ui.familia.formatearHora
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ES = Locale("es")
private val FORMATO_FECHA = DateTimeFormatter.ofPattern("d MMM yyyy", ES)

@Composable
fun TodasActividadesScreen(viewModel: TodasActividadesViewModel = hiltViewModel()) {
    val pantalla by viewModel.pantalla.collectAsState()
    var seleccionadas by remember { mutableStateOf(setOf<CoverageNeedId>()) }
    var filtro by remember { mutableStateOf("") }
    var confirmarBorrado by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.recargar() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Todas las actividades", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            "El listado completo, para borrar de golpe actividades antiguas que ya no se pueden " +
                "gestionar en bloque desde la Guía.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        when (val estadoActual = pantalla) {
            is TodasActividadesPantallaEstado.Cargando -> {
                CircularProgressIndicator()
            }

            is TodasActividadesPantallaEstado.SinFamilia -> {
                Text(
                    "Vincúlate a una familia desde la pestaña Semana para ver esta pantalla.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            is TodasActividadesPantallaEstado.ConDatos -> {
                OutlinedTextField(
                    value = filtro,
                    onValueChange = { filtro = it },
                    label = { Text("Buscar por texto (p.ej. \"Hello English\")") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))

                val gruposFiltrados = estadoActual.grupos.map { grupo ->
                    grupo.copy(
                        actividades = grupo.actividades.filter {
                            filtro.isBlank() || it.descripcion.contains(filtro, ignoreCase = true)
                        }
                    )
                }
                val visiblesIds = gruposFiltrados.flatMap { it.actividades.map { a -> a.id } }.toSet()
                val hayActividadesVisibles = visiblesIds.isNotEmpty()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${seleccionadas.size} seleccionada(s)",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Row {
                        TextButton(onClick = { seleccionadas = visiblesIds }, enabled = hayActividadesVisibles) {
                            Text("Marcar todo")
                        }
                        TextButton(onClick = { seleccionadas = emptySet() }, enabled = seleccionadas.isNotEmpty()) {
                            Text("Ninguna")
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (!hayActividadesVisibles) {
                        Text(
                            if (filtro.isBlank()) "Todavía no hay actividades."
                            else "Ninguna actividad coincide con \"$filtro\".",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    gruposFiltrados.forEach { grupo ->
                        if (grupo.actividades.isNotEmpty()) {
                            Text(
                                grupo.child.nombre,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            grupo.actividades.forEach { actividad ->
                                FilaActividad(
                                    actividad = actividad,
                                    marcada = actividad.id in seleccionadas,
                                    onMarcar = { marcada ->
                                        seleccionadas = if (marcada) seleccionadas + actividad.id else seleccionadas - actividad.id
                                    }
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { confirmarBorrado = true },
                    enabled = seleccionadas.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Borrar ${seleccionadas.size} actividad(es)")
                }

                if (confirmarBorrado) {
                    AlertDialog(
                        onDismissRequest = { confirmarBorrado = false },
                        title = { Text("¿Borrar ${seleccionadas.size} actividad(es)?") },
                        text = { Text("No se puede deshacer.") },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.eliminarSeleccionadas(seleccionadas)
                                seleccionadas = emptySet()
                                confirmarBorrado = false
                            }) {
                                Text("Borrar", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissButton = { TextButton(onClick = { confirmarBorrado = false }) { Text("Cancelar") } }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilaActividad(actividad: CoverageNeed, marcada: Boolean, onMarcar: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onMarcar(!marcada) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = marcada, onCheckedChange = onMarcar)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${actividad.fecha.format(FORMATO_FECHA)} · ${formatearHora(actividad.horaInicio)}–" +
                    "${formatearHora(actividad.horaFin)} · ${actividad.descripcion}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (actividad.grupoRepeticionId != null) {
                Text(
                    "Se repite cada semana",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
