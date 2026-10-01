package com.encaja.app.ui.ocupaciones

// NOTA: depende de Hilt/ViewModel (androidx.lifecycle), no compilado en este entorno.

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.encaja.app.domain.model.AvailabilityBlock
import com.encaja.app.domain.model.CategoriasBase
import com.encaja.app.domain.model.FamilyId
import com.encaja.app.domain.model.categoriaEn
import com.encaja.app.domain.repository.AuthRepository
import com.encaja.app.domain.repository.AvailabilityRepository
import com.encaja.app.domain.repository.CaregiverRepository
import com.encaja.app.domain.repository.CategoriaRepository
import com.encaja.app.domain.repository.FamilyMembershipRepository
import com.encaja.app.domain.repository.FamilyUnitRepository
import com.encaja.app.ui.familia.mismaOcupacionQue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * Pantalla "Ocupaciones" (desde Ajustes): todas las ocupaciones vigentes de personas y
 * unidades familiares (desde hace un mes, que es lo que se conserva, hasta un año
 * adelante), para repasarlas con un filtro y borrarlas de golpe.
 */
@HiltViewModel
class OcupacionesViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val familyMembershipRepository: FamilyMembershipRepository,
    private val caregiverRepository: CaregiverRepository,
    private val familyUnitRepository: FamilyUnitRepository,
    private val categoriaRepository: CategoriaRepository,
    private val availabilityRepository: AvailabilityRepository
) : ViewModel() {

    private val _pantalla = MutableStateFlow<OcupacionesPantallaEstado>(OcupacionesPantallaEstado.Cargando)
    val pantalla: StateFlow<OcupacionesPantallaEstado> = _pantalla.asStateFlow()

    private var familyIdActual: FamilyId? = null

    init {
        cargar()
    }

    fun recargar() = cargar()

    private fun cargar() {
        viewModelScope.launch {
            _pantalla.value = OcupacionesPantallaEstado.Cargando

            val uid = authRepository.sesionActual()?.uid
            if (uid == null) {
                familyIdActual = null
                _pantalla.value = OcupacionesPantallaEstado.SinFamilia
                return@launch
            }
            val membresia = familyMembershipRepository.obtenerMembresia(uid)
            if (membresia == null) {
                familyIdActual = null
                _pantalla.value = OcupacionesPantallaEstado.SinFamilia
                return@launch
            }
            familyIdActual = membresia.familyId
            val familyId = membresia.familyId
            val hoy = LocalDate.now()

            coroutineScope {
                val cuidadoresDef = async { caregiverRepository.obtenerCuidadores(familyId) }
                val unidadesDef = async { familyUnitRepository.obtenerUnidades(familyId) }
                val categoriasDef = async { categoriaRepository.obtenerCategorias(familyId) }
                val bloquesDef = async { availabilityRepository.obtenerDisponibilidad(familyId, hoy.minusMonths(1), hoy.plusYears(1)) }

                val cuidadores = cuidadoresDef.await()
                val unidades = unidadesDef.await()
                val categorias = CategoriasBase.combinar(categoriasDef.await())
                val nombres = cuidadores.associate { it.id to it.nombre }

                // Se agrupan las que son la misma ocupación en varias personas.
                val grupos = mutableListOf<MutableList<AvailabilityBlock>>()
                bloquesDef.await().sortedWith(compareBy({ it.fecha }, { it.horaInicio })).forEach { bloque ->
                    val grupo = grupos.firstOrNull { it.first().mismaOcupacionQue(bloque) }
                    if (grupo != null) grupo += bloque else grupos += mutableListOf(bloque)
                }

                val ocupaciones = grupos.map { bloques ->
                    val ids = bloques.map { it.caregiverId }.toSet()
                    // Si las personas coinciden con los miembros de una unidad, es "de la unidad".
                    val unidad = unidades.firstOrNull { it.miembros.isNotEmpty() && it.miembros.toSet() == ids }
                    val quien = unidad?.nombre ?: ids.mapNotNull { nombres[it] }.joinToString(", ")
                    val idsFiltro = ids.map { it.value }.toSet() + listOfNotNull(unidad?.id?.value) +
                        unidades.filter { u -> u.miembros.any { it in ids } }.map { it.id.value }
                    val b = bloques.first()
                    Ocupacion(
                        clave = "${b.fecha}|${b.horaInicio}|${b.motivo}|${b.categoriaId?.value}|${b.etiqueta}|${ids.joinToString { it.value }}",
                        bloques = bloques,
                        categoria = b.categoriaEn(categorias),
                        quien = quien,
                        idsFiltro = idsFiltro
                    )
                }
                _pantalla.value = OcupacionesPantallaEstado.ConDatos(ocupaciones, cuidadores, unidades)
            }
        }
    }

    /** Borra las ocupaciones elegidas: cada una en todas las personas que la tengan. */
    fun eliminar(ocupaciones: List<Ocupacion>) {
        val familyId = familyIdActual ?: return
        if (ocupaciones.isEmpty()) return
        viewModelScope.launch {
            ocupaciones.flatMap { it.bloques }.forEach { bloque ->
                availabilityRepository.eliminarBloque(familyId, bloque.caregiverId, bloque.fecha, bloque.horaInicio)
            }
            cargar()
        }
    }
}
