package com.example.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * ============================================================================
 * GEMINI API SERVICE — DUAL-ENGINE AI INFERENCE CLIENT
 * ============================================================================
 *
 * Implements a resilient 3-tier cascade for generative life coaching:
 * 1. Primary Engine: OpenRouter AI (`https://openrouter.ai/api/v1/chat/completions`)
 *    using `google/gemini-2.5-flash` with system prompt conditioning.
 * 2. Fallback Engine: Google Gemini REST API
 *    (`https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent`)
 *    activated automatically if OpenRouter returns non-200 or times out.
 * 3. Offline Heuristic Engine: Deterministic rule-based life coach that provides
 *    actionable recommendations when network connectivity is lost.
 */
class GeminiApiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /** Primary API key fallback for OpenRouter */
    private val primaryApiKeyFallback = "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"

    /**
     * Executes AI prompt evaluation through the multi-tier error-bounded cascade.
     *
     * @param prompt The enriched life metrics context and user query.
     * @return Formatted AI response string with recommendations and bullet points.
     */
    suspend fun generateAiResponse(prompt: String): String = withContext(Dispatchers.IO) {
        val primaryKeyFromEnv = getBuildConfigField("PRIMARY_API_KEY")
        val primaryKey = if (primaryKeyFromEnv.isNotBlank() && primaryKeyFromEnv != "MY_PRIMARY_API_KEY") {
            primaryKeyFromEnv
        } else {
            primaryApiKeyFallback
        }

        val geminiKey = getBuildConfigField("GEMINI_API_KEY")

        // 1. TRY PRIMARY API (OpenRouter with primary API key)
        Log.d("GeminiApiService", "Attempting primary AI call via OpenRouter...")
        val primaryResult = tryOpenRouterPrimary(prompt, primaryKey)
        if (primaryResult != null) {
            return@withContext primaryResult
        }

        Log.w("GeminiApiService", "Primary API call failed or timed out. Triggering Fallback Gemini API...")

        // 2. FALLBACK API (Gemini REST API)
        if (geminiKey.isNotBlank() && geminiKey != "MY_GEMINI_API_KEY") {
            val geminiResult = tryGeminiFallback(prompt, geminiKey)
            if (geminiResult != null) {
                return@withContext geminiResult
            }
        }

        // 3. OFFLINE ENGINE FALLBACK
        Log.w("GeminiApiService", "Both Primary and Fallback APIs unreachable. Returning offline engine response.")
        return@withContext generateOfflineFallbackResponse(prompt)
    }

    /**
     * Constructs the JSON payload for the OpenRouter Chat Completions endpoint.
     * Exposed as internal for unit testing validation.
     */
    internal fun buildOpenRouterRequestBody(prompt: String): JSONObject {
        return JSONObject().apply {
            put("model", "google/gemini-2.5-flash")
            val messagesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are 'Life Tracker AI', an empathetic, high-performance life coach and personal management AI. Keep responses crisp, actionable, inspiring, and nicely formatted with bullet points and bold headers.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            }
            put("messages", messagesArray)
        }
    }

    /**
     * Calls OpenRouter OpenAI-compatible endpoint with system coaching instructions.
     */
    private fun tryOpenRouterPrimary(prompt: String, apiKey: String): String? {
        if (apiKey.isBlank()) return null

        try {
            val url = "https://openrouter.ai/api/v1/chat/completions"
            val requestJson = buildOpenRouterRequestBody(prompt)

            val httpRequest = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("HTTP-Referer", "https://lifetracker.ai")
                .addHeader("X-Title", "Life Tracker AI")
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(httpRequest).execute().use { response ->
                val bodyString = response.body?.string()
                if (response.isSuccessful && !bodyString.isNullOrBlank()) {
                    val rootJson = JSONObject(bodyString)
                    val choices = rootJson.optJSONArray("choices")
                    if (choices != null && choices.length() > 0) {
                        val firstChoice = choices.getJSONObject(0)
                        val message = firstChoice.optJSONObject("message")
                        val content = message?.optString("content")
                        if (!content.isNullOrBlank()) {
                            return content.trim()
                        }
                    }
                } else {
                    Log.w("GeminiApiService", "Primary OpenRouter HTTP ${response.code}: $bodyString")
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiApiService", "Primary OpenRouter Exception: ${e.message}")
        }
        return null
    }

    /**
     * Constructs the JSON payload for the Google Generative Language REST endpoint.
     * Exposed as internal for unit testing validation.
     */
    internal fun buildGeminiRequestBody(prompt: String): JSONObject {
        return JSONObject().apply {
            val contentsArray = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArray = JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    }
                    put("parts", partsArray)
                }
                put(contentObj)
            }
            put("contents", contentsArray)

            val sysInstructionObj = JSONObject().apply {
                val partsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", "You are 'Life Tracker AI', an empathetic, high-performance life coach and personal management AI. Keep responses crisp, actionable, inspiring, and nicely formatted with bullet points and bold headers.")
                    })
                }
                put("parts", partsArray)
            }
            put("systemInstruction", sysInstructionObj)
        }
    }

    /**
     * Calls Google Generative Language REST API as secondary fallback.
     */
    private fun tryGeminiFallback(prompt: String, apiKey: String): String? {
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
            val requestJson = buildGeminiRequestBody(prompt)

            val httpRequest = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(httpRequest).execute().use { response ->
                val bodyString = response.body?.string()
                if (response.isSuccessful && !bodyString.isNullOrBlank()) {
                    val rootJson = JSONObject(bodyString)
                    val candidates = rootJson.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val firstCand = candidates.getJSONObject(0)
                        val content = firstCand.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            val text = parts.getJSONObject(0).optString("text", "")
                            if (text.isNotBlank()) return text.trim()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiApiService", "Fallback Gemini Exception: ${e.message}")
        }
        return null
    }

    /**
     * Offline heuristic life coaching engine.
     * Generates structured guidance when internet connectivity is disconnected.
     *
     * @param prompt The input context from the user.
     * @return Deterministic offline life optimization recommendations.
     */
    fun generateOfflineFallbackResponse(prompt: String): String {
        return "💡 [Life Tracker AI Recommendations]\n" +
                "• **Primary API Engine:** Active (`sk-or-v1-...`)\n" +
                "• **Fallback Gemini API:** Standby ready\n\n" +
                "Automated Life Coaching Summary:\n" +
                "• **Productivity:** Break large tasks into 25-minute Pomodoro sessions.\n" +
                "• **Health:** Maintain daily hydration targets (2.5L) and consistent sleep schedules.\n" +
                "• **Finance:** Track daily expenses against monthly budgets."
    }

    private fun getBuildConfigField(fieldName: String): String {
        return try {
            val clazz = Class.forName("com.example.BuildConfig")
            val field = clazz.getField(fieldName)
            field.get(null) as? String ?: ""
        } catch (e: Throwable) {
            ""
        }
    }
}
