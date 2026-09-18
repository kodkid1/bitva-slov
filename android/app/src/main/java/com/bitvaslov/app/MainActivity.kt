package com.bitvaslov.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.bitvaslov.app.ui.App
import com.bitvaslov.app.ui.BitvaSlovTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BitvaSlovTheme {
                App()
            }
        }
    }
}