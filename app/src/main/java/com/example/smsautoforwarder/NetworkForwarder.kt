package com.example.smsautoforwarder

import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object NetworkForwarder {

    private const val TAG = "SmsAutoForwarder"
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 15_000

    private val executor = Executors.newCachedThreadPool()

    fun sendTelegram(
        botToken: String,
        chatId: String,
        text: String,
        onResult: (Boolean) -> Unit
    ) {
        executor.execute {
            val success = try {
                val url = URL("https://api.telegram.org/bot$botToken/sendMessage")
                val payload = JSONObject().apply {
                    put("chat_id", chatId)
                    put("text", text)
                }
                postJson(url, payload)
            } catch (t: Throwable) {
                Log.e(TAG, "Telegram forward failed.")
                false
            }
            onResult(success)
        }
    }

    fun sendWebhook(
        webhookUrl: String,
        sender: String,
        body: String,
        onResult: (Boolean) -> Unit
    ) {
        executor.execute {
            val success = try {
                val url = URL(webhookUrl)
                if (!isValidHttpsUrl(url)) {
                    throw IllegalArgumentException("Webhook must use HTTPS.")
                }
                val payload = JSONObject().apply {
                    put("sender", sender)
                    put("message", body)
                    put("received_at", System.currentTimeMillis())
                }
                postJson(url, payload)
            } catch (t: Throwable) {
                Log.e(TAG, "Webhook forward failed.")
                false
            }
            onResult(success)
        }
    }

    fun isValidHttpsUrl(value: String): Boolean {
        return try {
            isValidHttpsUrl(URL(value))
        } catch (_: Exception) {
            false
        }
    }

    private fun isValidHttpsUrl(url: URL): Boolean =
        url.protocol.equals("https", ignoreCase = true) && url.host.isNotBlank()

    private fun postJson(url: URL, payload: JSONObject): Boolean {
        val connection = url.openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")

            connection.outputStream.use { stream ->
                stream.write(payload.toString().toByteArray(Charsets.UTF_8))
            }

            val code = connection.responseCode
            code in 200..299
        } finally {
            connection.disconnect()
        }
    }
}
