package com.example.runningappdemo

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/** to tell dagger that this application is injectable*/
@HiltAndroidApp
class BaseApplication : Application() {


    override fun onCreate() {
        /**
         * @InstallIn(ApplicationComponent::class)
         * all dependencies declare in module will create here
         */
        super.onCreate()

        // setup Timber
        Timber.plant(Timber.DebugTree())
    }
}