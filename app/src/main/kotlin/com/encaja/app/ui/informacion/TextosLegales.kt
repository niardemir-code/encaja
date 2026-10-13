package com.encaja.app.ui.informacion

/**
 * Datos del titular de la app que se muestran en Ajustes > Privacidad y legal.
 * RELLENAR antes de publicar: los textos entre corchetes son marcadores que hay que sustituir
 * por los datos reales (los pide la normativa de protección de datos y de servicios de la
 * sociedad de la información).
 */
object DatosLegales {
    /** Correo de contacto del desarrollador; se usa también en Ajustes > Contacto. */
    const val EMAIL_CONTACTO = "development@tecnofan.org"

    const val TITULAR = "[Nombre y apellidos o razón social del titular]"
    const val DOCUMENTO_IDENTIFICACION = "[NIF/CIF]"
    const val DOMICILIO = "[Domicilio postal del titular]"
    const val ULTIMA_ACTUALIZACION = "[Fecha de la última actualización de estos textos]"
}

/** Un bloque de texto legal: un título y sus párrafos y puntos. */
data class ApartadoLegal(
    val titulo: String,
    val parrafos: List<String> = emptyList(),
    val puntos: List<String> = emptyList()
)

data class DocumentoLegal(val titulo: String, val resumen: String, val apartados: List<ApartadoLegal>)

/**
 * Los textos legales en el idioma de la app (castellano, catalán o inglés). Son largos, así que
 * cada idioma vive en su propio archivo en vez de en claves sueltas de `strings.xml`.
 * Los datos entre corchetes de [DatosLegales] se rellenan una sola vez y valen para los tres.
 */
fun documentosLegales(locale: java.util.Locale = com.encaja.app.ui.common.localeApp()): List<DocumentoLegal> =
    when (locale.language) {
        "ca" -> DOCUMENTOS_LEGALES_CA
        "en" -> DOCUMENTOS_LEGALES_EN
        else -> DOCUMENTOS_LEGALES_ES
    }
