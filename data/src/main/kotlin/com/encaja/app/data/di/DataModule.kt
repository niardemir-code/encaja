package com.encaja.app.data.di

import android.content.Context
import androidx.room.Room
import com.encaja.app.data.local.*
import com.encaja.app.data.repository.*
import com.encaja.app.domain.repository.*
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun proveerFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun proveerBaseDeDatos(@ApplicationContext context: Context): EncajaDatabase =
        Room.databaseBuilder(context, EncajaDatabase::class.java, "encaja.db").build()

    @Provides
    fun proveerCaregiverDao(db: EncajaDatabase): CaregiverDao = db.caregiverDao()

    @Provides
    fun proveerCoverageNeedDao(db: EncajaDatabase): CoverageNeedDao = db.coverageNeedDao()

    @Provides
    fun proveerAvailabilityDao(db: EncajaDatabase): AvailabilityDao = db.availabilityDao()

    @Provides
    fun proveerAssignmentDao(db: EncajaDatabase): AssignmentDao = db.assignmentDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun enlazarCaregiverRepository(impl: CaregiverRepositoryImpl): CaregiverRepository

    @Binds
    abstract fun enlazarCoverageNeedRepository(impl: CoverageNeedRepositoryImpl): CoverageNeedRepository

    @Binds
    abstract fun enlazarAvailabilityRepository(impl: AvailabilityRepositoryImpl): AvailabilityRepository

    @Binds
    abstract fun enlazarAssignmentRepository(impl: AssignmentRepositoryImpl): AssignmentRepository
}