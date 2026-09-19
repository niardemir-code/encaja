package com.encaja.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.encaja.app.domain.model.*

@Entity(tableName = "caregivers", primaryKeys = ["familyId", "id"])
data class CaregiverEntity(
    val familyId: String,
    val id: String,
    val nombre: String,
    val rol: String,
    val puedeDesplazarse: Boolean
) {
    fun aDominio() = Caregiver(
        CaregiverId(id), nombre, CaregiverRole.valueOf(rol), puedeDesplazarse
    )

    companion object {
        fun desdeDominio(familyId: String, caregiver: Caregiver) = CaregiverEntity(
            familyId, caregiver.id.value, caregiver.nombre, caregiver.rol.name, caregiver.puedeDesplazarse
        )
    }
}

@Entity(tableName = "coverage_needs", primaryKeys = ["familyId", "id"])
data class CoverageNeedEntity(
    val familyId: String,
    val id: String,
    val childId: String,
    val fecha: String,
    val horaInicio: String,
    val horaFin: String,
    val descripcion: String,
    val requiereDesplazamiento: Boolean
) {
    fun aDominio() = CoverageNeed(
        CoverageNeedId(id), ChildId(childId), java.time.LocalDate.parse(fecha),
        java.time.LocalTime.parse(horaInicio), java.time.LocalTime.parse(horaFin),
        descripcion, requiereDesplazamiento
    )

    companion object {
        fun desdeDominio(familyId: String, need: CoverageNeed) = CoverageNeedEntity(
            familyId, need.id.value, need.childId.value, need.fecha.toString(),
            need.horaInicio.toString(), need.horaFin.toString(), need.descripcion, need.requiereDesplazamiento
        )
    }
}

@Entity(tableName = "availability_blocks", primaryKeys = ["familyId", "caregiverId", "fecha", "horaInicio"])
data class AvailabilityBlockEntity(
    val familyId: String,
    val caregiverId: String,
    val fecha: String,
    val horaInicio: String,
    val horaFin: String,
    val motivo: String,
    val etiqueta: String?
) {
    fun aDominio() = AvailabilityBlock(
        CaregiverId(caregiverId), java.time.LocalDate.parse(fecha),
        java.time.LocalTime.parse(horaInicio), java.time.LocalTime.parse(horaFin),
        MotivoNoDisponibilidad.valueOf(motivo), etiqueta
    )

    companion object {
        fun desdeDominio(familyId: String, bloque: AvailabilityBlock) = AvailabilityBlockEntity(
            familyId, bloque.caregiverId.value, bloque.fecha.toString(),
            bloque.horaInicio.toString(), bloque.horaFin.toString(), bloque.motivo.name, bloque.etiqueta
        )
    }
}

@Entity(tableName = "patrones_cuidado", primaryKeys = ["familyId", "diaSemana"])
data class PatronCuidadoEntity(
    val familyId: String,
    val diaSemana: String,
    val caregiverId: String
) {
    fun aDominio() = PatronCuidado(java.time.DayOfWeek.valueOf(diaSemana), CaregiverId(caregiverId))

    companion object {
        fun desdeDominio(familyId: String, patron: PatronCuidado) = PatronCuidadoEntity(
            familyId, patron.diaSemana.name, patron.caregiverId.value
        )
    }
}

@Entity(tableName = "anulaciones", primaryKeys = ["familyId", "fecha"])
data class AnulacionEntity(
    val familyId: String,
    val fecha: String,
    val caregiverId: String
)

@Entity(tableName = "children", primaryKeys = ["familyId", "id"])
data class ChildEntity(
    val familyId: String,
    val id: String,
    val nombre: String
) {
    fun aDominio() = Child(ChildId(id), nombre)

    companion object {
        fun desdeDominio(familyId: String, child: Child) = ChildEntity(
            familyId, child.id.value, child.nombre
        )
    }
}

@Entity(tableName = "articulos_compra", primaryKeys = ["familyId", "id"])
data class ArticuloCompraEntity(
    val familyId: String,
    val id: String,
    val nombre: String,
    val tienda: String,
    val comprado: Boolean
) {
    fun aDominio() = ArticuloCompra(ArticuloCompraId(id), nombre, tienda, comprado)

    companion object {
        fun desdeDominio(familyId: String, articulo: ArticuloCompra) = ArticuloCompraEntity(
            familyId, articulo.id.value, articulo.nombre, articulo.tienda, articulo.comprado
        )
    }
}

@Entity(tableName = "menus", primaryKeys = ["familyId", "fecha"])
data class ComidaDelDiaEntity(
    val familyId: String,
    val fecha: String,
    val comida: String?,
    val cena: String?
) {
    fun aDominio() = ComidaDelDia(java.time.LocalDate.parse(fecha), comida, cena)

    companion object {
        fun desdeDominio(familyId: String, comidaDelDia: ComidaDelDia) = ComidaDelDiaEntity(
            familyId, comidaDelDia.fecha.toString(), comidaDelDia.comida, comidaDelDia.cena
        )
    }
}