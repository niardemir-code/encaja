package com.encaja.app.data.repository

import com.encaja.app.data.firestore.CategoriaFirestoreMapper
import com.encaja.app.data.local.CategoriaDao
import com.encaja.app.data.local.CategoriaEntity
import com.encaja.app.domain.model.CategoriaDisponibilidad
import com.encaja.app.domain.model.CategoriaId
import com.encaja.app.domain.model.FamilyId
import com.encaja.app.domain.repository.CategoriaRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class CategoriaRepositoryImpl @Inject constructor(
    private val dao: CategoriaDao,
    private val firestore: FirebaseFirestore
) : CategoriaRepository {

    private fun coleccion(familyId: FamilyId) =
        firestore.collection("families").document(familyId.value).collection("categorias")

    override suspend fun obtenerCategorias(familyId: FamilyId): List<CategoriaDisponibilidad> {
        return try {
            val snapshot = coleccion(familyId).get().await()
            val categorias = snapshot.documents.mapNotNull { doc ->
                CategoriaFirestoreMapper.desdeDocumento(doc.id, doc.data ?: emptyMap())
            }
            dao.guardarTodas(categorias.map { CategoriaEntity.desdeDominio(familyId.value, it) })
            categorias
        } catch (e: Exception) {
            dao.obtener(familyId.value).mapNotNull { it.aDominio() }
        }
    }

    override suspend fun guardarCategoria(familyId: FamilyId, categoria: CategoriaDisponibilidad) {
        coleccion(familyId).document(categoria.id.value).set(CategoriaFirestoreMapper.aDocumento(categoria)).await()
        dao.guardarTodas(listOf(CategoriaEntity.desdeDominio(familyId.value, categoria)))
    }

    override suspend fun eliminarCategoria(familyId: FamilyId, categoriaId: CategoriaId) {
        coleccion(familyId).document(categoriaId.value).delete().await()
        dao.eliminar(familyId.value, categoriaId.value)
    }
}
