package com.encaja.app.ui.guia

import com.encaja.app.domain.model.ChildId
import com.encaja.app.domain.model.CoverageNeed
import com.encaja.app.domain.model.CoverageNeedId
import com.encaja.app.domain.model.FamilyId
import com.encaja.app.domain.repository.CoverageNeedRepository
import com.encaja.app.domain.usecase.ObtenerSerieDeActividad
import com.encaja.app.domain.usecase.propagarASerie
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime

/** Repositorio en memoria: solo lo que usan las series (consultas por fecha y por grupo). */
private class RepositorioEnMemoria(private val needs: List<CoverageNeed>) : CoverageNeedRepository {
    override suspend fun obtenerNeeds(familyId: FamilyId, desde: LocalDate, hasta: LocalDate) =
        needs.filter { it.fecha in desde..hasta }
    override suspend fun guardarNeeds(familyId: FamilyId, needs: List<CoverageNeed>) = error("no se usa")
    override suspend fun eliminarNeed(familyId: FamilyId, needId: CoverageNeedId) = error("no se usa")
    override suspend fun eliminarNeeds(familyId: FamilyId, needIds: List<CoverageNeedId>) = error("no se usa")
    override suspend fun obtenerNeedsDelGrupo(familyId: FamilyId, grupoRepeticionId: String) =
        needs.filter { it.grupoRepeticionId == grupoRepeticionId }
    override suspend fun obtenerTodosLosNeeds(familyId: FamilyId) = needs
}

class SerieDeActividadesTest {

    private val familia = FamilyId("f")
    private val julia = ChildId("julia")
    private val etna = ChildId("etna")
    private val lunes = LocalDate.of(2026, 9, 21)

    private fun need(
        id: String,
        fecha: LocalDate,
        nombre: String = "Fútbol",
        nino: ChildId = julia,
        inicio: LocalTime = LocalTime.of(17, 0),
        fin: LocalTime = LocalTime.of(18, 0),
        grupo: String? = null
    ) = CoverageNeed(CoverageNeedId(id), nino, fecha, inicio, fin, nombre, grupoRepeticionId = grupo)

    private fun serie(vararg needs: CoverageNeed) = ObtenerSerieDeActividad(RepositorioEnMemoria(needs.toList()))

    @Test
    fun `con grupo devuelve todas las del grupo ordenadas por fecha`() = runBlocking {
        val a = need("a", lunes.plusWeeks(2), grupo = "g")
        val b = need("b", lunes, grupo = "g")
        val c = need("c", lunes.plusWeeks(1), grupo = "g")
        val resultado = serie(a, b, c)(familia, b)
        assertEquals(listOf("b", "c", "a"), resultado.map { it.id.value })
    }

    @Test
    fun `un grupo con una sola ocurrencia cae a la busqueda por actividades identicas`() = runBlocking {
        val a = need("a", lunes, grupo = "g")
        val suelta = need("s", lunes.plusWeeks(1)) // sin grupo, idéntica
        val resultado = serie(a, suelta)(familia, a)
        assertEquals(listOf("a", "s"), resultado.map { it.id.value })
    }

    @Test
    fun `sin grupo agrupa las identicas sin distinguir mayusculas ni espacios`() = runBlocking {
        val a = need("a", lunes, nombre = "Fútbol")
        val b = need("b", lunes.plusWeeks(1), nombre = "  fútbol ")
        val resultado = serie(a, b)(familia, a)
        assertEquals(listOf("a", "b"), resultado.map { it.id.value })
    }

    @Test
    fun `no mezcla otro nino, otro nombre ni otro horario`() = runBlocking {
        val a = need("a", lunes)
        val otroNino = need("n", lunes.plusWeeks(1), nino = etna)
        val otroNombre = need("m", lunes.plusWeeks(1), nombre = "Inglés")
        val otraHora = need("h", lunes.plusWeeks(1), inicio = LocalTime.of(16, 0))
        val resultado = serie(a, otroNino, otroNombre, otraHora)(familia, a)
        assertEquals(listOf("a"), resultado.map { it.id.value })
    }

    @Test
    fun `no recoge identicas de otro grupo distinto`() = runBlocking {
        val a = need("a", lunes, grupo = "g1")
        val deOtroGrupo = need("x", lunes.plusWeeks(1), grupo = "g2")
        val resultado = serie(a, deOtroGrupo)(familia, a)
        assertEquals(listOf("a"), resultado.map { it.id.value })
    }

    @Test
    fun `solo busca un anio antes y despues`() = runBlocking {
        val a = need("a", lunes)
        val dentro = need("d", lunes.plusYears(1))
        val fuera = need("f", lunes.plusYears(1).plusDays(1))
        val resultado = serie(a, dentro, fuera)(familia, a)
        assertEquals(listOf("a", "d"), resultado.map { it.id.value })
    }

    @Test
    fun `si la actividad no esta en el repositorio se incluye igualmente`() = runBlocking {
        val a = need("a", lunes)
        val resultado = serie()(familia, a)
        assertEquals(listOf(a), resultado)
    }

    @Test
    fun `una actividad unica no forma serie`() = runBlocking {
        val a = need("a", lunes)
        assertEquals(1, serie(a)(familia, a).size)
    }

    @Test
    fun `propagarASerie copia los cambios pero conserva id y fecha de cada una`() {
        val plantilla = need("p", lunes, nombre = "Natación", inicio = LocalTime.of(18, 0), fin = LocalTime.of(19, 0))
            .copy(avisoLlevarMin = 15)
        val existentes = listOf(need("a", lunes.plusWeeks(1)), need("b", lunes.plusWeeks(2)))
        val resultado = propagarASerie(plantilla, existentes)
        assertEquals(listOf("a", "b"), resultado.map { it.id.value })
        assertEquals(listOf(lunes.plusWeeks(1), lunes.plusWeeks(2)), resultado.map { it.fecha })
        assertTrue(resultado.all { it.descripcion == "Natación" && it.horaInicio == LocalTime.of(18, 0) && it.avisoLlevarMin == 15 })
    }
}
