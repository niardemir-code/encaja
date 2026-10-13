package com.encaja.app.ui.common

// NOTA: depende de Jetpack Compose, no compilado en este entorno.
// Hoja de formulario que sube desde abajo (sustituye a ModalBottomSheet en las hojas con
// campos de texto). La hoja de Material tiene anclas de arrastre que se recalculan con cada
// cambio del teclado (cada tecla cambia la franja de sugerencias) y eso hacía que bajara y
// subiera en cada pulsación. Esta es una ventana normal: ocupa desde la barra de estado hasta
// abajo, el teclado solo encoge su interior y no hay nada que recolocar al teclear.
// Minimizar el teclado no la cierra (ver GuardaTeclado).

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun HojaFormulario(
    onCerrar: () -> Unit,
    contenido: @Composable () -> Unit
) {
    val guardaTeclado = rememberGuardaTeclado()
    Dialog(
        onDismissRequest = { if (guardaTeclado.bloqueaCierre()) guardaTeclado.ocultar() else onCerrar() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        ObservarTeclado(guardaTeclado)
        // Toque en la zona oscura (la de la barra de estado) = cerrar, como en una hoja normal.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { if (guardaTeclado.bloqueaCierre()) guardaTeclado.ocultar() else onCerrar() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .imePadding(),
                color = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                contenido()
            }
        }
    }
}
