package com.sayvabr.assistentepedagogico

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.sayvabr.assistentepedagogico.ui.ApTheme
import com.sayvabr.assistentepedagogico.ui.BootstrapScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ApTheme {
                BootstrapScreen()
            }
        }
    }
}
