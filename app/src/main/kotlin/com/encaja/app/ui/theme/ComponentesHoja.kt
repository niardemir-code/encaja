package com.encaja.app.ui.theme

// NOTA: depende de Jetpack Compose (Material 3), no compilado en este entorno.
// Piezas de diseño compartidas por las hojas inferiores (ModalBottomSheet) de la app —
// nacieron en el diálogo de disponibilidad de Familia y se usan también en el de
// actividades de Guía, para que ambas hojas tengan exactamente el mismo estilo (tarjetas
// suaves con borde tenue, iconos en círculo, interruptores de acento...) en claro y en
// oscuro, ya que los colores salen del tema activo (MaterialTheme / LocalEncajaExtraColors).

import androidx.compose.ui.res.stringResource
import com.encaja.app.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Atajos a los colores del tema activo con los nombres del diseño (ver DialogoDisponibilidad). */
val tintaHoja: Color @Composable get() = MaterialTheme.colorScheme.onBackground
val tintaSuaveHoja: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
val tarjetaHoja: Color @Composable get() = LocalEncajaExtraColors.current.tarjetaSuave
val casillaHoja: Color @Composable get() = MaterialTheme.colorScheme.surface
val bordeHoja: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant
val acentoHoja: Color @Composable get() = LocalEncajaExtraColors.current.acento
val onAcentoHoja: Color @Composable get() = LocalEncajaExtraColors.current.onAcento
val lavandaHoja: Color @Composable get() = MaterialTheme.colorScheme.primaryContainer
val indigoHoja: Color @Composable get() = MaterialTheme.colorScheme.primary

@Composable
fun TituloSeccionHoja(titulo: String, subtitulo: String) {
    Text(titulo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = tintaHoja)
    Text(subtitulo, style = MaterialTheme.typography.bodyMedium, color = tintaSuaveHoja)
}

/** Tarjeta suave con borde tenue, base de cada sección de una hoja inferior. */
@Composable
fun TarjetaSeccionHoja(padding: Dp = 14.dp, contenido: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(tarjetaHoja)
            .border(1.dp, bordeHoja.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .padding(padding),
        content = contenido
    )
}

@Composable
fun SeparadorHoja() {
    HorizontalDivider(color = bordeHoja.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 14.dp))
}

@Composable
fun NotaSeccionHoja(texto: String, error: Boolean = false) {
    Text(
        texto,
        style = MaterialTheme.typography.bodySmall,
        color = if (error) MaterialTheme.colorScheme.error else tintaSuaveHoja,
        modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 10.dp)
    )
}

@Composable
fun BotonRedondoSuaveHoja(icono: ImageVector, descripcion: String, onClick: () -> Unit, tinta: Color = tintaHoja) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(tarjetaHoja)
            .border(1.dp, bordeHoja.copy(alpha = 0.6f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icono, contentDescription = descripcion, tint = tinta, modifier = Modifier.size(20.dp))
    }
}

/** Botón cuadrado redondeado solo con icono (papelera, disquete), de la altura de "Cancelar". */
@Composable
fun BotonCuadradoHoja(
    icono: ImageVector,
    descripcion: String,
    fondo: Color,
    tinta: Color,
    habilitado: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(fondo)
            .clickable(enabled = habilitado, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icono, contentDescription = descripcion, tint = tinta, modifier = Modifier.size(24.dp))
    }
}

/**
 * Varios disquetes apilados: el icono de "Guardar toda la serie" (el de un solo disquete
 * es "Guardar solo esta").
 */
@Composable
fun IconoVariosDisquetes(tinta: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(30.dp)) {
        Icon(Icons.Default.Save, contentDescription = null, tint = tinta.copy(alpha = 0.40f),
            modifier = Modifier.align(Alignment.TopStart).size(19.dp))
        Icon(Icons.Default.Save, contentDescription = null, tint = tinta.copy(alpha = 0.70f),
            modifier = Modifier.align(Alignment.TopStart).offset(x = 5.dp, y = 5.dp).size(19.dp))
        Icon(Icons.Default.Save, contentDescription = null, tint = tinta,
            modifier = Modifier.align(Alignment.TopStart).offset(x = 10.dp, y = 10.dp).size(19.dp))
    }
}

/** Botón cuadrado igual que [BotonCuadradoHoja] pero con el icono de varios disquetes. */
@Composable
fun BotonVariosDisquetesHoja(
    descripcion: String,
    fondo: Color,
    tinta: Color,
    habilitado: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(fondo)
            .semantics { contentDescription = descripcion }
            .clickable(enabled = habilitado, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        IconoVariosDisquetes(tinta)
    }
}

/**
 * Confirmación al guardar cuando hay una serie: [titulo] dice qué se va a guardar
 * ("Guardar solo esta actividad" o "Guardar la serie completa de la actividad"). Con
 * [serie] el icono es el de varios disquetes; si no, el de un solo disquete.
 */
