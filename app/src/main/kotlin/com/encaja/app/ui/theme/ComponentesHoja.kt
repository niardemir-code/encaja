package com.encaja.app.ui.theme

// NOTA: depende de Jetpack Compose (Material 3), no compilado en este entorno.
// Piezas de diseño compartidas por las hojas inferiores (ModalBottomSheet) de la app —
// nacieron en el diálogo de disponibilidad de Familia y se usan también en el de
// actividades de Guía, para que ambas hojas tengan exactamente el mismo estilo (tarjetas
// suaves con borde tenue, iconos en círculo, interruptores de acento...) en claro y en
// oscuro, ya que los colores salen del tema activo (MaterialTheme / LocalEncajaExtraColors).

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
 * ("Guardar solo esta actividad" o "Guardar la serie completa de la actividad").
 */
@Composable
fun ConfirmarGuardadoHoja(
    titulo: String,
    detalle: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = { Text(detalle) },
        confirmButton = { TextButton(onClick = onConfirmar) { Text("Guardar", fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
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
