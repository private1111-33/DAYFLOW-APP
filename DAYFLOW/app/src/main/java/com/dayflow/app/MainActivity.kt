package com.dayflow.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.dayflow.app.ui.DayflowApp
import com.dayflow.app.ui.theme.DayflowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DayflowTheme {
                DayflowApp()
            }
        }
    }
}
