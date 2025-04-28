package com.codewithkael.youtubertmpstream.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RtmpBroadcastReceiver : BroadcastReceiver() {

    @Inject lateinit var serviceRepository: RtmpServiceRepository
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "ACTION_EXIT") {
            //we want to exit the whole application
            serviceRepository.stopService()
        }
    }
}