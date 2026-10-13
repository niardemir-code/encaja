package com.encaja.app.ui.intro

// NOTA: depende de Jetpack Compose (Material 3), no compilado en este entorno.
// Pantallas de bienvenida (6 páginas) que se ven al abrir la app por primera vez.
// Usan una paleta clara fija (la del diseño), sea cual sea el tema del teléfono.

import androidx.compose.ui.res.stringResource
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.encaja.app.R
import kotlinx.coroutines.launch

private val Fondo = Color(0xFFF6F5FC)
private val Violeta = Color(0xFF3B2F86)
private val Acento = Color(0xFF6C5CE7)
private val Texto = Color(0xFF2A2547)
private val TextoSuave = Color(0xFF6B6785)
private val Lavanda = Color(0xFFE9E6F9)
private val Verde = Color(0xFF4CAF7A)
private val VerdeClaro = Color(0xFFDDF3E6)
private val Ambar = Color(0xFFF2B33D)
private val AmbarClaro = Color(0xFFFDF0DA)
private val Rojo = Color(0xFFE5645A)
private val RojoClaro = Color(0xFFFBE7E4)

private const val TOTAL = 6

/**
 * @param alTerminar se llama al acabar o saltar. [registrarse] es true si el usuario eligió
 * "Unirme" con un código (y por tanto lo normal es mostrar "Crear cuenta"), false si
 * pulsó "Ya tengo una cuenta" o "Saltar".
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IntroScreen(
    alTerminar: (registrarse: Boolean, codigo: String?) -> Unit,
    alCrearFamilia: () -> Unit
) {
    val pager = rememberPagerState(pageCount = { TOTAL })
    val scope = rememberCoroutineScope()
    val ultima = pager.currentPage == TOTAL - 1

    BackHandler(enabled = pager.currentPage > 0) {
        scope.launch { pager.animateScrollToPage(pager.currentPage - 1) }
    }

    Column(
        Modifier.fillMaxSize().background(Fondo).statusBarsPadding().navigationBarsPadding().imePadding()
    ) {
        // Cabecera: paso y "Saltar"
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (pager.currentPage > 0) {
                Text(
                    stringResource(R.string.intro_paso, pager.currentPage + 1, TOTAL),
                    color = Acento, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp
                )
            }
            Spacer(Modifier.weight(1f))
            if (!ultima) {
                TextButton(onClick = { alTerminar(false, null) }) {
                    Text(stringResource(R.string.intro_saltar), color = TextoSuave, fontWeight = FontWeight.SemiBold)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TextoSuave, modifier = Modifier.size(18.dp))
                }
            }
        }

        HorizontalPager(state = pager, modifier = Modifier.weight(1f).fillMaxWidth()) { pagina ->
            Column(
                Modifier.fillMaxSize().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (pagina) {
                    0 -> PaginaBienvenida()
                    1 -> PaginaSemaforo()
                    2 -> PaginaGenerica(
                        stringResource(R.string.intro_dia_titulo), stringResource(R.string.intro_dia_sub),
                        R.drawable.intro_dia,
                        listOf(
                            Fila(Icons.Default.Schedule, stringResource(R.string.intro_dia_f1), stringResource(R.string.intro_dia_f1_d)),
                            Fila(Icons.Default.DirectionsCar, stringResource(R.string.intro_dia_f2), stringResource(R.string.intro_dia_f2_d)),
                            Fila(Icons.Default.AccessTime, stringResource(R.string.intro_dia_f3), stringResource(R.string.intro_dia_f3_d))
                        )
                    )
                    3 -> PaginaGenerica(
                        stringResource(R.string.intro_disp_titulo), stringResource(R.string.intro_disp_sub),
                        R.drawable.intro_disponibilidad,
                        listOf(
                            Fila(Icons.Default.CalendarMonth, stringResource(R.string.intro_disp_f1), stringResource(R.string.intro_disp_f1_d)),
                            Fila(Icons.Default.Notifications, stringResource(R.string.intro_disp_f2), stringResource(R.string.intro_disp_f2_d, stringResource(R.string.app_name))),
                            Fila(Icons.Default.Group, stringResource(R.string.unidades_titulo), stringResource(R.string.intro_disp_f3_d))
                        )
                    )
                    4 -> PaginaGenerica(
                        stringResource(R.string.intro_avisos_titulo), stringResource(R.string.intro_avisos_sub),
                        R.drawable.intro_avisos,
                        listOf(
                            Fila(Icons.Default.Notifications, stringResource(R.string.intro_avisos_f1), stringResource(R.string.intro_avisos_f1_d)),
                            Fila(Icons.Default.PhoneAndroid, stringResource(R.string.intro_avisos_f2), stringResource(R.string.intro_avisos_f2_d)),
                            Fila(Icons.Default.Person, stringResource(R.string.intro_avisos_f3), stringResource(R.string.intro_avisos_f3_d))
                        )
                    )
                    else -> PaginaUnirse(
                        alUnirse = { codigo -> alTerminar(true, codigo) },
                        alCrearFamilia = alCrearFamilia,
                        alTenerCuenta = { alTerminar(false, null) }
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
        }

        // Pie: puntos + botones (la última página lleva los suyos dentro)
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(TOTAL) { i ->
                    val activo = i == pager.currentPage
                    Box(
                        Modifier.height(8.dp).width(if (activo) 24.dp else 8.dp)
                            .clip(CircleShape).background(if (activo) Acento else Lavanda)
                    )
                }
            }
            if (!ultima) {
                Spacer(Modifier.height(10.dp))
                if (pager.currentPage == 0) {
                    BotonPrincipal(stringResource(R.string.intro_comenzar)) { scope.launch { pager.animateScrollToPage(1) } }
                    TextButton(onClick = { alTerminar(false, null) }) {
                        Text(stringResource(R.string.intro_ya_tengo_cuenta), color = Acento, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Lavanda)
                        ) { Text(stringResource(R.string.intro_anterior), color = Violeta, fontWeight = FontWeight.Bold) }
                        Box(Modifier.weight(1f)) {
                            BotonPrincipal(stringResource(R.string.intro_siguiente)) { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BotonPrincipal(texto: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Violeta, contentColor = Color.White)
    ) {
        Text(texto, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.width(4.dp))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
    }
}

@Composable
private fun ColumnScope.Ilustracion(@DrawableRes recurso: Int) {
    // Ocupa el alto que sobra y se encoge (manteniendo proporción) para que la pantalla
    // entera quepa sin scroll.
    Image(
        painter = painterResource(recurso),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.weight(1f, fill = true).fillMaxWidth()
    )
}

private data class Fila(val icono: ImageVector, val titulo: String, val detalle: String)

@Composable
private fun FilaFuncion(f: Fila, fondoIcono: Color = Lavanda, colorIcono: Color = Acento) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(fondoIcono), contentAlignment = Alignment.Center) {
            Icon(f.icono, null, tint = colorIcono, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(f.titulo, color = Texto, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(f.detalle, color = TextoSuave, fontSize = 13.sp)
        }
    }
}

@Composable
private fun Titulo(texto: String, subtitulo: String) {
    Text(texto, color = Violeta, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
    Spacer(Modifier.height(4.dp))
    Text(subtitulo, color = TextoSuave, fontSize = 14.sp, textAlign = TextAlign.Center)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun ColumnScope.PaginaBienvenida() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.logo_encaja), null, modifier = Modifier.height(48.dp).aspectRatio(640f / 576f))
        Spacer(Modifier.width(8.dp))
        Text("encaja", color = Violeta, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
    }
    Spacer(Modifier.height(4.dp))
    Text(
        stringResource(R.string.login_lema),
        color = TextoSuave, fontSize = 16.sp, textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(8.dp))
    Ilustracion(R.drawable.intro_bienvenida)
    Spacer(Modifier.height(8.dp))
    FilaFuncion(Fila(Icons.Default.CalendarMonth, stringResource(R.string.actividades_titulo), stringResource(R.string.intro_b1_detalle)))
    FilaFuncion(Fila(Icons.Default.Group, stringResource(R.string.intro_b2), stringResource(R.string.intro_b2_detalle)))
    FilaFuncion(Fila(Icons.Default.CheckCircle, stringResource(R.string.intro_b3), stringResource(R.string.intro_b3_detalle)))
}

@Composable
private fun ColumnScope.PaginaGenerica(titulo: String, subtitulo: String, @DrawableRes imagen: Int, filas: List<Fila>) {
    Titulo(titulo, subtitulo)
    Ilustracion(imagen)
    Spacer(Modifier.height(8.dp))
    filas.forEach { FilaFuncion(it) }
}

@Composable
private fun ColumnScope.PaginaSemaforo() {
    Titulo(stringResource(R.string.intro_sem_titulo), stringResource(R.string.intro_sem_sub))
    Ilustracion(R.drawable.intro_semaforo)
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ChipLeyenda(Modifier.weight(1f), Verde, VerdeClaro, stringResource(R.string.intro_sem_verde), stringResource(R.string.intro_sem_verde_d))
        ChipLeyenda(Modifier.weight(1f), Ambar, AmbarClaro, stringResource(R.string.intro_sem_ambar), stringResource(R.string.intro_sem_ambar_d))
        ChipLeyenda(Modifier.weight(1f), Rojo, RojoClaro, stringResource(R.string.intro_sem_rojo), stringResource(R.string.intro_sem_rojo_d))
    }
    Spacer(Modifier.height(8.dp))
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(AmbarClaro).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Lightbulb, null, tint = Ambar, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            stringResource(R.string.intro_sem_consejo),
            color = Texto, fontSize = 12.sp, lineHeight = 15.sp
        )
    }
}

@Composable
private fun ChipLeyenda(modifier: Modifier, color: Color, fondo: Color, titulo: String, detalle: String) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(fondo).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(14.dp).clip(CircleShape).background(color))
        Spacer(Modifier.height(6.dp))
        Text(titulo, color = Texto, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
        Text(detalle, color = TextoSuave, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ColumnScope.PaginaUnirse(alUnirse: (String) -> Unit, alCrearFamilia: () -> Unit, alTenerCuenta: () -> Unit) {
    var codigo by remember { mutableStateOf("") }
    Titulo(stringResource(R.string.intro_unirse_titulo), stringResource(R.string.intro_unirse_sub))
    Ilustracion(R.drawable.intro_familia)
    Spacer(Modifier.height(8.dp))
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White).padding(12.dp)
    ) {
        OutlinedTextField(
            value = codigo,
            onValueChange = { codigo = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(12) },
            label = { Text(stringResource(R.string.intro_codigo_label)) },
            placeholder = { Text(stringResource(R.string.intro_codigo_placeholder)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { alUnirse(codigo) },
            enabled = codigo.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Violeta, contentColor = Color.White)
        ) { Text(stringResource(R.string.intro_unirme), fontWeight = FontWeight.Bold, fontSize = 16.sp) }
    }
    Spacer(Modifier.height(12.dp))
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Lavanda).padding(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Default.Info, null, tint = Acento, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(stringResource(R.string.intro_como_codigo), color = Violeta, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(
                stringResource(R.string.intro_como_codigo_texto, stringResource(R.string.app_name)),
                color = TextoSuave, fontSize = 12.sp, lineHeight = 15.sp
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    Button(
        onClick = alCrearFamilia,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Acento, contentColor = Color.White)
    ) {
        Icon(Icons.Default.GroupAdd, null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.intro_crear_familia), fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
    Spacer(Modifier.height(6.dp))
    OutlinedButton(
        onClick = alTenerCuenta,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Acento)
    ) {
        Text(stringResource(R.string.intro_ya_tengo_cuenta), color = Acento, fontWeight = FontWeight.Bold)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Acento)
    }
}
