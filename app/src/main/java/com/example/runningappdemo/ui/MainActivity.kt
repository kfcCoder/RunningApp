package com.example.runningappdemo.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.runningappdemo.R
import com.example.runningappdemo.db.RunDao
import com.example.runningappdemo.util.Constants.ACTION_SHOW_TRACKING_FRAGMENT
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject

/**
 * bugs:
 * 1.start a run and cancel it, then start a new run again, 'FinishRun' will appear
 * 2.cancel run dialog can't survice config. changes
 * 3.reload #RunFragment when we already in it
 */
@AndroidEntryPoint // annotation to indicate @Inject dependency here
class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    @Inject
    lateinit var s1: String

    @Inject
    lateinit var s2: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navController = navHost.findNavController()

        navigateToTrackingFragmentIfNeeded(intent)

        setSupportActionBar(toolbar)
        bottomNavigationView.setupWithNavController(navController)
        bottomNavigationView.setOnNavigationItemReselectedListener { /*NO OPERATION*/ } // prevent reload the frag.


        navController.addOnDestinationChangedListener { _, destination, _ ->
            when(destination.id) {
                R.id.runFragment, R.id.settingsFragment, R.id.statisticsFragment ->
                    bottomNavigationView.isVisible = true
                else -> bottomNavigationView.isVisible = false
            }
        }

        println(s1)
        println(s2)
        println("${s1.hashCode() == s2.hashCode()}")


    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        navigateToTrackingFragmentIfNeeded(intent)
    }


    private fun navigateToTrackingFragmentIfNeeded(intent: Intent?) {
        if (intent?.action == ACTION_SHOW_TRACKING_FRAGMENT) {
            navController.navigate(R.id.action_global_to_trackingFragment)
        }
    }


}