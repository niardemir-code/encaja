package com.encaja.app.ui.familia

import com.encaja.app.domain.model.AvailabilityBlock
import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.CategoriaDisponibilidad
import com.encaja.app.domain.model.CategoriaId
import com.encaja.app.domain.model.CategoriasBase
import com.encaja.app.domain.model.ModoCategoria
import com.encaja.app.domain.model.MotivoNoDisponibilidad
import com.encaja.app.domain.model.TurnoId
import com.encaja.app.domain.model.TurnoTrabajo
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class DisponibilidadEdicionTest {

    private val victor = CaregiverId("victor")
    private val lunes = LocalDate.of(2026, 9, 21)

    @Test
    fun `la casilla muestra las horas en dos lineas`() {
        val bloque = AvailabilityBlock(victor, lunes, LocalTime.of(6, 0), LocalTime.of(14, 0), MotivoNoDisponibilidad.TRABAJO)
        assertEquals("06:00\n14:00", textoCelda(listOf(bloque)))
    }

    @Test
    fun `un bloque de dia completo muestra el tipo, y varios bloques añaden el contador`() {
        val viaje = AvailabilityBlock(victor, lunes, AvailabilityBlock.INICIO_DIA, AvailabilityBlock.FIN_DIA, MotivoNoDisponibilidad.VIAJE)
        val otro = AvailabilityBlock(victor, lunes, LocalTime.of(18, 0), LocalTime.of(19, 0), MotivoNoDisponibilidad.OTRO)
        assertEquals("Viaje +1", textoCelda(listOf(otro, viaje)))
        assertNull(textoCelda(emptyList()))
    }

    @Test
    fun `un dia completo muestra el emoji de su categoria, uno por horas no`() {
        val categorias = CategoriasBase.combinar(emptyList())
        val vacaciones = AvailabilityBlock(victor, lunes, AvailabilityBlock.INICIO_DIA, AvailabilityBlock.FIN_DIA, MotivoNoDisponibilidad.VACACIONES)
        val trabajo = AvailabilityBlock(victor, lunes, LocalTime.of(6, 0), LocalTime.of(14, 0), MotivoNoDisponibilidad.TRABAJO)
        assertEquals("🏖️", emojiCelda(listOf(vacaciones), categorias))
        assertEquals("✈️", emojiCelda(listOf(vacaciones.copy(motivo = MotivoNoDisponibilidad.VIAJE)), categorias))
        assertNull(emojiCelda(listOf(trabajo), categorias))
        assertNull(emojiCelda(emptyList(), categorias))
    }

    @Test
    fun `el emoji sigue a la personalizacion y a las categorias propias`() {
        val playa = CategoriasBase.predeterminadas.first { it.base == MotivoNoDisponibilidad.VACACIONES }.copy(emoji = "🌴")
        val campamento = CategoriaDisponibilidad(CategoriaId("camp"), "Campamento", "🏕️", 0xFFB8E3DA, ModoCategoria.DIAS)
        val categorias = CategoriasBase.combinar(listOf(playa, campamento))
        val vacaciones = AvailabilityBlock(victor, lunes, AvailabilityBlock.INICIO_DIA, AvailabilityBlock.FIN_DIA, MotivoNoDisponibilidad.VACACIONES)
        assertEquals("🌴", emojiCelda(listOf(vacaciones), categorias))
        val bloqueCamp = bloqueDeCategoria(campamento, victor, lunes, AvailabilityBlock.INICIO_DIA, AvailabilityBlock.FIN_DIA, null)
        assertEquals("🏕️", emojiCelda(listOf(bloqueCamp), categorias))
    }

    @Test
    fun `por horas se ve el emoji si lo eligio la familia, y las horas si es el de serie`() {
        val gym = CategoriaDisponibilidad(CategoriaId("gym"), "Gimnasio", "🏋️", 0xFFB8E3DA, ModoCategoria.HORAS)
        val trabajoConEmoji = CategoriasBase.predeterminadas.first().copy(emoji = "🏭")
        val turno = AvailabilityBlock(victor, lunes, LocalTime.of(6, 0), LocalTime.of(14, 0), MotivoNoDisponibilidad.TRABAJO)
        val clase = bloqueDeCategoria(gym, victor, lunes, LocalTime.of(18, 0), LocalTime.of(19, 0), null)

        assertNull(emojiCelda(listOf(turno), CategoriasBase.combinar(listOf(gym))))
        assertEquals("🏋️", emojiCelda(listOf(clase), CategoriasBase.combinar(listOf(gym))))
        assertEquals("🏭", emojiCelda(listOf(turno), CategoriasBase.combinar(listOf(trabajoConEmoji))))
        // Volver a poner el emoji de serie devuelve las horas.
        assertNull(emojiCelda(listOf(turno), CategoriasBase.combinar(listOf(trabajoConEmoji.copy(emoji = "💼")))))
    }

    @Test
    fun `un bloque de categoria propia se guarda como OTRO con su id, uno de serie con su motivo`() {
        val gym = CategoriaDisponibilidad(CategoriaId("gym"), "Gimnasio", "🏋️", 0xFFB8E3DA, ModoCategoria.HORAS)
        val propio = bloqueDeCategoria(gym, victor, lunes, LocalTime.of(18, 0), LocalTime.of(19, 0), "Pilates")
        assertEquals(MotivoNoDisponibilidad.OTRO, propio.motivo)
        assertEquals(CategoriaId("gym"), propio.categoriaId)
        val medico = CategoriasBase.predeterminadas.first { it.base == MotivoNoDisponibilidad.MEDICO }
        val deSerie = bloqueDeCategoria(medico, victor, lunes, LocalTime.of(9, 0), LocalTime.of(10, 0), null)
        assertEquals(MotivoNoDisponibilidad.MEDICO, deSerie.motivo)
        assertNull(deSerie.categoriaId)
    }

    @Test
    fun `el nombre de una categoria no puede estar vacio ni repetirse`() {
        val categorias = CategoriasBase.combinar(emptyList())
        assertFalse(nombreCategoriaValido("  ", categorias, null))
        assertFalse(nombreCategoriaValido("trabajo", categorias, null))
        assertTrue(nombreCategoriaValido("Gimnasio", categorias, null))
        val trabajoId = CategoriasBase.idDe(MotivoNoDisponibilidad.TRABAJO)
        assertTrue(nombreCategoriaValido("Trabajo", categorias, trabajoId))
    }

    @Test
    fun `la galeria no repite emojis ni colores`() {
        val emojis = SECCIONES_EMOJI.flatMap { it.second }
        assertEquals(emojis.size, emojis.toSet().size)
        assertEquals(COLORES_CATEGORIA.size, COLORES_CATEGORIA.toSet().size)
    }

    @Test
    fun `los dias marcados se convierten en las fechas de esa semana`() {
        val fechas = fechasDeLaSemana(lunes, setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))
        assertEquals(listOf(lunes, lunes.plusDays(2)), fechas)
    }

    @Test
    fun `duplicar a la semana siguiente añade las mismas fechas una semana despues`() {
        val fechas = fechasTrabajo(listOf(lunes, lunes.plusDays(1)), duplicarSemanaSiguiente = true)
        assertEquals(listOf(lunes, lunes.plusDays(1), lunes.plusDays(7), lunes.plusDays(8)), fechas)
    }

    @Test
    fun `un rango de fechas incluye ambos extremos y acepta el orden invertido`() {
        assertEquals(3, fechasEntre(lunes, lunes.plusDays(2)).size)
        assertEquals(fechasEntre(lunes, lunes.plusDays(2)), fechasEntre(lunes.plusDays(2), lunes))
        assertEquals(listOf(lunes), fechasEntre(lunes, lunes))
    }

    @Test
    fun `la conversion a milisegundos del selector de fecha es reversible`() {
        assertEquals(lunes, millisUtcAFecha(fechaAMillisUtc(lunes)))
    }

    @Test
    fun `se reconoce el turno guardado que coincide con las horas elegidas`() {
        val manana = TurnoTrabajo(TurnoId("m"), "Mañana", LocalTime.of(6, 0), LocalTime.of(14, 0))
        val oficina = TurnoTrabajo(TurnoId("o"), "Oficina", LocalTime.of(7, 0), LocalTime.of(15, 0))
        assertEquals(oficina, turnoConHoras(listOf(manana, oficina), LocalTime.of(7, 0), LocalTime.of(15, 0)))
        assertNull(turnoConHoras(listOf(manana, oficina), LocalTime.of(7, 0), LocalTime.of(16, 0)))
    }

    @Test
    fun `la misma ocupacion en varias personas se muestra una sola vez`() {
        val deVictor = AvailabilityBlock(victor, lunes, LocalTime.of(8, 0), LocalTime.of(15, 0), MotivoNoDisponibilidad.TRABAJO)
        val deSilvia = deVictor.copy(caregiverId = CaregiverId("silvia"))
        val otra = deVictor.copy(horaFin = LocalTime.of(16, 0))
        val unicas = listOf(deVictor, deSilvia, otra).sinRepetirEntrePersonas()
        assertEquals(2, unicas.size)
        assertTrue(deVictor.mismaOcupacionQue(deSilvia))
        assertFalse(deVictor.mismaOcupacionQue(otra))
    }

    // ── «Repetir o borrar en otros días» ──────────────────────────────────────────

    @Test
    fun `miercoles y viernes marcados hasta el vencimiento encienden esos dos circulos`() {
        val hasta = lunes.plusDays(20) // 11 oct
        val miercoles = listOf(23, 30).map { LocalDate.of(2026, 9, it) } + LocalDate.of(2026, 10, 7)
        val viernes = listOf(LocalDate.of(2026, 9, 25), LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 9))
        val marcadas = (miercoles + viernes).toSet()
        assertEquals(setOf(DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), diasSemanaCompletos(lunes, hasta, marcadas))
    }

    @Test
    fun `si falta un solo miercoles ese circulo no se enciende`() {
        val hasta = lunes.plusDays(20)
        val marcadas = setOf(
            LocalDate.of(2026, 9, 23), LocalDate.of(2026, 10, 7), // falta el 30 de septiembre
            LocalDate.of(2026, 9, 25), LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 9)
        )
        assertEquals(setOf(DayOfWeek.FRIDAY), diasSemanaCompletos(lunes, hasta, marcadas))
    }

    @Test
    fun `un dia suelto no cuenta como repeticion semanal`() {
        // Entre el lunes y el domingo siguiente cada día de la semana aparece una sola vez.
        assertTrue(diasSemanaCompletos(lunes, lunes.plusDays(6), setOf(lunes, lunes.plusDays(2))).isEmpty())
    }

    @Test
    fun `las fechas marcadas antes del dia de partida no cuentan para los circulos`() {
        val hasta = lunes.plusDays(20)
        val marcadas = setOf(LocalDate.of(2026, 9, 16)) // un miércoles anterior, irrelevante
        assertTrue(diasSemanaCompletos(lunes, hasta, marcadas).isEmpty())
    }

    @Test
    fun `un miercoles borrado a mano deja ese circulo a medias, no apagado ni encendido`() {
        val hasta = lunes.plusDays(20)
        val marcadas = setOf(LocalDate.of(2026, 9, 23), LocalDate.of(2026, 10, 7)) // falta el 30
        assertFalse(DayOfWeek.WEDNESDAY in diasSemanaCompletos(lunes, hasta, marcadas))
        assertEquals(setOf(DayOfWeek.WEDNESDAY), diasSemanaParciales(lunes, hasta, marcadas))
    }

    @Test
    fun `un dia completo no es parcial y uno sin fechas tampoco`() {
        val hasta = lunes.plusDays(20)
        val todosLosViernes = setOf(LocalDate.of(2026, 9, 25), LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 9))
        assertTrue(diasSemanaParciales(lunes, hasta, todosLosViernes).isEmpty())
        assertTrue(diasSemanaParciales(lunes, hasta, emptySet()).isEmpty())
    }

    @Test
    fun `el propio dia de partida no cuenta para dejar un circulo a medias`() {
        val miercoles = LocalDate.of(2026, 9, 23)
        assertTrue(diasSemanaParciales(miercoles, miercoles.plusMonths(3), setOf(miercoles)).isEmpty())
    }

    @Test
    fun `en modo borrar solo cuentan los dias que existen`() {
        val hasta = lunes.plusDays(20)
        val existentes = setOf(LocalDate.of(2026, 9, 23), LocalDate.of(2026, 9, 30), LocalDate.of(2026, 9, 25))
        val marcadas = setOf(LocalDate.of(2026, 9, 23), LocalDate.of(2026, 9, 30))
        assertEquals(setOf(DayOfWeek.WEDNESDAY), diasSemanaCompletos(lunes, hasta, marcadas, existentes))
        assertTrue(diasSemanaParciales(lunes, hasta, marcadas, existentes).isEmpty())
    }

    @Test
    fun `en modo borrar un miercoles sin marcar deja el circulo a medias y un dia sin fechas no se enciende`() {
        val hasta = lunes.plusDays(20)
        val existentes = setOf(LocalDate.of(2026, 9, 23), LocalDate.of(2026, 9, 30), LocalDate.of(2026, 9, 25))
        val marcadas = setOf(LocalDate.of(2026, 9, 23))
        assertTrue(diasSemanaCompletos(lunes, hasta, marcadas, existentes).isEmpty())
        assertEquals(setOf(DayOfWeek.WEDNESDAY), diasSemanaParciales(lunes, hasta, marcadas, existentes))
        // El sábado no tiene ningún día apuntado: ni lleno ni a medias.
        assertFalse(DayOfWeek.SATURDAY in diasSemanaCompletos(lunes, hasta, existentes, existentes))
    }

    @Test
    fun `las fechas de un dia de la semana se limitan al rango y al universo`() {
        val hasta = lunes.plusDays(20)
        assertEquals(
            listOf(LocalDate.of(2026, 9, 23), LocalDate.of(2026, 9, 30), LocalDate.of(2026, 10, 7)),
            fechasDelDiaSemana(lunes, hasta, DayOfWeek.WEDNESDAY)
        )
        assertEquals(
            listOf(LocalDate.of(2026, 9, 30)),
            fechasDelDiaSemana(lunes, hasta, DayOfWeek.WEDNESDAY, setOf(LocalDate.of(2026, 9, 30)))
        )
    }
}

