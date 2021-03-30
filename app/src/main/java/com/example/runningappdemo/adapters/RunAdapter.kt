package com.example.runningappdemo.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.runningappdemo.R
import com.example.runningappdemo.db.Run
import com.example.runningappdemo.util.TrackingUtility
import kotlinx.android.synthetic.main.item_run.view.*
import java.text.SimpleDateFormat
import java.util.*

class RunAdapter
    : ListAdapter<Run, RunAdapter.RunViewHolder>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RunViewHolder {
        return RunViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_run, parent, false))
    }

    override fun onBindViewHolder(holder: RunViewHolder, position: Int) {
        val run = getItem(position)
        if (run != null) {
            holder.bind(run)
        }
    }

    object diffCallback : DiffUtil.ItemCallback<Run>() {
        override fun areItemsTheSame(oldItem: Run, newItem: Run): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Run, newItem: Run): Boolean {
            return oldItem == newItem // philip use oldItem.hashCode() == newItem.hashCode()
        }

    }


    inner class RunViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(run: Run) {
            with(itemView) {
                Glide.with(this).load(run.img).into(ivRunImage)

                val calendar = Calendar.getInstance().apply {
                    timeInMillis = run.timeStamp
                }
                
                val dateFormat = SimpleDateFormat("dd, mm, yy", Locale.getDefault())
                tvDate.text = dateFormat.format(calendar.time)

                val avgSpeed = "${run.avgSpeedInKmh} km/h"
                tvAvgSpeed.text = avgSpeed

                val distanceInKm = "${run.distanceInMeters / 1000f} km"
                tvDistance.text = distanceInKm

                tvTime.text = TrackingUtility.getFormattedStopWatchTime(run.timeInMillis)

                val caloriesBurned = "${run.caloriesBurned} kCal"
                tvCalories.text = caloriesBurned
            }

        }
    }




}