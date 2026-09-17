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
        // System insets are consumed once at the root; screens own their bottom navigation.
        // Only this Activity owns the SQLiteOpenHelper; children never close a shared store.
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
        if (isFinishing) store.close()
        super.onDestroy()
    }
}
