package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)
}

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects WHERE userId = :userId ORDER BY timestamp DESC")
    fun getSubjectsForUser(userId: String): Flow<List<Subject>>

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    suspend fun getSubjectById(id: Int): Subject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: Subject): Long

    @Delete
    suspend fun deleteSubject(subject: Subject)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE userId = :userId ORDER BY isPinned DESC, timestamp DESC")
    fun getNotesForUser(userId: String): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE subjectId = :subjectId AND userId = :userId ORDER BY timestamp DESC")
    fun getNotesForSubject(subjectId: Int, userId: String): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)
}

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents WHERE userId = :userId ORDER BY timestamp DESC")
    fun getDocumentsForUser(userId: String): Flow<List<DocumentItem>>

    @Query("SELECT * FROM documents WHERE subjectId = :subjectId AND userId = :userId ORDER BY timestamp DESC")
    fun getDocumentsForSubject(subjectId: Int, userId: String): Flow<List<DocumentItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentItem)

    @Delete
    suspend fun deleteDocument(document: DocumentItem)
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance WHERE userId = :userId")
    fun getAttendanceForUser(userId: String): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance WHERE subjectId = :subjectId AND userId = :userId LIMIT 1")
    suspend fun getAttendanceForSubject(subjectId: Int, userId: String): Attendance?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: Attendance)

    @Delete
    suspend fun deleteAttendance(attendance: Attendance)
}

@Dao
interface AssignmentDao {
    @Query("SELECT * FROM assignments WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAssignmentsForUser(userId: String): Flow<List<Assignment>>

    @Query("SELECT * FROM assignments WHERE subjectId = :subjectId AND userId = :userId ORDER BY timestamp DESC")
    fun getAssignmentsForSubject(subjectId: Int, userId: String): Flow<List<Assignment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignment(assignment: Assignment)

    @Delete
    suspend fun deleteAssignment(assignment: Assignment)
}

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos WHERE userId = :userId ORDER BY isCompleted ASC, timestamp DESC")
    fun getTodosForUser(userId: String): Flow<List<TodoItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: TodoItem)

    @Delete
    suspend fun deleteTodo(todo: TodoItem)
}

@Dao
interface TimetableDao {
    @Query("SELECT * FROM timetable WHERE userId = :userId")
    fun getTimetableForUser(userId: String): Flow<List<TimetableSlot>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimetableSlot(slot: TimetableSlot)

    @Delete
    suspend fun deleteTimetableSlot(slot: TimetableSlot)
}

@Dao
interface GradeDao {
    @Query("SELECT * FROM grades WHERE userId = :userId")
    fun getGradesForUser(userId: String): Flow<List<GradeItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrade(grade: GradeItem)

    @Delete
    suspend fun deleteGrade(grade: GradeItem)
}

@Dao
interface ResultVaultDao {
    @Query("SELECT * FROM result_vault WHERE userId = :userId ORDER BY timestamp DESC")
    fun getResultsForUser(userId: String): Flow<List<ResultVaultItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: ResultVaultItem)

    @Delete
    suspend fun deleteResult(result: ResultVaultItem)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE userId = :userId ORDER BY timestamp DESC")
    fun getRemindersForUser(userId: String): Flow<List<Reminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)
}
