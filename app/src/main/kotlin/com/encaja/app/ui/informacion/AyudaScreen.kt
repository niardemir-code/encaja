package com.encaja.app.ui.informacion

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.encaja.app.ui.theme.CabeceraPantalla
import com.encaja.app.ui.theme.FondoPantalla
import com.encaja.app.ui.theme.RotuloSeccionPantalla

/** Ajustes > Ayuda: instrucciones de uso de la app. El contenido está en [SECCIONES_AYUDA]. */
@Composable
fun AyudaScreen() {
    FondoPantalla {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            CabeceraPantalla("Ayuda", "Cómo funciona Encaja, pantalla por pantalla. Toca un tema para abrirlo.")

            SECCIONES_AYUDA.forEach { seccion ->
                RotuloSeccionPantalla(seccion.titulo)
                seccion.temas.forEach { tema ->
                    TarjetaPlegable(titulo = tema.titulo) { ContenidoTemaAyuda(tema) }
                    Spacer(Modifier.height(10.dp))
                }
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}
