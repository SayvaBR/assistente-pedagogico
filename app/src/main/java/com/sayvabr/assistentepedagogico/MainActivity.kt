package com.sayvabr.assistentepedagogico

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Modifier
import com.sayvabr.assistentepedagogico.data.TeacherStore
import com.sayvabr.assistentepedagogico.ui.ApColors
import com.sayvabr.assistentepedagogico.ui.ApTheme
import com.sayvabr.assistentepedagogico.ui.TeacherApp

class MainActivity : ComponentActivity() {
    private val store by lazy { TeacherStore(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Android 15/16 draw edge-to-edge by default. Consume the status-bar inset once
        // at the activity root, instead of letting each screen paint beneath the clock.
        // The bottom navigation already handles navigationBarsPadding in TeacherApp.
        // imePadding keeps forms and their save actions above the software keyboard.
        setContent {
            ApTheme {
                Box(Modifier.fillMaxSize().background(ApColors.Sky).statusBarsPadding().imePadding()) {
                    TeacherApp(store)
                }
            }
        }
    }

    override fun onDestroy() {
        // Configuration changes destroy this Activity too. Its replacement owns a new helper;
        // always close the old connection instead of leaking it until process termination.
        // A fresh TeacherStore reopens the same private DB without deleting any data.
        if (::store.isInitialized) store.close()
        super.onDestroy()
    }
}
