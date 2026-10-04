package com.encaja.app.ui.informacion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Tarjeta que se despliega al tocarla, para listas largas de texto (ayuda, textos legales). */
@Composable
fun TarjetaPlegable(
    titulo: String,
    subtitulo: String? = null,
    contenido: @Composable ColumnScope.() -> Unit
) {
    var abierta by rememberSaveable { mutableStateOf(false) }
    OutlinedCard(
        onClick = { abierta = !abierta },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(titulo, style = MaterialTheme.typography.titleMedium)
                    if (subtitulo != null) {
                        Text(
                            subtitulo,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    if (abierta) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (abierta) "Plegar" else "Desplegar"
                )
            }
            if (abierta) {
                Spacer(Modifier.height(12.dp))
                contenido()
            }
        }
    }
}

/** Lista de puntos: los que ya empiezan por un número (pasos) se muestran tal cual; el resto, con viñeta. */
@Composable
fun ListaDePuntos(puntos: List<String>) {
    puntos.forEach { punto ->
        val esPaso = punto.firstOrNull()?.isDigit() == true
        Text(
            if (esPaso) punto else "•  $punto",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 6.dp)
        )
    }
}

@Composable
fun ContenidoTemaAyuda(tema: TemaAyuda) {
    tema.parrafos.forEach { parrafo ->
        Text(parrafo, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))
    }
    ListaDePuntos(tema.puntos)
    tema.nota?.let {
        Spacer(Modifier.height(4.dp))
        Text(
            it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ContenidoApartadoLegal(apartado: ApartadoLegal) {
    Text(
        apartado.titulo,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
    )
    apartado.parrafos.forEach { parrafo ->
        Text(parrafo, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))
    }
    ListaDePuntos(apartado.puntos)
    Spacer(Modifier.height(8.dp))
}
