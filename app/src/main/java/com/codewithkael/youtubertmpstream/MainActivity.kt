package com.codewithkael.youtubertmpstream

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.codewithkael.youtubertmpstream.service.RtmpService
import com.codewithkael.youtubertmpstream.service.RtmpServiceRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity(), RtmpService.Listener {

    @Inject lateinit var repository: RtmpServiceRepository

    private var isStreaming by mutableStateOf(false)
    private var streamStatus by mutableStateOf("Status: Not Connected")
    private var urlText by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RtmpService.listener = this@MainActivity

        if (RtmpService.isStreamOn) {
            streamStarted()
            urlText = RtmpService.currentUrl
        }

        setContent {
            MainScreen()
        }
    }

    @SuppressLint("InlinedApi")
    @Composable
    fun MainScreen() {
        val context = LocalContext.current
        var showError by remember { mutableStateOf(false) }

        // Full list of permissions you need
        val permissionsToRequest = buildList {
            add(Manifest.permission.CAMERA)
            add(Manifest.permission.RECORD_AUDIO)
            add(Manifest.permission.FOREGROUND_SERVICE)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()

        val permissionsLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.values.all { it }
            if (allGranted) {
                if (urlText.isNotEmpty()) {
                    repository.startService(urlText)
                    streamStatus = "Status: Connecting"
                    showError = false
                } else {
                    showError = true
                    Toast.makeText(context, "Enter URL", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Permissions not granted", Toast.LENGTH_SHORT).show()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = urlText,
                onValueChange = {
                    urlText = it
                    if (showError && it.isNotEmpty()) {
                        showError = false
                    }
                },
                label = { Text("Enter URL") },
                isError = showError,
                enabled = !isStreaming,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )

            if (showError) {
                Text(
                    text = "Please enter a URL before starting!",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(start = 16.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(streamStatus)

            Spacer(modifier = Modifier.height(16.dp))

            Row {
                Button(
                    onClick = {
                        permissionsLauncher.launch(permissionsToRequest)
                    },
                    enabled = !isStreaming
                ) {
                    Text("Start")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        repository.stopService()
                        streamStopped()
                    },
                    enabled = isStreaming
                ) {
                    Text("Stop")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        repository.switchCamera()
                    },
                    enabled = isStreaming
                ) {
                    Text("Switch")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isStreaming) {
//                AndroidView(
//                    factory = { RtmpService.surfaceView!! },
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .weight(1f)
//                )
            }
        }
    }

    private fun streamStarted() {
        runOnUiThread {
            isStreaming = true
            streamStatus = "Status: Connected"
        }
    }

    private fun streamStopped() {
        runOnUiThread {
            isStreaming = false
            streamStatus = "Status: Not Connected"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            RtmpService.surfaceView?.let {
                (it.parent as? ViewGroup)?.removeView(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStreamStarted() {
        streamStarted()
    }

    override fun onStreamClosed() {
        streamStopped()
    }
}