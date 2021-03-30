package com.example.runningappdemo.di

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.runningappdemo.db.RunDao
import com.example.runningappdemo.db.RunningDatabase
import com.example.runningappdemo.util.Constants.KEY_FIRST_TIME_TOGGLE
import com.example.runningappdemo.util.Constants.KEY_NAME
import com.example.runningappdemo.util.Constants.KEY_WEIGHT
import com.example.runningappdemo.util.Constants.RUNNING_DATABASE_NAME
import com.example.runningappdemo.util.Constants.SHARED_PREFERENCES_NAME
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

/**
 * 1.the container of dependencies
 * 2.the manual to tell dagger how to create objects (dependencies)
 */
@Module
@InstallIn(ApplicationComponent::class) // scope to whole application
object AppModule {
    @Singleton
    @Provides
    fun provideRunningDatabase(
            @ApplicationContext app: Context
    ) = Room.databaseBuilder(
            app,
            RunningDatabase::class.java,
            RUNNING_DATABASE_NAME
    ).build()



    @Singleton
    @Provides
    fun provideRunDao(db: RunningDatabase) = db.getRunDao()


    @Singleton
    @Provides
    fun provideSharedPreferences(
            @ApplicationContext app: Context
    ) = app.getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE)


    @Singleton
    @Provides
    fun provideName(
            sharedPref: SharedPreferences
    ) = sharedPref.getString(KEY_NAME, "") ?: "" // may return null


    @Singleton
    @Provides
    fun provideWeight(
            sharedPref: SharedPreferences
    ) = sharedPref.getFloat(KEY_WEIGHT, 80f)


    @Singleton
    @Provides
    fun provideFirstTimeToggle(
            sharedPref: SharedPreferences
    ) = sharedPref.getBoolean(KEY_FIRST_TIME_TOGGLE, true)

}



