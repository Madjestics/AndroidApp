package com.example.movieandroid.util

import android.content.Context
import com.example.movieandroid.R
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

class ResponseUtils(private val context: Context) {
    fun messageFor(response: Response<*>): String {
        val backendMessage = response.backendMessage()
        return when (response.code()) {
            400 -> backendMessage ?: context.getString(R.string.error_bad_request)
            401 -> backendMessage ?: context.getString(R.string.error_unauthorized)
            403 -> backendMessage ?: context.getString(R.string.error_forbidden)
            404 -> backendMessage ?: context.getString(R.string.error_not_found)
            409 -> backendMessage ?: context.getString(R.string.error_conflict)
            422 -> backendMessage ?: context.getString(R.string.error_validation)
            in 500..599 -> backendMessage ?: context.getString(R.string.error_server_unavailable)
            else -> backendMessage ?: context.getString(R.string.error_request_failed, response.code())
        }
    }

    fun messageFor(throwable: Throwable): String {
        val root = throwable.rootCause()
        return when (root) {
            is UnknownHostException, is ConnectException, is NoRouteToHostException ->
                context.getString(R.string.error_network_unavailable)

            is SocketTimeoutException, is InterruptedIOException ->
                context.getString(R.string.error_timeout)

            is SSLException ->
                context.getString(R.string.error_ssl)

            else -> context.getString(R.string.error_unknown)
        }
    }
}

fun Response<*>.toUserMessage(context: Context): String = ResponseUtils(context).messageFor(this)

fun Throwable.toUserMessage(context: Context): String = ResponseUtils(context).messageFor(this)

fun Throwable.rethrowIfCancellation() {
    if (this is CancellationException) throw this
}

private fun Response<*>.backendMessage(): String? {
    return try {
        errorBody()
            ?.string()
            ?.trim()
            ?.trim('"')
            ?.takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
    }
}

private fun Throwable.rootCause(): Throwable {
    var current: Throwable = this
    while (current.cause != null && current.cause !== current) {
        current = current.cause!!
    }
    return current
}
