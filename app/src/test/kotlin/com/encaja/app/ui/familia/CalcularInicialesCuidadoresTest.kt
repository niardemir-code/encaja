package com.encaja.app.ui.familia

import com.encaja.app.domain.model.Caregiver
import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.CaregiverRole
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CalcularInicialesCuidadoresTest {

    @Test
    fun `sin colisiones, cada cuidador recibe nombre mas primer apellido`() {
        val victor = Caregiver(CaregiverId("victor"), "Víctor", "Oliver", "Vila", CaregiverRole.ADMIN)
        val silvia = Caregiver(CaregiverId("silvia"), "Sílvia", "Izquierdo", "Camps", CaregiverRole.ADMIN)

        val iniciales = calcularInicialesCuidadores(listOf(victor, silvia))

        assertEquals("VO", iniciales[victor.id])
        assertEquals("SI", iniciales[silvia.id])
    }

    @Test
    fun `colision de nombre y primer apellido se desambigua por segundo apellido`() {
        // Ejemplo exacto planteado por el usuario:
        // "Vicente Oliver Fortea" -> "VO", "Víctor Oliver Vila" -> "VV"
        val vicente = Caregiver(CaregiverId("vicente"), "Vicente", "Oliver", "Fortea", CaregiverRole.CUIDADOR)
        val victor = Caregiver(CaregiverId("victor"), "Víctor", "Oliver", "Vila", CaregiverRole.ADMIN)

        val iniciales = calcularInicialesCuidadores(listOf(vicente, victor))

        assertEquals("VO", iniciales[vicente.id])
        assertEquals("VV", iniciales[victor.id])
    }

    @Test
    fun `tres cuidadores con la misma colision, solo el ultimo alfabeticamente cambia`() {
        val a = Caregiver(CaregiverId("a"), "Ana", "Ruiz", "Alba", CaregiverRole.CUIDADOR)
        val b = Caregiver(CaregiverId("b"), "Ana", "Ruiz", "Marín", CaregiverRole.CUIDADOR)
        val c = Caregiver(CaregiverId("c"), "Ana", "Ruiz", "Zamora", CaregiverRole.CUIDADOR)

        val iniciales = calcularInicialesCuidadores(listOf(a, b, c))

        assertEquals("AR", iniciales[a.id])
        assertEquals("AR", iniciales[b.id])
        assertEquals("AZ", iniciales[c.id])
    }

    @Test
    fun `cuidadores sin ninguna colision no se ven afectados por otro grupo`() {
        val victor = Caregiver(CaregiverId("victor"), "Víctor", "Oliver", "Vila", CaregiverRole.ADMIN)
        val vicente = Caregiver(CaregiverId("vicente"), "Vicente", "Oliver", "Fortea", CaregiverRole.CUIDADOR)
        val josefa = Caregiver(CaregiverId("josefa"), "Josefa", "Fernández", "Ruiz", CaregiverRole.CUIDADOR)

        val iniciales = calcularInicialesCuidadores(listOf(victor, vicente, josefa))

        assertEquals("JF", iniciales[josefa.id])
    }
}
