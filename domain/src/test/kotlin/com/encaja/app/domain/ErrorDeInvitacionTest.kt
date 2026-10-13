package com.encaja.app.domain

import com.encaja.app.domain.model.ErrorDeInvitacion
import com.encaja.app.domain.model.comoErrorDeInvitacion
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class ErrorDeInvitacionTest {

    @Test
    fun `un error de invitacion se reconoce directamente`() {
        assertSame(ErrorDeInvitacion.CodigoCaducado, ErrorDeInvitacion.CodigoCaducado.comoErrorDeInvitacion())
    }

    @Test
    fun `se reconoce aunque venga envuelto en otra excepcion`() {
        val envuelto = RuntimeException("transaccion fallida", ErrorDeInvitacion.CodigoYaUsado)
        assertSame(ErrorDeInvitacion.CodigoYaUsado, envuelto.comoErrorDeInvitacion())
    }

    @Test
    fun `se reconoce a varios niveles de profundidad`() {
        val envuelto = IllegalStateException("a", RuntimeException("b", ErrorDeInvitacion.CodigoNoExiste))
        assertSame(ErrorDeInvitacion.CodigoNoExiste, envuelto.comoErrorDeInvitacion())
    }

    @Test
    fun `una excepcion cualquiera no es un error de invitacion`() {
        assertNull(IllegalStateException("otra cosa").comoErrorDeInvitacion())
        assertNull(RuntimeException("x", java.io.IOException("red")).comoErrorDeInvitacion())
    }
}
