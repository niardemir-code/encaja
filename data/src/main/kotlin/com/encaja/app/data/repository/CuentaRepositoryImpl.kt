package com.encaja.app.data.repository

import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.FamilyId
import com.encaja.app.domain.repository.CuentaRepository
import com.encaja.app.domain.repository.ResultadoBajaCuenta
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class CuentaRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : CuentaRepository {

    // Misma región que las Cloud Functions desplegadas (REGION en functions/index.js).
    private val funciones: FirebaseFunctions by lazy { FirebaseFunctions.getInstance("europe-west1") }

    override suspend fun borrarMiCuenta(borrarDatosFamilia: Boolean): Result<ResultadoBajaCuenta> = try {
        val resultado = funciones.getHttpsCallable("borrarMiCuenta")
            .call(mapOf("borrarDatos" to borrarDatosFamilia)).await()
        val datos = resultado.data as? Map<*, *>
        Result.success(ResultadoBajaCuenta(datos?.get("codigo") as? String, datos?.get("datosBorrados") == true))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun desvincularCuenta(caregiverId: CaregiverId): Result<Unit> = try {
        funciones.getHttpsCallable("desvincularCuenta")
            .call(mapOf("caregiverId" to caregiverId.value))
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun cuidadoresVinculados(familyId: FamilyId): Set<CaregiverId> = try {
        firestore.collection("families").document(familyId.value)
            .collection("caregiverLinks").get().await()
            .documents.map { CaregiverId(it.id) }.toSet()
    } catch (e: Exception) {
        emptySet()
    }
}
