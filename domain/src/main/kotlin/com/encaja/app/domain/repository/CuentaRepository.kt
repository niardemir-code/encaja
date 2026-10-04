package com.encaja.app.domain.repository

import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.FamilyId

/** Darse de baja de la app y gestionar qué cuidadores tienen una cuenta vinculada. */
interface CuentaRepository {
    /**
     * Borra la cuenta con la sesión abierta: la desvincula de la familia y elimina el acceso.
     * El cuidador y su historial se conservan. Si era la última cuenta vinculada de la familia,
     * devuelve un código de invitación para ese cuidador (con él, otra cuenta retoma todo);
     * si no, devuelve null.
     */
    suspend fun borrarMiCuenta(): Result<String?>

    /** (Solo administradores) quita la cuenta vinculada a [caregiverId]; el cuidador sigue en la familia. */
    suspend fun desvincularCuenta(caregiverId: CaregiverId): Result<Unit>

    /** Cuidadores de la familia que ya tienen una cuenta vinculada. */
    suspend fun cuidadoresVinculados(familyId: FamilyId): Set<CaregiverId>
}
