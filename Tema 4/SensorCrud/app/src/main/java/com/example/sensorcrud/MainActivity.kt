@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.sensorcrud

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults

data class NotificationMessage(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)


class MainActivity : ComponentActivity() {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var shakeDetector: ShakeDetector? = null

    private var onShakeAction: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // PORNIM serverul TCP în C (NDK)
        NativeServer.startServer()

        // setăm senzorul
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        shakeDetector = ShakeDetector {
            Log.d("MainActivity", "Shake detected")
            onShakeAction?.invoke()
            SocketClient.sendShakeAsync()
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NotificationApp(
                        onRegisterShakeAction = { callback ->
                            onShakeAction = callback
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let { acc ->
            shakeDetector?.let { detector ->
                sensorManager.registerListener(
                    detector,
                    acc,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        shakeDetector?.let {
            sensorManager.unregisterListener(it)
        }
    }
}

@Composable
fun NotificationApp(
    onRegisterShakeAction: ((() -> Unit) -> Unit)
) {
    val notifications = remember { mutableStateListOf<NotificationMessage>() }

    fun addTestNotification() {
        val index = notifications.size + 1
        val notif = NotificationMessage(
            title = "Test notification #$index",
            message = "Created locally and sent to C TCP server."
        )
        notifications.add(0, notif)
        SocketClient.sendNotificationAsync(notif)
    }

    fun markAsRead(id: String) {
        val idx = notifications.indexOfFirst { it.id == id }
        if (idx != -1) {
            val n = notifications[idx]
            notifications[idx] = n.copy(isRead = true)
            SocketClient.markReadAsync(id)
        }
    }

    fun deleteNotification(id: String) {
        val idx = notifications.indexOfFirst { it.id == id }
        if (idx != -1) {
            notifications.removeAt(idx)
            SocketClient.deleteNotificationAsync(id)
        }
    }

    fun markAllAsReadFromShake() {
        for (i in notifications.indices) {
            val n = notifications[i]
            if (!n.isRead) {
                notifications[i] = n.copy(isRead = true)
                SocketClient.markReadAsync(n.id)
            }
        }
    }

    LaunchedEffect(Unit) {
        onRegisterShakeAction {
            markAllAsReadFromShake()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Senzor + CRUD + C TCP server")
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Shake phone = mark all notifications as read (local + notify server).",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Button = create notification și trimite CREATE la serverul TCP în C.",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { addTestNotification() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add test notification")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No notifications yet.\nPress the button or shake the phone.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                Text(
                    text = "Notifications:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notifications, key = { it.id }) { notif ->
                        NotificationItem(
                            notification = notif,
                            onMarkRead = { markAsRead(notif.id) },
                            onDelete = { deleteNotification(notif.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: NotificationMessage,
    onMarkRead: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold
                )
                Text(
                    text = formatTimestamp(notification.timestamp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = notification.message,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!notification.isRead) {
                    Button(
                        onClick = onMarkRead,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Mark read")
                    }
                }
                Button(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Delete")
                }
            }
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(date)
}
