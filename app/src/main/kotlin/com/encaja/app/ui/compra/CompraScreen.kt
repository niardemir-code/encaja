package com.encaja.app.ui.compra

// NOTA: depende de Jetpack Compose y Hilt, no compilado en este entorno.
// Diseño según la maqueta aportada: título grande con subtítulo, una tarjeta por tienda
// (cabecera de color pastel con círculo, nombre y nº de artículos, que se pliega y
// despliega, y debajo los artículos en filas blancas con casilla cuadrada y papelera) y
// un panel lavanda abajo para añadir artículos con "Artículo", "Tienda" y un botón "+".
// Sin logotipos de comercio: el círculo lleva la inicial de la tienda.

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.encaja.app.domain.model.ArticuloCompra
import com.encaja.app.domain.model.ArticuloCompraId
import com.encaja.app.ui.theme.LocalEncajaExtraColors
import kotlin.math.abs

@Composable
fun CompraScreen(viewModel: CompraViewModel = hiltViewModel()) {
    val pantalla by viewModel.pantalla.collectAsState()
    // Tiendas plegadas: vive aquí (fuera del "when") para no perderse al recargar la lista.
    val tiendasPlegadas = remember { mutableStateListOf<String>() }
    // El ViewModel vive ahora lo que vive la app (ver entradaDelGrafo en EncajaApp), para
    // no perder la semana/día elegido al cambiar de pestaña; a cambio, hay que recargar
    // al reentrar para reflejar cambios hechos desde otras pantallas.
    LaunchedEffect(Unit) { viewModel.recargar() }

    when (val estadoActual = pantalla) {
        is CompraPantallaEstado.Cargando -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is CompraPantallaEstado.SinFamilia -> {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Vincúlate a una familia desde la pestaña Semana para ver la lista de la compra.",
                    textAlign = TextAlign.Center
                )
            }
        }

        is CompraPantallaEstado.ConDatos -> {
            ContenidoCompra(
                estado = estadoActual.estado,
                tiendasPlegadas = tiendasPlegadas,
                onAgregar = { nombre, tienda -> viewModel.agregarArticulo(nombre, tienda) },
                onMarcarComprado = { articulo, comprado -> viewModel.marcarComprado(articulo, comprado) },
                onRenombrar = { articulo, nombre -> viewModel.renombrarArticulo(articulo, nombre) },
                onAgregarATienda = { nombre, tienda -> viewModel.agregarArticulo(nombre, tienda) },
                onEliminar = { id -> viewModel.eliminarArticulo(id) }
            )
        }
    }
}

@Composable
private fun ContenidoCompra(
    estado: CompraUiState,
    tiendasPlegadas: MutableList<String>,
    onAgregar: (String, String) -> Unit,
    onMarcarComprado: (ArticuloCompra, Boolean) -> Unit,
    onRenombrar: (ArticuloCompra, String) -> Unit,
    onAgregarATienda: (String, String) -> Unit,
    onEliminar: (ArticuloCompraId) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Cabecera: título grande y subtítulo.
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp)) {
            Text(
                "Compra",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Lista de la compra familiar",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (estado.grupos.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Todavía no hay nada en la lista de la compra.\nAñade el primer artículo abajo.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(estado.grupos, key = { it.tienda }) { grupo ->
                    TarjetaTienda(
                        grupo = grupo,
                        plegada = grupo.tienda in tiendasPlegadas,
                        onAlternarPlegada = {
                            if (grupo.tienda in tiendasPlegadas) tiendasPlegadas.remove(grupo.tienda)
                            else tiendasPlegadas.add(grupo.tienda)
                        },
                        onMarcarComprado = onMarcarComprado,
                        onRenombrar = onRenombrar,
                        onAgregarATienda = onAgregarATienda,
                        onEliminar = onEliminar
                    )
                }
            }
        }

        PanelNuevoArticulo(tiendas = estado.grupos.map { it.tienda }, onAgregar = onAgregar)
    }
}

