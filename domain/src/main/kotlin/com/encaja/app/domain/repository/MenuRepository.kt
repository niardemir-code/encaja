package com.encaja.app.domain.repository

import com.encaja.app.domain.model.ComidaDelDia
import com.encaja.app.domain.model.FamilyId
import java.time.LocalDate

interface MenuRepository {
    suspend fun obtenerSemana(familyId: FamilyId, desde: LocalDate, hasta: LocalDate): List<ComidaDelDia>

    /** Guarda de golpe todos los días recibidos (un único botón de guardar para la semana). */
    suspend fun guardarSemana(familyId: FamilyId, dias: List<ComidaDelDia>)
}
