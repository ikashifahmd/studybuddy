package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val email: String,
    val fullName: String,
    val username: String,
    val passwordHash: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val code: String,
    val colorHex: String,
    val description: String,
    val userId: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectId: Int? = null,
    val title: String,
    val content: String,
    val isPinned: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String
)

@Entity(tableName = "documents")
data class DocumentItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectId: Int? = null,
    val fileName: String,
    val fileType: String, // "PDF", "DOCX", "Image", etc.
    val fileUri: String,
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String
)

@Entity(tableName = "attendance")
data class Attendance(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectId: Int,
    val attended: Int = 0,
    val total: Int = 0,
    val userId: String
)

@Entity(tableName = "assignments")
data class Assignment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectId: Int,
    val title: String,
    val dueDate: String,
    val isCompleted: Boolean = false,
    val userId: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "todos")
data class TodoItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val task: String,
    val priority: String, // "Low", "Medium", "High"
    val isCompleted: Boolean = false,
    val userId: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "timetable")
data class TimetableSlot(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dayOfWeek: String, // "Monday", "Tuesday", etc.
    val timeSlot: String, // e.g. "09:00 - 10:00"
    val subjectId: Int? = null,
    val customSubjectName: String? = null,
    val room: String? = null,
    val userId: String
)

@Entity(tableName = "grades")
data class GradeItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val semester: String, // e.g. "Semester 1", "Semester 2"
    val courseName: String,
    val creditHours: Double,
    val gradePoints: Double,
    val gradeText: String,
    val userId: String
)

@Entity(tableName = "result_vault")
data class ResultVaultItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val semester: String,
    val fileName: String,
    val fileType: String,
    val fileUri: String,
    val userId: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val dateTime: String, // Format: YYYY-MM-DD HH:MM
    val category: String, // "Exam", "Assignment", "Personal"
    val isTriggered: Boolean = false,
    val userId: String,
    val timestamp: Long = System.currentTimeMillis()
)
