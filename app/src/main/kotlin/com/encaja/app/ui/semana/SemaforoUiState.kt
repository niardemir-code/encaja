package com.encaja.app.ui.semana

import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.Hueco
import java.time.LocalDate

/**
 * Todo lo que la pantalla del Semáforo necesita para pintarse, ya resuelto.
 * El Composable no calcula nada: solo recorre estos datos y los dibuja.
 * Eso es deliberado — mantiene la pantalla "tonta" y toda la lógica
 * testeable fuera de Compose.
 */
enum class EstadoDia { VERDE, ROJO, SIN_DATOS }

data class DiaSemaforo(
    val fecha: LocalDate,
    val estado: EstadoDia,
    val huecos: List<Hueco>
)

data class TramoReparto(
    val caregiverId: CaregiverId,
    val nombre: String,
    val tramos: Int
)

data class SemaforoUiState(
    val dias: List<DiaSemaforo>,
    val huecosDeLaSemana: List<Hueco>,
    val reparto: List<TramoReparto>
) {
    val totalTramos: Int get() = reparto.sumOf { it.tramos }
}
