package com.encaja.app.ui.auth

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ReglasPasswordTest {

    @Test
    fun `123456 no cumple: es corta, no tiene letra y es habitual`() {
        val r = evaluarPassword("123456")
        assertFalse(r.longitud)
        assertFalse(r.letra)
        assertFalse(r.cumple)
    }

    @Test
    fun `una contrasena con 8 caracteres, letra y numero cumple`() {
        assertTrue(evaluarPassword("sol2mar7").cumple)
    }

    @Test
    fun `sin numero no cumple`() {
        val r = evaluarPassword("solymarbonito")
        assertTrue(r.longitud)
        assertTrue(r.letra)
        assertFalse(r.numero)
        assertFalse(r.cumple)
    }

    @Test
    fun `sin letra no cumple aunque sea larga`() {
        assertFalse(evaluarPassword("9182736450").cumple)
    }

    @Test
    fun `las contrasenas habituales se rechazan aunque cumplan lo basico`() {
        val r = evaluarPassword("Password123")
        assertTrue(r.basicosCumplidos)
        assertFalse(r.noHabitual)
        assertFalse(r.cumple)
    }

    @Test
    fun `casi todo el mismo caracter se rechaza`() {
        assertFalse(evaluarPassword("aaaaaaa1").cumple)
    }

    @Test
    fun `no se exigen mayusculas ni simbolos`() {
        assertTrue(evaluarPassword("casita2024").cumple)
    }
}
