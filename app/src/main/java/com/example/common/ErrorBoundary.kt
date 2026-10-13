package com.example.common

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ============================================================================
 * LIFE TRACKER AI — ERROR BOUNDARY & FAULT TOLERANCE ENGINE
 * ============================================================================
 *
 * This module establishes the error boundary architecture for the 75% project
 * milestone. It provides:
 * 1. Sealed [AppResult] wrapper for strictly typed error handling across data/domain.
 * 2. Specific domain exception hierarchies (Network, Database, Auth, AI).
 * 3. [safeApiCall] and [safeDbCall] fault isolation wrappers.
 * 4. [LifeTrackerErrorBoundary] composable wrapper to isolate UI crashes and
 *    provide interactive recovery.
 */

/**
 * Sealed class representing exhaustive operation states with error boundaries.
 * Prevents unhandled runtime crashes by enforcing explicit error handling.
 *
 * @param T The type of the successfully wrapped payload.
 */
sealed class AppResult<out T> {
    /**
     * Represents a successful operation containing [data].
     */
    data class Success<out T>(val data: T) : AppResult<T>()

    /**
     * Represents a failed operation containing [exception] and user-friendly [errorMessage].
     */
    data class Error(
        val exception: Throwable,
        val errorMessage: String = exception.localizedMessage ?: "An unexpected error occurred"
    ) : AppResult<Nothing>()

    /**
     * Represents an active asynchronous operation in progress.
     */
    object Loading : AppResult<Nothing>()

    /**
     * Returns true if this result represents a successful operation.
     */
    val isSuccess: Boolean get() = this is Success

    /**
     * Returns true if this result represents a failed operation.
     */
    val isError: Boolean get() = this is Error

    /**
     * Unwraps the data if successful, or returns null if loading/error.
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    /**
     * Transforms the contained value via [transform] if successful.
     */
    inline fun <R> map(transform: (T) -> R): AppResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> Error(exception, errorMessage)
        is Loading -> Loading
    }
}

/**
 * Base domain exception representing application-level error boundaries.
 */
open class LifeTrackerException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Thrown when an external HTTP / REST API call fails (network unreachable, timeout, 4xx, 5xx).
 */
class NetworkUnavailableException(message: String = "Network connection unavailable or request timed out", cause: Throwable? = null) :
    LifeTrackerException(message, cause)

/**
 * Thrown when an internal SQLite or Room database operation fails (disk full, query constraint).
 */
class DatabaseSyncException(message: String = "Local database storage or query error occurred", cause: Throwable? = null) :
    LifeTrackerException(message, cause)

/**
 * Thrown when authentication or credential retrieval fails (token expired, cancelled sign-in).
 */
class AuthenticationException(message: String = "User authentication or credential validation failed", cause: Throwable? = null) :
    LifeTrackerException(message, cause)

/**
 * Thrown when the Gemini / OpenRouter AI inference fails or returns invalid structured output.
 */
class AiInferenceException(message: String = "AI engine processing failed; offline fallback engaged", cause: Throwable? = null) :
    LifeTrackerException(message, cause)

/**
 * Safely executes a network or external API call inside an isolated coroutine error boundary.
 * Prevents network drops and timeouts from crashing the calling ViewModel or Repository.
 *
 * @param block The suspending API invocation block.
 * @return [AppResult.Success] on completion or [AppResult.Error] wrapping domain exception.
 */
suspend fun <T> safeApiCall(
    tag: String = "API_CALL",
    block: suspend () -> T
): AppResult<T> = withContext(Dispatchers.IO) {
    try {
        val result = block()
        AppResult.Success(result)
    } catch (e: CancellationException) {
        // Must rethrow CancellationException to maintain Coroutine structured concurrency
        throw e
    } catch (e: java.net.SocketTimeoutException) {
        Log.e(tag, "Network timeout error boundary tripped: ${e.message}")
        AppResult.Error(NetworkUnavailableException("Server request timed out. Please check your internet connection.", e))
    } catch (e: java.io.IOException) {
        Log.e(tag, "Network I/O error boundary tripped: ${e.message}")
        AppResult.Error(NetworkUnavailableException("Unable to reach the server. Please check your connection.", e))
    } catch (e: Throwable) {
        Log.e(tag, "Unexpected API error boundary tripped: ${e.message}", e)
        AppResult.Error(AiInferenceException("Service call failed: ${e.localizedMessage ?: "Unknown error"}", e))
    }
}

/**
 * Safely executes a Room SQLite database query or mutation inside an isolated error boundary.
 *
 * @param block The suspending database invocation block.
 * @return [AppResult.Success] on completion or [AppResult.Error] wrapping database exception.
 */
suspend fun <T> safeDbCall(
    tag: String = "DB_CALL",
    block: suspend () -> T
): AppResult<T> = withContext(Dispatchers.IO) {
    try {
        val result = block()
        AppResult.Success(result)
    } catch (e: CancellationException) {
        throw e
    } catch (e: android.database.sqlite.SQLiteException) {
        Log.e(tag, "SQLite error boundary tripped: ${e.message}")
        AppResult.Error(DatabaseSyncException("Database constraint or storage error: ${e.message}", e))
    } catch (e: Throwable) {
        Log.e(tag, "Unexpected database error boundary tripped: ${e.message}", e)
        AppResult.Error(DatabaseSyncException("Failed to persist data locally: ${e.localizedMessage}", e))
    }
}

/**
 * Global Coroutine Exception Handler for ViewModel scopes to prevent uncaught thread crashes.
 */
fun createViewModelExceptionHandler(tag: String, onError: (String) -> Unit): CoroutineExceptionHandler {
    return CoroutineExceptionHandler { _, throwable ->
        Log.e(tag, "Uncaught Coroutine Exception captured by Error Boundary: ${throwable.message}", throwable)
        onError(throwable.localizedMessage ?: "An unexpected error occurred.")
    }
}

/**
 * Jetpack Compose Error Boundary UI Component.
 * Wraps arbitrary composable screens with a resilient error fallback interface.
 * If an error occurs, it renders an informative diagnostic card and a "Retry" recovery button.
 *
 * @param errorMessage Nullable error string. When non-null, displays the fallback recovery view.
 * @param onRetry Callback triggered when user taps the "Retry System" button.
 * @param content The regular screen content to display when there are no errors.
 */
@Composable
fun LifeTrackerErrorBoundary(
    errorMessage: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (errorMessage != null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp)
                .testTag("error_boundary_view"),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "System Warning",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(52.dp)
                    )

                    Text(
                        text = "System Boundary Alert",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )

                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                        lineHeight = 20.sp
                    )

                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("error_boundary_retry_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retry & Recover System", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    } else {
        content()
    }
}
