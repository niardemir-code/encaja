package com.encaja.app.domain.model

data class CategoriaId(val value: String)

/** Cómo se apunta una categoría: por horas dentro de un día, o por días completos (un rango de fechas). */
enum class ModoCategoria { HORAS, DIAS }

/**
 * Un tipo de "no disponibilidad" con su nombre, emoji y color, que se ve en la
 * cuadrícula de Familia. Todas tienen los mismos campos configurables: nombre, emoji,
 * color, [modo] (por horas o por días), si [bloquea] (si "ocupa" a la persona para el
 * cálculo de huecos o es solo informativa, p.ej. "Teletrabajo") y, las que van por
 * horas, sus horarios guardados (ver TurnoTrabajo). La única diferencia de las 5 de
 * serie ([base] != null) es que no se pueden borrar y que sus bloques se guardan con
 * su motivo fijo (para que los datos antiguos sigan cuadrando).
 * [color] va en ARGB (0xFFRRGGBB) para no depender de Compose en el dominio.
 */
data class CategoriaDisponibilidad(
    val id: CategoriaId,
    val nombre: String,
    val emoji: String,
    val color: Long,
    val modo: ModoCategoria,
    val bloquea: Boolean = true,
    val base: MotivoNoDisponibilidad? = null,
    /** Solo en las de serie: la familia la ha borrado. Se guarda así (en vez de borrar el
     * documento) porque, si no, volvería a aparecer con sus valores de serie. */
    val eliminada: Boolean = false
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
     * La lista completa que ve la familia: primero las 5 de serie (con lo que la familia
     * haya personalizado: nombre, emoji, color, modo y "bloquea") y detrás las propias,
     * por orden alfabético. De una personalización de serie se conserva siempre su id
     * y su motivo [base], que es lo que ata los bloques ya guardados.
     */
    fun combinar(guardadas: List<CategoriaDisponibilidad>): List<CategoriaDisponibilidad> {
        val porId = guardadas.associateBy { it.id }
        val bases = predeterminadas.mapNotNull { base ->
            val guardada = porId[base.id] ?: return@mapNotNull base
            if (guardada.eliminada) return@mapNotNull null
            base.copy(nombre = guardada.nombre.ifBlank { base.nombre }, emoji = guardada.emoji, color = guardada.color, modo = guardada.modo, bloquea = guardada.bloquea)
        }
        val propias = guardadas
            .filter { it.base == null && !it.id.value.startsWith(PREFIJO) }
            .sortedBy { it.nombre.lowercase() }
        return bases + propias
    }

    /** Las de serie que la familia ha borrado (para poder recuperarlas). */
    fun eliminadas(guardadas: List<CategoriaDisponibilidad>): List<CategoriaDisponibilidad> {
        val porId = guardadas.associateBy { it.id }
        return predeterminadas.filter { porId[it.id]?.eliminada == true }
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
 * Marca como "no ocupa" los bloques cuya categoría (propia o de serie) es solo
 * informativa, para que el cálculo de huecos no los cuente. Se aplica al leer la
 * disponibilidad, así que cambiar esa opción en una categoría afecta también a los días
 * que ya estaban apuntados.
 */
fun List<AvailabilityBlock>.conBloqueoDeCategorias(categorias: List<CategoriaDisponibilidad>): List<AvailabilityBlock> {
    if (categorias.all { it.bloquea }) return this
    return map { if (!it.categoriaEn(categorias).bloquea) it.copy(bloquea = false) else it }
}
