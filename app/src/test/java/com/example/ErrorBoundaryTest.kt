package com.example

import com.example.common.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * ============================================================================
 * UNIT TESTS: ERROR BOUNDARY & FAULT TOLERANCE ENGINE
 * ============================================================================
 *
 * Verifies:
 * 1. Sealed [AppResult] states (Success, Error, Loading) and mapper functions.
 * 2. [safeApiCall] fault isolation wrapping timeouts and IOExceptions.
 * 3. Domain exception classifications (Network, Database, Auth, AI).
 */
class ErrorBoundaryTest {

    @Test
    fun appResult_successState_unwrapsCorrectly() {
        val result: AppResult<String> = AppResult.Success("LifeOS Synchronized")

        assertTrue(result.isSuccess)
        assertFalse(result.isError)
        assertEquals("LifeOS Synchronized", result.getOrNull())

        val mapped = result.map { it.length }
        assertTrue(mapped is AppResult.Success)
        assertEquals(19, (mapped as AppResult.Success).data)
    }

    @Test
    fun appResult_errorState_containsExceptionAndMessage() {
        val exception = RuntimeException("Connection dropped")
        val result: AppResult<String> = AppResult.Error(exception, "Custom error message")

        assertFalse(result.isSuccess)
        assertTrue(result.isError)
        assertNull(result.getOrNull())

        val errorResult = result as AppResult.Error
        assertEquals("Custom error message", errorResult.errorMessage)
        assertEquals(exception, errorResult.exception)
    }

    @Test
    fun appResult_loadingState_returnsNullOnUnwrap() {
        val result: AppResult<Int> = AppResult.Loading

        assertFalse(result.isSuccess)
        assertFalse(result.isError)
        assertNull(result.getOrNull())
    }

    @Test
    fun safeApiCall_successfulExecution_returnsAppResultSuccess() = runBlocking {
        val result = safeApiCall {
            "Response 200 OK"
        }

        assertTrue(result is AppResult.Success)
        assertEquals("Response 200 OK", (result as AppResult.Success).data)
    }

    @Test
    fun safeApiCall_catchesSocketTimeoutException_wrapsInNetworkException() = runBlocking {
        val result = safeApiCall {
            throw SocketTimeoutException("Read timed out after 30000ms")
        }

        assertTrue(result is AppResult.Error)
        val error = result as AppResult.Error
        assertTrue(error.exception is NetworkUnavailableException)
        assertTrue(error.errorMessage.contains("timed out") || error.errorMessage.contains("internet connection"))
    }

    @Test
    fun safeApiCall_catchesIOException_wrapsInNetworkException() = runBlocking {
        val result = safeApiCall {
            throw IOException("No route to host")
        }

        assertTrue(result is AppResult.Error)
        val error = result as AppResult.Error
        assertTrue(error.exception is NetworkUnavailableException)
    }

    @Test
    fun domainExceptions_maintainInheritanceHierarchy() {
        val netEx = NetworkUnavailableException("Net fail")
        val dbEx = DatabaseSyncException("DB fail")
        val authEx = AuthenticationException("Auth fail")
        val aiEx = AiInferenceException("AI fail")

        assertTrue(netEx is LifeTrackerException)
        assertTrue(dbEx is LifeTrackerException)
        assertTrue(authEx is LifeTrackerException)
        assertTrue(aiEx is LifeTrackerException)
    }
}
