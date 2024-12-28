package com.example.splitsecond2

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log

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
    private lateinit var timerText: TextView
    private var points = 0
    private val sharedPreferences by lazy {
        getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
    }
    private var timer: CountDownTimer? = null

    @SuppressLint("InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pointsTextView = findViewById(R.id.pointsTextView)
        progressBar = findViewById(R.id.progressBar)
        timerText = findViewById(R.id.timerText)

        createNotificationChannel()

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

        startTimer()

        val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(20, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueue(workRequest)
    }

    private fun startTimer() {
        Log.d("TIMER", "Starting timer...")
        timer = object : CountDownTimer(20000, 1000) {
            @SuppressLint("SetTextI18n")
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                val e: Int = (secondsLeft*5).toInt()
                timerText.text = "Time left: $secondsLeft seconds"
                progressBar.progress = 100 - e
                Log.d("TIMER", "Seconds left: $secondsLeft")
            }

            override fun onFinish() {
                Log.d("TIMER", "Timer finished!")
                addPoints(100)
            }
        }.start()
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

    override fun onDestroy() {
        super.onDestroy()
        Log.d("TIMER", "Activity destroyed, cancelling timer...")
        timer?.cancel()
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
    }

    class ReminderWorker(appContext: Context, workerParams: WorkerParameters) :
        Worker(appContext, workerParams) {

        override fun doWork(): Result {
            if (ActivityCompat.checkSelfPermission(
                    applicationContext, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val channelId = "ReminderChannel"
                val builder = NotificationCompat.Builder(applicationContext, channelId)
                    .setContentTitle("Take a Break!")
                    .setContentText("Look away from your screen for a few minutes.")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)

                NotificationManagerCompat.from(applicationContext).notify(1, builder.build())
            }

            return Result.success()
        }
    }
}