@Composable
fun ConfirmarGuardadoHoja(
    titulo: String,
    detalle: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
    serie: Boolean = false
) {
    val iconoSerie: @Composable (Color) -> Unit = { tinta -> IconoVariosDisquetes(tinta) }
    AvisoHoja(
        icono = Icons.Default.Save,
        iconoContenido = if (serie) iconoSerie else null,
        titulo = titulo,
        texto = detalle,
        acciones = listOf(
            AccionAvisoHoja(stringResource(R.string.comun_cancelar), EstiloAccionAviso.BORDE) { onCancelar() },
            AccionAvisoHoja(stringResource(R.string.comun_guardar), EstiloAccionAviso.PRIMARIO) { onConfirmar() }
        ),
        onCerrar = onCancelar,
        conX = true,
        accionesEnFila = true
    )
}

@Composable
fun IconoEnCirculoHoja(icono: ImageVector, fondo: Color, tinta: Color, tamano: Dp = 40.dp) {
    Box(
        modifier = Modifier.size(tamano).clip(CircleShape).background(fondo),
        contentAlignment = Alignment.Center
    ) {
        Icon(icono, contentDescription = null, tint = tinta, modifier = Modifier.size(tamano / 2))
    }
}

/** Fila con icono en círculo, título, subtítulo e interruptor a la derecha. */
@Composable
fun FilaConInterruptorHoja(
    icono: ImageVector,
    titulo: String,
    subtitulo: String,
    activo: Boolean,
    onCambiar: (Boolean) -> Unit,
    tintaIcono: Color = indigoHoja
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCambiar(!activo) }.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconoEnCirculoHoja(icono, fondo = tintaIcono.copy(alpha = 0.18f), tinta = tintaIcono)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = tintaHoja)
            Text(subtitulo, style = MaterialTheme.typography.bodySmall, color = tintaSuaveHoja)
        }
        Switch(checked = activo, onCheckedChange = onCambiar, colors = coloresInterruptorEncaja())
    }
}

/** Caja "Desde / 09:00" (o cualquier valor) con un icono en círculo a la derecha; al
 * tocarla se ejecuta [onClick] (p.ej. abrir el selector de hora o de fecha). */
@Composable
fun CajaValorHoja(titulo: String, valor: String, icono: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(casillaHoja)
            .border(1.dp, bordeHoja.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodySmall, color = tintaSuaveHoja)
            Text(valor, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = tintaHoja, maxLines = 1)
        }
        IconoEnCirculoHoja(icono, fondo = lavandaHoja, tinta = indigoHoja, tamano = 34.dp)
    }
}

