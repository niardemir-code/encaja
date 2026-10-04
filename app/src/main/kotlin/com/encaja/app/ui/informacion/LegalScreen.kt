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

/** Ajustes > Privacidad y legal: política de privacidad, aviso legal y condiciones de uso. */
@Composable
fun LegalScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Privacidad y legal", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            "Cómo tratamos tus datos y las condiciones de uso. Toca un apartado para leerlo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))

        DOCUMENTOS_LEGALES.forEach { documento ->
            TarjetaPlegable(titulo = documento.titulo, subtitulo = documento.resumen) {
                documento.apartados.forEach { ContenidoApartadoLegal(it) }
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "¿Dudas sobre tus datos o quieres ejercer tus derechos? Escríbenos a ${DatosLegales.EMAIL_CONTACTO}.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
