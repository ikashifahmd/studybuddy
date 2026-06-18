package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import java.text.SimpleDateFormat
import java.util.*

// Main layout navigation
sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Dashboard : Screen("dashboard")
    object Subjects : Screen("subjects")
    object Notes : Screen("notes")
    object Tasks : Screen("tasks") // Includes To-Dos & Assignments
    object Timetable : Screen("timetable")
    object Cgpa : Screen("cgpa")
    object Results : Screen("results")
    object Reminders : Screen("reminders")
    data class SubjectDetail(val subjectId: Int) : Screen("subject_detail/$subjectId")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyBuddyApp(viewModel: StudyBuddyViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Observe all tables
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val documents by viewModel.documents.collectAsStateWithLifecycle()
    val attendanceList by viewModel.attendanceList.collectAsStateWithLifecycle()
    val assignments by viewModel.assignments.collectAsStateWithLifecycle()
    val todos by viewModel.todos.collectAsStateWithLifecycle()
    val timetableSlots by viewModel.timetableSlots.collectAsStateWithLifecycle()
    val gradeItems by viewModel.gradeItems.collectAsStateWithLifecycle()
    val results by viewModel.resultsField.collectAsStateWithLifecycle()
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()

    // Navigation State
    var currentScreenState by remember { mutableStateOf<String>(Screen.Auth.route) }
    var selectedSubjectIdForDetails by remember { mutableStateOf<Int?>(null) }

    // Sync screen changes when login state updates
    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            currentScreenState = Screen.Auth.route
            selectedSubjectIdForDetails = null
        } else if (currentScreenState == Screen.Auth.route) {
            currentScreenState = Screen.Dashboard.route
        }
    }

    Scaffold(
        bottomBar = {
            if (currentUser != null) {
                NavigationBar(
                    modifier = Modifier.testTag("app_navigation_bar")
                ) {
                    val items = listOf(
                        Triple(Screen.Dashboard.route, "Dashboard", Icons.Default.Dashboard),
                        Triple(Screen.Subjects.route, "Subjects", Icons.Default.FolderOpen),
                        Triple(Screen.Notes.route, "Notes", Icons.Default.EditNote),
                        Triple(Screen.Tasks.route, "Tasks", Icons.Default.PlaylistAddCheck)
                    )
                    items.forEach { (route, label, icon) ->
                        NavigationBarItem(
                            selected = (currentScreenState == route && selectedSubjectIdForDetails == null),
                            onClick = {
                                selectedSubjectIdForDetails = null
                                currentScreenState = route
                            },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, fontSize = 11.sp) },
                            modifier = Modifier.testTag("nav_${route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                currentUser == null -> {
                    AuthScreen(
                        authError = authError,
                        onLogin = { email, pass -> viewModel.login(email, pass, {}) },
                        onSignUp = { name, user, email, pass -> viewModel.signUp(name, user, email, pass, {}) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                selectedSubjectIdForDetails != null -> {
                    val subjectId = selectedSubjectIdForDetails!!
                    val subject = subjects.find { it.id == subjectId }
                    if (subject != null) {
                        SubjectOrganizerScreen(
                            subject = subject,
                            notes = notes.filter { it.subjectId == subjectId },
                            documents = documents.filter { it.subjectId == subjectId },
                            assignments = assignments.filter { it.subjectId == subjectId },
                            attendance = attendanceList.find { it.subjectId == subjectId },
                            onBack = { selectedSubjectIdForDetails = null },
                            onAddNote = { title, content -> viewModel.addOrUpdateNote(subjectId = subjectId, title = title, content = content, isPinned = false) },
                            onDeleteNote = { viewModel.deleteNote(it) },
                            onUploadDoc = { name, type -> viewModel.addMockDocument(subjectId, name, type) },
                            onDeleteDoc = { viewModel.deleteDocument(it) },
                            onAddAssignment = { title, date -> viewModel.addOrUpdateAssignment(subjectId = subjectId, title = title, dueDate = date, isCompleted = false) },
                            onToggleAssignment = { viewModel.toggleAssignmentCompleted(it) },
                            onDeleteAssignment = { viewModel.deleteAssignment(it) },
                            onUpdateAttendance = { attended, total ->
                                val current = attendanceList.find { it.subjectId == subjectId }
                                if (current != null) {
                                    viewModel.updateAttendance(current.copy(attended = attended, total = total))
                                } else {
                                    viewModel.updateAttendance(Attendance(subjectId = subjectId, attended = attended, total = total, userId = currentUser!!.email))
                                }
                            }
                        )
                    } else {
                        selectedSubjectIdForDetails = null
                    }
                }
                else -> {
                    when (currentScreenState) {
                        Screen.Dashboard.route -> {
                            DashboardScreen(
                                currentUser = currentUser!!,
                                subjectsCount = subjects.size,
                                attendanceList = attendanceList,
                                subjects = subjects,
                                recentNotes = notes.take(3),
                                upcomingAssignments = assignments.filter { !it.isCompleted },
                                onNavigate = { route -> currentScreenState = route },
                                onSelectSubject = { id -> selectedSubjectIdForDetails = id },
                                onLogout = { viewModel.logout() },
                                onSeedSandbox = {
                                    viewModel.seedSandboxData()
                                    Toast.makeText(context, "Sandbox sandbox initialized!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        Screen.Subjects.route -> {
                            SubjectsScreen(
                                subjects = subjects,
                                attendanceList = attendanceList,
                                onAddSubject = { name, code, color, desc -> viewModel.addSubject(name, code, color, desc) },
                                onDeleteSubject = { viewModel.deleteSubject(it) },
                                onSelectSubject = { id -> selectedSubjectIdForDetails = id }
                            )
                        }
                        Screen.Notes.route -> {
                            NotesScreen(
                                notes = notes,
                                subjects = subjects,
                                onAddNote = { title, content, subId, pin -> viewModel.addOrUpdateNote(subjectId = subId, title = title, content = content, isPinned = pin) },
                                onDeleteNote = { viewModel.deleteNote(it) }
                            )
                        }
                        Screen.Tasks.route -> {
                            TasksScreen(
                                todos = todos,
                                assignments = assignments,
                                subjects = subjects,
                                onAddTodo = { task, p -> viewModel.addTodoItem(task, p) },
                                onToggleTodo = { viewModel.toggleTodoCompleted(it) },
                                onDeleteTodo = { viewModel.deleteTodo(it) },
                                onAddAssignment = { subId, title, due -> viewModel.addOrUpdateAssignment(subjectId = subId, title = title, dueDate = due, isCompleted = false) },
                                onToggleAssignment = { viewModel.toggleAssignmentCompleted(it) },
                                onDeleteAssignment = { viewModel.deleteAssignment(it) }
                            )
                        }
                        Screen.Timetable.route -> {
                            TimetableScreen(
                                slots = timetableSlots,
                                subjects = subjects,
                                onAddSlot = { day, slot, subId, custom, room -> viewModel.addTimetableSlot(day, slot, subId, custom, room) },
                                onDeleteSlot = { viewModel.deleteTimetableSlot(it) },
                                onBack = { currentScreenState = Screen.Dashboard.route }
                            )
                        }
                        Screen.Cgpa.route -> {
                            CgpaScreen(
                                gradeItems = gradeItems,
                                onAddGrade = { sem, course, hours, points, txt -> viewModel.addGradeItem(sem, course, hours, points, txt) },
                                onDeleteGrade = { viewModel.deleteGradeItem(it) },
                                onBack = { currentScreenState = Screen.Dashboard.route }
                            )
                        }
                        Screen.Results.route -> {
                            ResultsVaultScreen(
                                items = results,
                                onAddResult = { sem, name, type -> viewModel.addResultItem(sem, name, type) },
                                onDeleteResult = { viewModel.deleteResultItem(it) },
                                onBack = { currentScreenState = Screen.Dashboard.route }
                            )
                        }
                        Screen.Reminders.route -> {
                            RemindersScreen(
                                reminders = reminders,
                                onAddReminder = { title, dateText, cat -> viewModel.addReminder(title, dateText, cat) },
                                onDeleteReminder = { viewModel.deleteReminder(it) },
                                onBack = { currentScreenState = Screen.Dashboard.route }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// MODULE 1: AUTHENTICATION SCREEN
// ==========================================
@Composable
fun AuthScreen(
    authError: String?,
    onLogin: (email: String, passwordHash: String) -> Unit,
    onSignUp: (name: String, user: String, email: String, passwordHash: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isLoginMode by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Circular Glowing Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3B82F6)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "StudyBuddy App Icon",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "StudyBuddy",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.SansSerif
                )

                Text(
                    text = if (isLoginMode) "Welcome back to your study organizer" else "Create an offline study companion",
                    fontSize = 13.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (authError != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = authError,
                            color = Color(0xFFFCA5A5),
                            modifier = Modifier.padding(12.dp),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (!isLoginMode) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color(0xFF3B82F6),
                            unfocusedContainerColor = Color(0xFF0F172A),
                            focusedContainerColor = Color(0xFF0F172A)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("signup_name_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null) },
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color(0xFF3B82F6),
                            unfocusedContainerColor = Color(0xFF0F172A),
                            focusedContainerColor = Color(0xFF0F172A)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("signup_username_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color(0xFF3B82F6),
                        unfocusedContainerColor = Color(0xFF0F172A),
                        focusedContainerColor = Color(0xFF0F172A)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_email_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color(0xFF3B82F6),
                        unfocusedContainerColor = Color(0xFF0F172A),
                        focusedContainerColor = Color(0xFF0F172A)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_password_input"),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (isLoginMode) {
                            onLogin(email, password)
                        } else {
                            onSignUp(name, username, email, password)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("auth_submit_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isLoginMode) "Log In" else "Sign Up",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = { isLoginMode = !isLoginMode },
                    modifier = Modifier.testTag("toggle_auth_mode")
                ) {
                    Text(
                        text = if (isLoginMode) "Don't have an account? Sign Up" else "Already absolute registered? Log In",
                        color = Color(0xFF60A5FA),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// ==========================================
// MODULE 2: DASHBOARD SCREEN
// ==========================================
@Composable
fun DashboardScreen(
    currentUser: User,
    subjectsCount: Int,
    attendanceList: List<Attendance>,
    subjects: List<Subject>,
    recentNotes: List<Note>,
    upcomingAssignments: List<Assignment>,
    onNavigate: (String) -> Unit,
    onSelectSubject: (Int) -> Unit,
    onLogout: () -> Unit,
    onSeedSandbox: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .verticalScroll(rememberScrollState())
    ) {
        // Welcome Header Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1E3A8A), Color(0xFF0F172A))
                    )
                )
                .padding(vertical = 24.dp, horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hello, ${currentUser.fullName}!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Let's organize your studies today",
                        fontSize = 13.sp,
                        color = Color.LightGray
                    )
                }
                Row {
                    IconButton(
                        onClick = onSeedSandbox,
                        modifier = Modifier.testTag("seed_sandbox_button")
                    ) {
                        Icon(Icons.Default.CardGiftcard, contentDescription = "Seed SandBox Data", tint = Color(0xFF10B981))
                    }
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.testTag("logout_button")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = "Sign Out", tint = Color(0xFFEF4444))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // MODULE 2: OVERVIEW CARDS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OverviewStatCard(
                title = "Subjects",
                value = subjectsCount.toString(),
                icon = Icons.Default.LibraryBooks,
                color = Color(0xFF3B82F6),
                modifier = Modifier.weight(1f)
            )

            val avgAttendance = if (attendanceList.isNotEmpty()) {
                val sum = attendanceList.sumOf {
                    if (it.total > 0) (it.attended.toDouble() / it.total) * 100 else 100.0
                }
                (sum / attendanceList.size).toInt()
            } else 0

            OverviewStatCard(
                title = "Attendance",
                value = "$avgAttendance%",
                icon = Icons.Default.EventAvailable,
                color = if (avgAttendance < 75 && attendanceList.isNotEmpty()) Color(0xFFEF4444) else Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OverviewStatCard(
                title = "To-Do Checked",
                value = upcomingAssignments.size.toString(),
                icon = Icons.Default.AssignmentLate,
                color = Color(0xFFF59E0B),
                modifier = Modifier.weight(1f)
            )

            OverviewStatCard(
                title = "Recent Notes",
                value = recentNotes.size.toString(),
                icon = Icons.Default.EditNote,
                color = Color(0xFF8B5CF6),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // QUICK NAVIGATION GRID FOR ALL 13 MODULES/AUX FEATURES
        Text(
            text = "Study modules",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        val menuItems = listOf(
            Triple(Screen.Subjects.route, "Subjects Organizer & Files", Icons.Default.FolderZip),
            Triple(Screen.Notes.route, "Quick Notes Take", Icons.Default.Book),
            Triple(Screen.Tasks.route, "Assignments & Tasks", Icons.Default.TaskAlt),
            Triple(Screen.Timetable.route, "Timetable Scheduler", Icons.Default.CalendarViewWeek),
            Triple(Screen.Cgpa.route, "CGPA grade Calculator", Icons.Default.Calculate),
            Triple(Screen.Results.route, "Results Vault Sheets", Icons.Default.WorkspacePremium),
            Triple(Screen.Reminders.route, "Smart Reminders", Icons.Default.NotificationsActive)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(menuItems) { (route, label, icon) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clickable { onNavigate(route) }
                        .testTag("menu_item_${route}"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Icon(icon, contentDescription = label, tint = Color(0xFF60A5FA), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // QUICK VIEW SUBJECTS & LINKS
        if (subjects.isNotEmpty()) {
            Text(
                text = "My Subject folders",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                subjects.forEach { subject ->
                    val colorHex = subject.colorHex
                    val cardColor = try {
                        Color(android.graphics.Color.parseColor(colorHex))
                    } catch (e: Exception) {
                        Color(0xFF3B82F6)
                    }

                    Card(
                        modifier = Modifier
                            .width(150.dp)
                            .height(100.dp)
                            .clickable { onSelectSubject(subject.id) }
                            .testTag("quick_subject_${subject.id}"),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(cardColor)
                            )
                            Column {
                                Text(
                                    text = subject.name,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = subject.code,
                                    color = Color.LightGray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun OverviewStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = title, fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ==========================================
// MODULE 3: SUBJECTS MODULE
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectsScreen(
    subjects: List<Subject>,
    attendanceList: List<Attendance>,
    onAddSubject: (name: String, code: String, colorHex: String, description: String) -> Unit,
    onDeleteSubject: (Subject) -> Unit,
    onSelectSubject: (Int) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var selectedColorIndex by remember { mutableStateOf(0) }
    var desc by remember { mutableStateOf("") }

    val colors = listOf("#3B82F6", "#EF4444", "#10B981", "#F59E0B", "#EC4899", "#8B5CF6")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subjects Organizer", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF3B82F6),
                modifier = Modifier.testTag("add_subject_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Subject", tint = Color.White)
            }
        },
        containerColor = Color(0xFF0F172A)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF0F172A))
        ) {
            if (subjects.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.FolderOpen,
                    mainText = "No subjects configured yet.",
                    subText = "Tap + to construct details like code, track documents, notes, & homework files for each course."
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(subjects) { subject ->
                        val colorHex = subject.colorHex
                        val folderColor = try {
                            Color(android.graphics.Color.parseColor(colorHex))
                        } catch (e: Exception) {
                            Color(0xFF3B82F6)
                        }

                        val attendance = attendanceList.find { it.subjectId == subject.id }
                        val attendPercent = if (attendance != null && attendance.total > 0) {
                            (attendance.attended.toDouble() / attendance.total) * 100
                        } else 0.0

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectSubject(subject.id) }
                                .testTag("subject_card_${subject.id}"),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(folderColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Folder, contentDescription = null, tint = folderColor, modifier = Modifier.size(28.dp))
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = subject.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text(text = subject.code, color = Color.Gray, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    if (attendance != null) {
                                        Text(
                                            text = "Attendance: ${attendance.attended}/${attendance.total} (${attendPercent.toInt()}%)",
                                            fontSize = 11.sp,
                                            color = if (attendPercent < 75 && attendance.total > 0) Color(0xFFEF4444) else Color.LightGray
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { onDeleteSubject(subject) },
                                    modifier = Modifier.testTag("delete_subject_${subject.id}")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete course", tint = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Create Subject", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Subject Name") },
                        modifier = Modifier.fillMaxWidth().testTag("add_subject_name_input")
                    )
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Subject Code") },
                        modifier = Modifier.fillMaxWidth().testTag("add_subject_code_input")
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Brief Description") },
                        modifier = Modifier.fillMaxWidth().testTag("add_subject_desc_input")
                    )
                    Text("Select Color Folder Accent:", color = Color.White, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        colors.forEachIndexed { index, hex ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                    .clickable { selectedColorIndex = index }
                                    .then(
                                        if (selectedColorIndex == index) Modifier.background(
                                            Color.White.copy(alpha = 0.4f)
                                        ) else Modifier
                                    )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onAddSubject(name, code, colors[selectedColorIndex], desc)
                            name = ""
                            code = ""
                            desc = ""
                            showAddDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_add_subject")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}

// ==========================================
// MODULE 4: QUICK NOTES MODULE
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    notes: List<Note>,
    subjects: List<Subject>,
    onAddNote: (title: String, content: String, subjectId: Int?, isPinned: Boolean) -> Unit,
    onDeleteNote: (Note) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedSubjectId by remember { mutableStateOf<Int?>(null) }
    var isPinned by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<Note?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredNotes = notes.filter {
        it.title.contains(searchQuery, ignoreCase = true) || it.content.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Color(0xFF0F172A))) {
                TopAppBar(
                    title = { Text("Lectures & Quick Notes", color = Color.White) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search your notes...", color = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("search_notes_field"),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.LightGray) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B)
                    )
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingNote = null
                    title = ""
                    content = ""
                    selectedSubjectId = null
                    isPinned = false
                    showDialog = true
                },
                containerColor = Color(0xFF3B82F6),
                modifier = Modifier.testTag("add_note_fab")
            ) {
                Icon(Icons.Default.NoteAdd, contentDescription = "Add Note", tint = Color.White)
            }
        },
        containerColor = Color(0xFF0F172A)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (filteredNotes.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.EditNote,
                    mainText = if (searchQuery.isEmpty()) "Notebook empty" else "No notes found",
                    subText = "Take quick notes with auto-save & pin support for study reference."
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredNotes) { note ->
                        val sub = subjects.find { it.id == note.subjectId }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    editingNote = note
                                    title = note.title
                                    content = note.content
                                    selectedSubjectId = note.subjectId
                                    isPinned = note.isPinned
                                    showDialog = true
                                }
                                .testTag("note_item_${note.id}"),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (note.isPinned) {
                                            Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(text = note.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                    IconButton(onClick = { onDeleteNote(note) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(20.dp))
                                    }
                                }
                                if (sub != null) {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = try {
                                                Color(android.graphics.Color.parseColor(sub.colorHex)).copy(alpha = 0.2f)
                                            } catch (e: Exception) {
                                                Color(0xFF3B82F6).copy(alpha = 0.2f)
                                            }
                                        ),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = sub.name,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = note.content,
                                    color = Color.LightGray,
                                    fontSize = 13.sp,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editingNote != null) "Edit Note" else "Create Note", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth().testTag("note_title_input")
                    )

                    OutlinedTextField(
                        value = content,
                        onValueChange = {
                            content = it
                            // Simulate Auto-Save Feature:
                            if (editingNote != null) {
                                onAddNote(title, content, selectedSubjectId, isPinned)
                            }
                        },
                        label = { Text("Lecture Notes Content") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .testTag("note_content_input")
                    )

                    // Link to folder subject
                    Text("Associate with subject folder:", color = Color.White, fontSize = 12.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedSubjectId == null) Color(0xFF3B82F6) else Color(0xFF0F172A)
                            ),
                            modifier = Modifier.clickable { selectedSubjectId = null }
                        ) {
                            Text("None", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                        }
                        subjects.forEach { subject ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedSubjectId == subject.id) Color(0xFF3B82F6) else Color(0xFF0F172A)
                                ),
                                modifier = Modifier.clickable { selectedSubjectId = subject.id }
                            ) {
                                Text(subject.name, color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isPinned, onCheckedChange = { isPinned = it })
                        Text("Pin at the top of notebooks", color = Color.White, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onAddNote(title, content, selectedSubjectId, isPinned)
                            title = ""
                            content = ""
                            selectedSubjectId = null
                            isPinned = false
                            showDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_submit_note")
                ) {
                    Text("Save Note")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}

// ==========================================
// MODULE 7 & MODULE 8: TASKS SCREEN (To-Dos + Homework)
// ==========================================
@Composable
fun TasksScreen(
    todos: List<TodoItem>,
    assignments: List<Assignment>,
    subjects: List<Subject>,
    onAddTodo: (task: String, priority: String) -> Unit,
    onToggleTodo: (TodoItem) -> Unit,
    onDeleteTodo: (TodoItem) -> Unit,
    onAddAssignment: (subId: Int, title: String, due: String) -> Unit,
    onToggleAssignment: (Assignment) -> Unit,
    onDeleteAssignment: (Assignment) -> Unit
) {
    var activeTab by remember { mutableStateOf(0) } // 0 = To-Dos, 1 = Assignments

    // Todo states
    var todoText by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf("Medium") }

    // Assignment states
    var assignmentText by remember { mutableStateOf("") }
    var selectedSubjectId by remember { mutableStateOf<Int?>(null) }
    var dueDateText by remember { mutableStateOf("2026-06-25") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = Color(0xFF1E293B),
            contentColor = Color.White
        ) {
            Tab(selected = activeTab == 0, onClick = { activeTab = 0 }, text = { Text("To-Do Checklist", fontWeight = FontWeight.Bold) })
            Tab(selected = activeTab == 1, onClick = { activeTab = 1 }, text = { Text("Subject Assignments", fontWeight = FontWeight.Bold) })
        }

        if (activeTab == 0) {
            // To-Do list UI
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Add Checklist Item", color = Color.White, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = todoText,
                            onValueChange = { todoText = it },
                            label = { Text("What needs to be done?") },
                            modifier = Modifier.fillMaxWidth().testTag("todo_input")
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Priority:", color = Color.White, fontSize = 12.sp)
                            listOf("Low", "Medium", "High").forEach { p ->
                                FilterChip(
                                    selected = selectedPriority == p,
                                    onClick = { selectedPriority = p },
                                    label = { Text(p) }
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (todoText.isNotBlank()) {
                                    onAddTodo(todoText, selectedPriority)
                                    todoText = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("add_todo_button")
                        ) {
                            Text("Add Task")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(todos) { item ->
                        val priorityColor = when (item.priority) {
                            "High" -> Color(0xFFEF4444)
                            "Medium" -> Color(0xFFF59E0B)
                            else -> Color(0xFF10B981)
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = item.isCompleted, onCheckedChange = { onToggleTodo(item) })
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.task,
                                        color = if (item.isCompleted) Color.Gray else Color.White,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = priorityColor.copy(alpha = 0.2f)),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Text(
                                            item.priority,
                                            fontSize = 9.sp,
                                            color = priorityColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                IconButton(onClick = { onDeleteTodo(item) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Assignments Tracker
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Add Class Assignment", color = Color.White, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = assignmentText,
                            onValueChange = { assignmentText = it },
                            label = { Text("Assignment title or lab name") },
                            modifier = Modifier.fillMaxWidth().testTag("assign_input")
                        )

                        OutlinedTextField(
                            value = dueDateText,
                            onValueChange = { dueDateText = it },
                            label = { Text("Due Date (YYYY-MM-DD)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Subject Link selection
                        Text("Associate with study folder:", color = Color.White, fontSize = 12.sp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            subjects.forEach { s ->
                                FilterChip(
                                    selected = selectedSubjectId == s.id,
                                    onClick = { selectedSubjectId = s.id },
                                    label = { Text(s.name) }
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (assignmentText.isNotBlank() && selectedSubjectId != null) {
                                    onAddAssignment(selectedSubjectId!!, assignmentText, dueDateText)
                                    assignmentText = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("add_assign_button")
                        ) {
                            Text("Save Assignment")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(assignments) { item ->
                        val sub = subjects.find { it.id == item.subjectId }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = item.isCompleted, onCheckedChange = { onToggleAssignment(item) })
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        color = if (item.isCompleted) Color.Gray else Color.White,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        if (sub != null) {
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFF2563EB))
                                            ) {
                                                Text(sub.name, fontSize = 9.sp, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        }
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF475569))
                                        ) {
                                            Text("Due: ${item.dueDate}", fontSize = 9.sp, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                                IconButton(onClick = { onDeleteAssignment(item) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// MODULE 9: TIMETABLE SCHEDULE SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    slots: List<TimetableSlot>,
    subjects: List<Subject>,
    onAddSlot: (day: String, slot: String, subId: Int?, custom: String?, room: String?) -> Unit,
    onDeleteSlot: (TimetableSlot) -> Unit,
    onBack: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var selectedDay by remember { mutableStateOf("Monday") }
    var timeSlot by remember { mutableStateOf("09:00 - 10:30") }
    var selectedSubjectId by remember { mutableStateOf<Int?>(null) }
    var customSubjectName by remember { mutableStateOf("") }
    var classRoom by remember { mutableStateOf("") }

    val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Class Weekly Timetable", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }, containerColor = Color(0xFF3B82F6)) {
                Icon(Icons.Default.Add, contentDescription = "Scheduler", tint = Color.White)
            }
        },
        containerColor = Color(0xFF0F172A)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            daysOfWeek.forEach { day ->
                val daySlots = slots.filter { it.dayOfWeek == day }
                Text(
                    text = day,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF60A5FA),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                if (daySlots.isEmpty()) {
                    Text(
                        text = "  No classes scheduled today",
                        fontSize = 12.sp,
                        color = Color.DarkGray,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                    )
                } else {
                    daySlots.forEach { slot ->
                        val matchedSub = subjects.find { it.id == slot.subjectId }
                        val subjectLabel = matchedSub?.name ?: slot.customSubjectName ?: "Subject"
                        val folderColor = try {
                            Color(android.graphics.Color.parseColor(matchedSub?.colorHex ?: "#475569"))
                        } catch (e: Exception) {
                            Color(0xFF3B82F6)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(folderColor)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(text = subjectLabel, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(text = "Room/Location: ${slot.room ?: "N/A"}", color = Color.LightGray, fontSize = 11.sp)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = slot.timeSlot, color = Color(0xFFF59E0B), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    IconButton(onClick = { onDeleteSlot(slot) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Schedule Class Slot", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select Day:", color = Color.White, fontSize = 12.sp)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        daysOfWeek.forEach { d ->
                            FilterChip(
                                selected = selectedDay == d,
                                onClick = { selectedDay = d },
                                label = { Text(d) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = timeSlot,
                        onValueChange = { timeSlot = it },
                        label = { Text("Time Slot (e.g. 09:00 - 10:30)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = classRoom,
                        onValueChange = { classRoom = it },
                        label = { Text("Room / Location") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Subject Folder Link:", color = Color.White, fontSize = 12.sp)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        subjects.forEach { s ->
                            FilterChip(
                                selected = selectedSubjectId == s.id,
                                onClick = { selectedSubjectId = s.id },
                                label = { Text(s.name) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = customSubjectName,
                        onValueChange = { customSubjectName = it },
                        label = { Text("Or Type Class Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddSlot(
                            selectedDay,
                            timeSlot,
                            selectedSubjectId,
                            customSubjectName.ifBlank { null },
                            classRoom.ifBlank { null }
                        )
                        showDialog = false
                    }
                ) {
                    Text("Schedule")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}

// ==========================================
// MODULE 10: AUTOMATIC CGPA CALCULATOR SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CgpaScreen(
    gradeItems: List<GradeItem>,
    onAddGrade: (sem: String, course: String, hours: Double, points: Double, txt: String) -> Unit,
    onDeleteGrade: (GradeItem) -> Unit,
    onBack: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var semesterName by remember { mutableStateOf("Semester 1") }
    var courseName by remember { mutableStateOf("") }
    var creditHours by remember { mutableStateOf("3") }
    var gradeText by remember { mutableStateOf("A") }

    val gradePointsMap = mapOf(
        "A" to 4.0, "A-" to 3.7, "B+" to 3.3, "B" to 3.0,
        "B-" to 2.7, "C+" to 2.3, "C" to 2.0, "D" to 1.0, "F" to 0.0
    )

    // Calculate CGPA
    val totalCredits = gradeItems.sumOf { it.creditHours }
    val totalWeightedPoints = gradeItems.sumOf { it.creditHours * it.gradePoints }
    val calculatedCgpa = if (totalCredits > 0) totalWeightedPoints / totalCredits else 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CGPA Grade Assistant", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }, containerColor = Color(0xFF3B82F6)) {
                Icon(Icons.Default.Add, contentDescription = "Grade Item", tint = Color.White)
            }
        },
        containerColor = Color(0xFF0F172A)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Summary CGPA Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A8A)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("COMPUTED CGPA", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format("%.2f", calculatedCgpa),
                        color = Color.White,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Based on ${gradeItems.size} course records (${totalCredits.toInt()} credit hours)",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }
            }

            Text("Course grade ledger", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))

            if (gradeItems.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Calculate,
                    mainText = "No course sheets saved yet.",
                    subText = "Add credit ratings & grades to calculate cumulative transcripts instantly."
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(gradeItems) { grade ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = grade.courseName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(text = "${grade.semester} • ${grade.creditHours.toInt()} Credits", color = Color.Gray, fontSize = 11.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF2563EB)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = grade.gradeText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    IconButton(onClick = { onDeleteGrade(grade) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Log Completed Course", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = semesterName,
                        onValueChange = { semesterName = it },
                        label = { Text("Semester (e.g. Semester 1)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = courseName,
                        onValueChange = { courseName = it },
                        label = { Text("Course Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = creditHours,
                        onValueChange = { creditHours = it },
                        label = { Text("Credit Hours") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Letter Grade:", color = Color.White, fontSize = 12.sp)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        gradePointsMap.keys.forEach { letter ->
                            FilterChip(
                                selected = gradeText == letter,
                                onClick = { gradeText = letter },
                                label = { Text(letter) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val hours = creditHours.toDoubleOrNull() ?: 3.0
                        val points = gradePointsMap[gradeText] ?: 4.0
                        onAddGrade(semesterName, courseName, hours, points, gradeText)
                        showDialog = false
                    }
                ) {
                    Text("Save Course Ledger")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}

// ==========================================
// MODULE 11: RESULTS VAULT SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsVaultScreen(
    items: List<ResultVaultItem>,
    onAddResult: (sem: String, name: String, type: String) -> Unit,
    onDeleteResult: (ResultVaultItem) -> Unit,
    onBack: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var semesterName by remember { mutableStateOf("Semester 1") }
    var fileName by remember { mutableStateOf("") }
    var fileType by remember { mutableStateOf("PDF") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report Sheet Vault", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }, containerColor = Color(0xFF3B82F6)) {
                Icon(Icons.Default.CloudUpload, contentDescription = "Upload Document", tint = Color.White)
            }
        },
        containerColor = Color(0xFF0F172A)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (items.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.WorkspacePremium,
                    mainText = "Vault is empty",
                    subText = "Store official transcripts, degree results, and semester report sheets securely."
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(items) { record ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(text = record.fileName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(text = "Semester: ${record.semester} • ${record.fileType}", color = Color.Gray, fontSize = 11.sp)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { /* Simulate Download */ }) {
                                        Icon(Icons.Default.Download, contentDescription = "Simulate file fetch", tint = Color(0xFF10B981))
                                    }
                                    IconButton(onClick = { onDeleteResult(record) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete record", tint = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Upload Transcript Document", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = semesterName,
                        onValueChange = { semesterName = it },
                        label = { Text("Semester Category") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = fileName,
                        onValueChange = { fileName = it },
                        label = { Text("Document File Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("File Type Format:", color = Color.White, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("PDF", "JPG", "PNG").forEach { format ->
                            FilterChip(
                                selected = fileType == format,
                                onClick = { fileType = format },
                                label = { Text(format) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (fileName.isNotBlank()) {
                            onAddResult(semesterName, fileName, fileType)
                            fileName = ""
                            showDialog = false
                        }
                    }
                ) {
                    Text("Upload & Log to Vault")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}

// ==========================================
// MODULE 12: SMART REMINDERS SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    reminders: List<Reminder>,
    onAddReminder: (title: String, dateText: String, cat: String) -> Unit,
    onDeleteReminder: (Reminder) -> Unit,
    onBack: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var dateString by remember { mutableStateOf("2026-06-25 15:00") }
    var selectedCategory by remember { mutableStateOf("Assignment") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Class Alerts & Reminders", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }, containerColor = Color(0xFF3B82F6)) {
                Icon(Icons.Default.AddAlarm, contentDescription = "Alert Scheduler", tint = Color.White)
            }
        },
        containerColor = Color(0xFF0F172A)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (reminders.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.NotificationsActive,
                    mainText = "No alarms scheduled",
                    subText = "Schedule alarms for exams, assignment deadlines, or personal study hours."
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(reminders) { reminder ->
                        val alertColor = when (reminder.category) {
                            "Exam" -> Color(0xFFEF4444)
                            "Assignment" -> Color(0xFFF59E0B)
                            else -> Color(0xFF10B981)
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = alertColor, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(text = reminder.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(text = "Scheduled: ${reminder.dateTime} • ${reminder.category}", color = Color.LightGray, fontSize = 11.sp)
                                    }
                                }
                                IconButton(onClick = { onDeleteReminder(reminder) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Construct Alarm Alert", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Reminder Title") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = dateString,
                        onValueChange = { dateString = it },
                        label = { Text("Date-Time (YYYY-MM-DD HH:MM)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Alert Category Label:", color = Color.White, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Exam", "Assignment", "Personal").forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onAddReminder(title, dateString, selectedCategory)
                            title = ""
                            showDialog = false
                        }
                    }
                ) {
                    Text("Save Alert Alarm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}

// ==========================================
// MODULE 13 & MODULE 5: SUBJECT ORGANIZER & VIRTUAL CABINET CARDS
// ==========================================
@Composable
fun SubjectOrganizerScreen(
    subject: Subject,
    notes: List<Note>,
    documents: List<DocumentItem>,
    assignments: List<Assignment>,
    attendance: Attendance?,
    onBack: () -> Unit,
    onAddNote: (title: String, content: String) -> Unit,
    onDeleteNote: (Note) -> Unit,
    onUploadDoc: (name: String, format: String) -> Unit,
    onDeleteDoc: (DocumentItem) -> Unit,
    onAddAssignment: (title: String, date: String) -> Unit,
    onToggleAssignment: (Assignment) -> Unit,
    onDeleteAssignment: (Assignment) -> Unit,
    onUpdateAttendance: (attended: Int, total: Int) -> Unit
) {
    var activeSubTab by remember { mutableStateOf(0) } // 0=Home Organizer, 1=Notebook, 2=Vault Documents, 3=Homework

    val subColor = try {
        Color(android.graphics.Color.parseColor(subject.colorHex))
    } catch (e: Exception) {
        Color(0xFF3B82F6)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Banner course header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(subColor.copy(alpha = 0.15f))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = subject.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text(text = "Subject Folder • ${subject.code}", color = Color.LightGray, fontSize = 12.sp)
                }
            }
        }

        TabRow(
            selectedTabIndex = activeSubTab,
            containerColor = Color(0xFF1E293B),
            contentColor = Color.White
        ) {
            Tab(selected = activeSubTab == 0, onClick = { activeSubTab = 0 }, icon = { Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(20.dp)) }, text = { Text("Overview", fontSize = 11.sp) })
            Tab(selected = activeSubTab == 1, onClick = { activeSubTab = 1 }, icon = { Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(20.dp)) }, text = { Text("Notes", fontSize = 11.sp) })
            Tab(selected = activeSubTab == 2, onClick = { activeSubTab = 2 }, icon = { Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(20.dp)) }, text = { Text("Docs", fontSize = 11.sp) })
            Tab(selected = activeSubTab == 3, onClick = { activeSubTab = 3 }, icon = { Icon(Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(20.dp)) }, text = { Text("Tasks", fontSize = 11.sp) })
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (activeSubTab) {
            0 -> {
                // Folder Overview Screen
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Syllabus details", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = subject.description.ifBlank { "No course description configured, write note additions to populate workspace." }, color = Color.LightGray, fontSize = 13.sp)
                        }
                    }

                    // MODULE 6: ATTENDANCE TRACKER INTEGRATION
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Attendance rating stats", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(10.dp))

                            val attendedVal = attendance?.attended ?: 0
                            val totalVal = attendance?.total ?: 0
                            val ratio = if (totalVal > 0) (attendedVal.toDouble() / totalVal) else 1.0
                            val pct = (ratio * 100).toInt()

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Class records: $attendedVal / $totalVal total lectures", color = Color.LightGray, fontSize = 13.sp)
                                Text(
                                    text = "$pct %",
                                    color = if (pct < 75 && totalVal > 0) Color(0xFFEF4444) else Color(0xFF10B981),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = ratio.toFloat(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (pct < 75 && totalVal > 0) Color(0xFFEF4444) else Color(0xFF10B981),
                                trackColor = Color.DarkGray
                            )

                            if (pct < 75 && totalVal > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Warning! Attendance ratio fell below standard 75% limit threshold.",
                                        color = Color(0xFFFCA5A5),
                                        modifier = Modifier.padding(8.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { onUpdateAttendance(attendedVal + 1, totalVal + 1) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                ) {
                                    Text("Attended Class", fontSize = 11.sp)
                                }
                                Button(
                                    onClick = { onUpdateAttendance(attendedVal, totalVal + 1) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                                ) {
                                    Text("Missed Class", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    // QUICK STATE PREVIEWS
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Study Cabinet Folders contains:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("• Lecture Notes: ${notes.size} sheets", color = Color.LightGray, fontSize = 13.sp)
                            Text("• Cabinet Documents: ${documents.size} pdf files", color = Color.LightGray, fontSize = 13.sp)
                            Text("• Open homework milestones: ${assignments.filter { !it.isCompleted }.size} items", color = Color.LightGray, fontSize = 13.sp)
                        }
                    }
                }
            }
            1 -> {
                // Lecture Notebook Module
                var nTitle by remember { mutableStateOf("") }
                var nDesc by remember { mutableStateOf("") }

                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Quick write sheet", color = Color.White, fontWeight = FontWeight.Bold)
                            OutlinedTextField(value = nTitle, onValueChange = { nTitle = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = nDesc, onValueChange = { nDesc = it }, label = { Text("Lecture contents...") }, modifier = Modifier.fillMaxWidth())
                            Button(
                                onClick = {
                                    if (nTitle.isNotBlank()) {
                                        onAddNote(nTitle, nDesc)
                                        nTitle = ""
                                        nDesc = ""
                                    }
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) { Text("Save note") }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(notes) { note ->
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), modifier = Modifier.fillMaxWidth()) {
                                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(note.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(note.content, color = Color.LightGray, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                    IconButton(onClick = { onDeleteNote(note) }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray) }
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                // Upload study sheets
                var fName by remember { mutableStateOf("") }
                var selectedFormat by remember { mutableStateOf("PDF") }

                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Connect File Resource", color = Color.White, fontWeight = FontWeight.Bold)
                            OutlinedTextField(value = fName, onValueChange = { fName = it }, label = { Text("File Title Name") }, modifier = Modifier.fillMaxWidth())
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("PDF", "DOCX", "Image", "PPT").forEach { type ->
                                    FilterChip(
                                        selected = selectedFormat == type,
                                        onClick = { selectedFormat = type },
                                        label = { Text(type) }
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    if (fName.isNotBlank()) {
                                        onUploadDoc(fName, selectedFormat)
                                        fName = ""
                                    }
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) { Text("Virtual Upload") }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(documents) { doc ->
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (doc.fileType == "Image") Icons.Default.Image else Icons.Default.InsertDriveFile,
                                            contentDescription = null,
                                            tint = Color(0xFF3B82F6),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(doc.fileName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(doc.fileType, color = Color.LightGray, fontSize = 11.sp)
                                        }
                                    }
                                    Row {
                                        IconButton(onClick = { /* Simulate viewer */ }) { Icon(Icons.Default.Visibility, contentDescription = "PDF Viewer Simulator", tint = Color(0xFF10B981)) }
                                        IconButton(onClick = { onDeleteDoc(doc) }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            3 -> {
                // Assignments tracking inside this subject folder
                var hwTitle by remember { mutableStateOf("") }
                var hwDueDate by remember { mutableStateOf("2026-06-25") }

                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Add Course Homework", color = Color.White, fontWeight = FontWeight.Bold)
                            OutlinedTextField(value = hwTitle, onValueChange = { hwTitle = it }, label = { Text("Task description") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = hwDueDate, onValueChange = { hwDueDate = it }, label = { Text("Deadline") }, modifier = Modifier.fillMaxWidth())
                            Button(
                                onClick = {
                                    if (hwTitle.isNotBlank()) {
                                        onAddAssignment(hwTitle, hwDueDate)
                                        hwTitle = ""
                                    }
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) { Text("Add HW") }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(assignments) { item ->
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = item.isCompleted, onCheckedChange = { onToggleAssignment(item) })
                                        Column {
                                            Text(
                                                item.title,
                                                color = if (item.isCompleted) Color.Gray else Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text("Due: ${item.dueDate}", color = Color.Gray, fontSize = 11.sp)
                                        }
                                    }
                                    IconButton(onClick = { onDeleteAssignment(item) }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Common Friendly Empty State Component
@Composable
fun EmptyStateView(
    icon: ImageVector,
    mainText: String,
    subText: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.DarkGray,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = mainText,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subText,
            color = Color.Gray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}
