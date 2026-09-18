package com.encaja.app.ui.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.encaja.app.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Abre el selector de cuentas de Google (Credential Manager, la API
 * moderna — no el antiguo GoogleSignInClient). Si el usuario elige una
 * cuenta, extrae el idToken y se lo pasa al ViewModel para completar el
 * inicio de sesión con Firebase.
 *
 * TEMPORAL: mientras depuramos, cualquier fallo se imprime en Logcat
 * con la etiqueta "GoogleSignIn" en vez de tragarse en silencio.
 */
suspend fun iniciarSesionConGoogle(
    context: Context,
    viewModel: LoginViewModel,
    onExito: () -> Unit
) {
    try {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(context.getString(R.string.default_web_client_id))
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credentialManager = CredentialManager.create(context)
        val result = credentialManager.getCredential(context, request)

        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
        viewModel.iniciarSesionConGoogle(googleIdTokenCredential.idToken, onExito)
    } catch (e: GetCredentialException) {
        Log.e("GoogleSignIn", "Fallo de Credential Manager: ${e.type} — ${e.message}", e)
    } catch (e: Exception) {
        Log.e("GoogleSignIn", "Fallo inesperado", e)
    }
}