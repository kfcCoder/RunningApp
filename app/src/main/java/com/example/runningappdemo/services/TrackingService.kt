package com.example.runningappdemo.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.example.runningappdemo.R
import com.example.runningappdemo.ui.MainActivity
import com.example.runningappdemo.util.Constants.ACTION_PAUSE_SERVICE
import com.example.runningappdemo.util.Constants.ACTION_SHOW_TRACKING_FRAGMENT
import com.example.runningappdemo.util.Constants.ACTION_START_OR_RESUME_SERVICE
import com.example.runningappdemo.util.Constants.ACTION_STOP_SERVICE
import com.example.runningappdemo.util.Constants.FASTEST_LOCATION_INTERVAL
import com.example.runningappdemo.util.Constants.LOCATION_UPDATE_INTERVAL
import com.example.runningappdemo.util.Constants.NOTIFICATION_CHANNEL_ID
import com.example.runningappdemo.util.Constants.NOTIFICATION_CHANNEL_NAME
import com.example.runningappdemo.util.Constants.NOTIFICATION_ID
import com.example.runningappdemo.util.Constants.TIMER_UPDATE_INTERVAL
import com.example.runningappdemo.util.TrackingUtility
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject


typealias Polyline = MutableList<LatLng>
typealias Polylines = MutableList<Polyline>

/**
 * *.typealias
 * #PolyLine: 一堆#LatLng => 一段行進路線
 * #pathPoints: 一堆#Polyline => 很多段(中間可能暫停追蹤)的行進路線
 *
 * *.各種時間
 * #timeRunInSecondsLive -> service內部參考總時間
 * #timeRunInMillisLive -> 外部參考總時間
 *
 * #lapTime -> the time interval between start and resume
 * #timeRun -> the total time of our run(ie. sum of #lapTime)
 * #timeStarted -> 計時開始
 * #lastSecondTimeStamp -> used as ref for #timeRunInSecondsLive
 *
 * *. Notification
 * 1.NotificationCompat.Builder() -> create notification
 * 2.NotificationManager -> create channel
 *
 *
 */
@AndroidEntryPoint
class TrackingService : LifecycleService() { // 執行在main thread

    var isFirstRun = true

    var serviceKilled = false

    @Inject
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private val timeRunInSecondsLive = MutableLiveData<Long>() // used to send notification(no need for accuracy)

    @Inject
    lateinit var baseNotificationBuilder: NotificationCompat.Builder

    lateinit var currentNotificationBuilder: NotificationCompat.Builder

    companion object { // directly observing from outside
        val timeRunInMillisLive = MutableLiveData<Long>()
        val isTracking = MutableLiveData<Boolean>()
        val pathPoints = MutableLiveData<Polylines>()
    }

    private fun postInitialValues() {
        isTracking.postValue(false)
        pathPoints.postValue(mutableListOf())
        timeRunInSecondsLive.postValue(0L)
        timeRunInMillisLive.postValue(0L)
    }

    override fun onCreate() {
        super.onCreate()

        currentNotificationBuilder = baseNotificationBuilder
        postInitialValues()
        fusedLocationProviderClient = FusedLocationProviderClient(this)

        isTracking.observe(this, Observer {
            updateLocationTracking(it)
            updateNotificationTrackingState(it)
        })
    }

    // stop the service
    private fun killService() {
        serviceKilled = true
        isFirstRun = true
        pauseService()
        postInitialValues()
        stopForeground(true)
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_START_OR_RESUME_SERVICE -> {
                    if (isFirstRun) {
                        startRunningForeGroundService()
                        isFirstRun = false
                    } else {
                        Timber.e("Resuming service...")
                        startTimer()
                    }
                }
                ACTION_PAUSE_SERVICE -> {
                    Timber.e("pause service")
                    pauseService()
                }
                ACTION_STOP_SERVICE -> {
                    Timber.e("Stopped service")
                    killService()
                }
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }


    private var isTimerEnabled = false

    private var lapTime = 0L // the time interval between start and resume
    private var timeRun = 0L // the total time of our run(ie. sum of #lapTime)
    private var timeStarted = 0L
    private var lastSecondTimeStamp = 0L // used as ref for #timeRunInSecondsLive

