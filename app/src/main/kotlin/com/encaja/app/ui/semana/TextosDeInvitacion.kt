package com.encaja.app.ui.semana

import com.encaja.app.R
import com.encaja.app.domain.model.ErrorDeInvitacion
import com.encaja.app.domain.model.comoErrorDeInvitacion
import com.encaja.app.ui.common.TextoUi
import com.encaja.app.ui.common.textoUi

/**
 * El mensaje, en el idioma del móvil, de un fallo al generar o canjear un código de invitación.
 * Si el fallo no es de los conocidos (red, permisos...) se muestra [generico].
 */
fun Throwable.textoDeErrorDeInvitacion(generico: TextoUi): TextoUi = when (comoErrorDeInvitacion()) {
    ErrorDeInvitacion.CuidadorYaVinculado -> textoUi(R.string.invitacion_err_cuidador_vinculado)
    ErrorDeInvitacion.NoSeGeneroCodigo -> textoUi(R.string.invitacion_err_sin_codigo)
    ErrorDeInvitacion.CodigoNoExiste -> textoUi(R.string.invitacion_err_no_existe)
    ErrorDeInvitacion.CodigoYaUsado -> textoUi(R.string.invitacion_err_usado)
    ErrorDeInvitacion.CodigoCaducado -> textoUi(R.string.invitacion_err_caducado)
    ErrorDeInvitacion.CodigoIncompleto -> textoUi(R.string.invitacion_err_incompleto)
    null -> generico
}
