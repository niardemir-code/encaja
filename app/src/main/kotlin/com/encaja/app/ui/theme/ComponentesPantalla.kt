package com.encaja.app.ui.theme

// NOTA: depende de Jetpack Compose (Material 3), no compilado en este entorno.
// Piezas de diseño compartidas por las pantallas "a pantalla completa" de Ajustes (menú,
// Cuidadores, Niños, Actividades, Ocupaciones, Ayuda...): fondo con manchas lavanda, cabecera
// grande, tarjetas blancas redondeadas con sombra suave, avatares con iniciales y botones
// en píldora. Siguen la misma línea que las ventanas emergentes (AvisoHoja).

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Fondo y color de letra del avatar de cada persona, por orden de la lista. */
val PaletaAvatarPersonas = listOf(
    Color(0xFFE4E2FA) to Color(0xFF3B3597),
    Color(0xFFD6F1E3) to Color(0xFF1E6B45),
    Color(0xFFE6DEFA) to Color(0xFF4A3B9A),
    Color(0xFFFBDDE4) to Color(0xFF9A2A45),
    Color(0xFFDCEAFA) to Color(0xFF1E5A8C),
    Color(0xFFFCE4D6) to Color(0xFF9A3D1C)
)

/** Avatar de las unidades familiares: gris azulado, distinto de la paleta de personas. */
val ColorAvatarUnidad = Color(0xFFDDE3EE) to Color(0xFF3A4A6B)

/** Iniciales (máximo dos) de un nombre: "Sílvia Izquierdo" → "SI". */
fun inicialesDe(nombre: String): String =
    nombre.trim().split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }

/** Fondo de pantalla con dos manchas lavanda, igual que el de las ventanas emergentes. */
@Composable
fun FondoPantalla(modifier: Modifier = Modifier, contenido: @Composable BoxScope.() -> Unit) {
    val mancha = lavandaHoja
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = mancha.copy(alpha = 0.35f),
                radius = 130.dp.toPx(),
                center = Offset(size.width + 10.dp.toPx(), 40.dp.toPx())
            )
            drawCircle(
                color = mancha.copy(alpha = 0.22f),
                radius = 110.dp.toPx(),
                center = Offset(-20.dp.toPx(), size.height - 60.dp.toPx())
            )
        }
        contenido()
    }
}

/** Título grande con subtítulo opcional, arriba de cada pantalla. */
@Composable
fun CabeceraPantalla(titulo: String, subtitulo: String? = null) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 20.dp)) {
        Text(titulo, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = tintaHoja)
        if (!subtitulo.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(subtitulo, style = MaterialTheme.typography.bodyMedium, color = tintaSuaveHoja)
        }
    }
}

/** Rótulo de una sección dentro de una pantalla ("Personas", "Unidades familiares"). */
@Composable
fun RotuloSeccionPantalla(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = tintaHoja,
        modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
    )
}

/** Texto suelto (estado vacío, error, "vincúlate a una familia...") con el color suave del tema. */
@Composable
fun MensajePantalla(texto: String, error: Boolean = false) {
    Text(
        texto,
        style = MaterialTheme.typography.bodyMedium,
        color = if (error) MaterialTheme.colorScheme.error else tintaSuaveHoja,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

/** Tarjeta blanca redondeada con sombra suave; clicable si se pasa [onClick]. */
@Composable
fun TarjetaFilaHoja(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contenido: @Composable RowScope.() -> Unit
) {
    val forma = RoundedCornerShape(22.dp)
    val base = modifier
        .fillMaxWidth()
        .shadow(3.dp, forma, ambientColor = indigoHoja.copy(alpha = 0.10f), spotColor = indigoHoja.copy(alpha = 0.10f))
        .clip(forma)
        .background(casillaHoja)
        .border(1.dp, bordeHoja.copy(alpha = 0.5f), forma)
    Row(
        modifier = (if (onClick != null) base.clickable(onClick = onClick) else base).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = contenido
    )
}

/** Círculo con las iniciales de una persona o el código de una unidad. */
@Composable
fun AvatarInicialesHoja(iniciales: String, colores: Pair<Color, Color>, tamano: Dp = 46.dp) {
    Box(
        modifier = Modifier.size(tamano).clip(CircleShape).background(colores.first),
        contentAlignment = Alignment.Center
    ) {
        Text(iniciales, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = colores.second)
    }
}

/** Casilla redonda de selección (marcada = círculo con tic en el color de acento). */
@Composable
fun CasillaRedondaHoja(marcada: Boolean) {
    Icon(
        if (marcada) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
        contentDescription = null,
        tint = if (marcada) acentoHoja else bordeHoja,
        modifier = Modifier.size(26.dp)
    )
}

/** Botón en píldora de ancho completo: de acento, o rojo si es [peligro]. */
@Composable
fun BotonPildoraHoja(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    habilitado: Boolean = true,
    peligro: Boolean = false
) {
    val fondo = if (peligro) MaterialTheme.colorScheme.error else acentoHoja
    val tinta = if (peligro) MaterialTheme.colorScheme.onError else onAcentoHoja
    Button(
        onClick = onClick,
        enabled = habilitado,
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = fondo,
            contentColor = tinta,
            disabledContainerColor = fondo.copy(alpha = 0.35f),
            disabledContentColor = tinta.copy(alpha = 0.7f)
        ),
        modifier = modifier.fillMaxWidth().height(56.dp)
    ) {
        if (icono != null) {
            Icon(icono, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(texto, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

/** Botón en píldora con borde, para la acción secundaria ("Cerrar sesión"). */
@Composable
fun BotonPildoraBordeHoja(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier, icono: ImageVector? = null) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .border(1.5.dp, bordeHoja, RoundedCornerShape(28.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        if (icono != null) {
            Icon(icono, contentDescription = null, tint = tintaHoja, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(texto, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = tintaHoja)
    }
}

/** Chip de filtro en píldora: de acento si está elegido, con borde si no. */
@Composable
fun ChipFiltroHoja(texto: String, elegido: Boolean, onClick: () -> Unit) {
    val forma = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .clip(forma)
            .background(if (elegido) acentoHoja else casillaHoja)
            .border(1.dp, if (elegido) acentoHoja else bordeHoja, forma)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            texto,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (elegido) onAcentoHoja else tintaHoja
        )
    }
}
