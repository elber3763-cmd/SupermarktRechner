package com.einkaufsscanner.di

import android.content.Context
import androidx.room.Room
import com.einkaufsscanner.data.camera.CameraManager
import com.einkaufsscanner.data.database.ShoppingDatabase
import com.einkaufsscanner.data.repository.ShoppingCartRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideShoppingDatabase(
        @ApplicationContext context: Context
    ): ShoppingDatabase {
        return Room.databaseBuilder(
            context,
            ShoppingDatabase::class.java,
            "shopping_database"
        ).build()
    }

    @Singleton
    @Provides
    fun provideShoppingCartRepository(
        database: ShoppingDatabase
    ): ShoppingCartRepository {
        return ShoppingCartRepository(database)
    }

    @Singleton
    @Provides
    fun provideCameraManager(
        @ApplicationContext context: Context,
    ): CameraManager {
        return CameraManager(context)
    }
}
