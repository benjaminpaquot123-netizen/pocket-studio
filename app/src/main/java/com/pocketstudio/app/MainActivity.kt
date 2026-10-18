package com.pocketstudio.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pocketstudio.app.ui.AppRoot
import com.pocketstudio.ui.PocketStudioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Ask the system to keep CPU clocks stable instead of bursting and throttling.
        runCatching { window.setSustainedPerformanceMode(true) }
        setContent {
            PocketStudioTheme {
                AppRoot()
            }
        }
    }
}
