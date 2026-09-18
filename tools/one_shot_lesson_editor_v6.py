#!/usr/bin/env python3
from pathlib import Path
p=Path('app/src/main/java/com/sayvabr/assistentepedagogico/ui/TeacherApp.kt')
s=p.read_text(encoding='utf-8')
old='''                    "newLesson" -> if (currentClass != null) LessonForm(currentClass, selectedDay, { requestBack() },
                        { title, subject, day, time, objective, content, method ->
                            commit("planning", onSuccess = { selectedDay = day }) {
                                store.saveLesson(currentClass.id, title, subject, day, time, objective, content, method)
                            }
                        }, onDirty = { formDirty = true })
                    "editLesson" -> snapshot.lessons.firstOrNull { it.id == selectedLesson && !it.archived }?.let { lesson ->
                        snapshot.classrooms.firstOrNull { it.id == lesson.classroomId && !it.archived }?.let { lessonClass ->
                            LessonForm(lessonClass, lesson.date, { requestBack() }, { title, subject, day, time, objective, content, method ->
                                commit("planning", onSuccess = { selectedDay = day }) {
                                    store.updateLesson(lessonClass.id, lesson.id, title, subject, day, time, objective, content, method)
                                }
                            }, initial = lesson, archive = { commit("planning") { store.setLessonArchived(lessonClass.id, lesson.id, true) } },
                                onDirty = { formDirty = true })
                        }
                    } ?: Panel { Text("Plano indisponível. Retorne ao Planejamento.", color = ink); PrimaryButton("Voltar ao planejamento") { navigate("planning", root = true) } }'''
new='''                    "newLesson" -> if (currentClass != null) LessonEditorV6(
                        classroom = currentClass, initialDay = selectedDay, back = { requestBack() },
                        save = { input -> commit("planning", onSuccess = { selectedDay = input.day }) {
                            store.saveLesson(input, currentClass.id)
                        } }, onDirty = { formDirty = true })
                    "editLesson" -> snapshot.lessons.firstOrNull { it.id == selectedLesson && !it.archived }?.let { lesson ->
                        snapshot.classrooms.firstOrNull { it.id == lesson.classroomId && !it.archived }?.let { lessonClass ->
                            LessonEditorV6(
                                classroom = lessonClass, initialDay = lesson.date, initial = lesson, back = { requestBack() },
                                save = { input -> commit("planning", onSuccess = { selectedDay = input.day }) {
                                    store.updateLesson(lessonClass.id, lesson.id, input)
                                } },
                                archive = { commit("planning") { store.setLessonArchived(lessonClass.id, lesson.id, true) } },
                                onDirty = { formDirty = true })
                        }
                    } ?: Panel { Text("Plano indisponível. Retorne ao Planejamento.", color = ink); PrimaryButton("Voltar ao planejamento") { navigate("planning", root = true) } }'''
if s.count(old)!=1: raise SystemExit(f'Expected old lesson route once, found {s.count(old)}')
s=s.replace(old,new,1)
p.write_text(s,encoding='utf-8')
