package com.example.runningappdemo.viewmodels

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import com.example.runningappdemo.repository.MainRepository

class StatisticsViewModel @ViewModelInject constructor(
        val mainRepository: MainRepository
) : ViewModel(){

    val totalTimeRun = mainRepository.getTotalTimeInmillis()
    val totalDistance = mainRepository.getTotalDistance()
    val totalCaloriesBurned = mainRepository.getTotalCaloriesBurned()
    val totalAvgSpeed = mainRepository.getTotalAvgSpeed()

    // for graph based on chronological order
    val runsSortedByDate = mainRepository.getAllRunsSortedByDate()
}