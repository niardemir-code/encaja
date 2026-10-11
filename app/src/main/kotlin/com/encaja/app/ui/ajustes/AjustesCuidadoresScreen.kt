package com.encaja.app.ui.ajustes

// NOTA: depende de Jetpack Compose y Hilt, no compilado en este entorno.

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.encaja.app.domain.model.Caregiver
import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.FamilyUnit
import com.encaja.app.domain.model.FamilyUnitId
import com.encaja.app.ui.familia.DialogoNuevoCuidador
import com.encaja.app.ui.theme.AccionAvisoHoja
import com.encaja.app.ui.theme.AvatarInicialesHoja
import com.encaja.app.ui.theme.AvisoHoja
import com.encaja.app.ui.theme.BotonPildoraHoja
import com.encaja.app.ui.theme.BotonRedondoSuaveHoja
import com.encaja.app.ui.theme.CabeceraPantalla
import com.encaja.app.ui.theme.CampoTextoConIconoHoja
import com.encaja.app.ui.theme.CasillaRedondaHoja
import com.encaja.app.ui.theme.ColorAvatarUnidad
import com.encaja.app.ui.theme.EstiloAccionAviso
import com.encaja.app.ui.theme.FondoPantalla
import com.encaja.app.ui.theme.MensajePantalla
import com.encaja.app.ui.theme.PaletaAvatarPersonas
import com.encaja.app.ui.theme.RotuloSeccionPantalla
import com.encaja.app.ui.theme.TarjetaFilaHoja
import com.encaja.app.ui.theme.acentoHoja
import com.encaja.app.ui.theme.inicialesDe
import com.encaja.app.ui.theme.tintaHoja
import com.encaja.app.ui.theme.tintaSuaveHoja

/**
 * Pantalla de Ajustes > Cuidadores: personas de la familia y, dentro del mismo
 * bloque, sus unidades familiares (grupos como "los abuelos maternos").
 */
