package com.example.basicscodelab

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
import android.content.Context

/**
 * Settings screen for managing allowed phone numbers and app preferences
 * This is optional - add a navigation to this screen if you want whitelist functionality
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    context: Context,
    onBackPressed: () -> Unit
) {
    val dataManager = remember { context.getNotificationDataManager() }
    var allowedNumbers by remember { mutableStateOf(dataManager.getAllowedNumbers()) }
    var showAddNumberDialog by rememberSaveable { mutableStateOf(false) }
    val stats = remember { dataManager.getStatistics() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Statistics section
            item {
                StatisticsCard(stats)
            }

            // Whitelist section
            item {
                Text(
                    text = "Allowed Phone Numbers",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (allowedNumbers.isEmpty())
                        "All numbers are allowed. Add numbers to create a whitelist."
                    else
                        "Only messages from these numbers will be received.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            items(allowedNumbers) { number ->
                AllowedNumberItem(
                    phoneNumber = number,
                    onDelete = {
                        dataManager.removeAllowedNumber(number)
                        allowedNumbers = dataManager.getAllowedNumbers()
                    }
                )
            }

            item {
                Button(
                    onClick = { showAddNumberDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Phone Number")
                }
            }

            // Danger zone
            item {
                Spacer(Modifier.height(16.dp))
                DangerZoneCard(
                    onClearAllMessages = {
                        dataManager.clearAll()
                    },
                    onClearWhitelist = {
                        allowedNumbers.forEach { dataManager.removeAllowedNumber(it) }
                        allowedNumbers = emptyList()
                    }
                )
            }
        }
    }

    if (showAddNumberDialog) {
        AddNumberDialog(
            onDismiss = { showAddNumberDialog = false },
            onConfirm = { number ->
                dataManager.addAllowedNumber(number)
                allowedNumbers = dataManager.getAllowedNumbers()
                showAddNumberDialog = false
            }
        )
    }
}

@Composable
fun StatisticsCard(stats: NotificationDataManager.Statistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem("Total Messages", stats.totalMessages.toString())
                StatItem("Unread", stats.unreadMessages.toString())
            }

            Divider()

            Text(
                text = "By Priority",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )

            stats.messagesByPriority.forEach { (priority, count) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(priority.displayName)
                    Text(count.toString(), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun AllowedNumberItem(
    phoneNumber: String,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Phone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = phoneNumber,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun DangerZoneCard(
    onClearAllMessages: () -> Unit,
    onClearWhitelist: () -> Unit
) {
    var showClearMessagesDialog by rememberSaveable { mutableStateOf(false) }
    var showClearWhitelistDialog by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "⚠️ Danger Zone",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            OutlinedButton(
                onClick = { showClearMessagesDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Clear All Messages")
            }

            OutlinedButton(
                onClick = { showClearWhitelistDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Clear Whitelist")
            }
        }
    }

    if (showClearMessagesDialog) {
        AlertDialog(
            onDismissRequest = { showClearMessagesDialog = false },
            title = { Text("Clear All Messages?") },
            text = { Text("This will permanently delete all stored messages. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAllMessages()
                        showClearMessagesDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearMessagesDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showClearWhitelistDialog) {
        AlertDialog(
            onDismissRequest = { showClearWhitelistDialog = false },
            title = { Text("Clear Whitelist?") },
            text = { Text("This will remove all phone numbers from the whitelist. All numbers will be allowed again.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearWhitelist()
                        showClearWhitelistDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearWhitelistDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AddNumberDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Phone Number") },
        text = {
            Column {
                Text(
                    text = "Enter the phone number to allow. Include country code (e.g., +40712345678)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = {
                        phoneNumber = it
                        error = null
                    },
                    label = { Text("Phone Number") },
                    placeholder = { Text("+40712345678") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        phoneNumber.isBlank() -> error = "Phone number cannot be empty"
                        !phoneNumber.matches(Regex("^\\+?[\\d\\s-]+$")) ->
                            error = "Invalid phone number format"
                        else -> onConfirm(phoneNumber.trim())
                    }
                }
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