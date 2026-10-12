package com.encaja.app.ui.common

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Formatos de fecha de toda la app, por idioma (castellano, catalán, inglés). Todas las
 * pantallas usan estas funciones en vez de montar el texto a mano, porque el orden de las
 * palabras y los conectores cambian: «12 de octubre», «12 d'octubre», «October 12».
 * Los nombres de mes y de día se piden en su forma independiente (sin «de» delante, que en
 * catalán sí traen los nombres «de formato»).
 */

/** El idioma que usa la app ahora mismo: el del teléfono. */
fun localeApp(): Locale = Locale.getDefault()

private fun Locale.esIngles() = language == "en"
private fun Locale.esCatalan() = language == "ca"

private fun String.conMayuscula(locale: Locale): String =
    if (isEmpty()) this else substring(0, 1).uppercase(locale) + substring(1)

/** «octubre», «October» (en minúscula salvo en inglés). */
fun nombreMes(fecha: LocalDate, locale: Locale = localeApp()): String =
    fecha.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)

/** «oct», «Oct»: sin el punto final. */
fun nombreMesCorto(fecha: LocalDate, locale: Locale = localeApp()): String =
    fecha.month.getDisplayName(TextStyle.SHORT_STANDALONE, locale).removeSuffix(".")

/** «Martes», «Dimarts», «Tuesday»: con la primera letra en mayúscula. */
fun nombreDia(dia: DayOfWeek, locale: Locale = localeApp()): String =
    dia.getDisplayName(TextStyle.FULL_STANDALONE, locale).conMayuscula(locale)

/** «Mar», «Dt», «Tue»: abreviado, con mayúscula inicial y sin punto. */
fun nombreDiaCorto(dia: DayOfWeek, locale: Locale = localeApp()): String =
    dia.getDisplayName(TextStyle.SHORT_STANDALONE, locale).removeSuffix(".").conMayuscula(locale)

/** Una letra por día: L M X J V S D en castellano y catalán, M T W T F S S en inglés. */
fun letraDia(dia: DayOfWeek, locale: Locale = localeApp()): String =
    if (locale.esIngles()) when (dia) {
        DayOfWeek.MONDAY -> "M"
        DayOfWeek.TUESDAY -> "T"
        DayOfWeek.WEDNESDAY -> "W"
        DayOfWeek.THURSDAY -> "T"
        DayOfWeek.FRIDAY -> "F"
        DayOfWeek.SATURDAY -> "S"
        DayOfWeek.SUNDAY -> "S"
    } else when (dia) {
        DayOfWeek.MONDAY -> "L"
        DayOfWeek.TUESDAY -> "M"
        DayOfWeek.WEDNESDAY -> "X"
        DayOfWeek.THURSDAY -> "J"
        DayOfWeek.FRIDAY -> "V"
        DayOfWeek.SATURDAY -> "S"
        DayOfWeek.SUNDAY -> "D"
    }

private val VOCALES = "aeiouàèéíòóú"

/** «de octubre», «d'octubre» (catalán ante vocal), o solo «October» en inglés. */
private fun conConectorDeMes(mes: String, locale: Locale): String = when {
    locale.esIngles() -> mes
    locale.esCatalan() -> if (mes.isNotEmpty() && mes[0].lowercaseChar() in VOCALES) "d'$mes" else "de $mes"
    else -> "de $mes"
}

/** «12 de octubre», «12 d'octubre», «October 12». */
fun diaYMes(fecha: LocalDate, locale: Locale = localeApp()): String {
    val mes = nombreMes(fecha, locale)
    return if (locale.esIngles()) "${mes.conMayuscula(locale)} ${fecha.dayOfMonth}"
    else "${fecha.dayOfMonth} ${conConectorDeMes(mes, locale)}"
}

/** «12 oct», «Oct 12»: para casillas estrechas. */
fun diaYMesCorto(fecha: LocalDate, locale: Locale = localeApp()): String {
    val mes = nombreMesCorto(fecha, locale)
    return if (locale.esIngles()) "${mes.conMayuscula(locale)} ${fecha.dayOfMonth}" else "${fecha.dayOfMonth} $mes"
}

/** «Martes 29», «Dimarts 29», «Tuesday 29». */
fun diaConNumero(fecha: LocalDate, locale: Locale = localeApp()): String =
    "${nombreDia(fecha.dayOfWeek, locale)} ${fecha.dayOfMonth}"

/** «mes año»: «Octubre 2026», «October 2026». */
fun mesYAnio(fecha: LocalDate, locale: Locale = localeApp()): String =
    "${nombreMes(fecha, locale).conMayuscula(locale)} ${fecha.year}"

/**
 * La semana que empieza en [lunes]: «21 - 27 de septiembre», «21 - 27 d'octubre», «September 21 - 27»; si
 * cambia de mes, con meses cortos: «28 sep - 4 oct», «Sep 28 - Oct 4». [separador] es el guion
 * que separa los días (cada pantalla usa el suyo).
 */
fun rangoSemana(lunes: LocalDate, locale: Locale = localeApp(), separador: String = "-"): String {
    val domingo = lunes.plusDays(6)
    return if (lunes.month == domingo.month) {
        if (locale.esIngles()) "${nombreMes(lunes, locale).conMayuscula(locale)} ${lunes.dayOfMonth} $separador ${domingo.dayOfMonth}"
        else "${lunes.dayOfMonth} $separador ${domingo.dayOfMonth} ${conConectorDeMes(nombreMes(domingo, locale), locale)}"
    } else {
        "${diaYMesCorto(lunes, locale)} $separador ${diaYMesCorto(domingo, locale)}"
    }
}
