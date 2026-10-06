package com.encaja.app.domain.repository

import com.encaja.app.domain.model.FamilyMembership

/** Resultado de consultar a qué familia pertenece una cuenta. */
sealed interface ResultadoMembresia {
    /** La cuenta está vinculada a una familia. */
    data class Tiene(val membresia: FamilyMembership) : ResultadoMembresia

    /** Se pudo consultar y la cuenta todavía no pertenece a ninguna familia. */
    data object NoTiene : ResultadoMembresia

    /** No se pudo consultar (sin conexión, permiso, etc.): NO significa que no tenga familia. */
    data object Error : ResultadoMembresia
}

interface FamilyMembershipRepository {
    /**
     * Distingue "no tiene familia" de "no se pudo comprobar". Hay que usarla donde un error
     * llevaría a mostrar o hacer algo que solo vale para quien no tiene familia (como crear una).
     */
    suspend fun consultarMembresia(uid: String): ResultadoMembresia

    /** null si el usuario todavía no pertenece a ninguna familia (o si no se pudo comprobar). */
    suspend fun obtenerMembresia(uid: String): FamilyMembership? =
        (consultarMembresia(uid) as? ResultadoMembresia.Tiene)?.membresia

    suspend fun vincularAFamilia(uid: String, membership: FamilyMembership)
}
