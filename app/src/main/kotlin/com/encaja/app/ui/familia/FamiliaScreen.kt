package com.encaja.app.ui.familia

// NOTA: depende de Jetpack Compose y Hilt, no compilado en este entorno.
// Rediseño: se quita la lista fija de "Patrón semanal" y todo se gestiona
// desde un único diálogo al tocar el avatar de un día: una sola lista de
// opciones (personas y unidades familiares) más una casilla "Todos los
// [día]" que decide si el cambio es solo para esa fecha o para siempre.
// Se añade cabecera con navegación de semana (mismo patrón que en Menú)
// y soporte para asignar unidades familiares además de personas sueltas.

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.encaja.app.domain.model.CaregiverId
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val VERDE = Color(0xFFC0DD97)
private val ROJO = Color(0xFFF7C1C1)
private val FONDO_UNIDAD = Color(0xFFDDD3F7)

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
            val iniciales = remember(estado.cuidadores) {
                calcularInicialesCuidadores(estado.cuidadores.map { it.caregiver })
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    CabeceraFamilia(
                        lunes = estado.lunes,
                        esSemanaActual = estado.esSemanaActual,
                        onSemanaAnterior = { viewModel.cambiarSemana(-1) },
                        onSemanaSiguiente = { viewModel.cambiarSemana(1) },
                        onIrASemanaActual = { viewModel.irASemanaActual() }
                    )
                }

                item {
                    Column {
                        Text("Con quién están las niñas", style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.height(6.dp))
                        FilaDeAsignacion(
                            dias = estado.diasAsignacion,
                            opciones = estado.opcionesAsignables,
                            iniciales = iniciales,
                            patronSemanal = estado.patronSemanal,
                            onAsignarHabitual = { dia, idTexto -> viewModel.asignarResponsableHabitual(dia, idTexto) },
                            onQuitarHabitual = { dia -> viewModel.quitarResponsableHabitual(dia) },
                            onAnularFecha = { fecha, idTexto -> viewModel.anularParaEstaFecha(fecha, idTexto) },
                            onQuitarCambioPuntual = { fecha -> viewModel.quitarCambioPuntual(fecha) }
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Toca un día para elegir quién es responsable, solo esta semana o siempre.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
private fun CabeceraFamilia(
    lunes: LocalDate,
    esSemanaActual: Boolean,
    onSemanaAnterior: () -> Unit,
    onSemanaSiguiente: () -> Unit,
    onIrASemanaActual: () -> Unit
) {
    var menuAbierto by remember { mutableStateOf(false) }
    val domingo = lunes.plusDays(6)
    val rango = if (lunes.month == domingo.month) {
        "${lunes.dayOfMonth} - ${domingo.dayOfMonth} de ${mesEs(domingo)}"
    } else {
        "${lunes.dayOfMonth} de ${mesEs(lunes)} - ${domingo.dayOfMonth} de ${mesEs(domingo)}"
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text("Familia", style = MaterialTheme.typography.headlineSmall)

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
        Spacer(Modifier.height(2.dp))
        Text(
            "Semana del $rango",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun mesEs(fecha: LocalDate): String =
    fecha.month.getDisplayName(TextStyle.FULL, Locale("es"))

@Composable
private fun FilaDeAsignacion(
    dias: List<DiaAsignado>,
    opciones: List<Responsable>,
    iniciales: Map<CaregiverId, String>,
    patronSemanal: Map<java.time.DayOfWeek, Responsable>,
    onAsignarHabitual: (java.time.DayOfWeek, String) -> Unit,
    onQuitarHabitual: (java.time.DayOfWeek) -> Unit,
    onAnularFecha: (LocalDate, String) -> Unit,
    onQuitarCambioPuntual: (LocalDate) -> Unit
) {
    var diaEnEdicion by remember { mutableStateOf<DiaAsignado?>(null) }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        dias.forEach { dia ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { diaEnEdicion = dia }
            ) {
                Text(
                    dia.fecha.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("es")).take(1).uppercase(),
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(Modifier.height(4.dp))
                AvatarResponsable(responsable = dia.responsable, iniciales = iniciales, tamano = 32.dp)
            }
        }
    }

    val diaActual = diaEnEdicion
    if (diaActual != null) {
        DialogoAsignarDia(
            dia = diaActual,
            opciones = opciones,
            iniciales = iniciales,
            hayResponsableHabitual = patronSemanal[diaActual.fecha.dayOfWeek] != null,
            onAsignarHabitual = { idTexto -> onAsignarHabitual(diaActual.fecha.dayOfWeek, idTexto); diaEnEdicion = null },
            onQuitarHabitual = { onQuitarHabitual(diaActual.fecha.dayOfWeek); diaEnEdicion = null },
            onAnularFecha = { idTexto -> onAnularFecha(diaActual.fecha, idTexto); diaEnEdicion = null },
            onQuitarCambioPuntual = { onQuitarCambioPuntual(diaActual.fecha); diaEnEdicion = null },
            onCerrar = { diaEnEdicion = null }
        )
    }
}

/**
 * Una única lista de opciones (personas y unidades familiares). La casilla "Todos los
 * [día]" decide qué pasa al tocar una opción: marcada, la deja como responsable habitual
 * de ese día de la semana (todas las semanas); sin marcar, solo cambia esta fecha
 * concreta. Así se evita tener dos listas duplicadas una debajo de la otra.
 */
@Composable
private fun DialogoAsignarDia(
    dia: DiaAsignado,
    opciones: List<Responsable>,
    iniciales: Map<CaregiverId, String>,
    hayResponsableHabitual: Boolean,
    onAsignarHabitual: (String) -> Unit,
    onQuitarHabitual: () -> Unit,
    onAnularFecha: (String) -> Unit,
    onQuitarCambioPuntual: () -> Unit,
    onCerrar: () -> Unit
) {
    val etiquetaDia = dia.fecha.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("es")).replaceFirstChar { it.uppercase() }
    var todosLosDias by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("$etiquetaDia ${dia.fecha.dayOfMonth}") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { todosLosDias = !todosLosDias }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = todosLosDias, onCheckedChange = { todosLosDias = it })
                    Text("Todos los $etiquetaDia (responsable habitual)")
                }
                Text(
                    if (todosLosDias) {
                        "Se aplicará todas las semanas, hasta que lo cambies."
                    } else {
                        "Solo cambia este día. El patrón habitual no se toca."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))

                opciones.forEach { opcion ->
                    FilaOpcion(opcion, iniciales) {
                        if (todosLosDias) onAsignarHabitual(opcion.idTexto) else onAnularFecha(opcion.idTexto)
                    }
                }

                if (dia.esCambioPuntual || hayResponsableHabitual) {
                    Spacer(Modifier.height(6.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(6.dp))
                }
                if (dia.esCambioPuntual) {
                    TextButton(onClick = onQuitarCambioPuntual) {
                        Text("Quitar cambio puntual de este día", color = MaterialTheme.colorScheme.error)
                    }
                }
                if (hayResponsableHabitual) {
                    TextButton(onClick = onQuitarHabitual) {
                        Text("Quitar responsable habitual de los $etiquetaDia", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCerrar) { Text("Cerrar") }
        }
    )
}

@Composable
private fun FilaOpcion(opcion: Responsable, iniciales: Map<CaregiverId, String>, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarResponsable(responsable = opcion, iniciales = iniciales, tamano = 24.dp)
        Spacer(Modifier.width(10.dp))
        Text(opcion.etiqueta)
    }
}

/** Avatar de un día o de una fila de opción: iniciales (desambiguadas con
 * calcularInicialesCuidadores, igual que en el resto de la app) sobre fondo liso
 * para una persona, código en negrita sobre fondo rayado en diagonal para una
 * unidad familiar. */
@Composable
private fun AvatarResponsable(
    responsable: Responsable?,
    iniciales: Map<CaregiverId, String>,
    tamano: androidx.compose.ui.unit.Dp
) {
    when (responsable) {
        null -> Box(
            modifier = Modifier.size(tamano).clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) { Text("–", style = MaterialTheme.typography.labelSmall) }

        is Responsable.Persona -> Box(
            modifier = Modifier.size(tamano).clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) { Text(iniciales[responsable.caregiver.id] ?: "", style = MaterialTheme.typography.labelSmall) }

        is Responsable.Unidad -> Box(
            modifier = Modifier.size(tamano).clip(RoundedCornerShape(8.dp)).fondoRayado(FONDO_UNIDAD),
            contentAlignment = Alignment.Center
        ) {
            Text(
                responsable.unidad.codigo.take(2).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

/** Fondo rayado en diagonal, para distinguir a simple vista una unidad familiar de una persona. */
private fun Modifier.fondoRayado(colorFondo: Color): Modifier = this.drawBehind {
    drawRect(colorFondo)
    val espaciado = 6.dp.toPx()
    val grosor = 2.dp.toPx()
    val diagonal = kotlin.math.sqrt(size.width * size.width + size.height * size.height)
    val colorRaya = Color.White.copy(alpha = 0.55f)
    rotate(degrees = 45f, pivot = center) {
        var x = -diagonal
        while (x < diagonal) {
            drawLine(
                color = colorRaya,
                start = Offset(x, -diagonal),
                end = Offset(x, diagonal),
                strokeWidth = grosor
            )
            x += espaciado
        }
    }
}

@Composable
private fun FilaDeCuidador(cuidadorSemana: CuidadorDisponibilidadSemana) {
    Column {
        Text(cuidadorSemana.caregiver.nombreCompleto, style = MaterialTheme.typography.labelMedium)
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
