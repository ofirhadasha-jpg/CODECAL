package com.vmm.manager.di

import android.content.Context
import androidx.room.Room
import com.vmm.manager.data.db.VmmDatabase
import com.vmm.manager.data.remote.PricezApiService
import com.vmm.manager.data.remote.SnaplistApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): VmmDatabase =
        Room.databaseBuilder(ctx, VmmDatabase::class.java, "vmm_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideProductDao(db: VmmDatabase) = db.productDao()
    @Provides fun provideMachineDao(db: VmmDatabase) = db.machineDao()
    @Provides fun provideTransactionDao(db: VmmDatabase) = db.transactionDao()

    @Provides @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    @Provides @Singleton @Named("pricez")
    fun providePricezRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://www.pricez.co.il/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides @Singleton @Named("snaplist")
    fun provideSnaplistRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://app.snaplist.one/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides @Singleton
    fun providePricezApi(@Named("pricez") r: Retrofit): PricezApiService =
        r.create(PricezApiService::class.java)

    @Provides @Singleton
    fun provideSnaplistApi(@Named("snaplist") r: Retrofit): SnaplistApiService =
        r.create(SnaplistApiService::class.java)
}
