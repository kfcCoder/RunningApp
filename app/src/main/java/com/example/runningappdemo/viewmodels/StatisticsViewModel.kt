package com.example.runningappdemo.viewmodels

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import com.example.runningappdemo.repository.MainRepository

class StatisticsViewModel @ViewModelInject constructor(
        val mainRepository: MainRepository
) : ViewModel(){

}