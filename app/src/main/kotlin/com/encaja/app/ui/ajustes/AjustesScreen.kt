package com.encaja.app.ui.ajustes

// NOTA: depende de Jetpack Compose y Hilt, no compilado en este entorno.

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.encaja.app.domain.model.Child
import com.encaja.app.domain.model.ChildId

@Composable
fun AjustesScreen(onCerrarSesion: () -> Unit, viewModel: AjustesViewModel = hiltViewModel()) {
    val pantalla by viewModel.pantalla.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Ajustes", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))

        when (val estadoActual = pantalla) {
            is AjustesPantallaEstado.Cargando -> {
                CircularProgressIndicator()
            }

            is AjustesPantallaEstado.SinFamilia -> {
                Text(
                    "Vincúlate a una familia desde la pestaña Semana para gestionar sus niños.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            is AjustesPantallaEstado.ConDatos -> {
                SeccionNinos(
                    ninos = estadoActual.ninos,
                    onAgregar = { nombre -> viewModel.agregarNino(nombre) },
                    onEliminar = { childId -> viewModel.eliminarNino(childId) }
                )
            }
        }

        Spacer(Modifier.height(32.dp))
        OutlinedButton(onClick = onCerrarSesion, modifier = Modifier.fillMaxWidth()) {
            Text("Cerrar sesión")
        }
    }
}

@Composable
private fun SeccionNinos(
    ninos: List<Child>,
    onAgregar: (String) -> Unit,
    onEliminar: (ChildId) -> Unit
) {
    var nombreNuevo by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Niños de la familia", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))

        if (ninos.isEmpty()) {
            Text(
                "Todavía no has añadido ningún niño.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
        } else {
            ninos.forEach { nino ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(nino.nombre, style = MaterialTheme.typography.bodyLarge)
                    IconButton(onClick = { onEliminar(nino.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar a ${nino.nombre}")
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = nombreNuevo,
                onValueChange = { nombreNuevo = it },
                label = { Text("Nombre del niño") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = {
                if (nombreNuevo.isNotBlank()) {
                    onAgregar(nombreNuevo)
                    nombreNuevo = ""
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "Añadir niño")
            }
        }
    }
}
