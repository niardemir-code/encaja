package com.encaja.app.ui.ayuda

// NOTA: depende de Jetpack Compose (Material 3), no compilado en este entorno.
// Ayuda interactiva de primera vez: la primera vez que se entra en una pantalla se oscurece
// el resto y se ilumina, uno a uno, cada elemento importante con un globo explicativo.
//
// Piezas:
//  - PreferenciaAyuda: recuerda qué pantallas ya han enseñado su ayuda (en el móvil).
//  - AyudaState: el recorrido en curso y la posición en pantalla de cada elemento marcado.
//  - Modifier.ayudaObjetivo("clave"): marca un elemento para poder iluminarlo.
//  - AyudaPantalla(...): declara los pasos de una pantalla y los lanza la primera vez.
//  - CapaAyuda(...): dibuja el fondo oscuro, el foco y el globo; va una sola vez, en la raíz.

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private const val PREFS = "encaja_prefs"
private const val PREFIJO = "ayuda_vista_"

/** Qué pantallas ya han enseñado su ayuda en este móvil. */
object PreferenciaAyuda {
    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun vista(c: Context, id: String): Boolean = prefs(c).getBoolean(PREFIJO + id, false)
    fun marcarVista(c: Context, id: String) = prefs(c).edit().putBoolean(PREFIJO + id, true).apply()

    /** Hace que todas las ayudas vuelvan a mostrarse al entrar en cada pantalla. */
    fun reiniciar(c: Context) {
        val editor = prefs(c).edit()
        prefs(c).all.keys.filter { it.startsWith(PREFIJO) }.forEach { editor.remove(it) }
        editor.apply()
    }
}

/** Un paso de la visita: [clave] del elemento a iluminar (null = globo centrado, sin foco). */
data class PasoAyuda(val clave: String?, val titulo: String, val texto: String)

class AyudaState {
    /** Posición en pantalla (coordenadas de la ventana) de cada elemento marcado. */
    val posiciones: SnapshotStateMap<String, Rect> = mutableStateMapOf()

    var pasos by mutableStateOf<List<PasoAyuda>>(emptyList())
        private set
    var indice by mutableIntStateOf(0)
        private set

    val activa: Boolean get() = pasos.isNotEmpty()

    fun iniciar(nuevos: List<PasoAyuda>) {
        if (activa || nuevos.isEmpty()) return
        indice = 0
        pasos = nuevos
    }

    fun siguiente() {
        if (indice < pasos.lastIndex) indice++ else cerrar()
    }

    fun cerrar() {
        pasos = emptyList()
        indice = 0
    }
}

val LocalAyuda = staticCompositionLocalOf<AyudaState?> { null }

/** Marca un elemento con una [clave] para que un paso de la ayuda pueda iluminarlo. */
fun Modifier.ayudaObjetivo(clave: String): Modifier = composed {
    val ayuda = LocalAyuda.current
    onGloballyPositioned { coordenadas ->
        if (ayuda != null) {
            val rect = coordenadas.boundsInRoot()
            if (ayuda.posiciones[clave] != rect) ayuda.posiciones[clave] = rect
        }
    }
}

/**
 * Declara la ayuda de una pantalla. La primera vez que [listo] sea true (la pantalla ya tiene
 * datos y sus elementos están dibujados) lanza la visita, y la marca como vista.
 */
@Composable
fun AyudaPantalla(id: String, listo: Boolean, pasos: List<PasoAyuda>) {
    val ayuda = LocalAyuda.current ?: return
    val contexto = LocalContext.current
    LaunchedEffect(id, listo) {
        if (listo && !PreferenciaAyuda.vista(contexto, id)) {
            delay(700) // deja que termine de dibujarse la pantalla y se midan los elementos
            PreferenciaAyuda.marcarVista(contexto, id)
            ayuda.iniciar(pasos)
        }
    }
}

/** Fondo oscuro con un hueco sobre el elemento actual y un globo con el texto. Una sola vez, en la raíz. */
@Composable
fun CapaAyuda(ayuda: AyudaState) {
    if (!ayuda.activa) return
    BackHandler { ayuda.cerrar() }

    val paso = ayuda.pasos[ayuda.indice]
    val objetivo = paso.clave?.let { ayuda.posiciones[it] }
    val ultimo = ayuda.indice == ayuda.pasos.lastIndex

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            // Absorbe los toques: mientras dura la ayuda no se pulsa nada de debajo.
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        val density = LocalDensity.current
        val altoPx = with(density) { maxHeight.toPx() }
        val margenFoco = with(density) { 6.dp.toPx() }
        val separacion = with(density) { 14.dp.toPx() }

        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
            drawRect(Color.Black.copy(alpha = 0.72f))
            if (objetivo != null) {
                val topLeft = Offset(objetivo.left - margenFoco, objetivo.top - margenFoco)
                val tamano = Size(objetivo.width + margenFoco * 2, objetivo.height + margenFoco * 2)
                val radio = CornerRadius(18.dp.toPx())
                drawRoundRect(Color.Black, topLeft, tamano, radio, blendMode = BlendMode.Clear)
                drawRoundRect(Color.White.copy(alpha = 0.9f), topLeft, tamano, radio, style = Stroke(2.dp.toPx()))
            }
        }

        // El globo va debajo del foco si éste está en la mitad de arriba; si no, encima.
        val debajo = objetivo != null && objetivo.center.y < altoPx / 2f
        val encima = objetivo != null && !debajo
        val modificadorGlobo = when {
            debajo -> Modifier
                .align(Alignment.TopCenter)
                .padding(top = with(density) { (objetivo!!.bottom + margenFoco + separacion).toDp() })
            encima -> Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = with(density) { (altoPx - objetivo!!.top + margenFoco + separacion).toDp() })
            else -> Modifier.align(Alignment.Center)
        }

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = modificadorGlobo.padding(horizontal = 20.dp).fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    paso.titulo,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(6.dp))
                Text(paso.texto, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${ayuda.indice + 1} de ${ayuda.pasos.size}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    if (!ultimo) {
                        TextButton(onClick = { ayuda.cerrar() }) { Text("Saltar") }
                        Spacer(Modifier.width(4.dp))
                    }
                    Button(onClick = { ayuda.siguiente() }, shape = RoundedCornerShape(14.dp)) {
                        Text(if (ultimo) "Entendido" else "Siguiente", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
