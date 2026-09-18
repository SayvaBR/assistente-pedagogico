package com.sayvabr.assistentepedagogico.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.sayvabr.assistentepedagogico.data.TeacherStore

/**
 * A screen must use the Activity-owned helper, not open an independent SQLiteOpenHelper.
 * Missing providers fail explicitly instead of creating a second, unmanaged connection.
 */
val LocalTeacherStore = staticCompositionLocalOf<TeacherStore> {
    error("TeacherStore não foi fornecido pela Activity.")
}
