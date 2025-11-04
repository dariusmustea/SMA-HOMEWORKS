package com.example.basicscodelab

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.basicscodelab.ui.theme.BasicsCodelabTheme
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import java.text.SimpleDateFormat
import java.util.*

// Model de date pentru notificări
data class NotificationMessage(
    val id: String = UUID.randomUUID().toString(),
    val appName: String = "Unknown",
    val title: String,
    val message: String,
    val priority: Priority = Priority.NORMAL,
    val timestamp: Long = System.currentTimeMillis(),
    val phoneNumber: String = "",
    val isRead: Boolean = false
)

enum class Priority(val displayName: String, val level: Int) {
    LOW("Low", 1),
    NORMAL("Normal", 3),
    HIGH("High", 5),
    URGENT("Urgent", 8);

    companion object {
        fun fromString(value: String): Priority {
            return when (value.uppercase()) {
                "LOW" -> LOW
                "NORMAL" -> NORMAL
                "HIGH" -> HIGH
                "URGENT" -> URGENT
                else -> NORMAL
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permission results
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create notification channel
        createNotificationChannel()

        // Request permissions
        requestNecessaryPermissions()

        setContent {
            BasicsCodelabTheme {
                NotificationApp(
                    modifier = Modifier.fillMaxSize(),
                    context = this
                )
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    "messages_low",
                    "Low Priority Messages",
                    NotificationManager.IMPORTANCE_LOW
                ),
                NotificationChannel(
                    "messages_normal",
                    "Normal Priority Messages",
                    NotificationManager.IMPORTANCE_DEFAULT
                ),
                NotificationChannel(
                    "messages_high",
                    "High Priority Messages",
                    NotificationManager.IMPORTANCE_HIGH
                ),
                NotificationChannel(
                    "messages_urgent",
                    "Urgent Messages",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 250, 500)
                }
            )

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            channels.forEach { notificationManager.createNotificationChannel(it) }
        }
    }

