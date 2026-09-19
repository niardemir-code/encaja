package com.encaja.app.domain.model

/**
 * Un niño de la familia. Hasta ahora los CoverageNeed solo llevaban un
 * ChildId suelto (sin nombre asociado en ningún sitio); esto permite
 * mostrar "Júlia" o "Etna" en vez del identificador en pantallas como
 * la Guía del día.
 */
data class Child(
    val id: ChildId,
    val nombre: String
)
