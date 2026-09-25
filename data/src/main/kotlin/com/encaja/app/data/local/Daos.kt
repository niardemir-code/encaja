package com.encaja.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CaregiverDao {
    @Query("SELECT * FROM caregivers WHERE familyId = :familyId")
    suspend fun obtener(familyId: String): List<CaregiverEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(caregivers: List<CaregiverEntity>)

    @Query("DELETE FROM caregivers WHERE familyId = :familyId AND id = :id")
    suspend fun eliminar(familyId: String, id: String)
}

@Dao
interface CoverageNeedDao {
    @Query("SELECT * FROM coverage_needs WHERE familyId = :familyId AND fecha BETWEEN :desde AND :hasta")
    suspend fun obtener(familyId: String, desde: String, hasta: String): List<CoverageNeedEntity>

    @Query("SELECT * FROM coverage_needs WHERE familyId = :familyId")
    suspend fun obtenerTodos(familyId: String): List<CoverageNeedEntity>

    @Query("SELECT * FROM coverage_needs WHERE familyId = :familyId AND grupoRepeticionId = :grupoRepeticionId")
    suspend fun obtenerPorGrupo(familyId: String, grupoRepeticionId: String): List<CoverageNeedEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(needs: List<CoverageNeedEntity>)

    @Query("DELETE FROM coverage_needs WHERE familyId = :familyId AND id = :id")
    suspend fun eliminar(familyId: String, id: String)

    @Query("DELETE FROM coverage_needs WHERE familyId = :familyId AND id IN (:ids)")
    suspend fun eliminarVarios(familyId: String, ids: List<String>)
}

@Dao
interface AvailabilityDao {
    @Query("SELECT * FROM availability_blocks WHERE familyId = :familyId AND fecha BETWEEN :desde AND :hasta")
    suspend fun obtener(familyId: String, desde: String, hasta: String): List<AvailabilityBlockEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(bloque: AvailabilityBlockEntity)

    @Query("DELETE FROM availability_blocks WHERE familyId = :familyId AND caregiverId = :caregiverId AND fecha = :fecha AND horaInicio = :horaInicio")
    suspend fun eliminar(familyId: String, caregiverId: String, fecha: String, horaInicio: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(bloques: List<AvailabilityBlockEntity>)
}

@Dao
interface FilaVisibleDao {
    @Query("SELECT * FROM filas_visibles WHERE familyId = :familyId AND lunes = :lunes")
    suspend fun obtener(familyId: String, lunes: String): FilaVisibleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(fila: FilaVisibleEntity)
}

@Dao
interface TurnoDao {
    @Query("SELECT * FROM turnos_trabajo WHERE familyId = :familyId ORDER BY horaInicio")
    suspend fun obtener(familyId: String): List<TurnoTrabajoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(turnos: List<TurnoTrabajoEntity>)

    @Query("DELETE FROM turnos_trabajo WHERE familyId = :familyId AND id = :id")
    suspend fun eliminar(familyId: String, id: String)
}

@Dao
interface CategoriaDao {
    @Query("SELECT * FROM categorias_disponibilidad WHERE familyId = :familyId")
    suspend fun obtener(familyId: String): List<CategoriaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodas(categorias: List<CategoriaEntity>)

    @Query("DELETE FROM categorias_disponibilidad WHERE familyId = :familyId AND id = :id")
    suspend fun eliminar(familyId: String, id: String)
}

@Dao
interface FamilyUnitDao {
    @Query("SELECT * FROM family_units WHERE familyId = :familyId")
    suspend fun obtener(familyId: String): List<FamilyUnitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(unidades: List<FamilyUnitEntity>)

    @Query("DELETE FROM family_units WHERE familyId = :familyId AND id = :id")
    suspend fun eliminar(familyId: String, id: String)
}

@Dao
interface ChildDao {
    @Query("SELECT * FROM children WHERE familyId = :familyId")
    suspend fun obtener(familyId: String): List<ChildEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(ninos: List<ChildEntity>)

    @Query("DELETE FROM children WHERE familyId = :familyId AND id = :id")
    suspend fun eliminar(familyId: String, id: String)
}

@Dao
interface CompraDao {
    @Query("SELECT * FROM articulos_compra WHERE familyId = :familyId")
    suspend fun obtener(familyId: String): List<ArticuloCompraEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(articulos: List<ArticuloCompraEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(articulo: ArticuloCompraEntity)

    @Query("DELETE FROM articulos_compra WHERE familyId = :familyId AND id = :id")
    suspend fun eliminar(familyId: String, id: String)
}

@Dao
interface AnuncioDao {
    @Query("SELECT * FROM anuncios WHERE familyId = :familyId ORDER BY publicadoEn DESC")
    suspend fun obtener(familyId: String): List<AnuncioEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(anuncio: AnuncioEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(anuncios: List<AnuncioEntity>)

    @Query("DELETE FROM anuncios WHERE familyId = :familyId AND id = :id")
    suspend fun eliminar(familyId: String, id: String)
}

@Dao
interface MenuDao {
    @Query("SELECT * FROM menus WHERE familyId = :familyId AND fecha BETWEEN :desde AND :hasta")
    suspend fun obtener(familyId: String, desde: String, hasta: String): List<ComidaDelDiaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(dias: List<ComidaDelDiaEntity>)
}