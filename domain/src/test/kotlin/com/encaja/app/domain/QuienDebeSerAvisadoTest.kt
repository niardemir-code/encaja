package com.encaja.app.domain

import com.encaja.app.domain.model.*
import com.encaja.app.domain.usecase.TipoAviso
import com.encaja.app.domain.usecase.debeAvisarA
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime

class QuienDebeSerAvisadoTest {

    private val ana = CaregiverId("ana")
    private val luis = CaregiverId("luis")
    private val marta = CaregiverId("marta")

    private fun need(
        lleva: String? = null,
        recoge: String? = null,
        creador: String? = "ana"
    ) = CoverageNeed(
        id = CoverageNeedId("n1"),
        childId = ChildId("etna"),
        fecha = LocalDate.of(2026, 10, 7),
        horaInicio = LocalTime.of(17, 0),
        horaFin = LocalTime.of(18, 0),
        descripcion = "Natación",
        quienLlevaId = lleva,
        quienRecogeId = recoge,
        creadoPorId = creador
    )

    private val parejaLuisMarta = FamilyUnit(FamilyUnitId("u1"), "U1", "Luis y Marta", listOf(luis, marta))

    @Test
    fun `una actividad antigua sin creador avisa a todos`() {
        val antigua = need(creador = null)
        assertTrue(debeAvisarA(antigua, TipoAviso.LLEVAR, luis, emptyList()))
        assertTrue(debeAvisarA(antigua, TipoAviso.RECOGER, marta, emptyList()))
    }

    @Test
    fun `el creador recibe siempre los avisos aunque no lleve ni recoja`() {
        val n = need(lleva = "luis", recoge = "luis", creador = "ana")
        assertTrue(debeAvisarA(n, TipoAviso.LLEVAR, ana, emptyList()))
        assertTrue(debeAvisarA(n, TipoAviso.RECOGER, ana, emptyList()))
    }

    @Test
    fun `quien lleva recibe el aviso de llevar pero no el de recoger`() {
        val n = need(lleva = "luis", recoge = "marta", creador = "ana")
        assertTrue(debeAvisarA(n, TipoAviso.LLEVAR, luis, emptyList()))
        assertFalse(debeAvisarA(n, TipoAviso.RECOGER, luis, emptyList()))
    }

    @Test
    fun `quien recoge recibe el aviso de recoger pero no el de llevar`() {
        val n = need(lleva = "luis", recoge = "marta", creador = "ana")
        assertTrue(debeAvisarA(n, TipoAviso.RECOGER, marta, emptyList()))
        assertFalse(debeAvisarA(n, TipoAviso.LLEVAR, marta, emptyList()))
    }

    @Test
    fun `un tercero sin relacion no recibe ningun aviso`() {
        val n = need(lleva = "luis", recoge = "marta", creador = "ana")
        val otro = CaregiverId("pedro")
        assertFalse(debeAvisarA(n, TipoAviso.LLEVAR, otro, emptyList()))
        assertFalse(debeAvisarA(n, TipoAviso.RECOGER, otro, emptyList()))
    }

    @Test
    fun `sin nadie asignado solo avisa al creador`() {
        val n = need(creador = "ana")
        assertTrue(debeAvisarA(n, TipoAviso.LLEVAR, ana, emptyList()))
        assertFalse(debeAvisarA(n, TipoAviso.LLEVAR, luis, emptyList()))
        assertFalse(debeAvisarA(n, TipoAviso.RECOGER, luis, emptyList()))
    }

    @Test
    fun `si lo asignado es una unidad familiar avisa a todos sus miembros`() {
        val n = need(lleva = "u1", creador = "ana")
        val unidades = listOf(parejaLuisMarta)
        assertTrue(debeAvisarA(n, TipoAviso.LLEVAR, luis, unidades))
        assertTrue(debeAvisarA(n, TipoAviso.LLEVAR, marta, unidades))
        assertFalse(debeAvisarA(n, TipoAviso.LLEVAR, CaregiverId("pedro"), unidades))
    }

    @Test
    fun `la unidad asignada a llevar no recibe el aviso de recoger`() {
        val n = need(lleva = "u1", creador = "ana")
        assertFalse(debeAvisarA(n, TipoAviso.RECOGER, luis, listOf(parejaLuisMarta)))
    }

    @Test
    fun `un id de unidad que no existe en la lista no avisa a nadie por ella`() {
        val n = need(lleva = "u1", creador = "ana")
        assertFalse(debeAvisarA(n, TipoAviso.LLEVAR, luis, emptyList()))
    }
}
