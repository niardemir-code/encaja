package com.encaja.app.ui.semana

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SemaforoViewModel @Inject constructor() : ViewModel() {

    private val mapper = SemaforoUiStateMapper(
        caregivers = DatosEjemploFamilia.caregivers,
        patrones = DatosEjemploFamilia.patrones,
        anulaciones = DatosEjemploFamilia.anulaciones,
        disponibilidad = DatosEjemploFamilia.disponibilidad
    )

    private val _uiState = MutableStateFlow(
        mapper.construir(DatosEjemploFamilia.lunes, DatosEjemploFamilia.needsDeLaSemana)
    )
    val uiState: StateFlow<SemaforoUiState> = _uiState.asStateFlow()

    fun recargar() {
        viewModelScope.launch {
            _uiState.value = mapper.construir(DatosEjemploFamilia.lunes, DatosEjemploFamilia.needsDeLaSemana)
        }
    }
}