@file:OptIn(ExperimentalLayoutApi::class)

package com.encaja.app.ui.ocupaciones

// NOTA: depende de Jetpack Compose y Hilt, no compilado en este entorno.
// Pantalla "Ocupaciones" (Ajustes → Ocupaciones): el listado de todas las ocupaciones
// vigentes de personas y unidades familiares, con filtro por quién (una ventana) y por texto,
// una casilla por ocupación y un botón para borrar de golpe la selección. Mismo estilo
// que "Todas las actividades".

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.encaja.app.ui.familia.textoHorario
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ES = Locale("es")
private val FORMATO_FECHA = DateTimeFormatter.ofPattern("EEE d MMM yyyy", ES)

@Composable
fun OcupacionesScreen(viewModel: OcupacionesViewModel = hiltViewModel()) {
    val pantalla by viewModel.pantalla.collectAsState()
    var seleccionadas by remember { mutableStateOf(setOf<String>()) }
    var filtroQuien by remember { mutableStateOf<String?>(null) }
    var filtroTexto by remember { mutableStateOf("") }
    var confirmarBorrado by remember { mutableStateOf(false) }
    var selectorAbierto by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.recargar() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Ocupaciones", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))

        when (val estadoActual = pantalla) {
            is OcupacionesPantallaEstado.Cargando -> CircularProgressIndicator()

            is OcupacionesPantallaEstado.SinFamilia -> Text(
                "Vincúlate a una familia desde la pestaña Semana para ver esta pantalla.",
                style = MaterialTheme.typography.bodyMedium
            )

            is OcupacionesPantallaEstado.ConDatos -> {
                // Filtro por quién (Todos, cada persona, cada unidad): un solo botón que
                // muestra la elección actual y abre una ventana para cambiarla, para no
                // comerle sitio al listado. Al lado, el buscador por texto.
                val opciones = listOf(OpcionFiltro(null, "Todos")) +
                    estadoActual.cuidadores.map { OpcionFiltro(it.id.value, it.nombre) } +
                    estadoActual.unidades.map { OpcionFiltro(it.id.value, it.nombre) }
                val etiquetaFiltro = opciones.firstOrNull { it.idTexto == filtroQuien }?.etiqueta ?: "Todos"
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = filtroTexto,
                        onValueChange = { filtroTexto = it },
                        placeholder = { Text("Buscar…") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        selected = filtroQuien != null,
                        onClick = { selectorAbierto = true },
                        label = { Text(etiquetaFiltro) },
                        leadingIcon = { Icon(Icons.Default.FilterAlt, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
                Spacer(Modifier.height(4.dp))

                if (selectorAbierto) {
                    AlertDialog(
                        onDismissRequest = { selectorAbierto = false },
                        title = { Text("Mostrar ocupaciones de") },
                        text = {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                opciones.forEachIndexed { indice, opcion ->
                                    if (indice == 1 && estadoActual.cuidadores.isNotEmpty()) {
                                        Text("Personas", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                    }
                                    if (indice == 1 + estadoActual.cuidadores.size && estadoActual.unidades.isNotEmpty()) {
                                        Spacer(Modifier.height(6.dp))
                                        Text("Unidades familiares", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { filtroQuien = opcion.idTexto; selectorAbierto = false }
                                            .padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = filtroQuien == opcion.idTexto,
                                            onClick = { filtroQuien = opcion.idTexto; selectorAbierto = false }
                                        )
                                        Text(opcion.etiqueta, style = MaterialTheme.typography.bodyLarge)
                                    }
                                }
                            }
                        },
                        confirmButton = { TextButton(onClick = { selectorAbierto = false }) { Text("Cerrar") } }
                    )
                }

                val visibles = estadoActual.ocupaciones.filter { ocupacion ->
                    (filtroQuien == null || filtroQuien in ocupacion.idsFiltro) &&
                        (filtroTexto.isBlank() ||
                            ocupacion.categoria.nombre.contains(filtroTexto, ignoreCase = true) ||
                            ocupacion.representante.etiqueta.orEmpty().contains(filtroTexto, ignoreCase = true) ||
                            ocupacion.quien.contains(filtroTexto, ignoreCase = true))
                }
                val visiblesClaves = visibles.map { it.clave }.toSet()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${seleccionadas.size} seleccionada(s)", style = MaterialTheme.typography.labelMedium)
                    Row {
                        TextButton(onClick = { seleccionadas = visiblesClaves }, enabled = visibles.isNotEmpty()) { Text("Marcar todo") }
                        TextButton(onClick = { seleccionadas = emptySet() }, enabled = seleccionadas.isNotEmpty()) { Text("Ninguna") }
                    }
                }
                Spacer(Modifier.height(4.dp))

                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    if (visibles.isEmpty()) {
                        Text(
                            if (estadoActual.ocupaciones.isEmpty()) "Todavía no hay ninguna ocupación apuntada."
                            else "Ninguna ocupación coincide con el filtro.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    visibles.forEach { ocupacion ->
                        FilaOcupacion(
                            ocupacion = ocupacion,
                            marcada = ocupacion.clave in seleccionadas,
                            onMarcar = { marcada ->
                                seleccionadas = if (marcada) seleccionadas + ocupacion.clave else seleccionadas - ocupacion.clave
                            }
                        )
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
                    Text("Borrar ${seleccionadas.size} ocupación(es)")
                }

                if (confirmarBorrado) {
                    AlertDialog(
                        onDismissRequest = { confirmarBorrado = false },
                        title = { Text("¿Borrar ${seleccionadas.size} ocupación(es)?") },
                        text = { Text("Se quitan de todas las personas que las tengan. No se puede deshacer.") },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.eliminar(estadoActual.ocupaciones.filter { it.clave in seleccionadas })
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
private fun FilaOcupacion(ocupacion: Ocupacion, marcada: Boolean, onMarcar: (Boolean) -> Unit) {
    val bloque = ocupacion.representante
    val categoria = ocupacion.categoria
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onMarcar(!marcada) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = marcada, onCheckedChange = onMarcar)
        Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Color(categoria.color)),
            contentAlignment = Alignment.Center
        ) {
            Text(categoria.emoji, fontSize = 18.sp)
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${categoria.nombre} · ${textoHorario(bloque)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                listOfNotNull(
                    bloque.fecha.format(FORMATO_FECHA).replaceFirstChar { it.uppercase() },
                    ocupacion.quien,
                    bloque.etiqueta?.takeIf { it.isNotBlank() }
                ).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
