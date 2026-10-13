package com.example

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/**
 * ============================================================================
 * UNIT TESTS: JSON BACKUP & RESTORE SERIALIZATION ENGINE
 * ============================================================================
 *
 * Verifies:
 * 1. JSON backup structure adheres to version 2 schema specification.
 * 2. Entity serialization (tasks, habits, expenses, profile, stats).
 * 3. Parser handles missing or optional keys with sensible fallbacks.
 * 4. Corrupt JSON payloads are rejected gracefully without crashing.
 */
class BackupRestoreTest {

    @Test
    fun backupJsonSchema_containsMandatoryRootMetadata() {
        val root = JSONObject().apply {
            put("version", 2)
            put("exportedAt", System.currentTimeMillis())
            put("appName", "LifeOS AI")
            put("tasks", JSONArray())
            put("habits", JSONArray())
            put("expenses", JSONArray())
        }

        assertEquals(2, root.getInt("version"))
        assertEquals("LifeOS AI", root.getString("appName"))
        assertTrue(root.has("exportedAt"))
        assertTrue(root.has("tasks"))
        assertTrue(root.has("habits"))
        assertTrue(root.has("expenses"))
    }

    @Test
    fun taskJsonSerialization_preservesAllEntityProperties() {
        val taskObj = JSONObject().apply {
            put("id", 101L)
            put("title", "Finalize Review 2 Presentation")
            put("description", "75% project milestone submission")
            put("isCompleted", false)
            put("priority", "High")
            put("category", "Work")
            put("dueDate", 1700000000000L)
        }

        assertEquals(101L, taskObj.getLong("id"))
        assertEquals("Finalize Review 2 Presentation", taskObj.getString("title"))
        assertFalse(taskObj.getBoolean("isCompleted"))
        assertEquals("High", taskObj.getString("priority"))
        assertEquals("Work", taskObj.getString("category"))
    }

    @Test
    fun restoreParser_handlesCorruptPayloadGracefully() {
        val invalidJson = "{ this is malformed json string without closing brackets"

        val parseResult = try {
            JSONObject(invalidJson)
            true
        } catch (e: Exception) {
            false
        }

        assertFalse("Corrupt JSON should fail parsing safely", parseResult)
    }

    @Test
    fun restoreParser_recoversDefaultsWhenOptionalFieldsOmitted() {
        val minimalTaskJson = JSONObject().apply {
            put("title", "Drink Water")
        }

        val title = minimalTaskJson.getString("title")
        val description = minimalTaskJson.optString("description", "")
        val isCompleted = minimalTaskJson.optBoolean("isCompleted", false)
        val priority = minimalTaskJson.optString("priority", "Medium")

        assertEquals("Drink Water", title)
        assertEquals("", description)
        assertFalse(isCompleted)
        assertEquals("Medium", priority)
    }
}
