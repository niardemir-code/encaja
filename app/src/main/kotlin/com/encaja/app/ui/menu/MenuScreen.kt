package com.encaja.app.ui.menu

// NOTA: depende de Jetpack Compose y Hilt, no compilado en este entorno.
// Rediseño para acercarse a la maqueta aportada por el usuario: cabecera
// con título/subtítulo y chip "Esta semana" (ahora funcional: permite
// saltar a la semana anterior/siguiente o volver a la actual), y una
// tarjeta por día con un panel de color a la izquierda (nombre/número/mes
// del día) y un panel gris claro a la derecha con las filas de Comida y
// Cena (icono sol/luna + texto + lápiz de edición), todo dentro de un
// único recuadro para que quede bien delimitado y alineado.

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.encaja.app.domain.model.ComidaDelDia
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val paletaDias = listOf(
    Color(0xFFB5D4F4), // azul
    Color(0xFFF4C0D1), // rosa
    Color(0xFF9FE1CB), // verde
    Color(0xFFDDD3F7), // lila
    Color(0xFFFAC775), // ámbar
    Color(0xFFF0997B), // coral
    Color(0xFFC0DD97)  // verde claro
)

private val GRIS_TARJETA = Color(0xFFF3F1F6)

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
                esSemanaActual = estadoActual.esSemanaActual,
                onGuardarDia = { fecha, comida, cena -> viewModel.guardarDia(fecha, comida, cena) },
                onSemanaAnterior = { viewModel.cambiarSemana(-1) },
                onSemanaSiguiente = { viewModel.cambiarSemana(1) },
                onIrASemanaActual = { viewModel.irASemanaActual() }
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
    onIrASemanaActual: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CabeceraMenu(
                esSemanaActual = esSemanaActual,
                onSemanaAnterior = onSemanaAnterior,
                onSemanaSiguiente = onSemanaSiguiente,
                onIrASemanaActual = onIrASemanaActual
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
    esSemanaActual: Boolean,
    onSemanaAnterior: () -> Unit,
    onSemanaSiguiente: () -> Unit,
    onIrASemanaActual: () -> Unit
) {
    var menuAbierto by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            Text(
                "Menú semanal",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Planifica, organiza y disfruta",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .clickable { menuAbierto = true }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (esSemanaActual) "Esta semana" else "Otra semana",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(Modifier.width(2.dp))
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                DropdownMenuItem(
                    text = { Text("Semana anterior") },
                    onClick = { menuAbierto = false; onSemanaAnterior() }
                )
                DropdownMenuItem(
                    text = { Text("Esta semana") },
                    enabled = !esSemanaActual,
                    onClick = { menuAbierto = false; onIrASemanaActual() }
                )
                DropdownMenuItem(
                    text = { Text("Semana siguiente") },
                    onClick = { menuAbierto = false; onSemanaSiguiente() }
                )
            }
        }
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
                .padding(vertical = 14.dp, horizontal = 14.dp)
        ) {
            FilaComida(
                icono = Icons.Default.WbSunny,
                etiqueta = "Comida",
                valorActual = dia.comida,
                onGuardar = onGuardarComida
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
            FilaComida(
                icono = Icons.Default.NightsStay,
                etiqueta = "Cena",
                valorActual = dia.cena,
                onGuardar = onGuardarCena
            )
        }
    }
}

@Composable
private fun PanelDelDia(fecha: LocalDate, color: Color) {
    val nombreDia = fecha.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("es"))
        .replaceFirstChar { it.uppercase() }
    val nombreMes = fecha.month.getDisplayName(TextStyle.SHORT, Locale("es"))
        .replaceFirstChar { it.uppercase() }

    Column(
        modifier = Modifier
            .width(76.dp)
            .fillMaxHeight()
            .background(color, shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(nombreDia, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(fecha.dayOfMonth.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(nombreMes, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun FilaComida(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    etiqueta: String,
    valorActual: String?,
    onGuardar: (String?) -> Unit
) {
    var editando by remember(valorActual) { mutableStateOf(false) }
    var texto by remember(valorActual) { mutableStateOf(valorActual ?: "") }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(32.dp)
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
            IconButton(onClick = {
                onGuardar(texto.ifBlank { null })
                editando = false
            }) {
                Icon(Icons.Default.Check, contentDescription = "Confirmar $etiqueta")
            }
        } else {
            Column(modifier = Modifier.weight(1f)) {
                Text(etiqueta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    valorActual ?: "Sin planificar",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            IconButton(onClick = { editando = true }) {
                Icon(Icons.Default.Edit, contentDescription = "Editar $etiqueta", modifier = Modifier.size(18.dp))
            }
        }
    }
}
