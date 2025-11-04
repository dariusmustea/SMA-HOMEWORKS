package com.example.basicscodelab

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            for (smsMessage in messages) {
                val messageBody = smsMessage.messageBody
                val sender = smsMessage.originatingAddress ?: "Unknown"

                // Parse the SMS message
                val notification = parseSmsToNotification(messageBody, sender)

                // Save to local storage (you'll implement this)
                saveNotification(context, notification)

                // Show Android notification
                showNotification(context, notification)
            }
        }
    }

    /**
     * Parses SMS message into NotificationMessage
     *
     * Expected format:
     * [APP:AppName] PRIORITY: Title | Message
     *
     * Examples:
     * [APP:Server] HIGH: Disk space low | Only 5GB remaining
     * [APP:HomeAssistant] URGENT: Motion detected | Front door camera
     * Simple message without format (uses defaults)
     */
    private fun parseSmsToNotification(smsBody: String, sender: String): NotificationMessage {
        var appName = "SMS"
        var priority = Priority.NORMAL
        var title = "New Message"
        var message = smsBody

        try {
            // Check if message follows the format
            if (smsBody.startsWith("[APP:")) {
                // Extract app name
                val appEndIndex = smsBody.indexOf("]")
                if (appEndIndex > 5) {
                    appName = smsBody.substring(5, appEndIndex).trim()

                    // Extract remaining content
                    val remaining = smsBody.substring(appEndIndex + 1).trim()

                    // Check for priority
                    val priorityPattern = Regex("^(LOW|NORMAL|HIGH|URGENT):", RegexOption.IGNORE_CASE)
                    val priorityMatch = priorityPattern.find(remaining)

                    if (priorityMatch != null) {
                        priority = Priority.fromString(priorityMatch.groupValues[1])
                        val content = remaining.substring(priorityMatch.value.length).trim()

                        // Split title and message by |
                        val parts = content.split("|", limit = 2)
                        title = parts[0].trim()
                        message = if (parts.size > 1) parts[1].trim() else ""
                    } else {
                        // No priority, just title | message
                        val parts = remaining.split("|", limit = 2)
                        title = parts[0].trim()
                        message = if (parts.size > 1) parts[1].trim() else ""
                    }
                }
            } else {
                // Simple SMS without format - use first line as title
                val lines = smsBody.lines()
                if (lines.isNotEmpty()) {
                    title = lines[0].take(50) // First 50 chars as title
                    message = if (lines.size > 1) lines.drop(1).joinToString("\n") else ""
                }
            }
        } catch (e: Exception) {
            // If parsing fails, use the whole message as title
            title = smsBody.take(50)
            message = if (smsBody.length > 50) smsBody.substring(50) else ""
        }

        return NotificationMessage(
            appName = appName,
            title = title,
            message = message,
            priority = priority,
            phoneNumber = sender
        )
    }

    private fun saveNotification(context: Context, notification: NotificationMessage) {
        // Check if number is allowed (if whitelist is configured)
        val dataManager = context.getNotificationDataManager()

        if (!dataManager.isNumberAllowed(notification.phoneNumber)) {
            android.util.Log.d("SmsReceiver", "Blocked message from: ${notification.phoneNumber}")
            return
        }

        // Save to persistent storage
        dataManager.saveNotification(notification)
        android.util.Log.d("SmsReceiver", "Saved notification: ${notification.title} from ${notification.appName}")
    }

    private fun showNotification(context: Context, notification: NotificationMessage) {
        val channelId = when (notification.priority) {
            Priority.LOW -> "messages_low"
            Priority.NORMAL -> "messages_normal"
            Priority.HIGH -> "messages_high"
            Priority.URGENT -> "messages_urgent"
        }

        // Create intent to open app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notification.id.hashCode(),
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            else
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build notification
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_email) // Use your app icon
            .setContentTitle(notification.title)
            .setContentText(notification.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.message))
            .setPriority(
                when (notification.priority) {
                    Priority.LOW -> NotificationCompat.PRIORITY_LOW
                    Priority.NORMAL -> NotificationCompat.PRIORITY_DEFAULT
                    Priority.HIGH -> NotificationCompat.PRIORITY_HIGH
                    Priority.URGENT -> NotificationCompat.PRIORITY_MAX
                }
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSubText(notification.appName)

        // Add vibration for urgent messages
        if (notification.priority == Priority.URGENT) {
            builder.setVibrate(longArrayOf(0, 500, 250, 500))
        }

        // Show notification
        try {
            with(NotificationManagerCompat.from(context)) {
                notify(notification.id.hashCode(), builder.build())
            }
        } catch (e: SecurityException) {
            android.util.Log.e("SmsReceiver", "Permission denied to show notification", e)
        }
    }
}

/**
 * Example SMS formats:
 *
 * 1. Full format with all fields:
 *    [APP:ServerMonitor] HIGH: CPU Usage Alert | CPU usage exceeded 85% on server-prod-01
 *
 * 2. Without message body:
 *    [APP:Backup] NORMAL: Daily backup completed
 *
 * 3. Without priority:
 *    [APP:HomeAssistant] Motion detected at front door
 *
 * 4. Simple SMS (no format):
 *    Hello, this is a test message
 *    (Will be shown as: App="SMS", Title="Hello, this is a test...", Priority=NORMAL)
 */