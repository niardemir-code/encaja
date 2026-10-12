@file:OptIn(ExperimentalMaterial3Api::class)

package com.encaja.app.ui.menu

// NOTA: depende de Jetpack Compose y Hilt, no compilado en este entorno.
// Rediseño para acercarse a la maqueta aportada por el usuario: cabecera
// con título/subtítulo y chip "Esta semana" (ahora funcional: permite
// saltar a la semana anterior/siguiente o volver a la actual), y una
// tarjeta por día con un panel de color a la izquierda (nombre/número/mes
// del día) y un panel gris claro a la derecha con las filas de Comida y
// Cena (icono sol/luna + texto + lápiz de edición), todo dentro de un
// único recuadro para que quede bien delimitado y alineado.

import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import com.encaja.app.R
import com.encaja.app.ui.common.nombreDiaCorto
import com.encaja.app.ui.common.nombreMesCorto
import com.encaja.app.ui.theme.SelectorFechaHoja
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.encaja.app.domain.model.ComidaDelDia
import com.encaja.app.ui.familia.fechaAMillisUtc
import com.encaja.app.ui.familia.millisUtcAFecha
import java.time.LocalDate

private val paletaDias = listOf(
    Color(0xFFB5D4F4), // azul
    Color(0xFFF4C0D1), // rosa
    Color(0xFF9FE1CB), // verde
    Color(0xFFDDD3F7), // lila
    Color(0xFFFAC775), // ámbar
    Color(0xFFF0997B), // coral
    Color(0xFFC0DD97)  // verde claro
)

private val GRIS_TARJETA: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceVariant

@Composable
fun MenuScreen(viewModel: MenuViewModel = hiltViewModel()) {
    val pantalla by viewModel.pantalla.collectAsState()
    // El ViewModel vive ahora lo que vive la app (ver entradaDelGrafo en EncajaApp), para
    // no perder la semana/día elegido al cambiar de pestaña; a cambio, hay que recargar
    // al reentrar para reflejar cambios hechos desde otras pantallas.
    LaunchedEffect(Unit) { viewModel.recargar() }

    when (val estadoActual = pantalla) {
        is MenuPantallaEstado.Cargando -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is MenuPantallaEstado.SinFamilia -> {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.comun_sin_familia_ver),
                    textAlign = TextAlign.Center
                )
            }
        }

        is MenuPantallaEstado.ConDatos -> {
            ContenidoMenuSemanal(
                dias = estadoActual.dias,
                esSemanaActual = estadoActual.esSemanaActual,
                onGuardarDia = { fecha, comida, cena -> viewModel.guardarDia(fecha, comida, cena) },
                onSemanaAnterior = { viewModel.cambiarSemana(-1) },
                onSemanaSiguiente = { viewModel.cambiarSemana(1) },
                onIrASemanaActual = { viewModel.irASemanaActual() },
                onIrASemanaDe = { fecha -> viewModel.irASemanaDe(fecha) }
            )
        }
    }
}

@Composable
private fun ContenidoMenuSemanal(
    dias: List<ComidaDelDia>,
    esSemanaActual: Boolean,
    onGuardarDia: (fecha: LocalDate, comida: String?, cena: String?) -> Unit,
    onSemanaAnterior: () -> Unit,
    onSemanaSiguiente: () -> Unit,
    onIrASemanaActual: () -> Unit,
    onIrASemanaDe: (LocalDate) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            CabeceraMenu(
                primerDia = dias.firstOrNull()?.fecha,
                esSemanaActual = esSemanaActual,
                onSemanaAnterior = onSemanaAnterior,
                onSemanaSiguiente = onSemanaSiguiente,
                onIrASemanaActual = onIrASemanaActual,
                onIrASemanaDe = onIrASemanaDe
            )
        }

        items(dias.size) { index ->
            val dia = dias[index]
            TarjetaDia(
                dia = dia,
                color = paletaDias[index % paletaDias.size],
                onGuardarComida = { texto -> onGuardarDia(dia.fecha, texto, dia.cena) },
                onGuardarCena = { texto -> onGuardarDia(dia.fecha, dia.comida, texto) }
            )
        }
    }
}

