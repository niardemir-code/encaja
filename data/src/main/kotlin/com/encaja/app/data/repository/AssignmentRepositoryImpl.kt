package com.encaja.app.data.repository

import com.encaja.app.data.firestore.PatronCuidadoFirestoreMapper
import com.encaja.app.data.local.AnulacionEntity
import com.encaja.app.data.local.AssignmentDao
import com.encaja.app.data.local.PatronCuidadoEntity
import com.encaja.app.domain.model.Anulaciones
import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.FamilyId
import com.encaja.app.domain.model.PatronCuidado
import com.encaja.app.domain.repository.AssignmentRepository
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

class AssignmentRepositoryImpl @Inject constructor(
    private val dao: AssignmentDao,
    private val firestore: FirebaseFirestore
) : AssignmentRepository {

    private fun familia(familyId: FamilyId) = firestore.collection("families").document(familyId.value)

    override suspend fun obtenerPatrones(familyId: FamilyId): List<PatronCuidado> {
        return try {
            val snapshot = familia(familyId).collection("patrones").get().await()
            val patrones = snapshot.documents.mapNotNull { doc ->
                PatronCuidadoFirestoreMapper.desdeDocumento(doc.data ?: emptyMap())
            }
            dao.guardarPatrones(patrones.map { PatronCuidadoEntity.desdeDominio(familyId.value, it) })
            patrones
        } catch (e: Exception) {
            dao.obtenerPatrones(familyId.value).map { it.aDominio() }
        }
    }

    override suspend fun obtenerAnulaciones(familyId: FamilyId, desde: LocalDate, hasta: LocalDate): Anulaciones {
        return try {
            val snapshot = familia(familyId).collection("anulaciones")
                .whereGreaterThanOrEqualTo(FieldPath.documentId(), desde.toString())
                .whereLessThanOrEqualTo(FieldPath.documentId(), hasta.toString())
                .get()
                .await()

            val anulaciones = snapshot.documents.mapNotNull { doc ->
                val caregiverId = doc.getString("caregiverId") ?: return@mapNotNull null
                LocalDate.parse(doc.id) to CaregiverId(caregiverId)
            }.toMap()

            anulaciones.forEach { (fecha, caregiverId) ->
                dao.guardarAnulacion(AnulacionEntity(familyId.value, fecha.toString(), caregiverId.value))
            }
            anulaciones
        } catch (e: Exception) {
            dao.obtenerAnulaciones(familyId.value, desde.toString(), hasta.toString())
                .associate { LocalDate.parse(it.fecha) to CaregiverId(it.caregiverId) }
        }
    }

    override suspend fun guardarPatrones(familyId: FamilyId, patrones: List<PatronCuidado>) {
        patrones.forEach { patron ->
            familia(familyId).collection("patrones").document(patron.diaSemana.name)
                .set(PatronCuidadoFirestoreMapper.aDocumento(patron)).await()
        }
        dao.guardarPatrones(patrones.map { PatronCuidadoEntity.desdeDominio(familyId.value, it) })
    }

    override suspend fun eliminarPatron(familyId: FamilyId, diaSemana: DayOfWeek) {
        familia(familyId).collection("patrones").document(diaSemana.name).delete().await()
        dao.eliminarPatron(familyId.value, diaSemana.name)
    }

    override suspend fun anularParaFecha(familyId: FamilyId, fecha: LocalDate, caregiverId: CaregiverId) {
        familia(familyId).collection("anulaciones").document(fecha.toString())
            .set(mapOf("caregiverId" to caregiverId.value)).await()
        dao.guardarAnulacion(AnulacionEntity(familyId.value, fecha.toString(), caregiverId.value))
    }

    override suspend fun eliminarAnulacion(familyId: FamilyId, fecha: LocalDate) {
        familia(familyId).collection("anulaciones").document(fecha.toString()).delete().await()
        dao.eliminarAnulacion(familyId.value, fecha.toString())
    }
}