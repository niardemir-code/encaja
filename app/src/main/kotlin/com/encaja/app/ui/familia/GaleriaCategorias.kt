package com.encaja.app.ui.familia

import com.encaja.app.domain.model.AvailabilityBlock
import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.CategoriaDisponibilidad
import com.encaja.app.domain.model.CategoriaId
import com.encaja.app.domain.model.MotivoNoDisponibilidad
import java.time.LocalDate
import java.time.LocalTime

/**
 * Lo que se puede elegir al crear o editar una categoría (colores y emojis) y la
 * lógica pura del editor. Sin Compose, para poder testearlo.
 */

/** Colores pastel a elegir (ARGB). Ninguno es verde, para no confundirse con "Libre". */
val COLORES_CATEGORIA: List<Long> = listOf(
    0xFFF7C1C1, 0xFFFBD3B5, 0xFFFAC775, 0xFFF5E6A3,
    0xFFB8E3DA, 0xFFA8D8EA, 0xFFB5D4F4, 0xFFC9CFF5,
    0xFFDDD3F7, 0xFFEFC6E8, 0xFFD9C8B4, 0xFFE0E0E0
)

/** Galería de emojis agrupada por temas: (título de la sección, emojis). */
val SECCIONES_EMOJI: List<Pair<String, List<String>>> = listOf(
    "Trabajo y estudios" to listOf("💼", "🏢", "💻", "📚", "🎓", "✏️", "🔧", "🚚"),
    "Salud" to listOf("🩺", "🏥", "💊", "🦷", "👓", "💉", "🧘", "💆"),
    "Deporte" to listOf("🏃", "⚽", "🏊", "🚴", "🎾", "🏀", "🏋️", "🥾"),
    "Viajes y ocio" to listOf("✈️", "🚗", "🚆", "🏖️", "⛰️", "🏕️", "🎬", "🎭"),
    "Casa y familia" to listOf("🏠", "🛒", "🍳", "🧹", "🐶", "👶", "🎂", "🎁"),
    "Otros" to listOf("📌", "⭐", "❤️", "🎵", "🎸", "🎨", "📅", "⏰", "🙏", "🗣️", "🧾", "🔔")
)

/**
 * El bloque que se guarda al apuntar algo de una categoría. Las de serie se guardan
 * con su motivo, como siempre; las propias con motivo OTRO y su categoriaId.
 */
fun bloqueDeCategoria(
    categoria: CategoriaDisponibilidad,
    caregiverId: CaregiverId,
    fecha: LocalDate,
    inicio: LocalTime,
    fin: LocalTime,
    etiqueta: String?
): AvailabilityBlock = AvailabilityBlock(
    caregiverId = caregiverId,
    fecha = fecha,
    horaInicio = inicio,
    horaFin = fin,
    motivo = categoria.base ?: MotivoNoDisponibilidad.OTRO,
    etiqueta = etiqueta,
    categoriaId = if (categoria.base == null) categoria.id else null
)

/**
 * Si [nombre] sirve para una categoría: no vacío y sin repetir el de otra (sin
 * distinguir mayúsculas). [idPropio] es la que se está editando, que no cuenta.
 */
fun nombreCategoriaValido(
    nombre: String,
    categorias: List<CategoriaDisponibilidad>,
    idPropio: CategoriaId?
): Boolean {
    val limpio = nombre.trim()
    if (limpio.isEmpty()) return false
    return categorias.none { it.id != idPropio && it.nombre.trim().equals(limpio, ignoreCase = true) }
}
