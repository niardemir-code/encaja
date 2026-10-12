package com.encaja.app.ui.ajustes

// NOTA: depende de Jetpack Compose y Hilt, no compilado en este entorno.

import androidx.compose.ui.res.stringResource
import com.encaja.app.R
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.encaja.app.domain.model.Child
import com.encaja.app.domain.model.ChildId
import com.encaja.app.ui.theme.AccionAvisoHoja
import com.encaja.app.ui.theme.AvisoHoja
import com.encaja.app.ui.theme.BotonPildoraHoja
import com.encaja.app.ui.theme.BotonRedondoSuaveHoja
import com.encaja.app.ui.theme.CabeceraPantalla
import com.encaja.app.ui.theme.CampoTextoConIconoHoja
import com.encaja.app.ui.theme.EstiloAccionAviso
import com.encaja.app.ui.theme.FondoPantalla
import com.encaja.app.ui.theme.MensajePantalla
import com.encaja.app.ui.theme.TarjetaFilaHoja
import com.encaja.app.ui.theme.tintaHoja

/** Pantalla de Ajustes > Niños: solo el alta y baja de niños de la familia (solo el nombre). */
@Composable
fun AjustesNinosScreen(viewModel: AjustesViewModel = hiltViewModel()) {
    val pantalla by viewModel.pantalla.collectAsState()

    FondoPantalla {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            CabeceraPantalla(stringResource(R.string.ninos_titulo), stringResource(R.string.ninos_subtitulo))

            when (val estadoActual = pantalla) {
                is AjustesPantallaEstado.Cargando -> CircularProgressIndicator()
                is AjustesPantallaEstado.SinFamilia -> {
                    MensajePantalla(stringResource(R.string.comun_sin_familia_gestionar))
                }
                is AjustesPantallaEstado.ConDatos -> {
                    SeccionNinos(
                        ninos = estadoActual.ninos,
                        onAgregar = { nombre -> viewModel.agregarNino(nombre) },
                        onEliminar = { childId -> viewModel.eliminarNino(childId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SeccionNinos(
    ninos: List<Child>,
    onAgregar: (String) -> Unit,
    onEliminar: (ChildId) -> Unit
) {
    var nuevoAbierto by remember { mutableStateOf(false) }
    var ninoAEliminar by remember { mutableStateOf<Child?>(null) }

    if (nuevoAbierto) {
        DialogoNuevoNino(
            onCrear = { nombre ->
                onAgregar(nombre)
                nuevoAbierto = false
            },
            onCerrar = { nuevoAbierto = false }
        )
    }

    ninoAEliminar?.let { nino ->
        AvisoHoja(
            icono = Icons.Default.Delete,
            titulo = stringResource(R.string.ninos_eliminar_titulo, nino.nombre),
            texto = stringResource(R.string.ninos_eliminar_texto),
            acciones = listOf(
                AccionAvisoHoja(stringResource(R.string.comun_eliminar), EstiloAccionAviso.PELIGRO) {
                    onEliminar(nino.id)
                    ninoAEliminar = null
                },
                AccionAvisoHoja(stringResource(R.string.comun_cancelar), EstiloAccionAviso.BORDE) { ninoAEliminar = null }
            ),
            onCerrar = { ninoAEliminar = null }
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (ninos.isEmpty()) {
            MensajePantalla(stringResource(R.string.ninos_vacio))
        }
        ninos.forEach { nino ->
            TarjetaFilaHoja {
                Text(
                    nino.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = tintaHoja,
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                )
                BotonRedondoSuaveHoja(
                    Icons.Default.Delete,
                    stringResource(R.string.ninos_eliminar_a, nino.nombre),
                    { ninoAEliminar = nino },
                    tinta = MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(6.dp))
        BotonPildoraHoja(stringResource(R.string.ninos_nuevo), onClick = { nuevoAbierto = true }, icono = Icons.Default.Add)
    }
}

/** Pide solo el nombre del niño nuevo; "Crear" se activa cuando hay nombre. */
@Composable
private fun DialogoNuevoNino(onCrear: (String) -> Unit, onCerrar: () -> Unit) {
    var nombre by remember { mutableStateOf("") }
    AvisoHoja(
        icono = Icons.Default.ChildCare,
        titulo = stringResource(R.string.ninos_anadir_titulo),
        texto = null,
        peligro = false,
        conX = true,
        accionesEnFila = true,
        acciones = listOf(
            AccionAvisoHoja(stringResource(R.string.comun_cancelar), EstiloAccionAviso.BORDE) { onCerrar() },
            AccionAvisoHoja(stringResource(R.string.comun_anadir), EstiloAccionAviso.PRIMARIO, habilitada = nombre.isNotBlank()) {
                onCrear(nombre.trim())
            }
        ),
        onCerrar = onCerrar,
        contenido = {
            CampoTextoConIconoHoja(nombre, { nombre = it }, stringResource(R.string.ninos_nombre), Icons.Default.ChildCare)
        }
    )
}