    // starting timer
    private fun startTimer() {
        addEmptyPolyline()

        isTracking.postValue(true)
        timeStarted = System.currentTimeMillis()
        isTimerEnabled = true
        CoroutineScope(Main).launch { // use dispatcher.main for updating LiveData objects
            while(isTracking.value!!) {
                lapTime = System.currentTimeMillis() - timeStarted // time difference between now and timeStarted
                timeRunInMillisLive.postValue(timeRun + lapTime) // post the new lapTime in millis

                if (timeRunInMillisLive.value!! >= lastSecondTimeStamp + 1000L) {
                    timeRunInSecondsLive.postValue(timeRunInSecondsLive.value!! + 1) // post the new runTime in seconds
                    lastSecondTimeStamp += 1000L
                }
                delay(TIMER_UPDATE_INTERVAL) // for increasing performance
            }

            timeRun += lapTime
        }
    }

    // update #isTracking when paused
    private fun pauseService() {
        isTracking.postValue(false)
        isTimerEnabled = false
    }

    // update the acton of our current notification
    private fun updateNotificationTrackingState(isTracking: Boolean) {
        val notificationActionText = if (isTracking) "Pause" else "Resume"

        val pendingIntent = if (isTracking) {
            val pauseIntent = Intent(this, TrackingService::class.java).apply {
                action = ACTION_PAUSE_SERVICE
            }
            // Retrieve a PendingIntent that will start a service
            PendingIntent.getService(this, 1, pauseIntent, FLAG_UPDATE_CURRENT)
        } else {
            val resumeIntent = Intent(this, TrackingService::class.java).apply {
                action = ACTION_START_OR_RESUME_SERVICE
            }
            // Retrieve a PendingIntent that will start a service
            PendingIntent.getService(this, 2, resumeIntent, FLAG_UPDATE_CURRENT)
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // remove all actions when we update a new notification
        currentNotificationBuilder.javaClass.getDeclaredField("mActions").apply {
            isAccessible = true // allow to modify
            set(currentNotificationBuilder, ArrayList<NotificationCompat.Action>()) // clear all actions with an empty arrayList
        }

        if (!serviceKilled) {
            currentNotificationBuilder = baseNotificationBuilder
                    .addAction(R.drawable.ic_pause_black, notificationActionText, pendingIntent)
            notificationManager.notify(NOTIFICATION_ID, currentNotificationBuilder.build())
        }
    }


    // request location updates based on #isTracking or not
    @SuppressLint("MissingPermission")
    private fun updateLocationTracking(isTracking: Boolean) {
        if (isTracking) {
            if (TrackingUtility.hasLocationPermissions(this)) {
                val request = LocationRequest().apply {
                    interval = LOCATION_UPDATE_INTERVAL
                    fastestInterval = FASTEST_LOCATION_INTERVAL
                    priority = PRIORITY_HIGH_ACCURACY
                }

                fusedLocationProviderClient.requestLocationUpdates(
                        request,
                        locationCallback,
                        Looper.getMainLooper()
                )
            }
        } else {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        }
    }

    // add all instant location result
    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult?) {
            super.onLocationResult(result)
            if (isTracking.value!!) {
                result?.locations?.let {
                    for (location in it) {
                        addPathPoint(location)
                        Timber.e("NEW LOCATION: ${location.latitude}, ${location.longitude}")
                    }
                }
            }
        }
    }

    // add a Polyline to #pathPoints
    private fun addPathPoint(location: Location?) {
        location?.let {
            val pos = LatLng(location.latitude, location.longitude)
            pathPoints.value?.apply {
                last().add(pos) // add a new polyline at the end of #pathPoints
                pathPoints.postValue(this)
            }
        }
    }

    // calling at the beginning of @startRunningForeGroundService()
    private fun addEmptyPolyline() {
        pathPoints.value?.apply {
            add(mutableListOf()) // add an empty polyline to pathPoints
            pathPoints.postValue(this) // 提交上面add的那筆資料, 以通知變更
        } ?: pathPoints.postValue(mutableListOf(mutableListOf()))
    }

    // use #NotificationCompat to create notification
    private fun startRunningForeGroundService() {
        startTimer()

        isTracking.postValue(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager)
        }

        // launch a foreground service
        startForeground(NOTIFICATION_ID, baseNotificationBuilder.build())

        timeRunInSecondsLive.observe(this, Observer {
            if (!serviceKilled)  {
                val notification = currentNotificationBuilder
                        .setContentText(TrackingUtility.getFormattedStopWatchTime(it * 1000L))
                notificationManager.notify(NOTIFICATION_ID, notification.build())
            }
        })
    }

    // use #NotificationManager to build notification channel
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                IMPORTANCE_LOW
        )

        notificationManager.createNotificationChannel(channel)
    }


}