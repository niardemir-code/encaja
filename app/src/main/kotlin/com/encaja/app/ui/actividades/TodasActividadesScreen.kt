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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.encaja.app.domain.model.ChildId
import com.encaja.app.domain.model.CoverageNeed
import com.encaja.app.domain.model.CoverageNeedId
import com.encaja.app.ui.common.formatearHora
import com.encaja.app.ui.theme.AccionAvisoHoja
import com.encaja.app.ui.theme.AvisoHoja
import com.encaja.app.ui.theme.BotonPildoraHoja
import com.encaja.app.ui.theme.CabeceraPantalla
import com.encaja.app.ui.theme.CampoTextoConIconoHoja
import com.encaja.app.ui.theme.CasillaRedondaHoja
import com.encaja.app.ui.theme.ChipFiltroHoja
import com.encaja.app.ui.theme.EstiloAccionAviso
import com.encaja.app.ui.theme.FondoPantalla
import com.encaja.app.ui.theme.MensajePantalla
import com.encaja.app.ui.theme.RotuloSeccionPantalla
import com.encaja.app.ui.theme.TarjetaFilaHoja
import com.encaja.app.ui.theme.acentoHoja
import com.encaja.app.ui.theme.tintaHoja
import com.encaja.app.ui.theme.tintaSuaveHoja
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ES = Locale("es")
private val FORMATO_FECHA = DateTimeFormatter.ofPattern("d MMM yyyy", ES)

@Composable
fun TodasActividadesScreen(viewModel: TodasActividadesViewModel = hiltViewModel()) {
    val pantalla by viewModel.pantalla.collectAsState()
    var seleccionadas by remember { mutableStateOf(setOf<CoverageNeedId>()) }
    var filtro by remember { mutableStateOf("") }
    var filtroNino by remember { mutableStateOf<ChildId?>(null) }
    var confirmarBorrado by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.recargar() }

    FondoPantalla {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            CabeceraPantalla("Todas las actividades")

            when (val estadoActual = pantalla) {
                is TodasActividadesPantallaEstado.Cargando -> {
                    CircularProgressIndicator()
                }

                is TodasActividadesPantallaEstado.SinFamilia -> {
                    MensajePantalla("Vincúlate a una familia desde la pestaña Semana para ver esta pantalla.")
                }

                is TodasActividadesPantallaEstado.ConDatos -> {
                    CampoTextoConIconoHoja(
                        filtro,
                        { filtro = it },
                        "Buscar por texto…",
                        Icons.Default.Search,
                        modifier = Modifier.fillMaxWidth(0.75f)
                    )
                    if (estadoActual.grupos.size > 1) {
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ChipFiltroHoja("Todos", filtroNino == null) { filtroNino = null }
                            estadoActual.grupos.forEach { grupo ->
                                ChipFiltroHoja(grupo.child.nombre, filtroNino == grupo.child.id) {
                                    filtroNino = grupo.child.id
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    val gruposFiltrados = estadoActual.grupos.filter { filtroNino == null || it.child.id == filtroNino }.map { grupo ->
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
                            style = MaterialTheme.typography.labelLarge,
                            color = tintaSuaveHoja
                        )
                        Row {
                            TextButton(onClick = { seleccionadas = visiblesIds }, enabled = hayActividadesVisibles) {
                                Text("Marcar todo", color = acentoHoja, fontWeight = FontWeight.Bold)
                            }
                            TextButton(onClick = { seleccionadas = emptySet() }, enabled = seleccionadas.isNotEmpty()) {
                                Text("Ninguna", color = acentoHoja, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (!hayActividadesVisibles) {
                            MensajePantalla(
                                if (filtro.isBlank()) "Todavía no hay actividades."
                                else "Ninguna actividad coincide con \"$filtro\"."
                            )
                        }
                        gruposFiltrados.forEach { grupo ->
                            if (grupo.actividades.isNotEmpty()) {
                                RotuloSeccionPantalla(grupo.child.nombre)
                                grupo.actividades.forEach { actividad ->
                                    FilaActividad(
                                        actividad = actividad,
                                        marcada = actividad.id in seleccionadas,
                                        onMarcar = { marcada ->
                                            seleccionadas = if (marcada) seleccionadas + actividad.id else seleccionadas - actividad.id
                                        }
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    BotonPildoraHoja(
                        texto = "Borrar ${seleccionadas.size} actividad(es)",
                        onClick = { confirmarBorrado = true },
                        icono = Icons.Default.Delete,
                        habilitado = seleccionadas.isNotEmpty(),
                        peligro = true
                    )
                    Spacer(Modifier.height(16.dp))

                    if (confirmarBorrado) {
                        AvisoHoja(
                            icono = Icons.Default.Delete,
                            titulo = "¿Borrar ${seleccionadas.size} actividad(es)?",
                            texto = "No se puede deshacer.",
                            acciones = listOf(
                                AccionAvisoHoja("Borrar", EstiloAccionAviso.PELIGRO) {
                                    viewModel.eliminarSeleccionadas(seleccionadas)
                                    seleccionadas = emptySet()
                                    confirmarBorrado = false
                                },
                                AccionAvisoHoja("Cancelar", EstiloAccionAviso.BORDE) { confirmarBorrado = false }
                            ),
                            onCerrar = { confirmarBorrado = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaActividad(actividad: CoverageNeed, marcada: Boolean, onMarcar: (Boolean) -> Unit) {
    TarjetaFilaHoja(onClick = { onMarcar(!marcada) }) {
        CasillaRedondaHoja(marcada)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                actividad.descripcion,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = tintaHoja
            )
            Text(
                "${actividad.fecha.format(FORMATO_FECHA)} · ${formatearHora(actividad.horaInicio)}–" +
                    formatearHora(actividad.horaFin),
                style = MaterialTheme.typography.bodySmall,
                color = tintaSuaveHoja
            )
            if (actividad.grupoRepeticionId != null) {
                Text(
                    "Se repite cada semana",
                    style = MaterialTheme.typography.labelSmall,
                    color = tintaSuaveHoja
                )
            }
        }
    }
}
