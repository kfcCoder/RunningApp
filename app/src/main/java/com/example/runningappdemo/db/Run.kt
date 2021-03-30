package com.example.runningappdemo.db

import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "running_table")
data class Run(
    var img: Bitmap? = null,
    var timeStamp: Long = 0L, // when's the run
    var avgSpeedInKmh: Float = 0f,
    var distanceInMeters: Int = 0,
    var timeInMillis: Long = 0L, // how long's the run
    var caloriesBurned: Int = 0
) {
    // Room will take care of 'id', we don't pass in ctor
    @PrimaryKey(autoGenerate = true) var id: Int? = null
}