package com.encaja.app.ui.menu

// NOTA: depende de Hilt/ViewModel (androidx.lifecycle), no compilado en este entorno.

import androidx.lifecycle.ViewModel
import com.encaja.app.ui.common.lanzarSeguro
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
import java.time.temporal.ChronoUnit
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

    /** Recarga completa: vuelve a comprobar sesión y familia (por si han cambiado). Se
     * usa al entrar en la pestaña; cambiar de semana no lo necesita. */
    fun recargar() {
        // Si ya había datos, se recarga la misma semana sin pasar por "Cargando".
        if (familyIdActual != null && _pantalla.value is MenuPantallaEstado.ConDatos) cargarDatos() else cargar()
    }

    /** Avanza o retrocede semanas desde el botón "Esta semana" (-1 anterior, +1 siguiente). */
    fun cambiarSemana(delta: Int) {
        offsetSemanas += delta
        cargarDatos()
    }

    /** Vuelve directamente a la semana actual, sin acumular desplazamientos previos. */
    fun irASemanaActual() {
        offsetSemanas = 0
        cargarDatos()
    }

    /** Salta directamente a la semana que contiene [fecha], elegida en el calendario. */
    fun irASemanaDe(fecha: LocalDate) {
        val lunesHoy = LocalDate.now().lunesDeEstaSemana()
        offsetSemanas = ChronoUnit.WEEKS.between(lunesHoy, fecha.lunesDeEstaSemana()).toInt()
        cargarDatos()
    }

    /** Primera carga (o recarga forzada): valida sesión y familia y solo entonces
     * pide los datos de la semana. */
    private fun cargar() {
        lanzarSeguro {
            _pantalla.value = MenuPantallaEstado.Cargando

            val uid = authRepository.sesionActual()?.uid
            if (uid == null) {
                _pantalla.value = MenuPantallaEstado.SinFamilia
                return@lanzarSeguro
            }

            val membresia = familyMembershipRepository.obtenerMembresia(uid)
            if (membresia == null) {
                _pantalla.value = MenuPantallaEstado.SinFamilia
                return@lanzarSeguro
            }
            familyIdActual = membresia.familyId
            cargarDatos()
        }
    }

    /** Recarga solo el menú de la semana con el desplazamiento actual: cambiar de
     * semana no necesita volver a comprobar sesión ni familia. */
    private fun cargarDatos() {
        val familyId = familyIdActual ?: return
        lanzarSeguro {
            val lunes = LocalDate.now().lunesDeEstaSemana().plusWeeks(offsetSemanas.toLong())
            val domingo = lunes.plusDays(6)
            val guardados = menuRepository.obtenerSemana(familyId, lunes, domingo)
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

        lanzarSeguro {
            menuRepository.guardarSemana(familyId, actualizados)
        }
    }
}
