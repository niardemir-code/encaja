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

val DOCUMENTOS_LEGALES: List<DocumentoLegal> = listOf(
    DocumentoLegal(
        titulo = "Política de privacidad",
        resumen = "Qué datos guarda Encaja, para qué y cuáles son tus derechos.",
        apartados = listOf(
            ApartadoLegal(
                titulo = "1. Quién es el responsable",
                parrafos = listOf(
                    "El responsable del tratamiento de tus datos es ${DatosLegales.TITULAR} (${DatosLegales.DOCUMENTO_IDENTIFICACION}), con domicilio en ${DatosLegales.DOMICILIO}.",
                    "Puedes escribirnos en ${DatosLegales.EMAIL_CONTACTO} para cualquier cuestión sobre tus datos."
                )
            ),
            ApartadoLegal(
                titulo = "2. Qué datos tratamos",
                parrafos = listOf("Solo los necesarios para que la app funcione:"),
                puntos = listOf(
                    "Datos de tu cuenta: tu correo electrónico y, si entras con Google, el identificador de esa cuenta. Si te registras con contraseña, esta la gestiona el servicio de autenticación de Google y no la vemos.",
                    "Datos de las personas de la familia: nombre y apellidos de los cuidadores, y el nombre de pila de los niños. No pedimos apellidos de los niños, fecha de nacimiento, teléfono, dirección, fotos ni ubicación.",
                    "Datos que tú introduces: actividades de los niños (descripción, fecha, horas, quién lleva y quién recoge, avisos), ocupaciones de los cuidadores, menús, lista de la compra y anuncios del tablón.",
                    "Datos técnicos de notificaciones: un identificador del móvil (token) para poder enviarte avisos.",
                    "En tu móvil se guarda una copia local de estos datos y tus preferencias (tema, ayudas vistas)."
                )
            ),
            ApartadoLegal(
                titulo = "3. Para qué y con qué base legal",
                parrafos = listOf(
                    "Usamos tus datos únicamente para prestarte el servicio: organizar la logística de tu familia y enviarte los avisos que te corresponden. La base legal es la ejecución del servicio que has solicitado al crear tu cuenta (art. 6.1.b del RGPD).",
                    "No usamos tus datos para publicidad, no los vendemos, no elaboramos perfiles y la app no incluye herramientas de analítica ni de seguimiento."
                )
            ),
            ApartadoLegal(
                titulo = "4. Datos de menores",
                parrafos = listOf(
                    "La app está pensada para que sean los adultos de la familia quienes la usen. De los niños solo se guarda el nombre de pila y las actividades que sus adultos apuntan. Al introducir esos datos, declaras que tienes la patria potestad o la autoridad para hacerlo.",
                    "Los menores no deben crear una cuenta propia."
                )
            ),
            ApartadoLegal(
                titulo = "5. Quién más accede a los datos",
                parrafos = listOf(
                    "Los datos se guardan en los servicios de Google Firebase (autenticación, base de datos, notificaciones y funciones en la nube), que actúa como encargado del tratamiento. La base de datos y las funciones están alojadas en la Unión Europea; algunos servicios de Google, como la autenticación y las notificaciones, pueden implicar transferencias fuera de ella, que se realizan con las garantías que Google ofrece.",
                    "Los datos de una familia solo son accesibles para las cuentas vinculadas a esa familia. No se ceden a otros terceros salvo obligación legal."
                )
            ),
            ApartadoLegal(
                titulo = "6. Cuánto tiempo los conservamos",
                puntos = listOf(
                    "Las actividades y ocupaciones con más de un mes de antigüedad se borran automáticamente.",
                    "El resto de datos de la familia se conservan mientras la familia use la app.",
                    "Si borras tu cuenta, se elimina tu acceso, tu vínculo con la familia y los identificadores de tus móviles. Tu persona (nombre y apellidos) y su historial se mantienen en la familia, porque son información compartida con el resto de sus miembros y permiten que otra cuenta retome esa persona.",
                    "Si quieres que eliminemos también esa información, o todos los datos de una familia, escríbenos y lo haremos."
                )
            ),
            ApartadoLegal(
                titulo = "7. Tus derechos",
                parrafos = listOf(
                    "Puedes ejercer los derechos de acceso, rectificación, supresión, limitación del tratamiento, oposición y portabilidad escribiendo a ${DatosLegales.EMAIL_CONTACTO}. Muchas cosas las puedes hacer tú mismo desde la app: corregir o borrar tus datos, desvincular cuentas y borrar tu cuenta.",
                    "Si crees que tus datos no se tratan correctamente, puedes reclamar ante la Agencia Española de Protección de Datos (www.aepd.es)."
                )
            ),
            ApartadoLegal(
                titulo = "8. Seguridad",
                parrafos = listOf(
                    "Las comunicaciones con los servidores van cifradas, y las reglas de acceso de la base de datos impiden que una familia vea los datos de otra. Aun así, ningún sistema es infalible: avísanos si detectas cualquier problema."
                )
            ),
            ApartadoLegal(
                titulo = "9. Permisos del móvil",
                puntos = listOf(
                    "Notificaciones: para el tablón y los avisos de actividades.",
                    "Alarmas y recordatorios: para que los avisos suenen a su hora.",
                    "Ahorro de batería: opcional, para que los avisos lleguen con la app cerrada.",
                    "La app no pide acceso a la cámara, los contactos, la ubicación ni los archivos."
                )
            ),
            ApartadoLegal(
                titulo = "10. Cambios en esta política",
                parrafos = listOf(
                    "Si cambiamos este texto de forma relevante, te lo avisaremos en la app. Última actualización: ${DatosLegales.ULTIMA_ACTUALIZACION}."
                )
            )
        )
    ),
    DocumentoLegal(
        titulo = "Aviso legal",
        resumen = "Quién está detrás de Encaja y cómo contactar.",
        apartados = listOf(
            ApartadoLegal(
                titulo = "Titular de la aplicación",
                puntos = listOf(
                    "Titular: ${DatosLegales.TITULAR}",
                    "Identificación fiscal: ${DatosLegales.DOCUMENTO_IDENTIFICACION}",
                    "Domicilio: ${DatosLegales.DOMICILIO}",
                    "Correo de contacto: ${DatosLegales.EMAIL_CONTACTO}"
                )
            ),
            ApartadoLegal(
                titulo = "Propiedad intelectual",
                parrafos = listOf(
                    "El nombre, el logotipo, el diseño y el código de Encaja pertenecen a su titular. No se pueden copiar ni reutilizar sin permiso. Los datos que introduces siguen siendo tuyos y de tu familia."
                )
            )
        )
    ),
    DocumentoLegal(
        titulo = "Condiciones de uso",
        resumen = "Las reglas básicas para usar Encaja.",
        apartados = listOf(
            ApartadoLegal(
                titulo = "Uso de la app",
                puntos = listOf(
                    "Encaja es para uso personal y familiar.",
                    "Eres responsable de la información que introduces y de que tengas derecho a hacerlo, en especial la de otras personas y la de los niños.",
                    "No uses la app para fines ilícitos ni para molestar a otras personas, por ejemplo con anuncios del tablón.",
                    "Cada persona debe usar su propia cuenta y no compartir su acceso."
                )
            ),
            ApartadoLegal(
                titulo = "Avisos y disponibilidad",
                parrafos = listOf(
                    "Los avisos del móvil son una ayuda, no una garantía: pueden retrasarse o no llegar por el ahorro de batería, la falta de conexión o los ajustes del móvil. No dependas únicamente de ellos para cuestiones importantes.",
                    "Procuramos que la app funcione siempre, pero puede haber interrupciones por mantenimiento o por causas ajenas, y podemos cambiar o retirar funciones."
                )
            ),
            ApartadoLegal(
                titulo = "Responsabilidad",
                parrafos = listOf(
                    "La app se ofrece tal cual. No nos hacemos responsables de los perjuicios derivados de un uso indebido ni de un fallo en un aviso, salvo lo que establezca la ley.",
                    "Podemos suspender cuentas que incumplan estas condiciones."
                )
            ),
            ApartadoLegal(
                titulo = "Ley aplicable",
                parrafos = listOf("Estas condiciones se rigen por la legislación española.")
            )
        )
    )
)
