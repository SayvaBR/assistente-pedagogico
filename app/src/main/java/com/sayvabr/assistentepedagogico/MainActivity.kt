package com.sayvabr.assistentepedagogico

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.sayvabr.assistentepedagogico.data.TeacherStore
import com.sayvabr.assistentepedagogico.ui.ApTheme
import com.sayvabr.assistentepedagogico.ui.TeacherApp

class MainActivity : ComponentActivity() {
    private val store by lazy { TeacherStore(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ApTheme { TeacherApp(store) } }
    }

    override fun onDestroy() {
        if (isFinishing) store.close()
        super.onDestroy()
    }
}
