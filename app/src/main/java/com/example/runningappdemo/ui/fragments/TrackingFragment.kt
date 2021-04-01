package com.example.runningappdemo.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.runningappdemo.R
import com.example.runningappdemo.db.Run
import com.example.runningappdemo.services.Polyline
import com.example.runningappdemo.services.TrackingService
import com.example.runningappdemo.services.TrackingService.Companion.pathPoints
import com.example.runningappdemo.util.Constants.ACTION_PAUSE_SERVICE
import com.example.runningappdemo.util.Constants.ACTION_START_OR_RESUME_SERVICE
import com.example.runningappdemo.util.Constants.ACTION_STOP_SERVICE
import com.example.runningappdemo.util.Constants.MAP_ZOOM
import com.example.runningappdemo.util.Constants.POLYLINE_COLOR
import com.example.runningappdemo.util.Constants.POLYLINE_WIDTH
import com.example.runningappdemo.util.TrackingUtility
import com.example.runningappdemo.viewmodels.MainViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_tracking.*
import java.util.*
import javax.inject.Inject
import kotlin.math.round

const val CANCEL_TRACKING_DIALOG_TAG = "CancelDialog"

@AndroidEntryPoint
class TrackingFragment : Fragment(R.layout.fragment_tracking) {

    private val sharedViewModel by activityViewModels<MainViewModel>()

    private var isTracking = false

    private var pathPoints = mutableListOf<Polyline>() // 一堆Polyline...

    private var map: GoogleMap? = null

    private var curTimeInMillis = 0L

    private var menu: Menu? = null

    @set:Inject
    var weight = 80f

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        mapView.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            val cancelTrackingDialog = parentFragmentManager.findFragmentByTag(
                    CANCEL_TRACKING_DIALOG_TAG) as CancelTrackingDialog?

            cancelTrackingDialog?.setYesListener { stopRun() }
        }

        mapView.getMapAsync {
            map = it
            addAllPolylines()
        }

        btnToggleRun.setOnClickListener {
            toggleRun()
        }

        btnFinishRun.setOnClickListener {
            zoomToSeeWholeTrack()
            endRunAndSaveToDb()
        }

        subscribeToObservers()

    }

    // observing LiveData from #TrackingService
    private fun subscribeToObservers() {
        TrackingService.isTracking.observe(viewLifecycleOwner, Observer {
            updateTrackingUIs(it)
        })

        TrackingService.pathPoints.observe(viewLifecycleOwner, Observer {
            pathPoints = it
            addLatestPolyline()
            moveCameraToUser()
        })

        TrackingService.timeRunInMillisLive.observe(viewLifecycleOwner, Observer {
            curTimeInMillis = it
            val formattedTime = TrackingUtility.getFormattedStopWatchTime(curTimeInMillis, true)
            tvTimer.text = formattedTime
        })

    }

    // switch on/off #TrackingService
    private fun toggleRun() {
        if (isTracking) {
            menu?.getItem(0)?.isVisible = true
            sendCommandToService(ACTION_PAUSE_SERVICE)
        } else {
            sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
        }
    }

    // create menu
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.tracking_menu, menu)
        this.menu = menu
    }

    // specify the timing that shows the menu item(沒加也沒差...)
    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        if (curTimeInMillis > 0L) {
            this.menu?.getItem(0)?.isVisible = true
        }
    }

    // define the behavior after click on menu item
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.miCancelTracking -> {
                showCancelTrackingDialog()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // show a dialog when current run stopped
    private fun showCancelTrackingDialog() {
        CancelTrackingDialog().apply {
            setYesListener {
                stopRun()
            }
        }.show(parentFragmentManager, CANCEL_TRACKING_DIALOG_TAG)
    }

    // stop current run and navigate to #RunFragment
    private fun stopRun() {
        tvTimer.text  = "00:00:00:00"
        sendCommandToService(ACTION_STOP_SERVICE)
        findNavController().navigate(R.id.action_trackingFragment_to_runFragment)
    }

    // observe the data changes from service and update UIs
    private fun updateTrackingUIs(isTracking: Boolean) {
        this.isTracking = isTracking
        if (!isTracking && curTimeInMillis > 0L) {
            btnToggleRun.text = "Start"
            btnFinishRun.isVisible = true
        } else if (isTracking){
            btnToggleRun.text = "Stop"
            menu?.getItem(0)?.isVisible = true
            btnFinishRun.isVisible = false
        }
    }

    // move in to user's position when there is a new polyline in #pathPoints
    private fun moveCameraToUser() {
        if (pathPoints.isNotEmpty() && pathPoints.last().isNotEmpty()) {
            map?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                            pathPoints.last().last(), // the latest coordinate we got
                            MAP_ZOOM
                    )
            )
        }
    }

    // take the snapshot of the running track
    private fun zoomToSeeWholeTrack() {
        val bounds = LatLngBounds.builder()
        for (polyline in pathPoints) {
            for (position in polyline) {
                bounds.include(position) // each LatLng coordinate
            }
        }

        map?.moveCamera(
                CameraUpdateFactory.newLatLngBounds(
                        bounds.build(),
                        mapView.width,
                        mapView.height,
                        (mapView.height * 0.05f).toInt() // padding of the screenshot
                )
        )
    }

    private fun endRunAndSaveToDb() {
        map?.snapshot {
            var distanceInMeters = 0
            for (polyline in pathPoints) {
                distanceInMeters += TrackingUtility.calculatePolylineLength(polyline).toInt()
            }

            // 四捨五入
            val avgSpeed = round((distanceInMeters / 1000f) / (curTimeInMillis / 1000f / 60 / 60) * 10) / 10f

            val dateTimestamp = Calendar.getInstance().timeInMillis // current tim in ms from epoch
            val caloriesBurned = ((distanceInMeters / 1000f) * weight).toInt()
            val run = Run(it, dateTimestamp, avgSpeed, distanceInMeters, curTimeInMillis, caloriesBurned)

            sharedViewModel.insertRun(run)
            Snackbar.make(
                requireActivity().findViewById(R.id.rootView), // because we'll navigate back to #runFragment
                "Run saved successfully",
                Snackbar.LENGTH_LONG
            ).show()

            stopRun()
        }
    }


    // draw all polylines again when we rotate the device
    private fun addAllPolylines() {
        for (polyline in pathPoints) {
            val polylineOptiones = PolylineOptions()
                    .color(POLYLINE_COLOR)
                    .width(POLYLINE_WIDTH)
                    .addAll(polyline)

            map?.addPolyline(polylineOptiones)
        }
    }

    // connect the last 2 points of #pathPoints list
    private fun addLatestPolyline() {
        // list is not empty and the last polyline has at least 2 #LatLng
        if (pathPoints.isNotEmpty() && pathPoints.last().size > 1) {
            val preLastLatLng = pathPoints.last()[pathPoints.last().size - 2] // the preLast coordinate
            val lastLatLng = pathPoints.last().last() // the last coordinate
            val polylineOptions = PolylineOptions()
                    .color(POLYLINE_COLOR)
                    .width(POLYLINE_WIDTH)
                    .add(preLastLatLng)
                    .add(lastLatLng)

            map?.addPolyline(polylineOptions)
        }
    }


    // launch service
    private fun sendCommandToService(action: String): Intent {
        return Intent(requireContext(), TrackingService::class.java).also {
            it.action = action
            requireContext().startService(it)
        }
    }


    // handle the lifecycle of mapView
    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView?.onDestroy()
    }

}