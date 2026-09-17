package com.encaja.app.domain.model

/**
 * Identificadores tipados para evitar pasar un String de un tipo
 * donde se espera el de otro (ej. un childId donde toca un caregiverId).
 * Es deliberadamente ligero: un wrapper sobre String, sin lógica.
 */
data class CaregiverId(val value: String)
data class ChildId(val value: String)
data class FamilyId(val value: String)
data class CoverageNeedId(val value: String)
