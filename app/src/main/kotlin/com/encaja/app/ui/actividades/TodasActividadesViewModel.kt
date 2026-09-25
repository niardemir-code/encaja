package com.encaja.app.ui.actividades

// NOTA: depende de Hilt/ViewModel (androidx.lifecycle), no compilado en este entorno.

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.encaja.app.domain.model.CoverageNeedId
import com.encaja.app.domain.model.FamilyId
import com.encaja.app.domain.repository.AuthRepository
import com.encaja.app.domain.repository.ChildRepository
import com.encaja.app.domain.repository.CoverageNeedRepository
import com.encaja.app.domain.repository.FamilyMembershipRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Pantalla "Todas las actividades" (desde Ajustes): el listado completo de actividades de
 * la familia, sin el filtro por día de la Guía, para poder ver y borrar de golpe las
 * antiguas que ya no se pueden gestionar en bloque desde allí (p.ej. porque no comparten
 * grupoRepeticionId con nada, al ser de antes de tener repetición, o porque su serie ya
 * terminó y no vuelve a aparecer al navegar por días).
 */
@HiltViewModel
class TodasActividadesViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val familyMembershipRepository: FamilyMembershipRepository,
    private val childRepository: ChildRepository,
    private val coverageNeedRepository: CoverageNeedRepository
) : ViewModel() {

    private val _pantalla = MutableStateFlow<TodasActividadesPantallaEstado>(TodasActividadesPantallaEstado.Cargando)
    val pantalla: StateFlow<TodasActividadesPantallaEstado> = _pantalla.asStateFlow()

    private var familyIdActual: FamilyId? = null

    init {
        cargar()
    }

    fun recargar() = cargar()

    private fun cargar() {
        viewModelScope.launch {
            _pantalla.value = TodasActividadesPantallaEstado.Cargando

            val uid = authRepository.sesionActual()?.uid
            if (uid == null) {
                familyIdActual = null
                _pantalla.value = TodasActividadesPantallaEstado.SinFamilia
                return@launch
            }

            val membresia = familyMembershipRepository.obtenerMembresia(uid)
            if (membresia == null) {
                familyIdActual = null
                _pantalla.value = TodasActividadesPantallaEstado.SinFamilia
                return@launch
            }
            familyIdActual = membresia.familyId
            val familyId = membresia.familyId

            val grupos = coroutineScope {
                val ninosDef = async { childRepository.obtenerNinos(familyId) }
                val actividadesDef = async { coverageNeedRepository.obtenerTodosLosNeeds(familyId) }
                val ninos = ninosDef.await()
                val actividades = actividadesDef.await()
                val porNino = actividades.groupBy { it.childId }
                ninos.map { nino ->
                    GrupoActividadesNino(
                        child = nino,
                        actividades = porNino[nino.id].orEmpty()
                            .sortedWith(compareBy({ it.fecha }, { it.horaInicio }))
                    )
                }
            }
            _pantalla.value = TodasActividadesPantallaEstado.ConDatos(grupos)
        }
    }

    /** Borra de golpe todas las actividades cuyo id esté en [ids], sean del niño y de la
     * serie que sean. */
    fun eliminarSeleccionadas(ids: Set<CoverageNeedId>) {
        val familyId = familyIdActual ?: return
        if (ids.isEmpty()) return
        viewModelScope.launch {
            coverageNeedRepository.eliminarNeeds(familyId, ids.toList())
            cargar()
        }
    }
}
