package com.encaja.app.data.firestore

import com.encaja.app.domain.model.Caregiver
import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.CaregiverRole

/**
 * Convierte entre Caregiver y el mapa de campos que espera un documento
 * de Firestore. Deliberadamente Kotlin puro — no usa DocumentSnapshot ni
 * ninguna clase del SDK de Firebase, así se puede testear sin conexión
 * ni emulador. La capa de datos real (en :data, fuera de este archivo)
 * es la que llama a esto pasándole snapshot.data.
 */
object CaregiverFirestoreMapper {

    fun aDocumento(caregiver: Caregiver): Map<String, Any?> = mapOf(
        "nombre" to caregiver.nombre,
        "apellido1" to caregiver.apellido1,
        "apellido2" to caregiver.apellido2,
        "rol" to caregiver.rol.name,
        "puedeDesplazarse" to caregiver.puedeDesplazarse
    )

    /**
     * [id] se pasa aparte porque en Firestore el id del documento no es
     * un campo dentro del propio documento, sino la clave con la que se
     * guardó — así se refleja igual aquí.
     *
     * "apellido1"/"apellido2" se leen como opcionales (por defecto "")
     * para que los documentos ya existentes, guardados antes de añadir
     * estos campos, se sigan leyendo sin necesidad de migrarlos.
     */
    fun desdeDocumento(id: String, datos: Map<String, Any?>): Caregiver? {
        val nombre = datos["nombre"] as? String ?: return null
        val apellido1 = datos["apellido1"] as? String ?: ""
        val apellido2 = datos["apellido2"] as? String ?: ""
        val rolTexto = datos["rol"] as? String ?: return null
        val rol = runCatching { CaregiverRole.valueOf(rolTexto) }.getOrNull() ?: return null
        val puedeDesplazarse = datos["puedeDesplazarse"] as? Boolean ?: true

        return Caregiver(CaregiverId(id), nombre, apellido1, apellido2, rol, puedeDesplazarse)
    }
}
