package com.example.sensorcrud

import android.util.Log
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket

object SocketClient {

    // serverul C rulează în aceeași aplicație pe 127.0.0.1
    private const val SERVER_HOST = "127.0.0.1"
    private const val SERVER_PORT = 5555

    private const val TAG = "SocketClient"

    fun sendNotificationAsync(notification: NotificationMessage) {
        val payload = buildString {
            append("CREATE|")
            append(notification.id).append("|")
            append(notification.title.replace("|", " ")).append("|")
            append(notification.message.replace("|", " ").replace("\n", " "))
        }
        sendAsync("CREATE", payload)
    }

    fun deleteNotificationAsync(id: String) {
        val payload = "DELETE|$id"
        sendAsync("DELETE", payload)
    }

    fun markReadAsync(id: String) {
        val payload = "MARK_READ|$id"
        sendAsync("MARK_READ", payload)
    }

    fun sendShakeAsync() {
        sendAsync("SHAKE", "SHAKE")
    }

    private fun sendAsync(command: String, payload: String) {
        Thread {
            try {
                Socket(SERVER_HOST, SERVER_PORT).use { socket ->
                    val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
                    val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

                    writer.write("$payload\n")
                    writer.flush()

                    val response = reader.readLine()
                    Log.d(TAG, "Response for $command: $response")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending $command: ${e.message}", e)
            }
        }.start()
    }
}