    private fun requestNecessaryPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECEIVE_SMS)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_SMS)
        }

        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationApp(modifier: Modifier = Modifier, context: Context) {
    val dataManager = remember { context.getNotificationDataManager() }
    var messages by remember { mutableStateOf(dataManager.getMessages()) }
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var filterPriority by rememberSaveable { mutableStateOf<Priority?>(null) }

    // Refresh messages when returning to app
    LaunchedEffect(Unit) {
        messages = dataManager.getMessages()
    }

    val tabs = listOf("All", "Unread", "Apps")

    val filteredMessages = when (selectedTab) {
        0 -> messages.filter { filterPriority == null || it.priority == filterPriority }
        1 -> messages.filter { !it.isRead && (filterPriority == null || it.priority == filterPriority) }
        else -> messages.filter { filterPriority == null || it.priority == filterPriority }
    }.sortedByDescending { it.timestamp }

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text("Notifications")
                            Text(
                                text = "${messages.count { !it.isRead }} unread",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            dataManager.markAllAsRead()
                            messages = dataManager.getMessages()
                        }) {
                            Icon(Icons.Filled.DoneAll, "Mark all read")
                        }
                        IconButton(onClick = {
                            dataManager.clearAll()
                            messages = emptyList()
                        }) {
                            Icon(Icons.Filled.DeleteSweep, "Clear all")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )

                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }

                // Priority filter chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterPriority == null,
                        onClick = { filterPriority = null },
                        label = { Text("All") }
                    )
                    Priority.values().forEach { priority ->
                        FilterChip(
                            selected = filterPriority == priority,
                            onClick = {
                                filterPriority = if (filterPriority == priority) null else priority
                            },
                            label = { Text(priority.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = getPriorityColor(priority)
                            )
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Test Message")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (filteredMessages.isEmpty()) {
                EmptyState(selectedTab)
            } else {
                if (selectedTab == 2) {
                    AppsList(messages = messages)
                } else {
                    MessageList(
                        messages = filteredMessages,
                        onMarkRead = { messageId ->
                            dataManager.markAsRead(messageId)
                            messages = dataManager.getMessages()
                        },
                        onDeleteMessage = { messageId ->
                            dataManager.deleteNotification(messageId)
                            messages = dataManager.getMessages()
                        }
                    )
                }
            }
        }

        if (showAddDialog) {
            AddTestMessageDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { appName, title, message, priority ->
                    val newMessage = NotificationMessage(
                        appName = appName,
                        title = title,
                        message = message,
                        priority = priority,
                        phoneNumber = "+1234567890"
                    )
                    dataManager.saveNotification(newMessage)
                    messages = dataManager.getMessages()
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun EmptyState(selectedTab: Int) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = when (selectedTab) {
                1 -> Icons.Filled.MarkEmailRead
                2 -> Icons.Filled.Apps
                else -> Icons.Filled.Notifications
            },
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = when (selectedTab) {
                1 -> "All caught up!"
                2 -> "No apps yet"
                else -> "No notifications"
            },
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = when (selectedTab) {
                1 -> "No unread messages"
                2 -> "Messages from different apps will appear here"
                else -> "Waiting for SMS notifications..."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

@Composable
fun MessageList(
    messages: List<NotificationMessage>,
    onMarkRead: (String) -> Unit,
    onDeleteMessage: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items = messages, key = { it.id }) { message ->
            MessageItem(
                message = message,
                onMarkRead = { onMarkRead(message.id) },
                onDelete = { onDeleteMessage(message.id) }
            )
        }
    }
}

@Composable
fun MessageItem(
    message: NotificationMessage,
    onMarkRead: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (message.isRead) 1.dp else 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (message.isRead)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Priority indicator
                Surface(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(8.dp),
                    shape = MaterialTheme.shapes.small,
                    color = getPriorityColor(message.priority)
                ) {}

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    // App name and timestamp
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = message.appName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatTimestamp(message.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Title
                    Text(
                        text = message.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (message.isRead) FontWeight.Normal else FontWeight.Bold,
                        color = if (message.isRead)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        else
                            MaterialTheme.colorScheme.onSurface
                    )

                    if (!expanded && message.message.isNotEmpty()) {
                        Text(
                            text = message.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 2
                        )
                    }
                }

                Column {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (expanded) "Show less" else "Show more"
                        )
                    }
                }
            }

            if (expanded) {
                Divider(modifier = Modifier.padding(horizontal = 12.dp))

                if (message.message.isNotEmpty()) {
                    Text(
                        text = message.message,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!message.isRead) {
                        OutlinedButton(
                            onClick = onMarkRead,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.DoneAll, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Mark Read")
                        }
                    }
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Filled.Delete, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@Composable
fun AppsList(messages: List<NotificationMessage>) {
    val appGroups = messages.groupBy { it.appName }
        .map { (appName, msgs) ->
            Triple(appName, msgs.size, msgs.count { !it.isRead })
        }
        .sortedByDescending { it.second }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(appGroups) { (appName, total, unread) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = appName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$total messages",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    if (unread > 0) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = unread.toString(),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddTestMessageDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Priority) -> Unit
) {
    var appName by rememberSaveable { mutableStateOf("TestApp") }
    var title by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    var selectedPriority by rememberSaveable { mutableStateOf(Priority.NORMAL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Test Message") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = appName,
                    onValueChange = { appName = it },
                    label = { Text("App Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Text("Priority:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Priority.values().forEach { priority ->
                        FilterChip(
                            selected = selectedPriority == priority,
                            onClick = { selectedPriority = priority },
                            label = { Text(priority.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = getPriorityColor(priority)
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(appName.trim(), title.trim(), message.trim(), selectedPriority)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun getPriorityColor(priority: Priority): androidx.compose.ui.graphics.Color {
    return when (priority) {
        Priority.LOW -> MaterialTheme.colorScheme.surfaceVariant
        Priority.NORMAL -> MaterialTheme.colorScheme.primary
        Priority.HIGH -> MaterialTheme.colorScheme.tertiary
        Priority.URGENT -> MaterialTheme.colorScheme.error
    }
}

fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        else -> SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
}