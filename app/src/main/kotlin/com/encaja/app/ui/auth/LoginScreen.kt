package com.encaja.app.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.encaja.app.R
import com.encaja.app.ui.common.resolver
import kotlinx.coroutines.launch

// NOTA: depende de Jetpack Compose (Material 3), no compilado en este entorno.
@Composable
fun LoginScreen(
    onLoginExitoso: () -> Unit,
    modoRegistroInicial: Boolean = false,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var verPassword by remember { mutableStateOf(false) }
    var modoRegistro by remember { mutableStateOf(modoRegistroInicial) }

    val colores = MaterialTheme.colorScheme

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to colores.primaryContainer.copy(alpha = 0.75f),
                    0.45f to colores.background,
                    1f to colores.background
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            // Logo + nombre
            Image(
                painter = painterResource(R.drawable.logo_encaja),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.height(80.dp).aspectRatio(640f / 576f)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.app_wordmark),
                color = colores.primary,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                stringResource(R.string.login_lema),
                color = colores.onBackground.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            // Tarjeta del formulario
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = colores.surface,
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.pendienteVerificar != null) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.login_verifica_titulo),
                            color = colores.onSurface,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.login_verifica_texto, uiState.pendienteVerificar!!),
                            color = colores.onSurface.copy(alpha = 0.75f),
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        listOf(R.string.login_verifica_paso1, R.string.login_verifica_paso2, R.string.login_verifica_paso3)
                            .forEachIndexed { i, paso ->
                                Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = CircleShape, color = colores.primary, modifier = Modifier.size(22.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("${i + 1}", color = colores.onPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Text(stringResource(paso), color = colores.onSurface, fontSize = 14.sp)
                                }
                            }
                        uiState.error?.let {
                            Spacer(Modifier.height(10.dp))
                            Text(it.resolver(), color = colores.error, fontSize = 13.sp)
                        }
                        uiState.info?.let {
                            Spacer(Modifier.height(10.dp))
                            Text(it.resolver(), color = colores.primary, fontSize = 13.sp)
                        }
                        var espera by remember { mutableStateOf(30) }
                        LaunchedEffect(espera) {
                            if (espera > 0) { kotlinx.coroutines.delay(1000); espera -= 1 }
                        }
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = { viewModel.comprobarVerificado(onLoginExitoso) },
                            enabled = !uiState.cargando,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) {
                            if (uiState.cargando) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Text(stringResource(R.string.login_verifica_hecho), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { viewModel.reenviarVerificacion(); espera = 30 },
                            enabled = !uiState.cargando && espera == 0,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) {
                            Text(
                                if (espera > 0) stringResource(R.string.login_verifica_reenviar_en, espera)
                                else stringResource(R.string.login_verifica_reenviar),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TextButton(
                            onClick = { viewModel.cancelarVerificacion() },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) { Text(stringResource(R.string.login_verifica_otra), fontWeight = FontWeight.Bold) }
                    }
                } else {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        stringResource(if (modoRegistro) R.string.login_titulo_registro else R.string.login_titulo_entrar),
                        color = colores.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        stringResource(if (modoRegistro) R.string.login_sub_registro else R.string.login_sub_entrar),
                        color = colores.onSurface.copy(alpha = 0.65f),
                        fontSize = 13.sp
                    )
                    // Si en la intro se escribió un código de invitación, se explica que ya está
                    // guardado y que se canjea solo al entrar o crear la cuenta.
                    val codigoGuardado = remember { com.encaja.app.ui.intro.PreferenciaIntro.codigoPendiente(context) }
                    if (codigoGuardado != null) {
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = colores.primaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                stringResource(R.string.login_codigo_guardado, codigoGuardado),
                                color = colores.onSurface,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(stringResource(R.string.login_correo)) },
                        leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.login_password)) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { verPassword = !verPassword }) {
                                Icon(
                                    if (verPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = stringResource(if (verPassword) R.string.login_ocultar_password else R.string.login_mostrar_password)
                                )
                            }
                        },
                        visualTransformation = if (verPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Al crear la cuenta, los requisitos de la contraseña se van marcando mientras se escribe.
                    val requisitos = evaluarPassword(password)
                    if (modoRegistro) {
                        Spacer(Modifier.height(8.dp))
                        RequisitoPassword(stringResource(R.string.login_pw_req_longitud), requisitos.longitud)
                        RequisitoPassword(stringResource(R.string.login_pw_req_letra), requisitos.letra)
                        RequisitoPassword(stringResource(R.string.login_pw_req_numero), requisitos.numero)
                        if (requisitos.basicosCumplidos && !requisitos.noHabitual) {
                            Spacer(Modifier.height(4.dp))
                            Text(stringResource(R.string.login_pw_habitual), color = colores.error, fontSize = 13.sp)
                        }
                    }

                    if (!modoRegistro) {
                        TextButton(
                            onClick = { viewModel.recuperarPassword(email) },
                            enabled = !uiState.cargando,
                            modifier = Modifier.align(Alignment.End)
                        ) { Text(stringResource(R.string.login_olvide), fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    }
                    if (uiState.error != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(uiState.error!!.resolver(), color = colores.error, fontSize = 13.sp)
                    }
                    uiState.info?.let {
                        Spacer(Modifier.height(10.dp))
                        Text(it.resolver(), color = colores.primary, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (modoRegistro) {
                                viewModel.registrarse(email, password)
                            } else {
                                viewModel.iniciarSesion(email, password, onLoginExitoso)
                            }
                        },
                        enabled = !uiState.cargando && email.isNotBlank() && password.isNotBlank() &&
                            (!modoRegistro || requisitos.cumple),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) {
                        if (uiState.cargando) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text(
                                stringResource(if (modoRegistro) R.string.login_crear_cuenta else R.string.login_entrar),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HorizontalDivider(Modifier.weight(1f))
                        Text(
                            stringResource(R.string.login_o),
                            modifier = Modifier.padding(horizontal = 10.dp),
                            color = colores.onSurface.copy(alpha = 0.55f),
                            fontSize = 13.sp
                        )
                        HorizontalDivider(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                iniciarSesionConGoogle(context, viewModel, onLoginExitoso)
                            }
                        },
                        enabled = !uiState.cargando,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) {
                        Text(stringResource(R.string.login_google), fontWeight = FontWeight.Bold)
                    }
                }
                }
            }

            Spacer(Modifier.height(4.dp))
            TextButton(onClick = { modoRegistro = !modoRegistro }) {
                Text(
                    stringResource(if (modoRegistro) R.string.login_cambiar_a_entrar else R.string.login_cambiar_a_registro),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Una línea de la lista de requisitos de la contraseña: marcada en verde cuando se cumple. */
@Composable
private fun RequisitoPassword(texto: String, cumplido: Boolean) {
    val colores = MaterialTheme.colorScheme
    Row(Modifier.padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (cumplido) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (cumplido) androidx.compose.ui.graphics.Color(0xFF2E9E5B) else colores.onSurface.copy(alpha = 0.45f),
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            texto,
            color = if (cumplido) colores.onSurface else colores.onSurface.copy(alpha = 0.65f),
            fontSize = 13.sp
        )
    }
}
