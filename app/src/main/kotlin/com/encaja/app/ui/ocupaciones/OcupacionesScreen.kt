@file:OptIn(ExperimentalLayoutApi::class)

package com.encaja.app.ui.ocupaciones

// NOTA: depende de Jetpack Compose y Hilt, no compilado en este entorno.
// Pantalla "Ocupaciones" (Ajustes → Ocupaciones): el listado de todas las ocupaciones
// vigentes de personas y unidades familiares, con filtro por quién (una ventana) y por texto,
// una casilla por ocupación y un botón para borrar de golpe la selección. Mismo estilo
// que "Todas las actividades".

import androidx.compose.ui.platform.LocalContext
import com.encaja.app.ui.common.nombreVisible
import com.encaja.app.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.encaja.app.ui.common.localeApp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.encaja.app.ui.theme.AccionAvisoHoja
import com.encaja.app.ui.theme.AvisoHoja
import com.encaja.app.ui.theme.BotonPildoraHoja
import com.encaja.app.ui.theme.CabeceraPantalla
import com.encaja.app.ui.theme.CampoTextoConIconoHoja
import com.encaja.app.ui.theme.CasillaRedondaHoja
import com.encaja.app.ui.theme.EstiloAccionAviso
import com.encaja.app.ui.theme.FondoPantalla
import com.encaja.app.ui.theme.MensajePantalla
import com.encaja.app.ui.theme.TarjetaFilaHoja
import com.encaja.app.ui.theme.acentoHoja
import com.encaja.app.ui.theme.bordeHoja
import com.encaja.app.ui.theme.casillaHoja
import com.encaja.app.ui.theme.tintaHoja
import com.encaja.app.ui.theme.tintaSuaveHoja
import java.time.format.DateTimeFormatter

private fun formatoFecha(): DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM yyyy", localeApp())

