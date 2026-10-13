package com.example

import com.example.data.remote.GeminiApiService
import org.junit.Assert.*
import org.junit.Test

/**
 * ============================================================================
 * UNIT TESTS: GEMINI & OPENROUTER AI SERVICE CLIENT
 * ============================================================================
 *
 * Verifies:
 * 1. Offline fallback response format and life coaching advice points.
 * 2. OpenRouter request JSON structure and messages hierarchy.
 * 3. Gemini REST API request JSON structure and contents array.
 */
class GeminiApiServiceTest {

    private val service = GeminiApiService()

    @Test
    fun generateOfflineFallbackResponse_returnsStructuredLifeCoaching() {
        val prompt = "Help me optimize my weekly schedule"
        val response = service.generateOfflineFallbackResponse(prompt)

        assertNotNull(response)
        assertTrue(response.contains("Life Tracker AI Recommendations"))
        assertTrue(response.contains("Productivity"))
        assertTrue(response.contains("Health"))
        assertTrue(response.contains("Finance"))
        assertTrue(response.contains("Pomodoro"))
    }

    @Test
    fun buildOpenRouterRequestBody_constructsValidSchema() {
        val prompt = "Analyze my habit consistency"
        val json = service.buildOpenRouterRequestBody(prompt)

        assertEquals("google/gemini-2.5-flash", json.getString("model"))
        val messages = json.getJSONArray("messages")
        assertEquals(2, messages.length())

        val systemMsg = messages.getJSONObject(0)
        assertEquals("system", systemMsg.getString("role"))
        assertTrue(systemMsg.getString("content").contains("Life Tracker AI"))

        val userMsg = messages.getJSONObject(1)
        assertEquals("user", userMsg.getString("role"))
        assertEquals(prompt, userMsg.getString("content"))
    }

    @Test
    fun buildGeminiRequestBody_constructsValidContentsPayload() {
        val prompt = "Summarize health logs"
        val json = service.buildGeminiRequestBody(prompt)

        val contents = json.getJSONArray("contents")
        assertEquals(1, contents.length())

        val contentObj = contents.getJSONObject(0)
        val parts = contentObj.getJSONArray("parts")
        assertEquals(1, parts.length())
        assertEquals(prompt, parts.getJSONObject(0).getString("text"))

        assertTrue(json.has("systemInstruction"))
    }
}
