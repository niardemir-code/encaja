package com.encaja.app.data.di

import android.content.Context
import androidx.room.Room
import com.encaja.app.data.local.*
import com.encaja.app.data.repository.*
import com.encaja.app.domain.repository.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// NOTA: depende de Hilt, Room y Firebase; no compilado en este entorno.

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun proveerFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun proveerFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun proveerBaseDeDatos(@ApplicationContext context: Context): EncajaDatabase =
        Room.databaseBuilder(context, EncajaDatabase::class.java, "encaja.db")
            // La base de datos es solo caché local (la fuente de verdad es Firestore),
            // así que ante un cambio de esquema es más simple recrearla que migrar.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun proveerCaregiverDao(db: EncajaDatabase): CaregiverDao = db.caregiverDao()

    @Provides
    fun proveerCoverageNeedDao(db: EncajaDatabase): CoverageNeedDao = db.coverageNeedDao()

    @Provides
    fun proveerAvailabilityDao(db: EncajaDatabase): AvailabilityDao = db.availabilityDao()

    @Provides
    fun proveerAssignmentDao(db: EncajaDatabase): AssignmentDao = db.assignmentDao()

    @Provides
    fun proveerMenuDao(db: EncajaDatabase): MenuDao = db.menuDao()

    @Provides
    fun proveerChildDao(db: EncajaDatabase): ChildDao = db.childDao()
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

    @Binds
    abstract fun enlazarAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    abstract fun enlazarFamilyMembershipRepository(impl: FamilyMembershipRepositoryImpl): FamilyMembershipRepository

    @Binds
    abstract fun enlazarInviteRepository(impl: InviteRepositoryImpl): InviteRepository

    @Binds
    abstract fun enlazarMenuRepository(impl: MenuRepositoryImpl): MenuRepository

    @Binds
    abstract fun enlazarChildRepository(impl: ChildRepositoryImpl): ChildRepository
}
