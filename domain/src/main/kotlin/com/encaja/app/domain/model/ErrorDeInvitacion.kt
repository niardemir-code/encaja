package com.encaja.app.domain.model

/**
 * Lo que puede salir mal al generar o canjear un código de invitación. Cada caso es un tipo
 * propio (y no una frase) para que la pantalla lo muestre en el idioma del móvil. El
 * [message] solo sirve para los registros de errores (logs) de los desarrolladores.
 */
sealed class ErrorDeInvitacion(mensaje: String) : Exception(mensaje) {
    /** Ese cuidador ya tiene una cuenta vinculada: no se genera otro código. */
    object CuidadorYaVinculado : ErrorDeInvitacion("El cuidador ya tiene una cuenta vinculada")

    /** No se encontró un código libre tras varios intentos. */
    object NoSeGeneroCodigo : ErrorDeInvitacion("No se pudo generar un código único")

    object CodigoNoExiste : ErrorDeInvitacion("El código no existe")
    object CodigoYaUsado : ErrorDeInvitacion("El código ya se ha usado")
    object CodigoCaducado : ErrorDeInvitacion("El código ha caducado")
    object CodigoIncompleto : ErrorDeInvitacion("El código está incompleto")
}

/**
 * El [ErrorDeInvitacion] de este fallo, si lo es. Mira también las causas porque Firestore
 * puede envolver lo que se lanza dentro de una transacción en otra excepción.
 */
fun Throwable.comoErrorDeInvitacion(): ErrorDeInvitacion? {
    var actual: Throwable? = this
    var saltos = 0
    while (actual != null && saltos < 10) {
        if (actual is ErrorDeInvitacion) return actual
        actual = actual.cause
        saltos++
    }
    return null
}
