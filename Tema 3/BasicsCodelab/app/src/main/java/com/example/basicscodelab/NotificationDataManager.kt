package com.example.basicscodelab

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Simple data manager for notifications using SharedPreferences
 * For production, replace with Room Database
 */
class NotificationDataManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("notification_storage", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_MESSAGES = "messages"
        private const val KEY_ALLOWED_NUMBERS = "allowed_numbers"
        private const val MAX_STORED_MESSAGES = 1000

        @Volatile
        private var instance: NotificationDataManager? = null

        fun getInstance(context: Context): NotificationDataManager {
            return instance ?: synchronized(this) {
                instance ?: NotificationDataManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    /**
     * Save a new notification
     */
    fun saveNotification(notification: NotificationMessage) {
        val messages = getMessages().toMutableList()
        messages.add(0, notification) // Add to beginning

        // Limit stored messages
        if (messages.size > MAX_STORED_MESSAGES) {
            messages.subList(MAX_STORED_MESSAGES, messages.size).clear()
        }

        saveMessages(messages)
    }

    /**
     * Get all notifications
     */
    fun getMessages(): List<NotificationMessage> {
        val json = prefs.getString(KEY_MESSAGES, "[]") ?: "[]"
        return try {
            val type = object : TypeToken<List<NotificationMessage>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Update a specific notification
     */
    fun updateNotification(notification: NotificationMessage) {
        val messages = getMessages().toMutableList()
        val index = messages.indexOfFirst { it.id == notification.id }

        if (index != -1) {
            messages[index] = notification
            saveMessages(messages)
        }
    }

    /**
     * Delete a notification by ID
     */
    fun deleteNotification(id: String) {
        val messages = getMessages().filter { it.id != id }
        saveMessages(messages)
    }

    /**
     * Mark notification as read
     */
    fun markAsRead(id: String) {
        val messages = getMessages().toMutableList()
        val index = messages.indexOfFirst { it.id == id }

        if (index != -1) {
            messages[index] = messages[index].copy(isRead = true)
            saveMessages(messages)
        }
    }

    /**
     * Mark all notifications as read
     */
    fun markAllAsRead() {
        val messages = getMessages().map { it.copy(isRead = true) }
        saveMessages(messages)
    }

    /**
     * Clear all notifications
     */
    fun clearAll() {
        prefs.edit().remove(KEY_MESSAGES).apply()
    }

    /**
     * Get unread count
     */
    fun getUnreadCount(): Int {
        return getMessages().count { !it.isRead }
    }

    /**
     * Get messages by app
     */
    fun getMessagesByApp(appName: String): List<NotificationMessage> {
        return getMessages().filter { it.appName == appName }
    }

    /**
     * Get unique app names
     */
    fun getAppNames(): List<String> {
        return getMessages().map { it.appName }.distinct().sorted()
    }

    // Allowed numbers management

    /**
     * Add a phone number to allowed list
     */
    fun addAllowedNumber(number: String) {
        val numbers = getAllowedNumbers().toMutableSet()
        numbers.add(normalizePhoneNumber(number))
        saveAllowedNumbers(numbers.toList())
    }

    /**
     * Remove a phone number from allowed list
     */
    fun removeAllowedNumber(number: String) {
        val numbers = getAllowedNumbers().filter {
            it != normalizePhoneNumber(number)
        }
        saveAllowedNumbers(numbers)
    }

    /**
     * Check if a phone number is allowed
     * If allowed list is empty, all numbers are allowed
     */
    fun isNumberAllowed(number: String): Boolean {
        val allowedNumbers = getAllowedNumbers()
        if (allowedNumbers.isEmpty()) return true // Allow all if list is empty

        val normalized = normalizePhoneNumber(number)
        return allowedNumbers.any { it == normalized }
    }

    /**
     * Get all allowed numbers
     */
    fun getAllowedNumbers(): List<String> {
        val json = prefs.getString(KEY_ALLOWED_NUMBERS, "[]") ?: "[]"
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Private helper methods

    private fun saveMessages(messages: List<NotificationMessage>) {
        val json = gson.toJson(messages)
        prefs.edit().putString(KEY_MESSAGES, json).apply()
    }

    private fun saveAllowedNumbers(numbers: List<String>) {
        val json = gson.toJson(numbers)
        prefs.edit().putString(KEY_ALLOWED_NUMBERS, json).apply()
    }

    private fun normalizePhoneNumber(number: String): String {
        // Remove all non-digit characters except +
        return number.replace(Regex("[^+\\d]"), "")
    }

    /**
     * Get statistics
     */
    data class Statistics(
        val totalMessages: Int,
        val unreadMessages: Int,
        val messagesByApp: Map<String, Int>,
        val messagesByPriority: Map<Priority, Int>
    )

    fun getStatistics(): Statistics {
        val messages = getMessages()

        return Statistics(
            totalMessages = messages.size,
            unreadMessages = messages.count { !it.isRead },
            messagesByApp = messages.groupBy { it.appName }
                .mapValues { it.value.size },
            messagesByPriority = messages.groupBy { it.priority }
                .mapValues { it.value.size }
        )
    }
}

/**
 * Extension functions for easy access
 */
fun Context.getNotificationDataManager(): NotificationDataManager {
    return NotificationDataManager.getInstance(this)
}