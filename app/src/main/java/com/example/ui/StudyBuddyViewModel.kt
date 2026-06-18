package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StudyBuddyViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val userDao = database.userDao()
    private val subjectDao = database.subjectDao()
    private val noteDao = database.noteDao()
    private val documentDao = database.documentDao()
    private val attendanceDao = database.attendanceDao()
    private val assignmentDao = database.assignmentDao()
    private val todoDao = database.todoDao()
    private val timetableDao = database.timetableDao()
    private val gradeDao = database.gradeDao()
    private val resultVaultDao = database.resultVaultDao()
    private val reminderDao = database.reminderDao()
    private val sharedPrefs = application.getSharedPreferences("studybuddy_prefs", android.content.Context.MODE_PRIVATE)

    // Auth States
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    init {
        val cachedEmail = sharedPrefs.getString("logged_in_email", null)
        if (cachedEmail != null) {
            viewModelScope.launch {
                val user = userDao.getUserByEmail(cachedEmail)
                if (user != null) {
                    _currentUser.value = user
                }
            }
        }
    }

    // Reactive Data Streams grouped by Logged-in User ID
    @OptIn(ExperimentalCoroutinesApi::class)
    val subjects: StateFlow<List<Subject>> = _currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else subjectDao.getSubjectsForUser(user.email)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes: StateFlow<List<Note>> = _currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else noteDao.getNotesForUser(user.email)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val documents: StateFlow<List<DocumentItem>> = _currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else documentDao.getDocumentsForUser(user.email)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val attendanceList: StateFlow<List<Attendance>> = _currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else attendanceDao.getAttendanceForUser(user.email)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val assignments: StateFlow<List<Assignment>> = _currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else assignmentDao.getAssignmentsForUser(user.email)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val todos: StateFlow<List<TodoItem>> = _currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else todoDao.getTodosForUser(user.email)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val timetableSlots: StateFlow<List<TimetableSlot>> = _currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else timetableDao.getTimetableForUser(user.email)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val gradeItems: StateFlow<List<GradeItem>> = _currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else gradeDao.getGradesForUser(user.email)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val resultsField: StateFlow<List<ResultVaultItem>> = _currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else resultVaultDao.getResultsForUser(user.email)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val reminders: StateFlow<List<Reminder>> = _currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else reminderDao.getRemindersForUser(user.email)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- MODULE 1: AUTHENTICATION ACTIONS ---
    fun signUp(fullName: String, username: String, email: String, passwordHash: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authError.value = null
            if (fullName.isBlank() || username.isBlank() || email.isBlank() || passwordHash.isBlank()) {
                _authError.value = "All fields are required"
                return@launch
            }
            val existingEmail = userDao.getUserByEmail(email)
            if (existingEmail != null) {
                _authError.value = "Email is already registered"
                return@launch
            }
            val existingUser = userDao.getUserByUsername(username)
            if (existingUser != null) {
                _authError.value = "Username is already taken"
                return@launch
            }

            val newUser = User(
                email = email.trim().lowercase(),
                fullName = fullName.trim(),
                username = username.trim().lowercase(),
                passwordHash = passwordHash
            )
            userDao.insertUser(newUser)
            sharedPrefs.edit().putString("logged_in_email", newUser.email).apply()
            _currentUser.value = newUser
            onSuccess()
        }
    }

    fun login(email: String, passwordHash: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authError.value = null
            val user = userDao.getUserByEmail(email.trim().lowercase())
            if (user == null) {
                _authError.value = "Account not found"
                return@launch
            }
            if (user.passwordHash != passwordHash) {
                _authError.value = "Incorrect password"
                return@launch
            }
            sharedPrefs.edit().putString("logged_in_email", user.email).apply()
            _currentUser.value = user
            onSuccess()
        }
    }

    fun logout() {
        sharedPrefs.edit().remove("logged_in_email").apply()
        _currentUser.value = null
        _authError.value = null
    }

    // --- MODULE 3: SUBJECTS OPERATIONS ---
    fun addSubject(name: String, code: String, colorHex: String, description: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val newSubjectId = subjectDao.insertSubject(
                Subject(
                    name = name.trim(),
                    code = code.trim().uppercase(),
                    colorHex = colorHex,
                    description = description.trim(),
                    userId = user.email
                )
            )
            // Auto initialize empty attendance for any new subject
            attendanceDao.insertAttendance(
                Attendance(
                    subjectId = newSubjectId.toInt(),
                    attended = 0,
                    total = 0,
                    userId = user.email
                )
            )
        }
    }

    fun deleteSubject(subject: Subject) {
        viewModelScope.launch {
            subjectDao.deleteSubject(subject)
        }
    }

    // --- MODULE 4: QUICK NOTES ---
    fun addOrUpdateNote(id: Int = 0, subjectId: Int?, title: String, content: String, isPinned: Boolean) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            noteDao.insertNote(
                Note(
                    id = id,
                    subjectId = subjectId,
                    title = title,
                    content = content,
                    isPinned = isPinned,
                    userId = user.email
                )
            )
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            noteDao.deleteNote(note)
        }
    }

    // --- MODULE 5: DOCUMENTS SYSTEM ---
    fun addMockDocument(subjectId: Int?, fileName: String, fileType: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val mockUri = "https://firebasestorage.googleapis.com/v0/b/studybuddy/o/${fileName.replace(" ", "_")}"
            documentDao.insertDocument(
                DocumentItem(
                    subjectId = subjectId,
                    fileName = fileName,
                    fileType = fileType,
                    fileUri = mockUri,
                    userId = user.email
                )
            )
        }
    }

    fun deleteDocument(doc: DocumentItem) {
        viewModelScope.launch {
            documentDao.deleteDocument(doc)
        }
    }

    // --- MODULE 6: ATTENDANCE TRACKER ---
    fun updateAttendance(attendance: Attendance) {
        viewModelScope.launch {
            attendanceDao.insertAttendance(attendance)
        }
    }

    // --- MODULE 7: ASSIGNMENTS TRACKER ---
    fun addOrUpdateAssignment(id: Int = 0, subjectId: Int, title: String, dueDate: String, isCompleted: Boolean) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            assignmentDao.insertAssignment(
                Assignment(
                    id = id,
                    subjectId = subjectId,
                    title = title,
                    dueDate = dueDate,
                    isCompleted = isCompleted,
                    userId = user.email
                )
            )
        }
    }

    fun toggleAssignmentCompleted(assignment: Assignment) {
        viewModelScope.launch {
            assignmentDao.insertAssignment(
                assignment.copy(isCompleted = !assignment.isCompleted)
            )
        }
    }

    fun deleteAssignment(assignment: Assignment) {
        viewModelScope.launch {
            assignmentDao.deleteAssignment(assignment)
        }
    }

    // --- MODULE 8: TO-DO LIST ---
    fun addTodoItem(task: String, priority: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            todoDao.insertTodo(
                TodoItem(
                    task = task,
                    priority = priority,
                    isCompleted = false,
                    userId = user.email
                )
            )
        }
    }

    fun toggleTodoCompleted(todo: TodoItem) {
        viewModelScope.launch {
            todoDao.insertTodo(
                todo.copy(isCompleted = !todo.isCompleted)
            )
        }
    }

    fun deleteTodo(todo: TodoItem) {
        viewModelScope.launch {
            todoDao.deleteTodo(todo)
        }
    }

    // --- MODULE 9: TIMETABLE ---
    fun addTimetableSlot(dayOfWeek: String, timeSlot: String, subjectId: Int?, customName: String?, room: String?) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            timetableDao.insertTimetableSlot(
                TimetableSlot(
                    dayOfWeek = dayOfWeek,
                    timeSlot = timeSlot,
                    subjectId = subjectId,
                    customSubjectName = customName,
                    room = room,
                    userId = user.email
                )
            )
        }
    }

    fun deleteTimetableSlot(slot: TimetableSlot) {
        viewModelScope.launch {
            timetableDao.deleteTimetableSlot(slot)
        }
    }

    // --- MODULE 10: CGPA CALCULATOR ---
    fun addGradeItem(semester: String, courseName: String, creditHours: Double, gradePoints: Double, gradeText: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            gradeDao.insertGrade(
                GradeItem(
                    semester = semester,
                    courseName = courseName,
                    creditHours = creditHours,
                    gradePoints = gradePoints,
                    gradeText = gradeText,
                    userId = user.email
                )
            )
        }
    }

    fun deleteGradeItem(grade: GradeItem) {
        viewModelScope.launch {
            gradeDao.deleteGrade(grade)
        }
    }

    // --- MODULE 11: RESULTS VAULT ---
    fun addResultItem(semester: String, fileName: String, fileType: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val mockUri = "https://firebasestorage.googleapis.com/v0/b/studybuddy/o/results_${fileName.replace(" ", "_")}"
            resultVaultDao.insertResult(
                ResultVaultItem(
                    semester = semester,
                    fileName = fileName,
                    fileType = fileType,
                    fileUri = mockUri,
                    userId = user.email
                )
            )
        }
    }

    fun deleteResultItem(result: ResultVaultItem) {
        viewModelScope.launch {
            resultVaultDao.deleteResult(result)
        }
    }

    // --- MODULE 12: SMART REMINDERS ---
    fun addReminder(title: String, dateTime: String, category: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            reminderDao.insertReminder(
                Reminder(
                    title = title,
                    dateTime = dateTime,
                    category = category,
                    userId = user.email
                )
            )
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            reminderDao.deleteReminder(reminder)
        }
    }

    // Seed Sandbox Data to easily preview the app complete with realistic stats
    fun seedSandboxData() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            // Check if user already has data to avoid over-seeding
            val currentSubs = subjectDao.getSubjectsForUser(user.email).first()
            if (currentSubs.isNotEmpty()) return@launch

            // 1. Seed Subjects
            val mathId = subjectDao.insertSubject(Subject(name = "Advanced Calculus", code = "MATH301", colorHex = "#FF5733", description = "Limits, integration, and multivariate differential concepts.", userId = user.email))
            val csId = subjectDao.insertSubject(Subject(name = "Mobile App Architecture", code = "CS420", colorHex = "#33FF57", description = "Android Architecture, Jetpack Compose, state flow.", userId = user.email))
            val phyId = subjectDao.insertSubject(Subject(name = "Quantum Physics", code = "PHYS210", colorHex = "#3357FF", description = "Wave-particle duality, Schrodinger equations.", userId = user.email))

            // 2. Attendance Values (Auto init sets 0/0, let's update with realistic attendance)
            attendanceDao.insertAttendance(Attendance(subjectId = mathId.toInt(), attended = 18, total = 20, userId = user.email))
            attendanceDao.insertAttendance(Attendance(subjectId = csId.toInt(), attended = 15, total = 22, userId = user.email)) // Below 75% warning trigger!
            attendanceDao.insertAttendance(Attendance(subjectId = phyId.toInt(), attended = 12, total = 14, userId = user.email))

            // 3. Quick Notes
            noteDao.insertNote(Note(subjectId = mathId.toInt(), title = "Derivatives cheat sheet", content = "d/dx (sin x) = cos x\nd/dx (cos x) = -sin x\nChain rule: dy/dx = dy/du * du/dx", isPinned = true, userId = user.email))
            noteDao.insertNote(Note(subjectId = csId.toInt(), title = "M3 Layout Essentials", content = "Centralized Theme.kt is mandatory. Always make touch targets >48dp. Keep elements in standard 8dp grid system.", isPinned = false, userId = user.email))
            noteDao.insertNote(Note(subjectId = null, title = "Project ideas for winter", content = "1. Personal finance app styled with Material design\n2. Real-time note taker for lectures with audio summaries", isPinned = true, userId = user.email))

            // 4. Documents
            documentDao.insertDocument(DocumentItem(subjectId = csId.toInt(), fileName = "Android_Design_Guidelines.pdf", fileType = "PDF", fileUri = "https://firebasestorage.googleapis.com/v0/b/studybuddy/o/Android_Design_Guidelines.pdf", userId = user.email))
            documentDao.insertDocument(DocumentItem(subjectId = phyId.toInt(), fileName = "Dirac_Notation_Lecture.pdf", fileType = "PDF", fileUri = "https://firebasestorage.googleapis.com/v0/b/studybuddy/o/Dirac_Notation_Lecture.pdf", userId = user.email))
            documentDao.insertDocument(DocumentItem(subjectId = phyId.toInt(), fileName = "lab_experiment_1.png", fileType = "Image", fileUri = "https://firebasestorage.googleapis.com/v0/b/studybuddy/o/lab_experiment_1.png", userId = user.email))

            // 5. Assignments
            assignmentDao.insertAssignment(Assignment(subjectId = mathId.toInt(), title = "Multivariate Integration Homework 4", dueDate = "2026-06-25", isCompleted = false, userId = user.email))
            assignmentDao.insertAssignment(Assignment(subjectId = csId.toInt(), title = "Lab 2: Room Integration & FlatMap", dueDate = "2026-06-20", isCompleted = true, userId = user.email))
            assignmentDao.insertAssignment(Assignment(subjectId = phyId.toInt(), title = "Thermodynamics Essay Draft", dueDate = "2026-06-30", isCompleted = false, userId = user.email))

            // 6. To-Dos
            todoDao.insertTodo(TodoItem(task = "Submit lab notebook", priority = "High", isCompleted = false, userId = user.email))
            todoDao.insertTodo(TodoItem(task = "Buy physics textbook", priority = "Low", isCompleted = true, userId = user.email))
            todoDao.insertTodo(TodoItem(task = "Setup GitHub repo", priority = "Medium", isCompleted = false, userId = user.email))

            // 7. Timetable
            timetableDao.insertTimetableSlot(TimetableSlot(dayOfWeek = "Monday", timeSlot = "09:00 - 10:30", subjectId = mathId.toInt(), room = "Room 401", userId = user.email))
            timetableDao.insertTimetableSlot(TimetableSlot(dayOfWeek = "Monday", timeSlot = "11:00 - 12:30", subjectId = phyId.toInt(), room = "Lab B", userId = user.email))
            timetableDao.insertTimetableSlot(TimetableSlot(dayOfWeek = "Wednesday", timeSlot = "09:00 - 10:30", subjectId = csId.toInt(), room = "Coding Lab 2", userId = user.email))
            timetableDao.insertTimetableSlot(TimetableSlot(dayOfWeek = "Thursday", timeSlot = "14:00 - 15:30", subjectId = mathId.toInt(), room = "Room 401", userId = user.email))

            // 8. Grades for CGPA
            gradeDao.insertGrade(GradeItem(semester = "Semester 1", courseName = "Intro to Programming", creditHours = 4.0, gradePoints = 4.0, gradeText = "A", userId = user.email))
            gradeDao.insertGrade(GradeItem(semester = "Semester 1", courseName = "Intro to Mechanics", creditHours = 3.0, gradePoints = 3.7, gradeText = "A-", userId = user.email))
            gradeDao.insertGrade(GradeItem(semester = "Semester 1", courseName = "Calculus I", creditHours = 4.0, gradePoints = 3.3, gradeText = "B+", userId = user.email))

            // 9. Results Vault
            resultVaultDao.insertResult(ResultVaultItem(semester = "Semester 1", fileName = "Sem_1_Official_Transcript.pdf", fileType = "PDF", fileUri = "https://firebasestorage.googleapis.com/v0/b/studybuddy/o/Sem_1_Official_Transcript.pdf", userId = user.email))

            // 10. Reminders
            reminderDao.insertReminder(Reminder(title = "Study for Physics Midterm", dateTime = "2026-06-22 10:00", category = "Exam", userId = user.email))
            reminderDao.insertReminder(Reminder(title = "Calculus Homework Submission", dateTime = "2026-06-25 23:59", category = "Assignment", userId = user.email))
        }
    }
}
