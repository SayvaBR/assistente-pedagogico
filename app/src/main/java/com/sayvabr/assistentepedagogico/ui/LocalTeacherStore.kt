package com.sayvabr.assistentepedagogico.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.sayvabr.assistentepedagogico.data.TeacherStore

/**
 * The activity is the unique owner of the open SQLiteOpenHelper. Screens must not open and
 * close competing TeacherStore helpers: doing so risks stale snapshots and locked transactions.
 * A missing provider is a programming error instead of silently opening a second database.
 */
val LocalTeacherStore = staticCompositionLocalOf<TeacherStore> {
    error("TeacherStore ausente. Forneça LocalTeacherStore na raiz da Activity.")
}
