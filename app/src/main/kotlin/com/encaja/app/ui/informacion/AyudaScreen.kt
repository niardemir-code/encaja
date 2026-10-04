package com.encaja.app.ui.informacion

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Ajustes > Ayuda: instrucciones de uso de la app. El contenido está en [SECCIONES_AYUDA]. */
@Composable
fun AyudaScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Ayuda", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            "Cómo funciona Encaja, pantalla por pantalla. Toca un tema para abrirlo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))

        SECCIONES_AYUDA.forEach { seccion ->
            Text(
                seccion.titulo,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            seccion.temas.forEach { tema ->
                TarjetaPlegable(titulo = tema.titulo) { ContenidoTemaAyuda(tema) }
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
