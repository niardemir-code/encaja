package com.encaja.app.ui.guia

// NOTA: depende de Jetpack Compose (Material 3) y material-icons-extended, no
// compilado en este entorno.
// Catálogo de iconos que se pueden elegir al crear/editar una actividad (deportivas,
// educativas, recreativas, extraescolares...). Solo se guarda el [IconoActividad.id]
// en CoverageNeed.icono; así, si en el futuro se cambia el icono asociado a un id, o se
// quita de este catálogo, las actividades ya guardadas no se rompen: simplemente caen
// en el icono por defecto (ver [iconoParaActividad]).

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SportsHandball
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.SportsVolleyball
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.filled.Translate
import androidx.compose.ui.graphics.vector.ImageVector

enum class IconoActividad(val id: String, val etiqueta: String, val icono: ImageVector) {
    COLEGIO("colegio", "Colegio", Icons.Default.School),
    DEBERES("deberes", "Deberes", Icons.AutoMirrored.Filled.MenuBook),
    IDIOMAS("idiomas", "Idiomas", Icons.Default.Translate),
    INFORMATICA("informatica", "Informática", Icons.Default.Computer),
    CIENCIAS("ciencias", "Ciencias", Icons.Default.Science),
    MATEMATICAS("matematicas", "Matemáticas", Icons.Default.Calculate),
    BIBLIOTECA("biblioteca", "Lectura/biblioteca", Icons.Default.LocalLibrary),
    FUTBOL("futbol", "Fútbol", Icons.Default.SportsSoccer),
    BALONCESTO("baloncesto", "Baloncesto", Icons.Default.SportsBasketball),
    TENIS("tenis", "Tenis/pádel", Icons.Default.SportsTennis),
    VOLEIBOL("voleibol", "Voleibol", Icons.Default.SportsVolleyball),
    BALONMANO("balonmano", "Balonmano", Icons.Default.SportsHandball),
    NATACION("natacion", "Natación/piscina", Icons.Default.Pool),
    ATLETISMO("atletismo", "Atletismo/correr", Icons.AutoMirrored.Filled.DirectionsRun),
    CICLISMO("ciclismo", "Ciclismo", Icons.AutoMirrored.Filled.DirectionsBike),
    GIMNASIO("gimnasio", "Gimnasia/fitness", Icons.Default.FitnessCenter),
    MUSICA("musica", "Música", Icons.Default.MusicNote),
    ARTE("arte", "Arte/manualidades", Icons.Default.Brush),
    TEATRO("teatro", "Teatro", Icons.Default.TheaterComedy),
    VIDEOJUEGOS("videojuegos", "Videojuegos/ocio", Icons.Default.SportsEsports),
    NATURALEZA("naturaleza", "Excursión/naturaleza", Icons.Default.Park),
    FIESTA("fiesta", "Cumpleaños/fiesta", Icons.Default.Celebration),
    MEDICO("medico", "Médico", Icons.Default.MedicalServices),
    COMIDA("comida", "Comida", Icons.Default.Restaurant),
    MASCOTA("mascota", "Mascota", Icons.Default.Pets),
    OTRO("otro", "Otro", Icons.Default.Star)
}

/** El icono elegido para [id], o el genérico de "Colegio" si no se elegido ninguno o
 * ya no existe en el catálogo (p.ej. una actividad antigua de antes de esta función). */
fun iconoParaActividad(id: String?): ImageVector =
    IconoActividad.entries.find { it.id == id }?.icono ?: Icons.Default.School