@Composable
fun AjustesCuidadoresScreen(viewModel: AjustesViewModel = hiltViewModel()) {
    val pantalla by viewModel.pantalla.collectAsState()

    FondoPantalla {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            CabeceraPantalla(
                "Cuidadores",
                "Las personas de la familia y los grupos que van juntos."
            )

            when (val estadoActual = pantalla) {
                is AjustesPantallaEstado.Cargando -> CircularProgressIndicator()
                is AjustesPantallaEstado.SinFamilia -> {
                    MensajePantalla("Vincúlate a una familia desde la pestaña Semana para gestionar sus datos.")
                }
                is AjustesPantallaEstado.ConDatos -> {
                    var errorBaja by remember { mutableStateOf<String?>(null) }
                    SeccionCuidadores(
                        cuidadores = estadoActual.cuidadores,
                        miCaregiverId = estadoActual.miCaregiverId,
                        soyAdmin = estadoActual.soyAdmin,
                        vinculados = estadoActual.vinculados,
                        onDesvincular = { caregiverId ->
                            errorBaja = null
                            viewModel.desvincularCuenta(caregiverId) { errorBaja = it }
                        },
                        onAgregar = { nombre, apellido1, apellido2 -> viewModel.agregarCuidador(nombre, apellido1, apellido2) },
                        onEliminar = { caregiverId -> viewModel.eliminarCuidador(caregiverId) },
                        onVincularme = { caregiverId -> viewModel.vincularmeAEsteCuidador(caregiverId) }
                    )
                    errorBaja?.let { MensajePantalla(it, error = true) }

                    Spacer(Modifier.height(28.dp))

                    SeccionUnidadesFamiliares(
                        unidades = estadoActual.unidades,
                        cuidadores = estadoActual.cuidadores,
                        onAgregar = { codigo, nombre, miembros -> viewModel.agregarUnidad(codigo, nombre, miembros) },
                        onEliminar = { unidadId -> viewModel.eliminarUnidad(unidadId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SeccionCuidadores(
    cuidadores: List<Caregiver>,
    miCaregiverId: CaregiverId?,
    soyAdmin: Boolean,
    vinculados: Set<CaregiverId>,
    onDesvincular: (CaregiverId) -> Unit,
    onAgregar: (nombre: String, apellido1: String, apellido2: String) -> Unit,
    onEliminar: (CaregiverId) -> Unit,
    onVincularme: (CaregiverId) -> Unit
) {
    var vinculadoAId by remember { mutableStateOf<CaregiverId?>(null) }
    var cuidadorADesvincular by remember { mutableStateOf<Caregiver?>(null) }
    var cuidadorAEliminar by remember { mutableStateOf<Caregiver?>(null) }
    var nuevoAbierto by remember { mutableStateOf(false) }

    if (nuevoAbierto) {
        DialogoNuevoCuidador(
            onCrear = { nombre, apellido1, apellido2 ->
                onAgregar(nombre, apellido1, apellido2)
                nuevoAbierto = false
            },
            onCerrar = { nuevoAbierto = false }
        )
    }

    cuidadorAEliminar?.let { cuidador ->
        AvisoHoja(
            icono = Icons.Default.Delete,
            titulo = "¿Eliminar a ${cuidador.nombreCompleto}?",
            texto = "Se eliminará de la lista, su cuenta perderá el acceso, se borrarán las unidades " +
                "familiares en las que esté y sus actividades asignadas quedarán sin responsable. " +
                "No se puede deshacer.",
            acciones = listOf(
                AccionAvisoHoja("Eliminar", EstiloAccionAviso.PELIGRO) {
                    onEliminar(cuidador.id)
                    cuidadorAEliminar = null
                },
                AccionAvisoHoja("Cancelar", EstiloAccionAviso.BORDE) { cuidadorAEliminar = null }
            ),
            onCerrar = { cuidadorAEliminar = null }
        )
    }

    cuidadorADesvincular?.let { cuidador ->
        AvisoHoja(
            icono = Icons.Default.LinkOff,
            titulo = "¿Desvincular a ${cuidador.nombreCompleto}?",
            texto = "Su cuenta dejará de tener acceso a los datos de la familia y dejará de recibir " +
                "avisos. ${cuidador.nombreCompleto} seguirá en la lista y podrás volver a invitarle.",
            peligro = false,
            acciones = listOf(
                AccionAvisoHoja("Desvincular", EstiloAccionAviso.PRIMARIO) {
                    onDesvincular(cuidador.id)
                    cuidadorADesvincular = null
                },
                AccionAvisoHoja("Cancelar", EstiloAccionAviso.BORDE) { cuidadorADesvincular = null }
            ),
            onCerrar = { cuidadorADesvincular = null }
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        RotuloSeccionPantalla("Personas")

        if (cuidadores.isEmpty()) {
            MensajePantalla("Todavía no has añadido ningún cuidador.")
        } else {
            Text(
                "Si eres tú quien tiene la sesión abierta, usa el icono de persona para vincularte a tu propio cuidador.",
                style = MaterialTheme.typography.bodySmall,
                color = tintaSuaveHoja,
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }

        cuidadores.forEachIndexed { indice, cuidador ->
            TarjetaFilaHoja {
                AvatarInicialesHoja(
                    iniciales = inicialesDe(cuidador.nombreCompleto),
                    colores = PaletaAvatarPersonas[indice % PaletaAvatarPersonas.size]
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        cuidador.nombreCompleto,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = tintaHoja
                    )
                    val estado = when {
                        vinculadoAId == cuidador.id -> "Vinculado"
                        cuidador.id == miCaregiverId -> "Tú"
                        cuidador.id in vinculados -> "Con cuenta vinculada"
                        else -> null
                    }
                    estado?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = tintaSuaveHoja)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (soyAdmin && cuidador.id in vinculados && cuidador.id != miCaregiverId) {
                        BotonRedondoSuaveHoja(
                            Icons.Default.LinkOff,
                            "Desvincular la cuenta de ${cuidador.nombreCompleto}",
                            { cuidadorADesvincular = cuidador }
                        )
                    }
                    BotonRedondoSuaveHoja(
                        Icons.Default.Person,
                        "Vincularme a ${cuidador.nombreCompleto}",
                        {
                            onVincularme(cuidador.id)
                            vinculadoAId = cuidador.id
                        }
                    )
                    BotonRedondoSuaveHoja(
                        Icons.Default.Delete,
                        "Eliminar a ${cuidador.nombreCompleto}",
                        { cuidadorAEliminar = cuidador },
                        tinta = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(6.dp))
        BotonPildoraHoja("Nuevo cuidador", onClick = { nuevoAbierto = true }, icono = Icons.Default.PersonAdd)
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
    var nuevaAbierta by remember { mutableStateOf(false) }
    var unidadAEliminar by remember { mutableStateOf<FamilyUnit?>(null) }

    if (nuevaAbierta) {
        DialogoNuevaUnidad(
            cuidadores = cuidadores,
            onCrear = { codigo, nombre, miembros ->
                onAgregar(codigo, nombre, miembros)
                nuevaAbierta = false
            },
            onCerrar = { nuevaAbierta = false }
        )
    }

    unidadAEliminar?.let { unidad ->
        AvisoHoja(
            icono = Icons.Default.Delete,
            titulo = "¿Eliminar la unidad ${unidad.nombre}?",
            texto = "Las personas del grupo siguen en la lista; solo desaparece el grupo. No se puede deshacer.",
            acciones = listOf(
                AccionAvisoHoja("Eliminar", EstiloAccionAviso.PELIGRO) {
                    onEliminar(unidad.id)
                    unidadAEliminar = null
                },
                AccionAvisoHoja("Cancelar", EstiloAccionAviso.BORDE) { unidadAEliminar = null }
            ),
            onCerrar = { unidadAEliminar = null }
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        RotuloSeccionPantalla("Unidades familiares")
        Text(
            "Grupos de cuidadores que van juntos (p.ej. los abuelos maternos), para poder asignarlos a un día de una vez.",
            style = MaterialTheme.typography.bodySmall,
            color = tintaSuaveHoja,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        if (unidades.isEmpty()) {
            MensajePantalla("Todavía no has creado ninguna unidad familiar.")
        }

        unidades.forEach { unidad ->
            TarjetaFilaHoja {
                AvatarInicialesHoja(
                    iniciales = unidad.codigo.take(2).uppercase(),
                    colores = ColorAvatarUnidad
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        unidad.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = tintaHoja
                    )
                    val nombresMiembros = unidad.miembros
                        .mapNotNull { id -> cuidadores.find { it.id == id }?.nombreCompleto }
                        .joinToString(", ")
                    if (nombresMiembros.isNotBlank()) {
                        Text(nombresMiembros, style = MaterialTheme.typography.bodySmall, color = tintaSuaveHoja)
                    }
                }
                Spacer(Modifier.width(8.dp))
                BotonRedondoSuaveHoja(
                    Icons.Default.Delete,
                    "Eliminar unidad ${unidad.nombre}",
                    { unidadAEliminar = unidad },
                    tinta = MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(6.dp))
        BotonPildoraHoja(
            "Nueva unidad familiar",
            onClick = { nuevaAbierta = true },
            icono = Icons.Default.Add,
            habilitado = cuidadores.isNotEmpty()
        )
    }
}

/** Pide código, nombre y miembros de la unidad nueva; "Crear" se activa con todo relleno. */
@Composable
private fun DialogoNuevaUnidad(
    cuidadores: List<Caregiver>,
    onCrear: (codigo: String, nombre: String, miembros: List<CaregiverId>) -> Unit,
    onCerrar: () -> Unit
) {
    var codigo by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var miembros by remember { mutableStateOf(setOf<CaregiverId>()) }
    AvisoHoja(
        icono = Icons.Default.Group,
        titulo = "Crear una unidad familiar",
        texto = null,
        peligro = false,
        conX = true,
        accionesEnFila = true,
        acciones = listOf(
            AccionAvisoHoja("Cancelar", EstiloAccionAviso.BORDE) { onCerrar() },
            AccionAvisoHoja(
                "Crear",
                EstiloAccionAviso.PRIMARIO,
                habilitada = codigo.isNotBlank() && nombre.isNotBlank() && miembros.isNotEmpty()
            ) {
                onCrear(codigo, nombre, miembros.toList())
            }
        ),
        onCerrar = onCerrar,
        contenido = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CampoTextoConIconoHoja(codigo, { codigo = it }, "Código del avatar (p.ej. GF)", Icons.Default.Badge)
                CampoTextoConIconoHoja(nombre, { nombre = it }, "Nombre (p.ej. Gregorio y Josefa)", Icons.Default.Group)
                Text(
                    "Miembros del grupo",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = tintaHoja,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Column(
                    modifier = Modifier
                        .heightIn(max = 190.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    cuidadores.forEach { cuidador ->
                        val marcado = cuidador.id in miembros
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { miembros = if (marcado) miembros - cuidador.id else miembros + cuidador.id }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CasillaRedondaHoja(marcado)
                            Spacer(Modifier.width(12.dp))
                            Text(cuidador.nombreCompleto, style = MaterialTheme.typography.bodyLarge, color = tintaHoja)
                        }
                    }
                }
            }
        }
    )
}
