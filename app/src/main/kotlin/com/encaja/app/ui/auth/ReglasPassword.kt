package com.encaja.app.ui.auth

/**
 * Requisitos mínimos de la contraseña al crear una cuenta. Lógica pura (sin Compose ni Firebase)
 * para poder comprobarla con un test.
 *
 * Se pide longitud (lo que más pesa), una letra y un número, y se descartan las contraseñas
 * típicas. No se obliga a mayúsculas ni símbolos: solo empujan a "Contraseña1!".
 */
const val PASSWORD_LONGITUD_MINIMA = 8

data class RequisitosPassword(
    val longitud: Boolean,
    val letra: Boolean,
    val numero: Boolean,
    /** false si es una contraseña de las más habituales o casi todo el mismo carácter. */
    val noHabitual: Boolean
) {
    /** Los tres requisitos básicos (lo que se muestra como lista) están cumplidos. */
    val basicosCumplidos: Boolean get() = longitud && letra && numero

    val cumple: Boolean get() = basicosCumplidos && noHabitual
}

private val PASSWORDS_HABITUALES = setOf(
    "12345678", "123456789", "1234567890", "12341234", "87654321", "11111111", "00000000",
    "password", "password1", "password12", "password123", "passw0rd", "p4ssw0rd",
    "contraseña1", "contrasena1", "contraseña123", "contrasena123", "contrasenya1",
    "qwerty123", "qwertyui", "qwertyuiop", "qwerty12", "asdf1234", "asdfghjk", "zxcvbnm1",
    "abc12345", "abcd1234", "a1234567", "a12345678", "admin123", "admin1234",
    "iloveyou", "iloveyou1", "welcome1", "welcome123", "letmein1", "monkey123", "dragon123",
    "encaja123", "encaixa123", "encaja1234", "encaixa1234", "tecnofan1"
)

fun evaluarPassword(password: String): RequisitosPassword {
    val minusculas = password.lowercase()
    return RequisitosPassword(
        longitud = password.length >= PASSWORD_LONGITUD_MINIMA,
        letra = password.any { it.isLetter() },
        numero = password.any { it.isDigit() },
        noHabitual = minusculas !in PASSWORDS_HABITUALES && minusculas.toSet().size >= 4
    )
}
