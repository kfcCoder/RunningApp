package com.example.runningappdemo.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Run::class],
    version = 1,
    //exportSchema = false
)
@TypeConverters(Converters::class)
abstract class RunningDatabase
    : RoomDatabase() {

    abstract fun getRunDao(): RunDao

    // Dagger will take care of the rest house keeping stuff...

}