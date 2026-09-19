package com.encaja.app.domain.model

import java.time.LocalDateTime

/**
 * Un anuncio del tablón de la familia: un mensaje corto y destacado que
 * cualquier miembro puede publicar (p. ej. "El sábado hay cumpleaños en
 * el cole de Etna"). Se muestran del más reciente al más antiguo.
 */
data class Anuncio(
    val id: AnuncioId,
    val autorNombre: String,
    val texto: String,
    val publicadoEn: LocalDateTime
)
