package com.example.workoutracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.workoutracker.ui.theme.WorkoutrackerTheme
import com.example.workoutracker.ui.HistoryScreen
import com.example.workoutracker.ui.MainScreen
import com.example.workoutracker.ui.ExercisesScreen
import com.example.workoutracker.ui.ExerciseViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WorkoutrackerTheme {
                MainScreen()
            }
        }
    }
}