@Composable
private fun CabeceraMenu(
    primerDia: LocalDate?,
    esSemanaActual: Boolean,
    onSemanaAnterior: () -> Unit,
    onSemanaSiguiente: () -> Unit,
    onIrASemanaActual: () -> Unit,
    onIrASemanaDe: (LocalDate) -> Unit
) {
    var calendarioAbierto by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // weight(1f) + una sola línea con elipsis: así el título largo se acorta en vez
        // de empujar los iconos de la derecha fuera de la pantalla (lo que hacía que
        // pareciera que faltaba el botón de avanzar semana al añadir un cuarto icono).
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                stringResource(R.string.menu_titulo),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                stringResource(R.string.menu_subtitulo),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onSemanaAnterior) {
                Icon(Icons.Default.ChevronLeft, contentDescription = stringResource(R.string.comun_semana_anterior))
            }
            IconButton(onClick = { calendarioAbierto = true }) {
                Icon(Icons.Default.CalendarMonth, contentDescription = stringResource(R.string.comun_ir_a_semana))
            }
            // Botones de ir adelante/atrás ya cubren cambiar de semana; este vuelve
            // directo a la actual sin acumular los pasos dados con las flechas. Mismo
            // icono ("punto de disparo") que en Guía, Familia y Semana.
            IconButton(onClick = onIrASemanaActual, enabled = !esSemanaActual) {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = stringResource(R.string.comun_ir_semana_actual),
                    tint = if (esSemanaActual) LocalContentColor.current.copy(alpha = 0.38f) else LocalContentColor.current
                )
            }
            IconButton(onClick = onSemanaSiguiente) {
                Icon(Icons.Default.ChevronRight, contentDescription = stringResource(R.string.comun_semana_siguiente))
            }
        }
    }

    if (calendarioAbierto) {
        SelectorFechaHoja(
            fecha = primerDia ?: LocalDate.now(),
            confirmar = "Ir",
            onElegir = onIrASemanaDe,
            onCerrar = { calendarioAbierto = false }
        )
    }
}

@Composable
private fun TarjetaDia(
    dia: ComidaDelDia,
    color: Color,
    onGuardarComida: (String?) -> Unit,
    onGuardarCena: (String?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(16.dp))
            .background(GRIS_TARJETA)
    ) {
        PanelDelDia(fecha = dia.fecha, color = color)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(vertical = 6.dp, horizontal = 12.dp)
        ) {
            FilaComida(
                icono = Icons.Default.WbSunny,
                etiquetaId = R.string.menu_comida,
                valorActual = dia.comida,
                onGuardar = onGuardarComida
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
            FilaComida(
                icono = Icons.Default.NightsStay,
                etiquetaId = R.string.menu_cena,
                valorActual = dia.cena,
                onGuardar = onGuardarCena
            )
        }
    }
}

@Composable
private fun PanelDelDia(fecha: LocalDate, color: Color) {
    val nombreDia = nombreDiaCorto(fecha.dayOfWeek)
    val nombreMes = nombreMesCorto(fecha).replaceFirstChar { it.uppercase() }

    Column(
        modifier = Modifier
            .width(60.dp)
            .fillMaxHeight()
            .background(color, shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(nombreDia, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(2.dp))
        Text(fecha.dayOfMonth.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(1.dp))
        Text(nombreMes, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun FilaComida(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    @StringRes etiquetaId: Int,
    valorActual: String?,
    onGuardar: (String?) -> Unit
) {
    val etiqueta = stringResource(etiquetaId)
    var editando by remember(valorActual) { mutableStateOf(false) }
    var texto by remember(valorActual) { mutableStateOf(valorActual ?: "") }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(icono, contentDescription = etiqueta, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(10.dp))

        if (editando) {
            OutlinedTextField(
                value = texto,
                onValueChange = { texto = it },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(4.dp))
            BotonCompactoMenu(Icons.Default.Check, stringResource(R.string.menu_confirmar, etiqueta), 24) {
                onGuardar(texto.ifBlank { null })
                editando = false
            }
        } else {
            Column(modifier = Modifier.weight(1f)) {
                Text(etiqueta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    valorActual ?: stringResource(R.string.menu_sin_planificar),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            BotonCompactoMenu(Icons.Default.Edit, stringResource(R.string.menu_editar, etiqueta), 18) { editando = true }
        }
    }
}

/** Botón de icono de 36 dp (los IconButton normales miden 48 dp y alargan cada fila). */
@Composable
private fun BotonCompactoMenu(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    descripcion: String,
    tamanoIcono: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icono, contentDescription = descripcion, modifier = Modifier.size(tamanoIcono.dp))
    }
}
