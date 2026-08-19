package com.example.smsautoforwarder

import android.util.Log
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.util.concurrent.Executors

enum class ForwardStatus {
    SUCCESS,
    HTTP_ERROR,
    TIMEOUT,
    NO_CONNECTION,
    INVALID_CONFIG,
    UNKNOWN_ERROR
}

data class ForwardResult(
    val status: ForwardStatus,
    val httpCode: Int? = null
) {
    val isSuccess: Boolean get() = status == ForwardStatus.SUCCESS
}

object NetworkForwarder {

    private const val TAG = "SmsAutoForwarder"
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 15_000
    private const val MAX_ATTEMPTS = 2
    private const val RETRY_DELAY_MS = 2_000L

    private val executor = Executors.newCachedThreadPool()

    fun sendTelegram(
        botToken: String,
        chatId: String,
        text: String,
        onResult: (ForwardResult) -> Unit
    ) {
        if (botToken.isBlank() || chatId.isBlank()) {
            onResult(ForwardResult(ForwardStatus.INVALID_CONFIG))
            return
        }
        executor.execute {
            val result = attemptWithRetry {
                val url = URL("https://api.telegram.org/bot$botToken/sendMessage")
                val payload = JSONObject().apply {
                    put("chat_id", chatId)
                    put("text", text)
                }
                postJson(url, payload)
            }
            onResult(result)
        }
    }

    fun sendWebhook(
        webhookUrl: String,
        sender: String,
        body: String,
        onResult: (ForwardResult) -> Unit
    ) {
        if (!isValidHttpsUrl(webhookUrl)) {
            onResult(ForwardResult(ForwardStatus.INVALID_CONFIG))
            return
        }
        executor.execute {
            val result = attemptWithRetry {
                val url = URL(webhookUrl)
                val payload = JSONObject().apply {
                    put("sender", sender)
                    put("message", body)
                    put("received_at", System.currentTimeMillis())
                }
                postJson(url, payload)
            }
            onResult(result)
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

    /**
     * Runs [attempt] and classifies the outcome. Transient network failures
     * (timeout, DNS/host unreachable) are retried once after a short delay;
     * HTTP error responses and malformed input are not retried since a retry
     * would not change the outcome.
     */
    private fun attemptWithRetry(attempt: () -> Int): ForwardResult {
        var lastResult: ForwardResult = ForwardResult(ForwardStatus.UNKNOWN_ERROR)

        for (tryIndex in 1..MAX_ATTEMPTS) {
            lastResult = try {
                val code = attempt()
                if (code in 200..299) {
                    return ForwardResult(ForwardStatus.SUCCESS)
                } else {
                    ForwardResult(ForwardStatus.HTTP_ERROR, code)
                }
            } catch (t: SocketTimeoutException) {
                Log.e(TAG, "Network forward timed out (attempt $tryIndex/$MAX_ATTEMPTS).")
                ForwardResult(ForwardStatus.TIMEOUT)
            } catch (t: UnknownHostException) {
                Log.e(TAG, "Network forward failed: no connection (attempt $tryIndex/$MAX_ATTEMPTS).")
                ForwardResult(ForwardStatus.NO_CONNECTION)
            } catch (t: IOException) {
                Log.e(TAG, "Network forward failed (attempt $tryIndex/$MAX_ATTEMPTS).")
                ForwardResult(ForwardStatus.UNKNOWN_ERROR)
            } catch (t: IllegalArgumentException) {
                return ForwardResult(ForwardStatus.INVALID_CONFIG)
            } catch (t: Throwable) {
                Log.e(TAG, "Network forward failed with an unexpected error.")
                ForwardResult(ForwardStatus.UNKNOWN_ERROR)
            }

            val isRetryable = lastResult.status == ForwardStatus.TIMEOUT ||
                lastResult.status == ForwardStatus.NO_CONNECTION ||
                lastResult.status == ForwardStatus.UNKNOWN_ERROR

            if (lastResult.isSuccess || !isRetryable || tryIndex == MAX_ATTEMPTS) {
                return lastResult
            }

            try {
                Thread.sleep(RETRY_DELAY_MS)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                return lastResult
            }
        }

        return lastResult
    }

    private fun postJson(url: URL, payload: JSONObject): Int {
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

            connection.responseCode
        } finally {
            connection.disconnect()
        }
    }
}
