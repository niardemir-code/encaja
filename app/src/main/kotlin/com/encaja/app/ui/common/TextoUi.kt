package com.encaja.app.ui.common

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Un texto para la pantalla que todavía no está en ningún idioma: los ViewModels y los
 * repositorios devuelven esto (qué mensaje y con qué datos) y la pantalla lo convierte en
 * texto con el idioma del teléfono. Así no se mezclan frases en castellano dentro de la
 * lógica, y cada idioma coloca las palabras donde le toca.
 */
sealed interface TextoUi {
    /** Un texto ya escrito (el nombre de una persona, lo que escribió el usuario...). */
    data class Literal(val texto: String) : TextoUi

    /** Un texto de `strings.xml`. Los [args] pueden ser otros [TextoUi]. */
    class Recurso(@StringRes val id: Int, val args: List<Any> = emptyList()) : TextoUi

    /** Un texto de `plurals` (uno, varios...), según [cantidad]. */
    class Plural(@PluralsRes val id: Int, val cantidad: Int, val args: List<Any> = emptyList()) : TextoUi
}

fun textoUi(@StringRes id: Int, vararg args: Any): TextoUi = TextoUi.Recurso(id, args.toList())

fun textoUi(texto: String): TextoUi = TextoUi.Literal(texto)

/** El texto en el idioma de [context]. */
fun TextoUi.resolver(context: Context): String = when (this) {
    is TextoUi.Literal -> texto
    is TextoUi.Recurso -> context.getString(id, *argsResueltos(args, context))
    is TextoUi.Plural -> context.resources.getQuantityString(id, cantidad, *argsResueltos(args, context))
}

/** Igual que [resolver], dentro de una pantalla. */
@Composable
fun TextoUi.resolver(): String = resolver(LocalContext.current)

private fun argsResueltos(args: List<Any>, context: Context): Array<Any> =
    args.map { if (it is TextoUi) it.resolver(context) else it }.toTypedArray()
