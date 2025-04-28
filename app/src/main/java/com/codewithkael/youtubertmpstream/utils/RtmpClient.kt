package com.codewithkael.youtubertmpstream.utils

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.util.Log
import com.haishinkit.event.Event
import com.haishinkit.event.IEventListener
import com.haishinkit.media.AudioRecordSource
import com.haishinkit.media.AudioSource
import com.haishinkit.media.Camera2Source
import com.haishinkit.rtmp.RtmpConnection
import com.haishinkit.rtmp.RtmpStream
import com.haishinkit.view.HkSurfaceView
import java.util.UUID
import javax.inject.Singleton

@Singleton
class RtmpClient(
    context: Context,
    surfaceView: HkSurfaceView,
    private val listener: Listener
) : IEventListener {

    private var connection: RtmpConnection = RtmpConnection()
    private var stream: RtmpStream = RtmpStream(context,connection)
    private val videoSource: Camera2Source by lazy {
        Camera2Source(context).apply {
            open(CameraCharacteristics.LENS_FACING_BACK)
        }
    }
    private val audioSource: AudioSource by lazy {
        AudioRecordSource(context)
    }


    init {
        stream.attachAudio(audioSource)
        stream.attachVideo(videoSource)
        connection.addEventListener(Event.RTMP_STATUS, this@RtmpClient)
        surfaceView.attachStream(stream)

    }

    fun start(streamName:String) {
        //you can set custom configs here using init function
        stream.audioSetting.bitRate = 128 * 1000  // Set audio bitrate to 128 kbps
        stream.videoSetting.width = 1280  // 1280x720 resolution
        stream.videoSetting.height = 720
        stream.videoSetting.bitRate = 4000000 // 4 Mbps video bitrate
        connection.connect("rtmp://a.rtmp.youtube.com/live2")
        stream.publish(streamName)

    }

    fun stop() {
        try {
            videoSource.close()
            audioSource.stopRunning()
            stream.close()
            connection.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun switchCamera() {
        videoSource.switchCamera()
    }


    override fun handleEvent(event: Event) {
        Log.d("Masoud Tag", "handleEvent: event = ${event.type} data = ${event.data}")

        if (event.data.toString().contains("code=NetConnection.Connect.Success")
            && event.type == "rtmpStatus"
        ) {
            listener.onStreamConnected()
        }
        if (event.data.toString().contains("code=NetConnection.Connect.Closed")
            && event.type == "rtmpStatus"
        ) {
            listener.onStreamClosed()
        }
    }


    interface Listener {
        fun onStreamConnected()
        fun onStreamClosed()
    }
}