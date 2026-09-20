package com.sayvabr.assistentepedagogico

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.sayvabr.assistentepedagogico.data.TeacherStore
import com.sayvabr.assistentepedagogico.ui.ApColors
import com.sayvabr.assistentepedagogico.ui.ApTheme
import com.sayvabr.assistentepedagogico.ui.LocalTeacherStore
import com.sayvabr.assistentepedagogico.ui.FirstAccessFlow

class MainActivity : ComponentActivity() {
    private val storeDelegate = lazy { TeacherStore(applicationContext) }
    private val store by storeDelegate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Apply system insets at one root shared by ALL routes, including full-screen editors
        // that hide the bottom navigation. Insets padding is consumed by Compose children:
        // BottomBar's own navigationBarsPadding will not double-apply the navigation inset.
        // IME padding is independent and keeps the scrollable form above the soft keyboard.
        setContent {
            ApTheme {
                Box(
                    Modifier.fillMaxSize().background(ApColors.Sky)
                        .statusBarsPadding().navigationBarsPadding().imePadding()
                ) {
                    CompositionLocalProvider(LocalTeacherStore provides store) {
                        FirstAccessFlow(store)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        // Rotation/configuration replacement owns a new helper, while this one must be closed.
        // Lazy.isInitialized avoids creating a database connection solely to close it.
        if (storeDelegate.isInitialized()) store.close()
        super.onDestroy()
    }
}
