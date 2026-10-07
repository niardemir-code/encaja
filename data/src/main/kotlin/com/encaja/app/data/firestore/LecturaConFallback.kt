package com.encaja.app.data.firestore

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/** Plazo máximo para esperar al servidor antes de recurrir a la caché local de Firestore. */
private const val PLAZO_SERVIDOR_MS = 6_000L

/**
 * Lee del servidor, pero sin quedarse esperando para siempre sin conexión: si no responde a
 * tiempo (o falla), devuelve lo que haya en la caché local de Firestore. Así las pantallas
 * cargan con los últimos datos conocidos en modo avión en vez de quedarse con la rueda.
 */
suspend fun Query.getConFallback(): QuerySnapshot =
    try {
        withTimeout(PLAZO_SERVIDOR_MS) { get().await() }
    } catch (e: kotlinx.coroutines.CancellationException) {
        // withTimeout lanza una TimeoutCancellationException: si es solo el plazo, se usa caché.
        if (e is kotlinx.coroutines.TimeoutCancellationException) get(Source.CACHE).await() else throw e
    } catch (e: Exception) {
        get(Source.CACHE).await()
    }

suspend fun DocumentReference.getConFallback(): DocumentSnapshot =
    try {
        withTimeout(PLAZO_SERVIDOR_MS) { get().await() }
    } catch (e: kotlinx.coroutines.CancellationException) {
        if (e is kotlinx.coroutines.TimeoutCancellationException) get(Source.CACHE).await() else throw e
    } catch (e: Exception) {
        get(Source.CACHE).await()
    }
