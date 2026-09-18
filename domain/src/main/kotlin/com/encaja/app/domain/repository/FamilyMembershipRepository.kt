package com.encaja.app.domain.repository

import com.encaja.app.domain.model.FamilyMembership

interface FamilyMembershipRepository {
    /** null si el usuario todavía no pertenece a ninguna familia. */
    suspend fun obtenerMembresia(uid: String): FamilyMembership?

    suspend fun vincularAFamilia(uid: String, membership: FamilyMembership)
}