/** Color pastel de la cabecera de una tienda: siempre el mismo para el mismo nombre. */
@Composable
private fun colorDeTienda(tienda: String): Color {
    val extra = LocalEncajaExtraColors.current
    val paleta = listOf(
        MaterialTheme.colorScheme.primaryContainer,
        extra.verdeContainer,
        extra.cremaAviso,
        extra.rosaHueco,
        MaterialTheme.colorScheme.secondaryContainer
    )
    return paleta[abs(tienda.trim().lowercase().hashCode()) % paleta.size]
}

@Composable
private fun TarjetaTienda(
    grupo: GrupoTienda,
    plegada: Boolean,
    onAlternarPlegada: () -> Unit,
    onMarcarComprado: (ArticuloCompra, Boolean) -> Unit,
    onRenombrar: (ArticuloCompra, String) -> Unit,
    onAgregarATienda: (String, String) -> Unit,
    onEliminar: (ArticuloCompraId) -> Unit
) {
    val colorCabecera = colorDeTienda(grupo.tienda)
    val formaTarjeta = RoundedCornerShape(24.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(formaTarjeta)
            .background(colorCabecera)
    ) {
        // Cabecera: círculo con la inicial, nombre, nº de artículos y flecha de plegar.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onAlternarPlegada)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    grupo.tienda.trim().take(1).uppercase(),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    grupo.tienda,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val pendientes = grupo.articulos.count { !it.comprado }
                val total = grupo.articulos.size
                Text(
                    textoArticulos(total, pendientes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                if (plegada) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                contentDescription = if (plegada) "Desplegar ${grupo.tienda}" else "Plegar ${grupo.tienda}",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        if (!plegada) {
            // Artículos sobre una base blanca redondeada que "cuelga" de la cabecera.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, end = 6.dp, bottom = 6.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                grupo.articulos.forEach { articulo ->
                    FilaArticulo(
                        articulo = articulo,
                        onMarcarComprado = { comprado -> onMarcarComprado(articulo, comprado) },
                        onRenombrar = { nombre -> onRenombrar(articulo, nombre) },
                        onEliminar = { onEliminar(articulo.id) }
                    )
                }
                FilaNuevoArticuloEnTienda(onAgregar = { nombre -> onAgregarATienda(nombre, grupo.tienda) })
            }
        }
    }
}

/** "+ Añadir artículo" al final de la lista de una tienda: al tocarlo aparece un campo para
 * escribir el nombre y el artículo se añade a ESA tienda, sin tener que repetir su nombre. */
