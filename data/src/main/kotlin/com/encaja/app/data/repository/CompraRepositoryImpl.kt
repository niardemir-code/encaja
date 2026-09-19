package com.encaja.app.data.repository

import com.encaja.app.data.firestore.ArticuloCompraFirestoreMapper
import com.encaja.app.data.local.ArticuloCompraEntity
import com.encaja.app.data.local.CompraDao
import com.encaja.app.domain.model.ArticuloCompra
import com.encaja.app.domain.model.ArticuloCompraId
import com.encaja.app.domain.model.FamilyId
import com.encaja.app.domain.repository.CompraRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class CompraRepositoryImpl @Inject constructor(
    private val dao: CompraDao,
    private val firestore: FirebaseFirestore
) : CompraRepository {

    private fun coleccion(familyId: FamilyId) =
        firestore.collection("families").document(familyId.value).collection("compra")

    override suspend fun obtenerArticulos(familyId: FamilyId): List<ArticuloCompra> {
        return try {
            val snapshot = coleccion(familyId).get().await()
            val articulos = snapshot.documents.mapNotNull { doc ->
                ArticuloCompraFirestoreMapper.desdeDocumento(doc.id, doc.data ?: emptyMap())
            }
            dao.guardarTodos(articulos.map { ArticuloCompraEntity.desdeDominio(familyId.value, it) })
            articulos
        } catch (e: Exception) {
            dao.obtener(familyId.value).map { it.aDominio() }
        }
    }

    override suspend fun guardarArticulo(familyId: FamilyId, articulo: ArticuloCompra) {
        coleccion(familyId).document(articulo.id.value).set(ArticuloCompraFirestoreMapper.aDocumento(articulo)).await()
        dao.guardar(ArticuloCompraEntity.desdeDominio(familyId.value, articulo))
    }

    override suspend fun eliminarArticulo(familyId: FamilyId, articuloId: ArticuloCompraId) {
        coleccion(familyId).document(articuloId.value).delete().await()
        dao.eliminar(familyId.value, articuloId.value)
    }
}
