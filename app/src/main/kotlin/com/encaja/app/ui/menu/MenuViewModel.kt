package com.encaja.app.ui.menu

// NOTA: depende de Hilt/ViewModel (androidx.lifecycle), no compilado en este entorno.

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.encaja.app.domain.model.ComidaDelDia
import com.encaja.app.domain.model.FamilyId
import com.encaja.app.domain.repository.AuthRepository
import com.encaja.app.domain.repository.FamilyMembershipRepository
import com.encaja.app.domain.repository.MenuRepository
import com.encaja.app.domain.usecase.lunesDeEstaSemana
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class MenuViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val familyMembershipRepository: FamilyMembershipRepository,
    private val menuRepository: MenuRepository
) : ViewModel() {

    private val _pantalla = MutableStateFlow<MenuPantallaEstado>(MenuPantallaEstado.Cargando)
    val pantalla: StateFlow<MenuPantallaEstado> = _pantalla.asStateFlow()

    private var familyIdActual: FamilyId? = null

    init {
        cargar()
    }

    fun recargar() = cargar()

    private fun cargar() {
        viewModelScope.launch {
            _pantalla.value = MenuPantallaEstado.Cargando

            val uid = authRepository.sesionActual()?.uid
            if (uid == null) {
                _pantalla.value = MenuPantallaEstado.SinFamilia
                return@launch
            }

            val membresia = familyMembershipRepository.obtenerMembresia(uid)
            if (membresia == null) {
                _pantalla.value = MenuPantallaEstado.SinFamilia
                return@launch
            }
            familyIdActual = membresia.familyId

            val lunes = LocalDate.now().lunesDeEstaSemana()
            val domingo = lunes.plusDays(6)
            val guardados = menuRepository.obtenerSemana(membresia.familyId, lunes, domingo)
            val porFecha = guardados.associateBy { it.fecha }

            // Se rellenan los 7 días de la semana aunque no tengan menú guardado todavía,
            // para que la pantalla siempre muestre una fila por día.
            val semanaCompleta = (0..6).map { offset ->
                val fecha = lunes.plusDays(offset.toLong())
                porFecha[fecha] ?: ComidaDelDia(fecha, comida = null, cena = null)
            }

            _pantalla.value = MenuPantallaEstado.ConDatos(semanaCompleta)
        }
    }

    /** Guarda de golpe el menú de toda la semana (un único botón, no uno por día). */
    fun guardarSemana(dias: List<ComidaDelDia>) {
        val familyId = familyIdActual ?: return
        viewModelScope.launch {
            val normalizados = dias.map { dia ->
                dia.copy(comida = dia.comida?.ifBlank { null }, cena = dia.cena?.ifBlank { null })
            }
            menuRepository.guardarSemana(familyId, normalizados)
            cargar()
        }
    }
}
