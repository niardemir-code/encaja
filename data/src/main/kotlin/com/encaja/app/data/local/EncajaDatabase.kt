package com.encaja.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        CaregiverEntity::class,
        CoverageNeedEntity::class,
        AvailabilityBlockEntity::class,
        PatronCuidadoEntity::class,
        AnulacionEntity::class,
        ComidaDelDiaEntity::class,
        ChildEntity::class,
        ArticuloCompraEntity::class,
        AnuncioEntity::class,
        FamilyUnitEntity::class,
        TurnoTrabajoEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class EncajaDatabase : RoomDatabase() {
    abstract fun caregiverDao(): CaregiverDao
    abstract fun coverageNeedDao(): CoverageNeedDao
    abstract fun availabilityDao(): AvailabilityDao
    abstract fun assignmentDao(): AssignmentDao
    abstract fun menuDao(): MenuDao
    abstract fun childDao(): ChildDao
    abstract fun compraDao(): CompraDao
    abstract fun anuncioDao(): AnuncioDao
    abstract fun familyUnitDao(): FamilyUnitDao
    abstract fun turnoDao(): TurnoDao
}