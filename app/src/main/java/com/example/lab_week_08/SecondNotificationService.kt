package com.example.lab_week_08

import android.app.*
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class SecondNotificationService : Service() {

    private lateinit var serviceHandler: Handler
    private lateinit var notificationBuilder: NotificationCompat.Builder

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        // Create handler in background thread
        val handlerThread = HandlerThread("SecondServiceThread").apply { start() }
        serviceHandler = Handler(handlerThread.looper)

        // Build notification IMMEDIATELY to avoid crash
        notificationBuilder = createAndStartNotification()
    }

    private fun createAndStartNotification(): NotificationCompat.Builder {
        val channelId = createNotificationChannel()

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Third worker process is done")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setTicker("All processes finished!")
            .setOngoing(true)

        // Start foreground IMMEDIATELY (important)
        startForeground(NOTIFICATION_ID, builder.build())

        return builder
    }

    private fun createNotificationChannel(): String {
        val channelId = "002"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Final Stage Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = ContextCompat.getSystemService(
                this,
                NotificationManager::class.java
            )!!
            manager.createNotificationChannel(channel)
        }
        return channelId
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getStringExtra(EXTRA_ID) ?: "002" // fallback to prevent crash

        // Run countdown safely on background thread
        serviceHandler.post {
            countDownFromFive(notificationBuilder)
            notifyCompletion(id)

            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        return START_STICKY
    }

    private fun countDownFromFive(builder: NotificationCompat.Builder) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        for (i in 5 downTo 0) {
            try {
                Thread.sleep(1000L)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            builder.setContentText("$i seconds before closing service").setSilent(true)
            manager.notify(NOTIFICATION_ID, builder.build())
        }
    }

    private fun notifyCompletion(id: String) {
        Handler(Looper.getMainLooper()).post {
            mutableID.value = id
        }
    }

    companion object {
        const val NOTIFICATION_ID = 0xCA8
        const val EXTRA_ID = "Id2"

        private val mutableID = MutableLiveData<String>()
        val trackingCompletion: LiveData<String> = mutableID
    }
}
