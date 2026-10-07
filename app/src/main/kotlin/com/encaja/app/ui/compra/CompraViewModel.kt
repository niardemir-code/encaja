package com.encaja.app.ui.compra

// NOTA: depende de Hilt/ViewModel (androidx.lifecycle), no compilado en este entorno.

import androidx.lifecycle.ViewModel
import com.encaja.app.ui.common.lanzarSeguro
import com.encaja.app.domain.model.ArticuloCompra
import com.encaja.app.domain.model.ArticuloCompraId
import com.encaja.app.domain.model.FamilyId
import com.encaja.app.domain.repository.AuthRepository
import com.encaja.app.domain.repository.CompraRepository
import com.encaja.app.domain.repository.FamilyMembershipRepository
import com.encaja.app.domain.repository.ResultadoMembresia
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

    /** [mostrarCargando] false al refrescar tras marcar/añadir/borrar un artículo: así la
     * lista no parpadea con la rueda de carga ni pierde lo que se estaba escribiendo. */
    private fun cargar(mostrarCargando: Boolean = true) {
        lanzarSeguro {
            if (mostrarCargando || _pantalla.value !is CompraPantallaEstado.ConDatos) {
                _pantalla.value = CompraPantallaEstado.Cargando
            }

            val uid = authRepository.sesionActual()?.uid
            if (uid == null) {
                familyIdActual = null
                _pantalla.value = CompraPantallaEstado.SinFamilia
                return@lanzarSeguro
            }

            val familyId = when (val resultado = familyMembershipRepository.consultarMembresia(uid)) {
                is ResultadoMembresia.Tiene -> resultado.membresia.familyId
                is ResultadoMembresia.NoTiene -> null
                // Sin conexión: se usa la última familia conocida (la lista sale de la caché local).
                is ResultadoMembresia.Error -> familyIdActual ?: run {
                    _pantalla.value = CompraPantallaEstado.ErrorDeConexion
                    return@lanzarSeguro
                }
            }
            if (familyId == null) {
                familyIdActual = null
                _pantalla.value = CompraPantallaEstado.SinFamilia
                return@lanzarSeguro
            }
            familyIdActual = familyId

            try {
                val articulos = compraRepository.obtenerArticulos(familyId)
                articulosActuales = articulos
                _pantalla.value = CompraPantallaEstado.ConDatos(mapper.construir(articulos))
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                // Que nunca se quede la rueda de carga para siempre.
                if (_pantalla.value !is CompraPantallaEstado.ConDatos) {
                    _pantalla.value = CompraPantallaEstado.ErrorDeConexion
                }
                throw e
            }
        }
    }

    /** Añade un artículo nuevo a una tienda, generando su id a partir del nombre. */
    fun agregarArticulo(nombre: String, tienda: String) {
        val familyId = familyIdActual ?: return
        val nombreLimpio = nombre.trim()
        val tiendaLimpia = tienda.trim()
        if (nombreLimpio.isBlank() || tiendaLimpia.isBlank()) return

        val id = generarArticuloCompraIdDesdeNombre(nombreLimpio, articulosActuales.map { it.id })

        lanzarSeguro {
            compraRepository.guardarArticulo(familyId, ArticuloCompra(id, nombreLimpio, tiendaLimpia))
            cargar(mostrarCargando = false)
        }
    }

    /** Marca (o desmarca) un artículo como comprado, sin borrarlo de la lista. */
    fun marcarComprado(articulo: ArticuloCompra, comprado: Boolean) {
        val familyId = familyIdActual ?: return
        // Actualización optimista: la lista cambia al instante y se guarda en segundo plano.
        // Solo si falla el guardado se recarga el estado real del servidor.
        val actualizado = articulo.copy(comprado = comprado)
        articulosActuales = articulosActuales.map { if (it.id == articulo.id) actualizado else it }
        _pantalla.value = CompraPantallaEstado.ConDatos(mapper.construir(articulosActuales))
        lanzarSeguro {
            try {
                compraRepository.guardarArticulo(familyId, actualizado)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                cargar(mostrarCargando = false)
                throw e
            }
        }
    }

    /** Cambia el nombre de un artículo (mismo id, misma tienda y mismo estado de comprado). */
    fun renombrarArticulo(articulo: ArticuloCompra, nuevoNombre: String) {
        val familyId = familyIdActual ?: return
        val nombreLimpio = nuevoNombre.trim()
        if (nombreLimpio.isBlank() || nombreLimpio == articulo.nombre) return
        lanzarSeguro {
            compraRepository.guardarArticulo(familyId, articulo.copy(nombre = nombreLimpio))
            cargar(mostrarCargando = false)
        }
    }

    fun eliminarArticulo(articuloId: ArticuloCompraId) {
        val familyId = familyIdActual ?: return
        lanzarSeguro {
            compraRepository.eliminarArticulo(familyId, articuloId)
            cargar(mostrarCargando = false)
        }
    }
}
