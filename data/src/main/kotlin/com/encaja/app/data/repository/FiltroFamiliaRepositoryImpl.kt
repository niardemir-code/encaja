package com.encaja.app.data.repository

import com.encaja.app.domain.model.FamilyId
import com.encaja.app.domain.repository.FiltroFamiliaRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import javax.inject.Inject

// Documento: families/{familyId}/filtrosFamilia/{lunes ISO} { ocultos: [ids] }.
// Lo cubren las reglas existentes de families/{id}/** (solo miembros de la familia).
class FiltroFamiliaRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : FiltroFamiliaRepository {

    private fun doc(familyId: FamilyId, lunes: LocalDate) =
        firestore.collection("families").document(familyId.value)
            .collection("filtrosFamilia").document(lunes.toString())

    override suspend fun obtener(familyId: FamilyId, lunes: LocalDate): Set<String>? {
        val snapshot = doc(familyId, lunes).get().await()
        if (!snapshot.exists()) return null
        val lista = snapshot.get("ocultos") as? List<*> ?: return emptySet()
        return lista.filterIsInstance<String>().toSet()
    }

    override suspend fun guardar(familyId: FamilyId, lunes: LocalDate, ocultos: Set<String>) {
        doc(familyId, lunes).set(mapOf("ocultos" to ocultos.toList())).await()
    }

    override suspend fun quitar(familyId: FamilyId, lunes: LocalDate) {
        doc(familyId, lunes).delete().await()
    }
}
