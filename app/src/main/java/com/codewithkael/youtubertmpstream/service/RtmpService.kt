package com.codewithkael.youtubertmpstream.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.codewithkael.youtubertmpstream.R
import com.codewithkael.youtubertmpstream.utils.RtmpClient
import com.haishinkit.media.Stream
import com.haishinkit.view.HkSurfaceView

class RtmpService : Service(), RtmpClient.Listener {

    companion object {
        var surfaceView: HkSurfaceView? = null
        var isServiceRunning = false
        var isStreamOn = false
        var listener: Listener? = null
        var currentUrl = ""
        var localStream: Stream? = null
    }

    //notification section
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var notificationManager: NotificationManager
    private var rtmpClient: RtmpClient? = null

    override fun onCreate() {
        super.onCreate()
        setupNotification()
        surfaceView = HkSurfaceView(this)
        rtmpClient = RtmpClient(this, surfaceView!!, this@RtmpService)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        intent?.let { incomingIntent ->
            when (incomingIntent.action) {
                RtmpServiceActions.START_SERVICE.name -> handleStartService(intent)
                RtmpServiceActions.STOP_SERVICE.name -> handleStopService()
                RtmpServiceActions.SWITCH_CAMERA.name -> handleSwitchCamera()
                else -> Unit
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


    private fun handleStartService(intent: Intent) {
        if (!isServiceRunning) {
            isServiceRunning = true
            val url = intent.getStringExtra("url")
            url?.let {
                currentUrl = it
                rtmpClient?.start(it) { stream: Stream ->
                    localStream = stream
                }
                startServiceWithNotification()
            }
        }

    }


    private fun handleStopService() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        isServiceRunning = false
        isStreamOn = false
        rtmpClient?.stop()
//        surfaceView?.dispose()
        surfaceView = null
    }

    private fun handleSwitchCamera() {
        rtmpClient?.switchCamera()
    }


    private fun setupNotification() {
        notificationManager = getSystemService(
            NotificationManager::class.java
        )
        val notificationChannel = NotificationChannel(
            "rtmpChannel", "foreground", NotificationManager.IMPORTANCE_HIGH
        )

        val intent = Intent(this, RtmpBroadcastReceiver::class.java).apply {
            action = "ACTION_EXIT"
        }
        val pendingIntent: PendingIntent =
            PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        notificationManager.createNotificationChannel(notificationChannel)
        notificationBuilder = NotificationCompat.Builder(
            this, "rtmpChannel"
        ).setSmallIcon(R.mipmap.ic_launcher)
            .addAction(R.drawable.ic_end_call, "Exit", pendingIntent)
    }

    private fun startServiceWithNotification() {
        startForeground(1, notificationBuilder.build())
    }


    interface Listener {
        fun onStreamStarted()
        fun onStreamClosed()
    }

    override fun onStreamConnected() {
        isStreamOn = true
        listener?.onStreamStarted()
    }

    override fun onStreamClosed() {
        listener?.onStreamClosed()
    }
}