@Composable
private fun FilaNuevoArticuloEnTienda(onAgregar: (String) -> Unit) {
    var abierta by remember { mutableStateOf(false) }
    var texto by remember { mutableStateOf("") }
    val foco = remember { FocusRequester() }
    val acento = LocalEncajaExtraColors.current.acento

    fun confirmar() {
        if (texto.isNotBlank()) {
            onAgregar(texto)
            texto = ""   // sigue abierto: lo normal es añadir varios seguidos
        }
    }

    if (!abierta) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { abierta = true }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(26.dp).clip(CircleShape).background(acento),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = LocalEncajaExtraColors.current.onAcento, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(14.dp))
            Text("Añadir artículo", color = acento, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LaunchedEffect(Unit) { foco.requestFocus() }
        OutlinedTextField(
            value = texto,
            onValueChange = { texto = it },
            placeholder = { Text("Nuevo artículo", maxLines = 1) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { confirmar() }),
            trailingIcon = {
                Row {
                    IconButton(onClick = { confirmar() }, enabled = texto.isNotBlank()) {
                        Icon(Icons.Default.Check, contentDescription = "Añadir", tint = if (texto.isNotBlank()) acento else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { abierta = false; texto = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().focusRequester(foco)
        )
    }
}

/** "1 artículo", "3 artículos" o, si ya hay comprados, "3 artículos · 1 por comprar". */
private fun textoArticulos(total: Int, pendientes: Int): String {
    val base = if (total == 1) "1 artículo" else "$total artículos"
    return if (pendientes in 1 until total) "$base · $pendientes por comprar" else base
}

@Composable
private fun FilaArticulo(
    articulo: ArticuloCompra,
    onMarcarComprado: (Boolean) -> Unit,
    onRenombrar: (String) -> Unit,
    onEliminar: () -> Unit
) {
    var editando by remember { mutableStateOf(false) }
    var nombreEditado by remember(articulo.nombre) { mutableStateOf(articulo.nombre) }

    if (editando) {
        AlertDialog(
            onDismissRequest = { editando = false },
            title = { Text("Cambiar nombre") },
            text = {
                OutlinedTextField(
                    value = nombreEditado,
                    onValueChange = { nombreEditado = it },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (nombreEditado.isNotBlank()) { onRenombrar(nombreEditado); editando = false }
                    }),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { onRenombrar(nombreEditado); editando = false },
                    enabled = nombreEditado.isNotBlank()
                ) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { editando = false }) { Text("Cancelar") } }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.background)
            .clickable { onMarcarComprado(!articulo.comprado) }
            .padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CasillaCompra(marcada = articulo.comprado)
        Spacer(Modifier.width(14.dp))
        Text(
            articulo.nombre,
            style = MaterialTheme.typography.bodyLarge,
            textDecoration = if (articulo.comprado) TextDecoration.LineThrough else null,
            color = if (articulo.comprado) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).padding(vertical = 8.dp)
        )
        IconButton(onClick = { nombreEditado = articulo.nombre; editando = true }) {
            Icon(
                Icons.Default.Edit,
                contentDescription = "Cambiar nombre de ${articulo.nombre}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onEliminar) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Eliminar ${articulo.nombre}",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** Casilla cuadrada redondeada con borde índigo; llena con un tic al marcarla. */
@Composable
private fun CasillaCompra(marcada: Boolean) {
    val color = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(if (marcada) color else Color.Transparent)
            .border(2.dp, color, RoundedCornerShape(7.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (marcada) {
            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
        }
    }
}

/** Panel lavanda de abajo: para crear una tienda nueva (artículo + tienda) con el botón redondo "+".
 * Para añadir a una tienda que ya existe se usa el "+ Añadir artículo" de su propia tarjeta. */
@Composable
private fun PanelNuevoArticulo(tiendas: List<String>, onAgregar: (String, String) -> Unit) {
    var nombre by remember { mutableStateOf("") }
    var tienda by remember { mutableStateOf("") }
    val extra = LocalEncajaExtraColors.current
    val puedeAgregar = nombre.isNotBlank() && tienda.isNotBlank()

    fun agregar() {
        if (puedeAgregar) {
            // Si ya existe esa tienda (aunque se escriba con otras mayúsculas), se usa el nombre
            // existente para que el artículo vaya a la misma lista y no se cree otra duplicada.
            val tiendaFinal = tiendas.firstOrNull { it.equals(tienda.trim(), ignoreCase = true) } ?: tienda
            onAgregar(nombre, tiendaFinal)
            nombre = ""
            // La tienda se conserva: lo normal es añadir varios artículos seguidos a la misma.
        }
    }

    Column {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(extra.tarjetaSuave)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CampoPanel(
            valor = nombre,
            onCambio = { nombre = it },
            marcador = "Artículo",
            icono = Icons.Default.ShoppingCart,
            accion = ImeAction.Next,
            onAccion = {},
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        CampoPanel(
            valor = tienda,
            onCambio = { tienda = it },
            marcador = "Tienda",
            icono = Icons.Default.Storefront,
            accion = ImeAction.Done,
            onAccion = { agregar() },
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (puedeAgregar) extra.acento else extra.acento.copy(alpha = 0.4f))
                .clickable(enabled = puedeAgregar, onClick = { agregar() }),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = "Añadir artículo", tint = extra.onAcento, modifier = Modifier.size(28.dp))
        }
    }
    }
}

@Composable
private fun CampoPanel(
    valor: String,
    onCambio: (String) -> Unit,
    marcador: String,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    accion: ImeAction,
    onAccion: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        placeholder = { Text(marcador, maxLines = 1) },
        leadingIcon = { Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = accion),
        keyboardActions = KeyboardActions(onNext = { onAccion() }, onDone = { onAccion() }),
        modifier = modifier
    )
}
