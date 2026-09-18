package com.encaja.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        CaregiverEntity::class,
        CoverageNeedEntity::class,
        AvailabilityBlockEntity::class,
        PatronCuidadoEntity::class,
        AnulacionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class EncajaDatabase : RoomDatabase() {
    abstract fun caregiverDao(): CaregiverDao
    abstract fun coverageNeedDao(): CoverageNeedDao
    abstract fun availabilityDao(): AvailabilityDao
    abstract fun assignmentDao(): AssignmentDao
}