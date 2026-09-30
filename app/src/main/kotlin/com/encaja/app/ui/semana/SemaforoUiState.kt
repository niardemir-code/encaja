package com.encaja.app.ui.semana

import com.encaja.app.domain.model.Anuncio
import com.encaja.app.domain.model.CoverageNeed
import com.encaja.app.domain.model.Hueco
import com.encaja.app.domain.usecase.AvisoConflicto
import java.time.LocalDate

/**
 * Todo lo que la pantalla del Semáforo necesita para pintarse, ya resuelto.
 * El Composable no calcula nada: solo recorre estos datos y los dibuja.
 * Eso es deliberado — mantiene la pantalla "tonta" y toda la lógica
 * testeable fuera de Compose.
 */
enum class EstadoDia { VERDE, AMBAR, ROJO, SIN_DATOS }

data class DiaSemaforo(
    val fecha: LocalDate,
    val estado: EstadoDia,
    val huecos: List<Hueco>,
    /** Todas las actividades (CoverageNeed) de ese día, huecos y avisos incluidos —
     * para poder mostrar su información al tocar el círculo del semáforo (sobre todo
     * en los días verdes, donde no hay ni huecos ni avisos que abrir). */
    val needs: List<CoverageNeed> = emptyList()
)

data class SemaforoUiState(
    val dias: List<DiaSemaforo>,
    val huecosDeLaSemana: List<Hueco>,
    /** El tablón de anuncios se añade aparte en el ViewModel (el mapper no lo conoce). */
    val anuncios: List<Anuncio> = emptyList(),
    val avisos: List<AvisoConflicto> = emptyList(),
    /** Igual que en Familia/Menú: si es false, la cabecera ofrece volver a la semana de hoy. */
    val esSemanaActual: Boolean = true
)
