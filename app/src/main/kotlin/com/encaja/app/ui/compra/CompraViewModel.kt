package com.encaja.app.ui.compra

// NOTA: depende de Hilt/ViewModel (androidx.lifecycle), no compilado en este entorno.

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.encaja.app.domain.model.ArticuloCompra
import com.encaja.app.domain.model.ArticuloCompraId
import com.encaja.app.domain.model.FamilyId
import com.encaja.app.domain.repository.AuthRepository
import com.encaja.app.domain.repository.CompraRepository
import com.encaja.app.domain.repository.FamilyMembershipRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CompraViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val familyMembershipRepository: FamilyMembershipRepository,
    private val compraRepository: CompraRepository
) : ViewModel() {

    private val mapper = CompraUiStateMapper()

    private val _pantalla = MutableStateFlow<CompraPantallaEstado>(CompraPantallaEstado.Cargando)
    val pantalla: StateFlow<CompraPantallaEstado> = _pantalla.asStateFlow()

    private var familyIdActual: FamilyId? = null
    private var articulosActuales: List<ArticuloCompra> = emptyList()

    init {
        cargar()
    }

    fun recargar() = cargar()

    private fun cargar() {
        viewModelScope.launch {
            _pantalla.value = CompraPantallaEstado.Cargando

            val uid = authRepository.sesionActual()?.uid
            if (uid == null) {
                familyIdActual = null
                _pantalla.value = CompraPantallaEstado.SinFamilia
                return@launch
            }

            val membresia = familyMembershipRepository.obtenerMembresia(uid)
            if (membresia == null) {
                familyIdActual = null
                _pantalla.value = CompraPantallaEstado.SinFamilia
                return@launch
            }
            familyIdActual = membresia.familyId

            val articulos = compraRepository.obtenerArticulos(membresia.familyId)
            articulosActuales = articulos
            _pantalla.value = CompraPantallaEstado.ConDatos(mapper.construir(articulos))
        }
    }

    /** Añade un artículo nuevo a una tienda, generando su id a partir del nombre. */
    fun agregarArticulo(nombre: String, tienda: String) {
        val familyId = familyIdActual ?: return
        val nombreLimpio = nombre.trim()
        val tiendaLimpia = tienda.trim()
        if (nombreLimpio.isBlank() || tiendaLimpia.isBlank()) return

        val id = generarArticuloCompraIdDesdeNombre(nombreLimpio, articulosActuales.map { it.id })

        viewModelScope.launch {
            compraRepository.guardarArticulo(familyId, ArticuloCompra(id, nombreLimpio, tiendaLimpia))
            cargar()
        }
    }

    /** Marca (o desmarca) un artículo como comprado, sin borrarlo de la lista. */
    fun marcarComprado(articulo: ArticuloCompra, comprado: Boolean) {
        val familyId = familyIdActual ?: return
        viewModelScope.launch {
            compraRepository.guardarArticulo(familyId, articulo.copy(comprado = comprado))
            cargar()
        }
    }

    fun eliminarArticulo(articuloId: ArticuloCompraId) {
        val familyId = familyIdActual ?: return
        viewModelScope.launch {
            compraRepository.eliminarArticulo(familyId, articuloId)
            cargar()
        }
    }
}
