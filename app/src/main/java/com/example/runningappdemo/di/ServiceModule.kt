package com.example.runningappdemo.di

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.runningappdemo.R
import com.example.runningappdemo.ui.MainActivity
import com.example.runningappdemo.util.Constants
import com.google.android.gms.location.FusedLocationProviderClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped

@Module
@InstallIn(ServiceComponent::class)
object ServiceModule {


    @ServiceScoped // only 1 copy of this dependencies during Service lifecycle
    @Provides
    fun provideFusedLocationProviderClient(
            @ApplicationContext app: Context
    ) = FusedLocationProviderClient(app)

    @ServiceScoped
    @Provides
    fun provideMainActivityPendingIntent( // #PendingIntent先發動至MainActivity再導航至TrackingFragment
            @ApplicationContext app: Context
    ) = PendingIntent.getActivity( // #getActivity: Retrieve a PendingIntent that will start a new activity
            app,
            0, // Private request code for the sender(we don't unique it)
            Intent(app, MainActivity::class.java).also { // launch MainActivity
                it.action = Constants.ACTION_SHOW_TRACKING_FRAGMENT
            },
            PendingIntent.FLAG_UPDATE_CURRENT
    )

    @ServiceScoped
    @Provides
    fun provideBaseNtificationBuilder(
            @ApplicationContext app: Context,
            pi: PendingIntent
    ) = NotificationCompat.Builder(app, Constants.NOTIFICATION_CHANNEL_ID)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_directions_run_black)
            .setContentTitle("Running App")
            .setContentText("00:00:00")
            .setContentIntent(pi)



}