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

    /** Semanas de desplazamiento respecto a la actual: 0 = esta semana, -1 = anterior, +1 = siguiente. */
    private var offsetSemanas = 0

    init {
        cargar()
    }

    fun recargar() = cargar()

    /** Avanza o retrocede semanas desde el botón "Esta semana" (-1 anterior, +1 siguiente). */
    fun cambiarSemana(delta: Int) {
        offsetSemanas += delta
        cargar()
    }

    /** Vuelve directamente a la semana actual, sin acumular desplazamientos previos. */
    fun irASemanaActual() {
        offsetSemanas = 0
        cargar()
    }

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

            val lunes = LocalDate.now().lunesDeEstaSemana().plusWeeks(offsetSemanas.toLong())
            val domingo = lunes.plusDays(6)
            val guardados = menuRepository.obtenerSemana(membresia.familyId, lunes, domingo)
            val porFecha = guardados.associateBy { it.fecha }

            // Se rellenan los 7 días de la semana aunque no tengan menú guardado todavía,
            // para que la pantalla siempre muestre una fila por día.
            val dias = (0..6).map { offset ->
                val fecha = lunes.plusDays(offset.toLong())
                porFecha[fecha] ?: ComidaDelDia(fecha, comida = null, cena = null)
            }

            _pantalla.value = MenuPantallaEstado.ConDatos(dias, esSemanaActual = offsetSemanas == 0)
        }
    }

    /**
     * Guarda un único día (comida y/o cena), tal como lo deja el lápiz de
     * edición de cada campo en la pantalla. El repositorio solo sabe guardar
     * una lista de días, así que se reconstruye la semana completa
     * sustituyendo ese día — de cara a la pantalla es una edición de un
     * campo suelto.
     *
     * El estado se actualiza aquí mismo (en vez de recargar con cargar(),
     * que pasa por Cargando y reinicia el scroll de la lista): así la
     * pantalla se queda quieta, en el mismo punto donde se estaba editando.
     */
    fun guardarDia(fecha: LocalDate, comida: String?, cena: String?) {
        val familyId = familyIdActual ?: return
        val actual = _pantalla.value as? MenuPantallaEstado.ConDatos ?: return
        val actualizados = actual.dias.map { dia ->
            if (dia.fecha == fecha) dia.copy(comida = comida?.ifBlank { null }, cena = cena?.ifBlank { null })
            else dia
        }
        _pantalla.value = actual.copy(dias = actualizados)

        viewModelScope.launch {
            menuRepository.guardarSemana(familyId, actualizados)
        }
    }
}
