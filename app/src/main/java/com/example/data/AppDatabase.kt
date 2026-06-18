package com.example.data

import android.content.Context
import androidx.room.*

@Database(
    entities = [
        User::class,
        Subject::class,
        Note::class,
        DocumentItem::class,
        Attendance::class,
        Assignment::class,
        TodoItem::class,
        TimetableSlot::class,
        GradeItem::class,
        ResultVaultItem::class,
        Reminder::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun subjectDao(): SubjectDao
    abstract fun noteDao(): NoteDao
    abstract fun documentDao(): DocumentDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun assignmentDao(): AssignmentDao
    abstract fun todoDao(): TodoDao
    abstract fun timetableDao(): TimetableDao
    abstract fun gradeDao(): GradeDao
    abstract fun resultVaultDao(): ResultVaultDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "studybuddy_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
