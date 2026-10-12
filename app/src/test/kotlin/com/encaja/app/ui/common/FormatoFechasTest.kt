package com.encaja.app.ui.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale

class FormatoFechasTest {

    private val es = Locale("es")
    private val ca = Locale("ca")
    private val en = Locale("en")
    private val martes29Sep = LocalDate.of(2026, 9, 29)
    private val lunes12Oct = LocalDate.of(2026, 10, 12)

    @Test
    fun `dia y mes en castellano, catalan e ingles`() {
        assertEquals("29 de septiembre", diaYMes(martes29Sep, es))
        assertEquals("29 de setembre", diaYMes(martes29Sep, ca))
        assertEquals("September 29", diaYMes(martes29Sep, en))
    }

    @Test
    fun `en catalan los meses que empiezan por vocal llevan d apostrofo`() {
        assertEquals("12 d'octubre", diaYMes(lunes12Oct, ca))
        assertEquals("1 d'abril", diaYMes(LocalDate.of(2026, 4, 1), ca))
        assertEquals("1 d'agost", diaYMes(LocalDate.of(2026, 8, 1), ca))
        assertEquals("12 de octubre", diaYMes(lunes12Oct, es))
    }

    @Test
    fun `el nombre del dia lleva mayuscula inicial en los tres idiomas`() {
        assertEquals("Martes 29", diaConNumero(martes29Sep, es))
        assertEquals("Dimarts 29", diaConNumero(martes29Sep, ca))
        assertEquals("Tuesday 29", diaConNumero(martes29Sep, en))
    }

    @Test
    fun `una letra por dia con X para el miercoles en castellano y catalan, y W en ingles`() {
        assertEquals("LMXJVSD", DayOfWeek.values().joinToString("") { letraDia(it, es) })
        assertEquals("LMXJVSD", DayOfWeek.values().joinToString("") { letraDia(it, ca) })
        assertEquals("MTWTFSS", DayOfWeek.values().joinToString("") { letraDia(it, en) })
    }

    @Test
    fun `rango de una semana dentro del mismo mes`() {
        val lunes21Sep = LocalDate.of(2026, 9, 21)
        assertEquals("21 - 27 de septiembre", rangoSemana(lunes21Sep, es))
        assertEquals("21 - 27 de setembre", rangoSemana(lunes21Sep, ca))
        assertEquals("September 21 - 27", rangoSemana(lunes21Sep, en))
        assertEquals("12 – 18 d'octubre", rangoSemana(lunes12Oct, ca, separador = "–"))
    }

    @Test
    fun `rango de una semana que cambia de mes usa meses cortos`() {
        val lunes28Dic = LocalDate.of(2026, 12, 28)
        assertEquals("28 dic - 3 ene", rangoSemana(lunes28Dic, es))
        assertEquals("Dec 28 - Jan 3", rangoSemana(lunes28Dic, en))
    }

    @Test
    fun `mes y anio con mayuscula inicial`() {
        assertEquals("Octubre 2026", mesYAnio(lunes12Oct, es))
        assertEquals("Octubre 2026", mesYAnio(lunes12Oct, ca))
        assertEquals("October 2026", mesYAnio(lunes12Oct, en))
    }
}
