package com.example.splitsecond2

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var pointsTextView: TextView
    private lateinit var progressBar: ProgressBar
    private val sharedPreferences by lazy {
        getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pointsTextView = findViewById(R.id.pointsTextView)
        progressBar = findViewById(R.id.progressBar)

        // Create notification channel
        createNotificationChannel()

        // Check for the POST_NOTIFICATIONS permission and request it if not granted
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1001
            )
        }

        // Update the UI initially
        updateUI()

        // Schedule periodic reminders
        val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(20, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueue(workRequest)
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI() {
        val points = getPoints()
        pointsTextView.text = "Points: $points"
        progressBar.progress = points.coerceAtMost(100) // Ensure progress stays within range
    }

    private fun getPoints(): Int {
        return sharedPreferences.getInt("POINTS", 0)
    }

    private fun addPoints(points: Int) {
        val currentPoints = getPoints()
        sharedPreferences.edit().putInt("POINTS", (currentPoints + points).coerceAtMost(100)).apply()
        updateUI()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "ReminderChannel",
            "Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Channel for SplitSecond reminders"
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(channel)
    }

    // This worker sends the reminder notification every 20 minutes
    class ReminderWorker(appContext: Context, workerParams: WorkerParameters) :
        Worker(appContext, workerParams) {

        override fun doWork(): Result {
            // Check if the permission is granted before showing the notification
            if (ActivityCompat.checkSelfPermission(
                    applicationContext, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Show notification
                val channelId = "ReminderChannel"
                val builder = NotificationCompat.Builder(applicationContext, channelId)
                    .setContentTitle("Take a Break!")
                    .setContentText("Look away from your screen for a few minutes.")
                    .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your app's icon
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)

                // Notify the user
                NotificationManagerCompat.from(applicationContext).notify(1, builder.build())
            }

            return Result.success()
        }
    }
}