@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.encaja.app.ui.familia

// NOTA: depende de Jetpack Compose (Material 3), no compilado en este entorno.
// Editor de categorías de disponibilidad. Para una categoría propia se elige
// todo: nombre, si se apunta por horas o por días, si ocupa a la persona,
// color e icono (galería de emojis). Para una de serie (Trabajo, Médico...)
// solo el icono y el color, porque su comportamiento es fijo.

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.encaja.app.domain.model.CategoriaDisponibilidad
import com.encaja.app.domain.model.CategoriaId
import com.encaja.app.domain.model.ModoCategoria
import java.util.UUID

private val TINTA_EDITOR = Color(0xFF1C1A4A)
private val LAVANDA_EDITOR = Color(0xFFEAE7FB)

/**
 * [inicial] null = crear una nueva. [onEliminar] null = no se puede borrar (las de serie).
 * [categorias] sirve para no repetir nombres.
 */
@Composable
fun DialogoCategoria(
    inicial: CategoriaDisponibilidad?,
    categorias: List<CategoriaDisponibilidad>,
    onGuardar: (CategoriaDisponibilidad) -> Unit,
    onEliminar: (() -> Unit)?,
    onCerrar: () -> Unit
) {
    val esBase = inicial?.esBase == true
    val idNueva = remember { CategoriaId(UUID.randomUUID().toString()) }
    var nombre by remember { mutableStateOf(inicial?.nombre ?: "") }
    var emoji by remember { mutableStateOf(inicial?.emoji ?: "📌") }
    var color by remember { mutableStateOf(inicial?.color ?: COLORES_CATEGORIA[4]) }
    var modo by remember { mutableStateOf(inicial?.modo ?: ModoCategoria.HORAS) }
    var bloquea by remember { mutableStateOf(inicial?.bloquea ?: true) }
    var galeriaAbierta by remember { mutableStateOf(false) }
    var confirmarBorrado by remember { mutableStateOf(false) }

    val nombreRepetido = !esBase && nombre.isNotBlank() && !nombreCategoriaValido(nombre, categorias, inicial?.id)
    val puedeGuardar = esBase || nombreCategoriaValido(nombre, categorias, inicial?.id)

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(if (inicial == null) "Nueva categoría" else "Editar ${inicial.nombre}") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Vista previa: así se verá en la casilla.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(color))
                            .clickable { galeriaAbierta = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, fontSize = 28.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            nombre.trim().ifBlank { "Nombre" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TINTA_EDITOR
                        )
                        Text(
                            if (modo == ModoCategoria.HORAS) "Por horas" else "Por días",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))

                if (esBase) {
                    Text(
                        "Es una categoría de serie: puedes cambiar su icono y su color. Si le pones otro " +
                            "icono, las casillas lo mostrarán en lugar de las horas (al tocarlas se ven).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it.take(24) },
                        label = { Text("Nombre (p.ej. Gimnasio, Clases)") },
                        singleLine = true,
                        isError = nombreRepetido,
                        supportingText = if (nombreRepetido) {
                            { Text("Ya hay una categoría con ese nombre") }
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))

                    Text("Cómo se apunta", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = modo == ModoCategoria.HORAS,
                            onClick = { modo = ModoCategoria.HORAS },
                            label = { Text("Por horas") }
                        )
                        FilterChip(
                            selected = modo == ModoCategoria.DIAS,
                            onClick = { modo = ModoCategoria.DIAS },
                            label = { Text("Por días") }
                        )
                    }
                    Text(
                        if (modo == ModoCategoria.HORAS) "Un rato de un día (de tal a tal hora) o el día entero."
                        else "Uno o varios días seguidos, eligiendo desde y hasta.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { bloquea = !bloquea },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Ocupa a la persona")
                            Text(
                                if (bloquea) "No podrá cuidar a las niñas en ese rato."
                                else "Solo informativa: sigue contando como disponible.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = bloquea, onCheckedChange = { bloquea = it })
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text("Icono", style = MaterialTheme.typography.labelMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(emoji, fontSize = 28.sp)
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { galeriaAbierta = true }) { Text("Elegir de la galería") }
                }

                Spacer(Modifier.height(8.dp))
                Text("Color", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    COLORES_CATEGORIA.forEach { opcion ->
                        val elegido = opcion == color
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(opcion))
                                .border(
                                    width = if (elegido) 3.dp else 1.dp,
                                    color = if (elegido) TINTA_EDITOR else MaterialTheme.colorScheme.outlineVariant,
                                    shape = CircleShape
                                )
                                .clickable { color = opcion },
                            contentAlignment = Alignment.Center
                        ) {
                            if (elegido) {
                                Icon(Icons.Default.Check, contentDescription = "Elegido", tint = TINTA_EDITOR, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                if (onEliminar != null) {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    TextButton(onClick = { confirmarBorrado = true }) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(6.dp))
                        Text("Borrar categoría", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val guardada = if (inicial != null && esBase) {
                        inicial.copy(emoji = emoji, color = color)
                    } else {
                        CategoriaDisponibilidad(
                            id = inicial?.id ?: idNueva,
                            nombre = nombre.trim(),
                            emoji = emoji,
                            color = color,
                            modo = modo,
                            bloquea = bloquea,
                            base = null
                        )
                    }
                    onGuardar(guardada)
                },
                enabled = puedeGuardar
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    )

    if (galeriaAbierta) {
        DialogoGaleriaEmojis(
            seleccionado = emoji,
            onElegir = { emoji = it; galeriaAbierta = false },
            onCerrar = { galeriaAbierta = false }
        )
    }

    if (confirmarBorrado && onEliminar != null) {
        AlertDialog(
            onDismissRequest = { confirmarBorrado = false },
            title = { Text("¿Borrar \"${nombre.trim()}\"?") },
            text = { Text("Los días ya apuntados con esta categoría no se borran: pasarán a verse como \"Otro\".") },
            confirmButton = {
                TextButton(onClick = { confirmarBorrado = false; onEliminar() }) {
                    Text("Borrar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmarBorrado = false }) { Text("Cancelar") } }
        )
    }
}

/** Galería de emojis por temas; al tocar uno se elige y se cierra. */
@Composable
private fun DialogoGaleriaEmojis(seleccionado: String, onElegir: (String) -> Unit, onCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Elige un icono") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                SECCIONES_EMOJI.forEach { (titulo, emojis) ->
                    Text(
                        titulo,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        emojis.forEach { opcion ->
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (opcion == seleccionado) LAVANDA_EDITOR else Color.Transparent)
                                    .clickable { onElegir(opcion) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(opcion, fontSize = 24.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Cerrar") } }
    )
}
