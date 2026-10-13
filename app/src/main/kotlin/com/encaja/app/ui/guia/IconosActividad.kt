package com.encaja.app.ui.guia

// NOTA: depende de Jetpack Compose (Material 3) y material-icons-extended, no
// compilado en este entorno.
// Catálogo de iconos que se pueden elegir al crear/editar una actividad (deportivas,
// educativas, recreativas, extraescolares...). Solo se guarda el [IconoActividad.id]
// en CoverageNeed.icono; así, si en el futuro se cambia el icono asociado a un id, o se
// quita de este catálogo, las actividades ya guardadas no se rompen: simplemente caen
// en el icono por defecto (ver [iconoParaActividad]).

import com.encaja.app.R
import androidx.annotation.StringRes
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

enum class IconoActividad(val id: String, @StringRes val etiqueta: Int, val icono: ImageVector) {
    COLEGIO("colegio", R.string.icono_colegio, Icons.Default.School),
    DEBERES("deberes", R.string.icono_deberes, Icons.AutoMirrored.Filled.MenuBook),
    IDIOMAS("idiomas", R.string.icono_idiomas, Icons.Default.Translate),
    INFORMATICA("informatica", R.string.icono_informatica, Icons.Default.Computer),
    CIENCIAS("ciencias", R.string.icono_ciencias, Icons.Default.Science),
    MATEMATICAS("matematicas", R.string.icono_matematicas, Icons.Default.Calculate),
    BIBLIOTECA("biblioteca", R.string.icono_biblioteca, Icons.Default.LocalLibrary),
    FUTBOL("futbol", R.string.icono_futbol, Icons.Default.SportsSoccer),
    BALONCESTO("baloncesto", R.string.icono_baloncesto, Icons.Default.SportsBasketball),
    TENIS("tenis", R.string.icono_tenis, Icons.Default.SportsTennis),
    VOLEIBOL("voleibol", R.string.icono_voleibol, Icons.Default.SportsVolleyball),
    BALONMANO("balonmano", R.string.icono_balonmano, Icons.Default.SportsHandball),
    NATACION("natacion", R.string.icono_natacion, Icons.Default.Pool),
    ATLETISMO("atletismo", R.string.icono_atletismo, Icons.AutoMirrored.Filled.DirectionsRun),
    CICLISMO("ciclismo", R.string.icono_ciclismo, Icons.AutoMirrored.Filled.DirectionsBike),
    GIMNASIO("gimnasio", R.string.icono_gimnasio, Icons.Default.FitnessCenter),
    MUSICA("musica", R.string.icono_musica, Icons.Default.MusicNote),
    ARTE("arte", R.string.icono_arte, Icons.Default.Brush),
    TEATRO("teatro", R.string.icono_teatro, Icons.Default.TheaterComedy),
    VIDEOJUEGOS("videojuegos", R.string.icono_videojuegos, Icons.Default.SportsEsports),
    NATURALEZA("naturaleza", R.string.icono_naturaleza, Icons.Default.Park),
    FIESTA("fiesta", R.string.icono_fiesta, Icons.Default.Celebration),
    MEDICO("medico", R.string.icono_medico, Icons.Default.MedicalServices),
    COMIDA("comida", R.string.icono_comida, Icons.Default.Restaurant),
    MASCOTA("mascota", R.string.icono_mascota, Icons.Default.Pets),
    OTRO("otro", R.string.icono_otro, Icons.Default.Star)
}

/** El icono elegido para [id], o el genérico de "Colegio" si no se elegido ninguno o
 * ya no existe en el catálogo (p.ej. una actividad antigua de antes de esta función). */
fun iconoParaActividad(id: String?): ImageVector =
    IconoActividad.entries.find { it.id == id }?.icono ?: Icons.Default.School
