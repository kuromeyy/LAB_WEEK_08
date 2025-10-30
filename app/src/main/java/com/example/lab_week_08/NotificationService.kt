package com.example.lab_week_08

import android.app.*
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class NotificationService : Service() {

    //In order to make the required notification, a service is required
    //to do the job for us in the foreground process
    //Create the notification builder that'll be called later on
    private lateinit var notificationBuilder: NotificationCompat.Builder

    //Create a system handler which controls what thread the process is being executed on
    private lateinit var serviceHandler: Handler

    //This is used to bind a two-way communication
    //In this tutorial, we will only be using a one-way communication
    //therefore, the return can be set to null
    override fun onBind(intent: Intent): IBinder? = null

    //This is a callback and part of the life cycle
    //The onCreate callback will be called when this service
    //is created for the first time
    override fun onCreate() {
        super.onCreate()

        //Create the notification with all of its contents and configurations
        //in the startForegroundService() custom function
        notificationBuilder = startForegroundService()

        //Create the handler to control which thread the notification will be executed on.
        //'HandlerThread' provides the different thread for the process to be executed on,
        //while on the other hand, 'Handler' enqueues the process to HandlerThread to be executed.
        //Here, we're instantiating a new HandlerThread called "SecondThread"
        //then we pass that HandlerThread into the main Handler called serviceHandler
        val handlerThread = HandlerThread("SecondThread").apply { start() }
        serviceHandler = Handler(handlerThread.looper)
    }

    //Create the notification with all of its contents and configurations all set up
    private fun startForegroundService(): NotificationCompat.Builder {
        //Create a pending Intent which is used to be executed when the user clicks the notification
        val pendingIntent = getPendingIntent()

        //To make a notification, you should know the keyword 'channel'
        val channelId = createNotificationChannel()

        //Combine both the pending Intent and the channel into a notification builder
        //Remember that getNotificationBuilder() is not a built-in function!
        val notificationBuilder = getNotificationBuilder(pendingIntent, channelId)

        //After all has been set and the notification builder is ready,
        //start the foreground service and the notification will appear on the user's device
        startForeground(NOTIFICATION_ID, notificationBuilder.build())

        return notificationBuilder
    }

    //A pending Intent is the Intent used to be executed when the user clicks the notification
    private fun getPendingIntent(): PendingIntent {
        //In order to create a pending Intent, a Flag is needed
        //A flag basically controls whether the Intent can be modified or not later on
        //Unfortunately Flag exists only for API 31 and above,
        //therefore we need to check for the SDK version of the device first
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_IMMUTABLE else 0

        //When the user clicks the notification, they will be redirected to MainActivity
        return PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            flag
        )
    }

    //To make a notification, a channel is required to set up the required configurations
    private fun createNotificationChannel(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //Create the channel id, name, and priority
            val channelId = "001"
            val channelName = "001 Channel"
            val channelPriority = NotificationManager.IMPORTANCE_DEFAULT

            //Build the channel notification based on all 3 previous attributes
            val channel = NotificationChannel(channelId, channelName, channelPriority)

            //Get the NotificationManager class
            val service = requireNotNull(
                ContextCompat.getSystemService(this, NotificationManager::class.java)
            )

            //Binds the channel into the NotificationManager
            service.createNotificationChannel(channel)

            //Return the channel id
            channelId
        } else {
            ""
        }

    //Build the notification with all of its contents and configurations
    private fun getNotificationBuilder(
        pendingIntent: PendingIntent,
        channelId: String
    ): NotificationCompat.Builder =
        NotificationCompat.Builder(this, channelId)
            //Sets the title
            .setContentTitle("Second worker process is done")
            //Sets the content
            .setContentText("Check it out!")
            //Sets the notification icon
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            //Sets the action/intent to be executed when the user clicks the notification
            .setContentIntent(pendingIntent)
            //Sets the ticker message (brief message on top of your device)
            .setTicker("Second worker process is done, check it out!")
            //setOnGoing() controls whether the notification is dismissible or not by the user
            //If true, the notification is not dismissible and can only be closed by the app
            .setOngoing(true)

    //This is a callback and part of a life cycle
    //This callback will be called when the service is started
    //in this case, after the startForeground() method is called
    //in your startForegroundService() custom function
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val returnValue = super.onStartCommand(intent, flags, startId)

        //Gets the channel id passed from the MainActivity through the Intent
        val Id = intent?.getStringExtra(EXTRA_ID)
            ?: throw IllegalStateException("Channel ID is missing")

        //Posts the notification task to the handler, which will be executed on a different thread
        serviceHandler.post {
            //Sets up what happens after the notification is posted
            //Here, we're counting down from 10 to 0 in the notification
            countDownFromTenToZero(notificationBuilder)

            //Here we're notifying the MainActivity that the service process is done
            //by returning the channel ID through LiveData
            notifyCompletion(Id)

            //Stops the foreground service, which closes the notification
            //but the service still goes on
            stopForeground(STOP_FOREGROUND_REMOVE)

            //Stop and destroy the service
            stopSelf()
        }

        return returnValue
    }

    //A function to update the notification to display a count down from 10 to 0
    private fun countDownFromTenToZero(notificationBuilder: NotificationCompat.Builder) {
        //Gets the notification manager
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        //Count down from 10 to 0
        for (i in 10 downTo 0) {
            Thread.sleep(1000L)

            //Updates the notification content text
            notificationBuilder.setContentText("$i seconds until last warning")
                .setSilent(true)

            //Notify the notification manager about the content update
            notificationManager.notify(
                NOTIFICATION_ID,
                notificationBuilder.build()
            )
        }
    }

    //Update the LiveData with the returned channel id through the Main Thread
    //the Main Thread is identified by calling the "getMainLooper()" method
    //This function is called after the count down has completed
    private fun notifyCompletion(Id: String) {
        Handler(Looper.getMainLooper()).post {
            mutableID.value = Id
        }
    }

    companion object {
        const val NOTIFICATION_ID = 0xCA7
        const val EXTRA_ID = "Id"

        //This is a LiveData which is a data holder that automatically updates the UI based on what is observed
        //It'll return the channel ID into the LiveData after the countdown has reached 0
        //giving a sign that the service process is done
        private val mutableID = MutableLiveData<String>()
        val trackingCompletion: LiveData<String> = mutableID
    }
}
