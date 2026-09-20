package com.encaja.app.ui.familia

import com.encaja.app.domain.model.Caregiver
import com.encaja.app.domain.model.CaregiverId

/**
 * Calcula las iniciales (2 letras) que se muestran como avatar de cada
 * cuidador. Normalmente son la inicial del nombre + la del primer
 * apellido. Cuando dos o más cuidadores comparten esa misma pareja de
 * iniciales (p.ej. "Vicente Oliver Fortea" y "Víctor Oliver Vila", ambos
 * "VO"), se desambigua: a todos menos al último (ordenando por segundo
 * apellido, ascendente) se les deja la inicial normal, y al último
 * (el de segundo apellido alfabéticamente más alto) se le da la inicial
 * del nombre + la del SEGUNDO apellido en su lugar. Así "Vicente Oliver
 * Fortea" sigue siendo "VO" y "Víctor Oliver Vila" pasa a ser "VV".
 *
 * Kotlin puro, sin dependencias de Android ni Compose, para poder
 * testear el algoritmo de desambiguación de forma aislada.
 */
fun calcularInicialesCuidadores(cuidadores: List<Caregiver>): Map<CaregiverId, String> {
    val resultado = mutableMapOf<CaregiverId, String>()

    val grupos = cuidadores.groupBy { inicial(it.nombre) + inicial(it.apellido1) }

    grupos.forEach { (inicialesBase, miembros) ->
        if (miembros.size == 1) {
            resultado[miembros.first().id] = inicialesBase
        } else {
            val ordenados = miembros.sortedBy { it.apellido2.lowercase() }
            ordenados.dropLast(1).forEach { cuidador ->
                resultado[cuidador.id] = inicialesBase
            }
            val ultimo = ordenados.last()
            resultado[ultimo.id] = inicial(ultimo.nombre) + inicial(ultimo.apellido2)
        }
    }

    return resultado
}

private fun inicial(texto: String): String =
    texto.trim().firstOrNull()?.uppercaseChar()?.toString() ?: ""
