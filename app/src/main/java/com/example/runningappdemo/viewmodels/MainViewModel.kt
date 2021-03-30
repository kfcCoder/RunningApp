package com.example.runningappdemo.viewmodels

import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runningappdemo.db.Run
import com.example.runningappdemo.repository.MainRepository
import com.example.runningappdemo.util.SortType
import kotlinx.coroutines.launch

/**
 * @ViewModelInject make dagger to take care of ViewModelFactory stuff
 */
class MainViewModel @ViewModelInject constructor(
        val mainRepository: MainRepository
) : ViewModel(){

    private val runSortedByDate = mainRepository.getAllRunsSortedByDate()
    private val runSortedByDistance = mainRepository.getAllRunsSortedByDistance()
    private val runSortedByCaloriesBurned = mainRepository.getAllRunsSortedByCaloriesBurned()
    private val runSortedByTimeInMillis = mainRepository.getAllRunsSortedByTimeInMillis()
    private val runSortedByAvgSpeed = mainRepository.getAllRunsSortedByAvgSpeed()

    val runsLive = MediatorLiveData<List<Run>>()

    var sortType = SortType.DATE // default sort type

    // add all #runsSortedBy LiveData as source of #runsLive mediatorLiveData
    init {
        runsLive.addSource(runSortedByDate) { origData -> // origData: List<Run>
            if (sortType == SortType.DATE) {
                origData?.let { runsLive.value = it }
            }
        }

        runsLive.addSource(runSortedByAvgSpeed) { origData ->
            if (sortType == SortType.AVG_SPEED) {
                origData?.let { runsLive.value = it }
            }
        }

        runsLive.addSource(runSortedByCaloriesBurned) { origData ->
            if (sortType == SortType.CALORIES_BURNED) {
                origData?.let { runsLive.value = it }
            }
        }

        runsLive.addSource(runSortedByDistance) { origData ->
            if (sortType == SortType.DISTANCE) {
                origData?.let { runsLive.value = it }
            }
        }

        runsLive.addSource(runSortedByTimeInMillis) { origData ->
            if (sortType == SortType.RUNNING_TIME) {
                origData?.let { runsLive.value = it }
            }
        }
    }

    // handle sort type changes in #RunFragment
    fun sortRuns(sortType: SortType) {
        when(sortType) {
            SortType.DATE -> runSortedByDate.value?.let { runsLive.value = it }
            SortType.RUNNING_TIME -> runSortedByTimeInMillis.value?.let { runsLive.value = it }
            SortType.AVG_SPEED -> runSortedByAvgSpeed.value?.let { runsLive.value = it }
            SortType.DISTANCE -> runSortedByDistance.value?.let { runsLive.value = it }
            SortType.CALORIES_BURNED -> runSortedByCaloriesBurned.value?.let { runsLive.value = it }
        }.also {
            this.sortType = sortType
        }
    }


    fun insertRun(run: Run) = viewModelScope.launch {
            mainRepository.insertRun(run)
    }



}