/** Campo de texto con un icono a la izquierda y el texto de ayuda dentro, para formularios. */
@Composable
fun CampoTextoConIconoHoja(
    valor: String,
    onCambiar: (String) -> Unit,
    placeholder: String,
    icono: ImageVector,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambiar,
        placeholder = { Text(placeholder, color = tintaSuaveHoja) },
        leadingIcon = { Icon(icono, contentDescription = null, tint = indigoHoja) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = casillaHoja,
            unfocusedContainerColor = casillaHoja,
            focusedBorderColor = acentoHoja,
            unfocusedBorderColor = lavandaHoja
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun CampoTextoHoja(valor: String, onCambiar: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambiar,
        placeholder = { Text(placeholder, color = tintaSuaveHoja) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = casillaHoja,
            unfocusedContainerColor = casillaHoja,
            focusedBorderColor = acentoHoja,
            unfocusedBorderColor = bordeHoja.copy(alpha = 0.6f)
        ),
        modifier = modifier.fillMaxWidth()
    )
}


/** Cómo se pinta cada botón de [AvisoHoja]. */
enum class EstiloAccionAviso { PELIGRO, SUAVE, BORDE, PRIMARIO }

/** Un botón a todo el ancho de [AvisoHoja]; [icono] es opcional. */
class AccionAvisoHoja(
    val texto: String,
    val estilo: EstiloAccionAviso,
    val icono: ImageVector? = null,
    val habilitada: Boolean = true,
    val onClick: () -> Unit
)

/**
 * Aviso de confirmación con el diseño de tarjeta grande de la app: icono en círculo con
 * chispas, título, texto y una columna de botones a todo el ancho. [peligro] pinta el
 * círculo del icono en rojo (acciones que no se pueden deshacer).
 */
@Composable
fun AvisoHoja(
    icono: ImageVector,
    titulo: String,
    texto: String?,
    acciones: List<AccionAvisoHoja>,
    onCerrar: () -> Unit,
    peligro: Boolean = true,
    /** Contenido propio entre el texto y los botones (p.ej. campos de un formulario). */
    contenido: (@Composable ColumnScope.() -> Unit)? = null,
    /** Dibuja el icono a mano (p.ej. varios disquetes) en vez de [icono]; recibe su tinta. */
    iconoContenido: (@Composable (Color) -> Unit)? = null,
    /** Añade una X arriba a la derecha que cierra el aviso. */
    conX: Boolean = false,
    /** Pone los botones uno al lado del otro en vez de apilados. */
    accionesEnFila: Boolean = false,
    /** Ancho máximo fijo (para calendarios y relojes, que no se encogen); null = ancho normal. */
    ancho: Dp? = null,
    /** Impide cerrarlo con el botón atrás o tocando fuera (avisos sin salida alternativa). */
    bloquearCierre: Boolean = false
) {
    val extra = LocalEncajaExtraColors.current
    val lavanda = MaterialTheme.colorScheme.primaryContainer
    val indigo = MaterialTheme.colorScheme.primary
    val fondoIcono = if (peligro) MaterialTheme.colorScheme.errorContainer else lavanda
    val tintaIcono = if (peligro) MaterialTheme.colorScheme.error else indigo

    Dialog(
        onDismissRequest = onCerrar,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !bloquearCierre,
            dismissOnClickOutside = !bloquearCierre
        )
    ) {
        Surface(
            shape = RoundedCornerShape(36.dp),
            color = MaterialTheme.colorScheme.background,
            modifier = if (ancho != null) {
                Modifier.padding(horizontal = 8.dp).widthIn(max = ancho).fillMaxWidth()
            } else {
                Modifier.padding(horizontal = 20.dp).fillMaxWidth()
            }
        ) {
            Box {
                // Mancha decorativa de fondo, igual que en el aviso de batería.
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = 36.dp, y = (-20).dp)
                        .size(210.dp)
                        .clip(CircleShape)
                        .background(lavanda.copy(alpha = 0.35f))
                )
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = if (ancho != null) 10.dp else 24.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier.size(84.dp).clip(CircleShape).background(fondoIcono),
                                contentAlignment = Alignment.Center
                            ) {
                                if (iconoContenido != null) {
                                    iconoContenido(tintaIcono)
                                } else {
                                    Icon(icono, contentDescription = null, tint = tintaIcono, modifier = Modifier.size(44.dp))
                                }
                            }
                            Canvas(modifier = Modifier.matchParentSize()) {
                                val color = extra.ambar
                                val trazo = 5.dp.toPx()
                                drawLine(color, Offset(size.width * 0.02f, size.height * 0.28f), Offset(size.width * 0.17f, size.height * 0.35f), trazo, StrokeCap.Round)
                                drawLine(color, Offset(size.width * 0.20f, size.height * 0.02f), Offset(size.width * 0.27f, size.height * 0.15f), trazo, StrokeCap.Round)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            titulo,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f).padding(end = if (conX) 36.dp else 0.dp)
                        )
                    }
                    if (texto != null) {
                        Text(texto, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                    }
                    contenido?.invoke(this)
                    if (accionesEnFila) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            acciones.forEach { accion -> BotonAvisoHoja(accion, Modifier.weight(1f)) }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            acciones.forEach { accion -> BotonAvisoHoja(accion, Modifier.fillMaxWidth()) }
                        }
                    }
                }
                if (conX) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(14.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(lavanda)
                            .clickable(onClick = onCerrar),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.comun_cerrar), tint = indigo, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun BotonAvisoHoja(accion: AccionAvisoHoja, modifierBase: Modifier) {
    val forma = RoundedCornerShape(28.dp)
    val modificador = modifierBase.height(56.dp)
    val contenido: @Composable androidx.compose.foundation.layout.RowScope.(Color) -> Unit = { tinta ->
        if (accion.icono != null) {
            Icon(accion.icono, contentDescription = null, tint = tinta, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
        }
        Text(accion.texto, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = tinta)
    }
    when (accion.estilo) {
        EstiloAccionAviso.PELIGRO -> {
            val tinta = MaterialTheme.colorScheme.error
            Button(
                onClick = accion.onClick, enabled = accion.habilitada, shape = forma, modifier = modificador,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = tinta)
            ) { contenido(tinta) }
        }
        EstiloAccionAviso.SUAVE -> {
            val tinta = MaterialTheme.colorScheme.primary
            Button(
                onClick = accion.onClick, enabled = accion.habilitada, shape = forma, modifier = modificador,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = tinta)
            ) { contenido(tinta) }
        }
        EstiloAccionAviso.PRIMARIO -> {
            val tinta = onAcentoHoja
            Button(
                onClick = accion.onClick, enabled = accion.habilitada, shape = forma, modifier = modificador,
                colors = ButtonDefaults.buttonColors(
                    containerColor = acentoHoja, contentColor = tinta,
                    disabledContainerColor = acentoHoja.copy(alpha = 0.35f), disabledContentColor = tinta.copy(alpha = 0.7f)
                )
            ) { contenido(tinta) }
        }
        EstiloAccionAviso.BORDE -> {
            val tinta = MaterialTheme.colorScheme.primary
            OutlinedButton(
                onClick = accion.onClick, enabled = accion.habilitada, shape = forma, modifier = modificador,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primaryContainer),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = tinta)
            ) { contenido(tinta) }
        }
    }
}
