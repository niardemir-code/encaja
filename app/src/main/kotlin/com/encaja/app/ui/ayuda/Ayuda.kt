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

import com.encaja.app.R
import androidx.compose.ui.res.stringResource
import androidx.annotation.StringRes
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.remember
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
        PreferenciaTour.reiniciar(c)
    }
}

/** Dónde va el tour de "Primeros pasos" (etapa actual; al llegar al total, terminado o saltado). */
object PreferenciaTour {
    private const val CLAVE = "tour_etapa"
    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun etapa(c: Context): Int = prefs(c).getInt(CLAVE, 0)
    fun guardar(c: Context, etapa: Int) = prefs(c).edit().putInt(CLAVE, etapa).apply()
    fun reiniciar(c: Context) = prefs(c).edit().remove(CLAVE).apply()
}

/** Datos de la etapa del tour en curso: cuál es, cuántas hay, y qué hacer al terminarla o saltar. */
class DatosTour(
    val etapa: Int,
    val total: Int,
    /** Último globo de todo el tour: en vez de "Siguiente" pone "Entendido". */
    val esUltimo: Boolean,
    val alTerminar: () -> Unit,
    val alSaltar: () -> Unit
)

/** Un paso de la visita: [clave] del elemento a iluminar (null = globo centrado, sin foco). */
data class PasoAyuda(
    val clave: String?,
    @StringRes val titulo: Int,
    @StringRes val texto: Int,
    /** El usuario debe tocar el elemento iluminado para continuar (no hay botón "Siguiente"). */
    val tocar: Boolean = false
)

class AyudaState {
    /** Posición en pantalla (coordenadas de la ventana) de cada elemento marcado. */
    val posiciones: SnapshotStateMap<String, Rect> = mutableStateMapOf()

    /** Qué hacer cuando el usuario toca el elemento iluminado de un paso "tocar". */
    val acciones: MutableMap<String, () -> Unit> = mutableMapOf()

    /** Cómo desplazar la pantalla hasta que el elemento marcado quede a la vista. */
    private val traedores: MutableMap<String, suspend () -> Unit> = mutableMapOf()

    fun registrarTraedor(clave: String, traer: suspend () -> Unit) {
        traedores[clave] = traer
    }

    /** Desplaza la lista (si hace falta) para que el elemento [clave] se vea entero. */
    suspend fun traerAVista(clave: String) {
        runCatching { traedores[clave]?.invoke() }
    }

    var pasos by mutableStateOf<List<PasoAyuda>>(emptyList())
        private set
    var indice by mutableIntStateOf(0)
        private set

    /** Si la visita en curso es una etapa del tour de primeros pasos. */
    var tour by mutableStateOf<DatosTour?>(null)
        private set

    /** Sube cada vez que se pide volver a ver el tour desde el principio. */
    var reinicios by mutableIntStateOf(0)
        private set

    val activa: Boolean get() = pasos.isNotEmpty()

    fun iniciar(nuevos: List<PasoAyuda>, datosTour: DatosTour? = null) {
        if (activa || nuevos.isEmpty()) return
        indice = 0
        tour = datosTour
        pasos = nuevos
    }

    fun siguiente() {
        if (indice < pasos.lastIndex) {
            indice++
        } else {
            val t = tour
            cerrar()
            t?.alTerminar?.invoke()
        }
    }

    /** Salta lo que queda; en el tour, salta el tour entero. */
    fun saltar() {
        val t = tour
        cerrar()
        t?.alSaltar?.invoke()
    }

    fun reiniciarTour() {
        reinicios++
    }

    fun cerrar() {
        pasos = emptyList()
        indice = 0
        tour = null
    }
}

val LocalAyuda = staticCompositionLocalOf<AyudaState?> { null }

/** Marca un elemento con una [clave] para que un paso de la ayuda pueda iluminarlo. */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.ayudaObjetivo(clave: String, alTocar: (() -> Unit)? = null): Modifier = composed {
    val ayuda = LocalAyuda.current
    val traer = remember { BringIntoViewRequester() }
    ayuda?.registrarTraedor(clave) { traer.bringIntoView() }
    bringIntoViewRequester(traer).onGloballyPositioned { coordenadas ->
        if (ayuda != null) {
            if (alTocar != null) ayuda.acciones[clave] = alTocar
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
    BackHandler { ayuda.saltar() }

    val paso = ayuda.pasos[ayuda.indice]
    val objetivo = paso.clave?.let { ayuda.posiciones[it] }
    val datosTour = ayuda.tour
    val ultimo = ayuda.indice == ayuda.pasos.lastIndex && (datosTour == null || datosTour.esUltimo)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            // Absorbe los toques: mientras dura la ayuda no se pulsa nada de debajo.
            // En un paso "tocar", un toque sobre el elemento iluminado ejecuta su acción.
            .pointerInput(paso, objetivo) {
                detectTapGestures { punto ->
                    if (paso.tocar && paso.clave != null && objetivo != null && objetivo.inflate(6.dp.toPx()).contains(punto)) {
                        ayuda.acciones[paso.clave]?.invoke()
                    }
                }
            }
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
                    stringResource(paso.titulo),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(6.dp))
                Text(stringResource(paso.texto), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (datosTour != null) stringResource(R.string.tour_paso_de, datosTour.etapa + 1, datosTour.total)
                        else stringResource(R.string.ayuda_paso_de, ayuda.indice + 1, ayuda.pasos.size),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    if (!ultimo) {
                        TextButton(onClick = { ayuda.saltar() }) { Text(stringResource(R.string.intro_saltar)) }
                        Spacer(Modifier.width(4.dp))
                    }
                    if (!paso.tocar) {
                        Button(onClick = { ayuda.siguiente() }, shape = RoundedCornerShape(14.dp)) {
                            Text(stringResource(if (ultimo) R.string.comun_entendido else R.string.intro_siguiente), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
