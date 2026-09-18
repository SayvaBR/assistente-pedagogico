package com.sayvabr.assistentepedagogico.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class LessonActivityV7InstrumentedTest {
    private lateinit var file: File
    private lateinit var db: SQLiteDatabase

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        file = File(context.cacheDir, "activity-v7-${System.nanoTime()}.db")
        db = SQLiteDatabase.openOrCreateDatabase(file, null)
        db.setForeignKeyConstraintsEnabled(true)
        db.execSQL("CREATE TABLE classrooms (id INTEGER PRIMARY KEY, name TEXT NOT NULL)")
        db.execSQL("CREATE TABLE lessons (id INTEGER PRIMARY KEY, classroom_id INTEGER NOT NULL REFERENCES classrooms(id) ON DELETE CASCADE)")
        db.execSQL("INSERT INTO classrooms(id,name) VALUES (1,'Turma Azul'),(2,'Turma Branca')")
        db.execSQL("INSERT INTO lessons(id,classroom_id) VALUES (10,1),(20,2)")
        LessonActivityV7.migrate(db)
    }

    @After fun tearDown() { db.close(); file.delete() }

    @Test fun createListUpdateDelete_preservesClassroomAndLessonLink() {
        val id = LessonActivityV7.create(db, LessonActivityV7.Input(1, 10, "Leitura guiada", "Ler o texto e registrar hipóteses.", 25))
        var activity = LessonActivityV7.list(db, 1, 10).single()
        assertEquals(id, activity.id)
        assertEquals("Leitura guiada", activity.title)
        assertEquals(25, activity.durationMinutes)

        LessonActivityV7.update(db, id, LessonActivityV7.Input(1, 10, "Leitura em dupla", "Ler em dupla e comparar as hipóteses.", 30))
        activity = LessonActivityV7.list(db, 1, 10).single()
        assertEquals("Leitura em dupla", activity.title)
        assertEquals(30, activity.durationMinutes)

        LessonActivityV7.delete(db, 1, id)
        assertEquals(emptyList<LessonActivityV7.Activity>(), LessonActivityV7.list(db, 1, 10))
    }

    @Test fun reusableActivity_canExistWithoutLesson() {
        LessonActivityV7.create(db, LessonActivityV7.Input(1, null, "Roda de leitura", "Selecionar um texto e organizar a roda.", 20))
        val activity = LessonActivityV7.list(db, 1).single()
        assertNull(activity.lessonId)
    }

    @Test fun crossClassLessonLink_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            LessonActivityV7.create(db, LessonActivityV7.Input(1, 20, "Atividade inválida", "Não pode cruzar turmas diferentes.", 20))
        }
        assertEquals(emptyList<LessonActivityV7.Activity>(), LessonActivityV7.list(db, 1))
    }

    @Test fun validation_rejectsInvalidDurationAndShortInstructions() {
        assertThrows(IllegalArgumentException::class.java) {
            LessonActivityV7.validated(LessonActivityV7.Input(1, 10, "Teste", "curto", 500))
        }
        assertThrows(IllegalArgumentException::class.java) {
            LessonActivityV7.validated(LessonActivityV7.Input(1, 10, "Teste", "x", 20))
        }
    }
}
