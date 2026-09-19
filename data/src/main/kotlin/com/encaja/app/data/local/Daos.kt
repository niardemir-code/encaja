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
}

@Dao
interface CoverageNeedDao {
    @Query("SELECT * FROM coverage_needs WHERE familyId = :familyId AND fecha BETWEEN :desde AND :hasta")
    suspend fun obtener(familyId: String, desde: String, hasta: String): List<CoverageNeedEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(needs: List<CoverageNeedEntity>)
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
interface AssignmentDao {
    @Query("SELECT * FROM patrones_cuidado WHERE familyId = :familyId")
    suspend fun obtenerPatrones(familyId: String): List<PatronCuidadoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarPatrones(patrones: List<PatronCuidadoEntity>)

    @Query("SELECT * FROM anulaciones WHERE familyId = :familyId AND fecha BETWEEN :desde AND :hasta")
    suspend fun obtenerAnulaciones(familyId: String, desde: String, hasta: String): List<AnulacionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarAnulacion(anulacion: AnulacionEntity)
}

@Dao
interface MenuDao {
    @Query("SELECT * FROM menus WHERE familyId = :familyId AND fecha BETWEEN :desde AND :hasta")
    suspend fun obtener(familyId: String, desde: String, hasta: String): List<ComidaDelDiaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTodos(dias: List<ComidaDelDiaEntity>)
}