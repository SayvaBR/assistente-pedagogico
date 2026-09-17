package com.sayvabr.assistentepedagogico

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.sayvabr.assistentepedagogico.data.TeacherStore
import com.sayvabr.assistentepedagogico.ui.ApColors
import com.sayvabr.assistentepedagogico.ui.ApTheme
import com.sayvabr.assistentepedagogico.ui.LocalTeacherStore
import com.sayvabr.assistentepedagogico.ui.TeacherApp

class MainActivity : ComponentActivity() {
    private val store by lazy { TeacherStore(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Insets are consumed once at the root; bottom bar owns navigation-bars padding.
        // The Activity owns the SQLiteOpenHelper. Nested Compose screens share it.
        setContent {
            CompositionLocalProvider(LocalTeacherStore provides store) {
                ApTheme {
                    Box(Modifier.fillMaxSize().background(ApColors.Sky).statusBarsPadding().imePadding()) {
                        TeacherApp(store)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        // A configuration change destroys this Activity as well: the replacement owns a new
        // helper. Closing only on isFinishing leaks a connection after every rotation.
        store.close()
        super.onDestroy()
    }
}
