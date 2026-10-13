package com.encaja.app.domain.repository

import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.FamilyId

/**
 * Resultado de darse de baja: [codigoParaVolver] si era la última cuenta y se conservan los datos
 * (con él otra cuenta retoma todo); [datosBorrados] si además se borraron los datos de la familia.
 */
data class ResultadoBajaCuenta(val codigoParaVolver: String?, val datosBorrados: Boolean)

/** Darse de baja de la app y gestionar qué cuidadores tienen una cuenta vinculada. */
interface CuentaRepository {
    /**
     * Borra la cuenta con la sesión abierta: la desvincula de la familia y elimina el acceso.
     * El cuidador y su historial se conservan, salvo que [borrarDatosFamilia] sea true y fuera la
     * única cuenta de la familia: entonces se borra la familia entera. Si era la última cuenta y se
     * conservan los datos, devuelve un código de invitación (30 días); los datos sin cuentas se
     * borran solos pasado ese plazo.
     */
    suspend fun borrarMiCuenta(borrarDatosFamilia: Boolean): Result<ResultadoBajaCuenta>

    /** (Solo administradores) quita la cuenta vinculada a [caregiverId]; el cuidador sigue en la familia. */
    suspend fun desvincularCuenta(caregiverId: CaregiverId): Result<Unit>

    /** Cuidadores de la familia que ya tienen una cuenta vinculada. */
    suspend fun cuidadoresVinculados(familyId: FamilyId): Set<CaregiverId>
}
