package com.encaja.app.ui.compra

// NOTA: depende de Jetpack Compose y Hilt, no compilado en este entorno.

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.encaja.app.domain.model.ArticuloCompra
import com.encaja.app.domain.model.ArticuloCompraId

@Composable
fun CompraScreen(viewModel: CompraViewModel = hiltViewModel()) {
    val pantalla by viewModel.pantalla.collectAsState()

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
                onAgregar = { nombre, tienda -> viewModel.agregarArticulo(nombre, tienda) },
                onMarcarComprado = { articulo, comprado -> viewModel.marcarComprado(articulo, comprado) },
                onEliminar = { id -> viewModel.eliminarArticulo(id) }
            )
        }
    }
}

@Composable
private fun ContenidoCompra(
    estado: CompraUiState,
    onAgregar: (String, String) -> Unit,
    onMarcarComprado: (ArticuloCompra, Boolean) -> Unit,
    onEliminar: (ArticuloCompraId) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Compra", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        if (estado.grupos.isEmpty()) {
            Text(
                "Todavía no hay nada en la lista de la compra.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                estado.grupos.forEach { grupo ->
                    item {
                        Text(
                            grupo.tienda,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    items(grupo.articulos, key = { it.id.value }) { articulo ->
                        FilaArticulo(
                            articulo = articulo,
                            onMarcarComprado = { comprado -> onMarcarComprado(articulo, comprado) },
                            onEliminar = { onEliminar(articulo.id) }
                        )
                    }
                    item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }
                }
            }
        }

        FormularioNuevoArticulo(onAgregar = onAgregar)
    }
}

@Composable
private fun FilaArticulo(
    articulo: ArticuloCompra,
    onMarcarComprado: (Boolean) -> Unit,
    onEliminar: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = articulo.comprado, onCheckedChange = onMarcarComprado)
        Text(
            articulo.nombre,
            style = MaterialTheme.typography.bodyLarge,
            textDecoration = if (articulo.comprado) TextDecoration.LineThrough else null,
            color = if (articulo.comprado) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onEliminar) {
            Icon(Icons.Default.Delete, contentDescription = "Eliminar ${articulo.nombre}")
        }
    }
}

@Composable
private fun FormularioNuevoArticulo(onAgregar: (String, String) -> Unit) {
    var nombre by remember { mutableStateOf("") }
    var tienda by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Artículo") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = tienda,
                onValueChange = { tienda = it },
                label = { Text("Tienda") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = {
                if (nombre.isNotBlank() && tienda.isNotBlank()) {
                    onAgregar(nombre, tienda)
                    nombre = ""
                    tienda = ""
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "Añadir artículo")
            }
        }
    }
}
