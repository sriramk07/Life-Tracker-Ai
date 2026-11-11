package com.example.ui.screens.health

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.HealthLogEntity
import com.example.domain.model.calculateBmi
import com.example.sensors.VitalityAnalytics
import com.example.ui.components.CircularProgressGauge
import com.example.ui.components.GlassmorphicCard
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScreen(
    healthLog: HealthLogEntity?,
    liveSensorSteps: Int = 0,
    isSensorAvailable: Boolean = false,
    onAddWater: (Int) -> Unit,
    onSyncSteps: (Int) -> Unit = {},
    onUpdateMetrics: (Float, Float, Float, Int, String, Int) -> Unit
) {
    val currentLog = healthLog ?: HealthLogEntity()

    var weightText by remember(currentLog) { mutableStateOf(currentLog.weightKg.toString()) }
    var heightText by remember(currentLog) { mutableStateOf(currentLog.heightCm.toString()) }
    var sleepText by remember(currentLog) { mutableStateOf(currentLog.sleepHours.toString()) }
    var workoutMinsText by remember(currentLog) { mutableStateOf(currentLog.workoutDurationMins.toString()) }
    var stepsText by remember(currentLog) { mutableStateOf(currentLog.stepsCount.toString()) }
    var selectedMood by remember(currentLog) { mutableStateOf(currentLog.mood) }

    val parsedWeight = weightText.toFloatOrNull() ?: currentLog.weightKg
    val parsedHeight = heightText.toFloatOrNull() ?: currentLog.heightCm
    val parsedSleep = sleepText.toFloatOrNull() ?: currentLog.sleepHours
    val parsedSteps = (stepsText.toIntOrNull() ?: currentLog.stepsCount) + liveSensorSteps
    val parsedWorkout = workoutMinsText.toIntOrNull() ?: currentLog.workoutDurationMins

    val bmiResult = calculateBmi(weightKg = parsedWeight, heightCm = parsedHeight)
    val waterPercent = ((currentLog.waterIntakeMl * 100) / 2500).coerceAtMost(100)
    val vitalityScore = VitalityAnalytics.calculateVitalityScore(
        waterMl = currentLog.waterIntakeMl,
        waterGoalMl = 2500,
        sleepHours = parsedSleep,
        stepsCount = parsedSteps,
        stepGoal = 10000,
        workoutMins = parsedWorkout
    )
    val caloriesBurned = VitalityAnalytics.estimateCaloriesBurned(
        stepsCount = parsedSteps,
        workoutDurationMins = parsedWorkout,
        weightKg = parsedWeight
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("health_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Vitality & Sensor Tracker",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Composite Vitality Readiness Index Card
        item {
            GlassmorphicCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFF10B981))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Composite Vitality Index", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = VitalityAnalytics.vitalityGrade(vitalityScore),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF10B981)
                        )
                        Text(
                            text = "Estimated burn: $caloriesBurned kcal • $parsedSteps / 10,000 steps",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    CircularProgressGauge(
                        progressPercent = vitalityScore,
                        modifier = Modifier.size(72.dp),
                        strokeWidth = 7.dp,
                        activeColor = Color(0xFF10B981)
                    )
                }
            }
        }

        // Pedometer & Step Counter Card
        item {
            GlassmorphicCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DirectionsWalk, contentDescription = "Steps", tint = Color(0xFF00E5FF))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pedometer & Motion", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isSensorAvailable) "Hardware step counter active (+$liveSensorSteps live)"
                            else "Manual / quick-sync step mode active",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            onSyncSteps(500)
                            val updated = (stepsText.toIntOrNull() ?: currentLog.stepsCount) + 500
                            stepsText = updated.toString()
                        },
                        modifier = Modifier.testTag("sync_steps_btn")
                    ) {
                        Text("+500 Steps")
                    }
                }
            }
        }

        // Water Tracker Card
        item {
            GlassmorphicCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WaterDrop, contentDescription = "Water", tint = Color(0xFF2196F3))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Water Tracker", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${currentLog.waterIntakeMl} / 2500 ml Goal", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onAddWater(250) },
                                modifier = Modifier.testTag("water_add_250_btn")
                            ) {
                                Text("+250 ml")
                            }
                            OutlinedButton(onClick = { onAddWater(500) }) {
                                Text("+500 ml")
                            }
                        }
                    }

                    CircularProgressGauge(
                        progressPercent = waterPercent,
                        modifier = Modifier.size(70.dp),
                        strokeWidth = 7.dp,
                        activeColor = Color(0xFF2196F3)
                    )
                }
            }
        }

        // Dynamic BMI Calculator Card
        item {
            GlassmorphicCard {
                Text("BMI Calculator & Body Metrics", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it },
                        label = { Text("Weight (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).testTag("weight_input")
                    )
                    OutlinedTextField(
                        value = heightText,
                        onValueChange = { heightText = it },
                        label = { Text("Height (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).testTag("height_input")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "BMI: ${String.format(Locale.US, "%.1f", bmiResult.bmiValue)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Surface(
                                color = Color(bmiResult.colorHex),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = bmiResult.category,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(bmiResult.advice, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Sleep, Steps & Workout Card (Fully Editable)
        item {
            GlassmorphicCard {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Hotel, contentDescription = "Sleep", tint = Color(0xFF673AB7))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sleep Hours", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                        OutlinedTextField(
                            value = sleepText,
                            onValueChange = { sleepText = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth().testTag("sleep_input")
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DirectionsRun, contentDescription = "Workout", tint = Color(0xFFFF5722))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Workout Mins", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                        OutlinedTextField(
                            value = workoutMinsText,
                            onValueChange = { workoutMinsText = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("workout_mins_input")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = stepsText,
                    onValueChange = { stepsText = it },
                    label = { Text("Daily Step Tally") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("steps_input")
                )
            }
        }

        // Mood Selector Card
        item {
            GlassmorphicCard {
                Text("Daily Mood & Mental Check-in", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("⚡ Energetic", "😊 Happy", "😌 Calm", "😐 Neutral", "😟 Anxious").forEach { mood ->
                        FilterChip(
                            selected = selectedMood == mood,
                            onClick = { selectedMood = mood },
                            label = { Text(mood.take(2)) }
                        )
                    }
                }
            }
        }

        // Save Button
        item {
            Button(
                onClick = {
                    onUpdateMetrics(parsedWeight, parsedHeight, parsedSleep, parsedSteps, selectedMood, parsedWorkout)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_health_btn")
            ) {
                Text("Save Today's Health Log", fontWeight = FontWeight.Bold)
            }
        }
    }
}
