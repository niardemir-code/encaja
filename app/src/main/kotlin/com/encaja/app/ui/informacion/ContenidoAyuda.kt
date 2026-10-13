package com.encaja.app.ui.informacion

/**
 * Contenido de Ajustes > Ayuda. Está separado de la pantalla para poder ampliarlo sin tocar
 * código de interfaz: para añadir preguntas frecuentes u otros temas, basta con añadir una
 * [SeccionAyuda] más (por ejemplo "Preguntas frecuentes") a cada lista de idioma
 * (ContenidoAyudaEs.kt, ContenidoAyudaCa.kt, ContenidoAyudaEn.kt); [seccionesAyuda] elige la del idioma del móvil.
 */
data class TemaAyuda(
    val titulo: String,
    /** Texto explicativo, un párrafo por elemento. */
    val parrafos: List<String> = emptyList(),
    /** Lista de puntos o pasos, que se muestran debajo de los párrafos. */
    val puntos: List<String> = emptyList(),
    /** Texto final, después de los puntos (p. ej. una advertencia). */
    val nota: String? = null
)

data class SeccionAyuda(val titulo: String, val temas: List<TemaAyuda>)

/** Las secciones de la ayuda en el idioma del móvil (castellano si no hay traducción). */
fun seccionesAyuda(locale: java.util.Locale = com.encaja.app.ui.common.localeApp()): List<SeccionAyuda> =
    when (locale.language) {
        "ca" -> SECCIONES_AYUDA_CA
        "en" -> SECCIONES_AYUDA_EN
        else -> SECCIONES_AYUDA_ES
    }
