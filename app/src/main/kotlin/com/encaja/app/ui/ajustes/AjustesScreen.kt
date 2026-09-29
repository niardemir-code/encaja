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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Menú raíz de Ajustes: solo enlaces a cada bloque (Niños, Cuidadores —que incluye
 * las Unidades familiares—, Actividades), para no amontonar aquí todos los
 * formularios. Cada bloque es su propia pantalla, con flecha de "atrás" hacia aquí.
 */
@Composable
fun AjustesScreen(
    onCerrarSesion: () -> Unit,
    onAbrirNinos: () -> Unit,
    onAbrirCuidadores: () -> Unit,
    onAbrirTodasActividades: () -> Unit,
    viewModel: AjustesViewModel = hiltViewModel()
) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
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

        FilaAjuste(
            icono = Icons.Default.ChildCare,
            titulo = "Niños",
            subtitulo = "Añadir o quitar niños de la familia",
            onClick = onAbrirNinos
        )
        Spacer(Modifier.height(12.dp))
        FilaAjuste(
            icono = Icons.Default.Group,
            titulo = "Cuidadores",
            subtitulo = "Personas y unidades familiares",
            onClick = onAbrirCuidadores
        )
        Spacer(Modifier.height(12.dp))
        FilaAjuste(
            icono = Icons.Default.CalendarViewWeek,
            titulo = "Actividades",
            subtitulo = "Ver y editar todas las actividades",
            onClick = onAbrirTodasActividades
        )

        Spacer(Modifier.height(32.dp))
        OutlinedButton(onClick = onCerrarSesion, modifier = Modifier.fillMaxWidth()) {
            Text("Cerrar sesión")
        }
    }
}

@Composable
private fun FilaAjuste(
    icono: ImageVector,
    titulo: String,
    subtitulo: String,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text(titulo, style = MaterialTheme.typography.titleMedium)
                    Text(
                        subtitulo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}
