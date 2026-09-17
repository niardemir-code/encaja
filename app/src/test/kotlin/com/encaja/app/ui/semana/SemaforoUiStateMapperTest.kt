package com.encaja.app.ui.semana

import com.encaja.app.domain.model.CaregiverId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SemaforoUiStateMapperTest {

    private val d = DatosEjemploFamilia
    private val mapper = SemaforoUiStateMapper(d.caregivers, d.patrones, d.anulaciones, d.disponibilidad)
    private val estado = mapper.construir(d.lunes, d.needsDeLaSemana)

    @Test
    fun `lunes queda en verde porque no hay hueco`() {
        assertEquals(EstadoDia.VERDE, estado.dias[0].estado)
    }

    @Test
    fun `martes queda en rojo por el futbol de Etna sin cubrir`() {
        assertEquals(EstadoDia.ROJO, estado.dias[1].estado)
        assertEquals(1, estado.dias[1].huecos.size)
        assertEquals("Fútbol de Etna", estado.dias[1].huecos.first().need.descripcion)
    }

    @Test
    fun `miercoles queda en verde gracias a la anulacion manual de Silvia`() {
        assertEquals(EstadoDia.VERDE, estado.dias[2].estado)
    }

    @Test
    fun `jueves queda en verde, cubierto por Josefa segun el patron`() {
        assertEquals(EstadoDia.VERDE, estado.dias[3].estado)
    }

    @Test
    fun `los dias sin necesidades registradas quedan SIN_DATOS`() {
        assertEquals(EstadoDia.SIN_DATOS, estado.dias[4].estado) // viernes, sin needs de ejemplo
        assertEquals(EstadoDia.SIN_DATOS, estado.dias[5].estado) // sábado
        assertEquals(EstadoDia.SIN_DATOS, estado.dias[6].estado) // domingo
    }

    @Test
    fun `hay exactamente un hueco en toda la semana`() {
        assertEquals(1, estado.huecosDeLaSemana.size)
    }

    @Test
    fun `el reparto solo incluye a quien tiene tramos asignados esta semana`() {
        val ids = estado.reparto.map { it.caregiverId }
        assertTrue(ids.contains(CaregiverId("josefa")))
        assertTrue(ids.contains(CaregiverId("vicente")))
        assertFalse(ids.contains(CaregiverId("dolors"))) // Dolors no tiene tramos esta semana en el ejemplo
    }

    @Test
    fun `el total de tramos coincide con la suma del reparto`() {
        val sumaManual = estado.reparto.sumOf { it.tramos }
        assertEquals(sumaManual, estado.totalTramos)
    }
}
