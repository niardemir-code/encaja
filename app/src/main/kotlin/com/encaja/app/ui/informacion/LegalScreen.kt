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
import com.encaja.app.ui.theme.CabeceraPantalla
import com.encaja.app.ui.theme.FondoPantalla
import com.encaja.app.ui.theme.tintaSuaveHoja

/** Ajustes > Privacidad y legal: política de privacidad, aviso legal y condiciones de uso. */
@Composable
fun LegalScreen() {
    FondoPantalla {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            CabeceraPantalla(
                "Privacidad y legal",
                "Cómo tratamos tus datos y las condiciones de uso. Toca un apartado para leerlo."
            )

            DOCUMENTOS_LEGALES.forEach { documento ->
                TarjetaPlegable(titulo = documento.titulo, subtitulo = documento.resumen) {
                    documento.apartados.forEach { ContenidoApartadoLegal(it) }
                }
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "¿Dudas sobre tus datos o quieres ejercer tus derechos? Escríbenos a ${DatosLegales.EMAIL_CONTACTO}.",
                style = MaterialTheme.typography.bodySmall,
                color = tintaSuaveHoja
            )
        }
    }
}
