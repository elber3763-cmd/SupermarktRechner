package com.einkaufsscanner.di

import android.content.Context
import com.einkaufsscanner.data.camera.CameraManager
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
    fun provideShoppingCartRepository(): ShoppingCartRepository {
        return ShoppingCartRepository()
    }

    @Singleton
    @Provides
    fun provideCameraManager(
        @ApplicationContext context: Context,
    ): CameraManager {
        return CameraManager(context)
    }
}
