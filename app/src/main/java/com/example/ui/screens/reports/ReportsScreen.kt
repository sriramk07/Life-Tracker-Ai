package com.example.ui.screens.reports

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.ExpenseEntity
import com.example.data.local.entities.HabitEntity
import com.example.data.local.entities.HealthLogEntity
import com.example.data.local.entities.TaskEntity
import com.example.export.LifeReportSnapshot
import com.example.export.ReportExportEngine
import com.example.ui.components.*
import java.util.Locale

/**
 * ============================================================================
 * REPORTS & ANALYTICS SCREEN (RELEASE 1.0 — LIVE METRICS + PDF/CSV EXPORT)
 * ============================================================================
 *
 * Aggregates live Room data into interactive charts and supports exporting
 * formatted PDF documents, RFC-4180 CSV ledgers, and clipboard digests.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    tasks: List<TaskEntity> = emptyList(),
    habits: List<HabitEntity> = emptyList(),
    expenses: List<ExpenseEntity> = emptyList(),
    latestHealth: HealthLogEntity? = null,
    userName: String = "Hunter",
    level: Int = 1,
    totalXp: Int = 0
) {
    val context = LocalContext.current
    var selectedTimeframe by remember { mutableStateOf("Weekly") }
    var showExportDialog by remember { mutableStateOf(false) }

    val completedTaskCount = tasks.count { it.isCompleted }
    val expenseTotal = expenses.filter { it.type.equals("Expense", true) }.sumOf { it.amount }
    val habitStreakMax = habits.maxOfOrNull { it.streakCount } ?: 0
    val waterIntakeMl = latestHealth?.waterIntakeMl ?: 0

    val efficiencyScore = remember(tasks, habits, waterIntakeMl) {
        ReportExportEngine.calculateEfficiencyScore(tasks, habits, waterIntakeMl)
    }
    val weeklyTrend = remember(tasks, habits) {
        ReportExportEngine.computeWeeklyFocusTrend(tasks, habits)
    }
    val expenseSlices = remember(expenses) {
        ReportExportEngine.computeExpenseSlices(expenses)
    }

    val snapshot = remember(userName, selectedTimeframe, tasks, habits, expenses, latestHealth, level, totalXp) {
        LifeReportSnapshot(
            userName = userName,
            timeframe = selectedTimeframe,
            tasks = tasks,
            habits = habits,
            expenses = expenses,
            latestHealth = latestHealth,
            level = level,
            totalXp = totalXp
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("reports_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Reports & Analytics 📊",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Live database intelligence & PDF / CSV export studio",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { showExportDialog = true },
                    modifier = Modifier.testTag("export_report_btn")
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Export Report", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Weekly", "Monthly", "Yearly").forEach { timeframe ->
                    FilterChip(
                        selected = selectedTimeframe == timeframe,
                        onClick = { selectedTimeframe = timeframe },
                        label = { Text(timeframe) }
                    )
                }
            }
        }

        // Dynamic Circular Gauge KPI Card
        item {
            GlassmorphicCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("$selectedTimeframe Efficiency Score", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("• $completedTaskCount/${tasks.size} Quests completed", fontSize = 12.sp, color = Color(0xFF1E88E5))
                        Text("• $${String.format(Locale.US, "%.2f", expenseTotal)} total logged expenses", fontSize = 12.sp, color = Color(0xFFFB8C00))
                        Text("• $habitStreakMax days top habit streak", fontSize = 12.sp, color = Color(0xFF43A047))
                    }

                    CircularProgressGauge(
                        progressPercent = efficiencyScore,
                        activeColor = Color(0xFF1E88E5),
                        modifier = Modifier.size(90.dp)
                    )
                }
            }
        }

        // Live Weekly Trend Chart
        item {
            GlassmorphicCard {
                Text("Focus & Execution Trend Line", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))
                WeeklyLineChart(dataPoints = weeklyTrend, lineColor = Color(0xFF1E88E5))
            }
        }

        // Real Expense Pie Chart & Breakdown
        item {
            GlassmorphicCard {
                Text("Expense Distribution Breakdown", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))

                if (expenseSlices.isEmpty()) {
                    Text(
                        text = "No expense records logged yet. Log expenses in Treasury to view live category breakdowns.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CustomPieChart(
                            slices = expenseSlices,
                            modifier = Modifier.size(130.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            expenseSlices.take(5).forEach { slice ->
                                LegendItem(
                                    label = slice.category,
                                    value = "$${String.format(Locale.US, "%.0f", slice.value)}",
                                    color = slice.color
                                )
                            }
                        }
                    }
                }
            }
        }

        // Habit Heatmap (GitHub Style)
        item {
            GlassmorphicCard {
                HabitContributionGraph()
            }
        }
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Report Studio") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Export your $selectedTimeframe LifeOS data cleanly to shareable files:")

                    Button(
                        onClick = {
                            val pdfFile = ReportExportEngine.writePdfReportToCache(context, snapshot)
                            ReportExportEngine.shareGeneratedFile(context, pdfFile, "application/pdf")
                            Toast.makeText(context, "PDF generated: ${pdfFile.name}", Toast.LENGTH_SHORT).show()
                            showExportDialog = false
                        },
                        modifier = Modifier.fillMaxWidth().testTag("export_pdf_btn")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export & Share Vector PDF")
                    }

                    OutlinedButton(
                        onClick = {
                            val csvFile = ReportExportEngine.writeCsvReportToCache(context, snapshot)
                            ReportExportEngine.shareGeneratedFile(context, csvFile, "text/csv")
                            Toast.makeText(context, "CSV generated: ${csvFile.name}", Toast.LENGTH_SHORT).show()
                            showExportDialog = false
                        },
                        modifier = Modifier.fillMaxWidth().testTag("export_csv_btn")
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export RFC-4180 CSV Ledger")
                    }

                    OutlinedButton(
                        onClick = {
                            val textSummary = ReportExportEngine.generateExecutiveSummaryText(snapshot)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("LifeOS_Report", textSummary))
                            Toast.makeText(context, "Executive report copied to clipboard!", Toast.LENGTH_SHORT).show()
                            showExportDialog = false
                        },
                        modifier = Modifier.fillMaxWidth().testTag("copy_report_summary_btn")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy Text Executive Summary")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun LegendItem(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = MaterialTheme.shapes.extraSmall, color = color, modifier = Modifier.size(10.dp)) {}
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "$label: ", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Text(text = value, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
