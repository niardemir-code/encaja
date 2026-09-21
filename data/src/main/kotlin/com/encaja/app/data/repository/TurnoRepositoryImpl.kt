package com.encaja.app.data.repository

import com.encaja.app.data.firestore.TurnoTrabajoFirestoreMapper
import com.encaja.app.data.local.TurnoDao
import com.encaja.app.data.local.TurnoTrabajoEntity
import com.encaja.app.domain.model.FamilyId
import com.encaja.app.domain.model.TurnoId
import com.encaja.app.domain.model.TurnoTrabajo
import com.encaja.app.domain.repository.TurnoRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class TurnoRepositoryImpl @Inject constructor(
    private val dao: TurnoDao,
    private val firestore: FirebaseFirestore
) : TurnoRepository {

    private fun coleccion(familyId: FamilyId) =
        firestore.collection("families").document(familyId.value).collection("turnos")

    override suspend fun obtenerTurnos(familyId: FamilyId): List<TurnoTrabajo> {
        return try {
            val snapshot = coleccion(familyId).get().await()
            val turnos = snapshot.documents.mapNotNull { doc ->
                TurnoTrabajoFirestoreMapper.desdeDocumento(doc.id, doc.data ?: emptyMap())
            }
            dao.guardarTodos(turnos.map { TurnoTrabajoEntity.desdeDominio(familyId.value, it) })
            turnos
        } catch (e: Exception) {
            dao.obtener(familyId.value).map { it.aDominio() }
        }
    }

    override suspend fun guardarTurno(familyId: FamilyId, turno: TurnoTrabajo) {
        coleccion(familyId).document(turno.id.value).set(TurnoTrabajoFirestoreMapper.aDocumento(turno)).await()
        dao.guardarTodos(listOf(TurnoTrabajoEntity.desdeDominio(familyId.value, turno)))
    }

    override suspend fun eliminarTurno(familyId: FamilyId, turnoId: TurnoId) {
        coleccion(familyId).document(turnoId.value).delete().await()
        dao.eliminar(familyId.value, turnoId.value)
    }
}
