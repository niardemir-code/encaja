package com.encaja.app.domain.model

data class CategoriaId(val value: String)

/** Cómo se apunta una categoría: por horas dentro de un día, o por días completos (un rango de fechas). */
enum class ModoCategoria { HORAS, DIAS }

/**
 * Un tipo de "no disponibilidad" con su nombre, emoji y color, que se ve en la
 * cuadrícula de Familia. Hay dos clases:
 *  - Las 5 de serie ([base] != null: Trabajo, Médico, Viaje, Vacaciones, Otro). Mantienen
 *    su comportamiento (Trabajo con turnos, Viaje por fechas...) y la familia solo puede
 *    cambiarles el emoji y el color.
 *  - Las creadas por la familia ([base] == null), totalmente libres: nombre, modo, color,
 *    emoji y si [bloquea] (si "ocupa" a la persona para el cálculo de huecos o es solo
 *    informativa, p.ej. "Teletrabajo").
 * [color] va en ARGB (0xFFRRGGBB) para no depender de Compose en el dominio.
 */
data class CategoriaDisponibilidad(
    val id: CategoriaId,
    val nombre: String,
    val emoji: String,
    val color: Long,
    val modo: ModoCategoria,
    val bloquea: Boolean = true,
    val base: MotivoNoDisponibilidad? = null
) {
    val esBase: Boolean get() = base != null
}

object CategoriasBase {
    private const val PREFIJO = "base_"

    /** Id fijo de la categoría de serie de cada motivo (también el id de su personalización guardada). */
    fun idDe(motivo: MotivoNoDisponibilidad) = CategoriaId(PREFIJO + motivo.name)

    val predeterminadas: List<CategoriaDisponibilidad> = listOf(
        base(MotivoNoDisponibilidad.TRABAJO, "Trabajo", "💼", 0xFFF7C1C1, ModoCategoria.HORAS),
        base(MotivoNoDisponibilidad.MEDICO, "Médico", "🩺", 0xFFB5D4F4, ModoCategoria.HORAS),
        base(MotivoNoDisponibilidad.VIAJE, "Viaje", "✈️", 0xFFFAC775, ModoCategoria.DIAS),
        base(MotivoNoDisponibilidad.VACACIONES, "Vacaciones", "🏖️", 0xFFDDD3F7, ModoCategoria.DIAS),
        base(MotivoNoDisponibilidad.OTRO, "Otro", "📌", 0xFFE0E0E0, ModoCategoria.HORAS)
    )

    private fun base(motivo: MotivoNoDisponibilidad, nombre: String, emoji: String, color: Long, modo: ModoCategoria) =
        CategoriaDisponibilidad(idDe(motivo), nombre, emoji, color, modo, bloquea = true, base = motivo)

    /**
     * Si el emoji de [categoria] lo eligió la familia: siempre en las propias, y en las de
     * serie solo si se cambió el que traen. En ese caso la casilla muestra el emoji en vez
     * de las horas (que se siguen viendo al tocarla).
     */
    fun tieneEmojiElegido(categoria: CategoriaDisponibilidad): Boolean {
        val base = categoria.base ?: return true
        return predeterminadas.first { it.base == base }.emoji != categoria.emoji
    }

    /**
     * La lista completa que ve la familia: primero las 5 de serie (con el emoji y el color
     * personalizados, si se guardaron) y detrás las propias, por orden alfabético. De una
     * personalización de serie solo se toman emoji y color; nombre, modo y "bloquea" no cambian.
     */
    fun combinar(guardadas: List<CategoriaDisponibilidad>): List<CategoriaDisponibilidad> {
        val porId = guardadas.associateBy { it.id }
        val bases = predeterminadas.map { base ->
            porId[base.id]?.let { base.copy(emoji = it.emoji, color = it.color) } ?: base
        }
        val propias = guardadas
            .filter { it.base == null && !it.id.value.startsWith(PREFIJO) }
            .sortedBy { it.nombre.lowercase() }
        return bases + propias
    }
}

/**
 * La categoría con la que se pinta un bloque: la suya propia si la tiene y sigue existiendo;
 * si no (bloques de serie, o una categoría propia que se borró), la de serie de su motivo.
 */
fun AvailabilityBlock.categoriaEn(categorias: List<CategoriaDisponibilidad>): CategoriaDisponibilidad =
    categorias.firstOrNull { it.id == categoriaId }
        ?: categorias.firstOrNull { it.id == CategoriasBase.idDe(motivo) }
        ?: CategoriasBase.predeterminadas.first { it.base == motivo }

/**
 * Marca como "no ocupa" los bloques cuya categoría propia es solo informativa, para que
 * el cálculo de huecos no los cuente. Se aplica al leer la disponibilidad, así que cambiar
 * esa opción en una categoría afecta también a los días que ya estaban apuntados.
 */
fun List<AvailabilityBlock>.conBloqueoDeCategorias(categorias: List<CategoriaDisponibilidad>): List<AvailabilityBlock> {
    val informativas = categorias.filter { !it.bloquea }.map { it.id }.toSet()
    if (informativas.isEmpty()) return this
    return map { if (it.categoriaId != null && it.categoriaId in informativas) it.copy(bloquea = false) else it }
}
