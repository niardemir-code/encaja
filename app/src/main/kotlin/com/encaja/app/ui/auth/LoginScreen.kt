package com.encaja.app.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
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

                    if (uiState.error != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(uiState.error!!.resolver(), color = colores.error, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (modoRegistro) {
                                viewModel.registrarse(email, password, onLoginExitoso)
                            } else {
                                viewModel.iniciarSesion(email, password, onLoginExitoso)
                            }
                        },
                        enabled = !uiState.cargando && email.isNotBlank() && password.isNotBlank(),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
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
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(stringResource(R.string.login_google), fontWeight = FontWeight.Bold)
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
