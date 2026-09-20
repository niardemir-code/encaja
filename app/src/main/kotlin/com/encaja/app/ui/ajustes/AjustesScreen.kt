package com.encaja.app.ui.ajustes

// NOTA: depende de Jetpack Compose y Hilt, no compilado en este entorno.

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Checkbox
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
import com.encaja.app.domain.model.Caregiver
import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.Child
import com.encaja.app.domain.model.ChildId
import com.encaja.app.domain.model.FamilyUnit
import com.encaja.app.domain.model.FamilyUnitId

@Composable
fun AjustesScreen(onCerrarSesion: () -> Unit, viewModel: AjustesViewModel = hiltViewModel()) {
    val pantalla by viewModel.pantalla.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Ajustes", style = MaterialTheme.typography.headlineSmall)
        viewModel.emailUsuarioActual?.let { email ->
            Spacer(Modifier.height(4.dp))
            Text(
                "Sesión iniciada como: $email",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(24.dp))

        when (val estadoActual = pantalla) {
            is AjustesPantallaEstado.Cargando -> {
                CircularProgressIndicator()
            }

            is AjustesPantallaEstado.SinFamilia -> {
                Text(
                    "Vincúlate a una familia desde la pestaña Semana para gestionar sus datos.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            is AjustesPantallaEstado.ConDatos -> {
                SeccionNinos(
                    ninos = estadoActual.ninos,
                    onAgregar = { nombre -> viewModel.agregarNino(nombre) },
                    onEliminar = { childId -> viewModel.eliminarNino(childId) }
                )

                Spacer(Modifier.height(32.dp))

                SeccionCuidadores(
                    cuidadores = estadoActual.cuidadores,
                    onAgregar = { nombre, apellido1, apellido2 -> viewModel.agregarCuidador(nombre, apellido1, apellido2) },
                    onEliminar = { caregiverId -> viewModel.eliminarCuidador(caregiverId) },
                    onVincularme = { caregiverId -> viewModel.vincularmeAEsteCuidador(caregiverId) }
                )

                Spacer(Modifier.height(32.dp))

                SeccionUnidadesFamiliares(
                    unidades = estadoActual.unidades,
                    cuidadores = estadoActual.cuidadores,
                    onAgregar = { codigo, nombre, miembros -> viewModel.agregarUnidad(codigo, nombre, miembros) },
                    onEliminar = { unidadId -> viewModel.eliminarUnidad(unidadId) }
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

@Composable
private fun SeccionCuidadores(
    cuidadores: List<Caregiver>,
    onAgregar: (nombre: String, apellido1: String, apellido2: String) -> Unit,
    onEliminar: (CaregiverId) -> Unit,
    onVincularme: (CaregiverId) -> Unit
) {
    var nombreNuevo by remember { mutableStateOf("") }
    var apellido1Nuevo by remember { mutableStateOf("") }
    var apellido2Nuevo by remember { mutableStateOf("") }
    var vinculadoAId by remember { mutableStateOf<CaregiverId?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Cuidadores de la familia", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "Si eres tú quien tiene la sesión abierta, usa el icono de persona para vincularte a tu propio cuidador.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        if (cuidadores.isEmpty()) {
            Text(
                "Todavía no has añadido ningún cuidador.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
        } else {
            cuidadores.forEach { cuidador ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(cuidador.nombreCompleto, style = MaterialTheme.typography.bodyLarge)
                        Row {
                            IconButton(onClick = {
                                onVincularme(cuidador.id)
                                vinculadoAId = cuidador.id
                            }) {
                                Icon(Icons.Default.Person, contentDescription = "Vincularme a ${cuidador.nombreCompleto}")
                            }
                            IconButton(onClick = { onEliminar(cuidador.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar a ${cuidador.nombreCompleto}")
                            }
                        }
                    }
                    if (vinculadoAId == cuidador.id) {
                        Text(
                            "Vinculado. Cierra y vuelve a abrir la app para verlo reflejado en el avatar.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = nombreNuevo,
            onValueChange = { nombreNuevo = it },
            label = { Text("Nombre") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = apellido1Nuevo,
            onValueChange = { apellido1Nuevo = it },
            label = { Text("Primer apellido") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = apellido2Nuevo,
            onValueChange = { apellido2Nuevo = it },
            label = { Text("Segundo apellido") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                if (nombreNuevo.isNotBlank()) {
                    onAgregar(nombreNuevo, apellido1Nuevo, apellido2Nuevo)
                    nombreNuevo = ""
                    apellido1Nuevo = ""
                    apellido2Nuevo = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Añadir cuidador")
        }
    }
}

/**
 * Un grupo de cuidadores (p.ej. "los abuelos maternos") que se puede asignar a un día
 * de golpe, igual que una persona individual — se distingue en la fila de la semana
 * por su código corto (p.ej. "GF") sobre un fondo rayado en vez de las iniciales.
 */
@Composable
private fun SeccionUnidadesFamiliares(
    unidades: List<FamilyUnit>,
    cuidadores: List<Caregiver>,
    onAgregar: (codigo: String, nombre: String, miembros: List<CaregiverId>) -> Unit,
    onEliminar: (FamilyUnitId) -> Unit
) {
    var codigoNuevo by remember { mutableStateOf("") }
    var nombreNuevo by remember { mutableStateOf("") }
    var miembrosSeleccionados by remember { mutableStateOf(setOf<CaregiverId>()) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Unidades familiares", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "Grupos de cuidadores que van juntos (p.ej. los abuelos maternos), para poder asignarlos a un día de una vez.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        if (unidades.isEmpty()) {
            Text(
                "Todavía no has creado ninguna unidad familiar.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
        } else {
            unidades.forEach { unidad ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    // Top (no CenterVertically): así la papelera queda siempre en el mismo
                    // sitio, tanto si el nombre de la unidad cabe en una línea como si el
                    // texto de los miembros se parte en dos y la fila crece.
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f).padding(top = 4.dp)) {
                        Text("${unidad.codigo} · ${unidad.nombre}", style = MaterialTheme.typography.bodyLarge)
                        val nombresMiembros = unidad.miembros
                            .mapNotNull { id -> cuidadores.find { it.id == id }?.nombreCompleto }
                            .joinToString(", ")
                        if (nombresMiembros.isNotBlank()) {
                            Text(
                                nombresMiembros,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = { onEliminar(unidad.id) }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar unidad ${unidad.nombre}",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            Spacer(Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = codigoNuevo,
            onValueChange = { codigoNuevo = it },
            label = { Text("Código (para el avatar, p.ej. \"GF\")") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = nombreNuevo,
            onValueChange = { nombreNuevo = it },
            label = { Text("Nombre (p.ej. \"Gregorio y Josefa\")") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Text("Miembros del grupo", style = MaterialTheme.typography.labelMedium)
        cuidadores.forEach { cuidador ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        miembrosSeleccionados = if (cuidador.id in miembrosSeleccionados) {
                            miembrosSeleccionados - cuidador.id
                        } else {
                            miembrosSeleccionados + cuidador.id
                        }
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = cuidador.id in miembrosSeleccionados,
                    onCheckedChange = { marcado ->
                        miembrosSeleccionados = if (marcado) miembrosSeleccionados + cuidador.id else miembrosSeleccionados - cuidador.id
                    }
                )
                Text(cuidador.nombreCompleto)
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                if (codigoNuevo.isNotBlank() && nombreNuevo.isNotBlank() && miembrosSeleccionados.isNotEmpty()) {
                    onAgregar(codigoNuevo, nombreNuevo, miembrosSeleccionados.toList())
                    codigoNuevo = ""
                    nombreNuevo = ""
                    miembrosSeleccionados = emptySet()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Añadir unidad familiar")
        }
    }
}
