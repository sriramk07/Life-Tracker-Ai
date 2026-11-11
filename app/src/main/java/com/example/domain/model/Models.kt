package com.example.domain.model

enum class AppModule(val title: String) {
    DASHBOARD("Dashboard"),
    TASKS("Tasks"),
    HABITS("Habits"),
    EXPENSES("Expenses"),
    HEALTH("Health Tracker"),
    JOURNAL("Journal"),
    NOTES("Notes"),
    GOALS("Goal Tracker"),
    CALENDAR("Calendar"),
    AI_ASSISTANT("AI Assistant"),
    REPORTS("Reports & Analytics"),
    GUILDS("Hunter Guilds"),
    SETTINGS("Settings"),
    PROFILE("Profile"),
    AUTH("Authentication")
}

data class DashboardSummary(
    val pendingTasksCount: Int = 0,
    val totalExpensesToday: Double = 0.0,
    val waterIntakeMl: Int = 0,
    val waterGoalMl: Int = 2500,
    val sleepHours: Float = 0f,
    val activeHabitStreak: Int = 0,
    val habitCompletionPercent: Int = 0,
    val topGoalProgress: Int = 0,
    val todayEventsCount: Int = 0,
    val moodToday: String = "😊 Happy"
)

data class BmiResult(
    val bmiValue: Float,
    val category: String,
    val advice: String,
    val colorHex: Long
)

fun calculateBmi(weightKg: Float, heightCm: Float): BmiResult {
    if (heightCm <= 0f || weightKg <= 0f) return BmiResult(0f, "Unknown", "Enter valid height and weight.", 0xFF9E9E9E)
    val heightM = heightCm / 100f
    val bmi = weightKg / (heightM * heightM)
    return when {
        bmi < 18.5f -> BmiResult(bmi, "Underweight", "Consider balanced nutritional intake and strength training.", 0xFF2196F3)
        bmi in 18.5f..24.9f -> BmiResult(bmi, "Normal Weight", "Optimal healthy weight range! Maintain your active routine.", 0xFF4CAF50)
        bmi in 25.0f..29.9f -> BmiResult(bmi, "Overweight", "Incorporate daily cardio and portion control.", 0xFFFF9800)
        else -> BmiResult(bmi, "Obese", "Consult health guidelines for a structured fitness plan.", 0xFFF44336)
    }
}
