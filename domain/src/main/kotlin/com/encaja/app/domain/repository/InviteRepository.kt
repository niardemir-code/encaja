package com.encaja.app.domain.repository

import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.FamilyId
import com.encaja.app.domain.model.FamilyMembership

interface InviteRepository {
    /** Genera un código de un solo uso que vincula a quien lo introduzca con este cuidador de esta familia. */
    suspend fun generarInvitacion(familyId: FamilyId, caregiverId: CaregiverId): Result<String>

    /** Valida el código, lo marca como usado, y devuelve a qué familia y cuidador da acceso. */
    suspend fun canjearInvitacion(codigo: String): Result<FamilyMembership>
}