@Composable
fun OcupacionesScreen(viewModel: OcupacionesViewModel = hiltViewModel()) {
    val pantalla by viewModel.pantalla.collectAsState()
    var seleccionadas by remember { mutableStateOf(setOf<String>()) }
    var filtroQuien by remember { mutableStateOf<String?>(null) }
    var filtroTexto by remember { mutableStateOf("") }
    var confirmarBorrado by remember { mutableStateOf(false) }
    var selectorAbierto by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.recargar() }

    FondoPantalla {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            CabeceraPantalla(stringResource(R.string.ajustes_ocupaciones_titulo))

            when (val estadoActual = pantalla) {
                is OcupacionesPantallaEstado.Cargando -> CircularProgressIndicator()

                is OcupacionesPantallaEstado.SinFamilia ->
                    MensajePantalla(stringResource(R.string.comun_sin_familia_ver))

                is OcupacionesPantallaEstado.ConDatos -> {
                    // Filtro por quién (Todos, cada persona, cada unidad): un solo botón que
                    // muestra la elección actual y abre una ventana para cambiarla, para no
                    // comerle sitio al listado. Encima, el buscador por texto.
                    val textoTodos = stringResource(R.string.comun_todos)
                    val opciones = listOf(OpcionFiltro(null, textoTodos)) +
                        estadoActual.cuidadores.map { OpcionFiltro(it.id.value, it.nombre) } +
                        estadoActual.unidades.map { OpcionFiltro(it.id.value, it.nombre) }
                    val etiquetaFiltro = opciones.firstOrNull { it.idTexto == filtroQuien }?.etiqueta ?: textoTodos

                    CampoTextoConIconoHoja(
                        filtroTexto,
                        { filtroTexto = it },
                        stringResource(R.string.comun_buscar),
                        Icons.Default.Search,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(casillaHoja)
                            .border(1.dp, if (filtroQuien != null) acentoHoja else bordeHoja, RoundedCornerShape(20.dp))
                            .clickable { selectorAbierto = true }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.FilterAlt, contentDescription = null, tint = acentoHoja, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(etiquetaFiltro, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = tintaHoja)
                    }

                    if (selectorAbierto) {
                        AvisoHoja(
                            icono = Icons.Default.FilterAlt,
                            titulo = stringResource(R.string.ocupaciones_filtro_titulo),
                            texto = null,
                            peligro = false,
                            conX = true,
                            acciones = listOf(
                                AccionAvisoHoja(stringResource(R.string.comun_cerrar), EstiloAccionAviso.BORDE) { selectorAbierto = false }
                            ),
                            onCerrar = { selectorAbierto = false },
                            contenido = {
                                Column(
                                    modifier = Modifier
                                        .heightIn(max = 300.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    opciones.forEachIndexed { indice, opcion ->
                                        if (indice == 1 && estadoActual.cuidadores.isNotEmpty()) {
                                            Text(
                                                stringResource(R.string.cuidadores_personas),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = acentoHoja,
                                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                                            )
                                        }
                                        if (indice == 1 + estadoActual.cuidadores.size && estadoActual.unidades.isNotEmpty()) {
                                            Text(
                                                stringResource(R.string.unidades_titulo),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = acentoHoja,
                                                modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)
                                            )
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { filtroQuien = opcion.idTexto; selectorAbierto = false }
                                                .padding(vertical = 10.dp, horizontal = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CasillaRedondaHoja(filtroQuien == opcion.idTexto)
                                            Spacer(Modifier.width(12.dp))
                                            Text(opcion.etiqueta, style = MaterialTheme.typography.bodyLarge, color = tintaHoja)
                                        }
                                    }
                                }
                            }
                        )
                    }

                    val contextoApp = LocalContext.current
                    val visibles = estadoActual.ocupaciones.filter { ocupacion ->
                        (filtroQuien == null || filtroQuien in ocupacion.idsFiltro) &&
                            (filtroTexto.isBlank() ||
                                ocupacion.categoria.nombreVisible(contextoApp).contains(filtroTexto, ignoreCase = true) ||
                                ocupacion.representante.etiqueta.orEmpty().contains(filtroTexto, ignoreCase = true) ||
                                ocupacion.quien.contains(filtroTexto, ignoreCase = true))
                    }
                    val visiblesClaves = visibles.map { it.clave }.toSet()

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(pluralStringResource(R.plurals.comun_seleccionadas, seleccionadas.size, seleccionadas.size), style = MaterialTheme.typography.labelLarge, color = tintaSuaveHoja)
                        Row {
                            TextButton(onClick = { seleccionadas = visiblesClaves }, enabled = visibles.isNotEmpty()) {
                                Text(stringResource(R.string.comun_marcar_todo), color = acentoHoja, fontWeight = FontWeight.Bold)
                            }
                            TextButton(onClick = { seleccionadas = emptySet() }, enabled = seleccionadas.isNotEmpty()) {
                                Text(stringResource(R.string.comun_ninguna), color = acentoHoja, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        if (visibles.isEmpty()) {
                            MensajePantalla(
                                stringResource(
                                    if (estadoActual.ocupaciones.isEmpty()) R.string.ocupaciones_vacia
                                    else R.string.ocupaciones_sin_coincidencias
                                )
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
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    BotonPildoraHoja(
                        texto = pluralStringResource(R.plurals.ocupaciones_borrar, seleccionadas.size, seleccionadas.size),
                        onClick = { confirmarBorrado = true },
                        icono = Icons.Default.Delete,
                        habilitado = seleccionadas.isNotEmpty(),
                        peligro = true
                    )
                    Spacer(Modifier.height(16.dp))

                    if (confirmarBorrado) {
                        AvisoHoja(
                            icono = Icons.Default.Delete,
                            titulo = pluralStringResource(R.plurals.ocupaciones_borrar_titulo, seleccionadas.size, seleccionadas.size),
                            texto = stringResource(R.string.ocupaciones_borrar_texto),
                            acciones = listOf(
                                AccionAvisoHoja(stringResource(R.string.comun_borrar), EstiloAccionAviso.PELIGRO) {
                                    viewModel.eliminar(estadoActual.ocupaciones.filter { it.clave in seleccionadas })
                                    seleccionadas = emptySet()
                                    confirmarBorrado = false
                                },
                                AccionAvisoHoja(stringResource(R.string.comun_cancelar), EstiloAccionAviso.BORDE) { confirmarBorrado = false }
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
private fun FilaOcupacion(ocupacion: Ocupacion, marcada: Boolean, onMarcar: (Boolean) -> Unit) {
    val bloque = ocupacion.representante
    val categoria = ocupacion.categoria
    TarjetaFilaHoja(onClick = { onMarcar(!marcada) }) {
        CasillaRedondaHoja(marcada)
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Color(categoria.color)),
            contentAlignment = Alignment.Center
        ) {
            Text(categoria.emoji, fontSize = 20.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${categoria.nombreVisible()} · ${if (bloque.todoElDia) stringResource(R.string.comun_todo_el_dia) else textoHorario(bloque)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = tintaHoja
            )
            Text(
                listOfNotNull(
                    bloque.fecha.format(formatoFecha()).replaceFirstChar { it.uppercase() },
                    ocupacion.quien,
                    bloque.etiqueta?.takeIf { it.isNotBlank() }
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = tintaSuaveHoja
            )
        }
    }
}
