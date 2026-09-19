package com.encaja.app.data.repository

import com.encaja.app.data.firestore.ComidaDelDiaFirestoreMapper
import com.encaja.app.data.local.ComidaDelDiaEntity
import com.encaja.app.data.local.MenuDao
import com.encaja.app.domain.model.ComidaDelDia
import com.encaja.app.domain.model.FamilyId
import com.encaja.app.domain.repository.MenuRepository
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import javax.inject.Inject

class MenuRepositoryImpl @Inject constructor(
    private val dao: MenuDao,
    private val firestore: FirebaseFirestore
) : MenuRepository {

    private fun familia(familyId: FamilyId) = firestore.collection("families").document(familyId.value)

    override suspend fun obtenerSemana(familyId: FamilyId, desde: LocalDate, hasta: LocalDate): List<ComidaDelDia> {
        return try {
            val snapshot = familia(familyId).collection("menus")
                .whereGreaterThanOrEqualTo(FieldPath.documentId(), desde.toString())
                .whereLessThanOrEqualTo(FieldPath.documentId(), hasta.toString())
                .get()
                .await()

            val dias = snapshot.documents.map { doc ->
                ComidaDelDiaFirestoreMapper.desdeDocumento(LocalDate.parse(doc.id), doc.data ?: emptyMap())
            }

            dao.guardarTodos(dias.map { ComidaDelDiaEntity.desdeDominio(familyId.value, it) })
            dias
        } catch (e: Exception) {
            dao.obtener(familyId.value, desde.toString(), hasta.toString()).map { it.aDominio() }
        }
    }

    override suspend fun guardarSemana(familyId: FamilyId, dias: List<ComidaDelDia>) {
        dias.forEach { dia ->
            familia(familyId).collection("menus").document(dia.fecha.toString())
                .set(ComidaDelDiaFirestoreMapper.aDocumento(dia)).await()
        }
        dao.guardarTodos(dias.map { ComidaDelDiaEntity.desdeDominio(familyId.value, it) })
    }
}
