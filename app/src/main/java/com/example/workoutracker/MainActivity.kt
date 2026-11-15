package com.example.workoutracker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.IntentCompat
import com.example.workoutracker.ui.MainScreen
import com.example.workoutracker.ui.theme.WorkoutrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Was the app opened by sharing (SEND) or opening (VIEW) a file? Only on a fresh start:
        // after a rotation the app is already on the right screen.
        val sharedFile: Uri? = if (savedInstanceState == null) fileFrom(intent) else null

        setContent {
            WorkoutrackerTheme {
                MainScreen(sharedFileUri = sharedFile)
            }
        }
    }

    private fun fileFrom(intent: Intent?): Uri? = when (intent?.action) {
        Intent.ACTION_SEND -> IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
        Intent.ACTION_VIEW -> intent.data
        else -> null
    }

}