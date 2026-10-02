package com.fyp.rebarcountingapp

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.line.lineSpec
import com.patrykandpatrick.vico.core.entry.entryModelOf
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun LineGraph(records: List<RebarCount>, groupBy: String) {
    val labelFormat = if (groupBy == "Month") "MMM''yy" else "d MMM''yy"


    fun RebarCount.normalizedDate(): Date {
        val cal = Calendar.getInstance().apply {
            time = this@normalizedDate.timestamp?.toDate() ?: Date()
            if (groupBy == "Month") {
                set(Calendar.DAY_OF_MONTH, 1)
            }
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.time
    }

    val grouped: Map<Date, List<RebarCount>> = records.groupBy { it.normalizedDate() }
    val sortedDates = grouped.keys.sorted()

    val labels = sortedDates.map {
        SimpleDateFormat(labelFormat, Locale.getDefault()).format(it)
    }

    val dataPoints = sortedDates.mapIndexed { idx, date ->
        idx.toFloat() to (grouped[date]?.sumOf { it.count }?.toFloat() ?: 0f)
    }

    val chartEntryModel = entryModelOf(*dataPoints.toTypedArray())
    val scrollState = rememberScrollState()
    val pointWidth: Dp = 100.dp
    val chartWidth = (dataPoints.size * pointWidth.value).dp

    val lineSpec = lineSpec(
        lineColor = Color(0xFF3F51B5),
        lineThickness = 2.dp,
    )

    Row(
        modifier = Modifier.horizontalScroll(scrollState)
    ) {
        Chart(
            chart = lineChart(lines = listOf(lineSpec)),
            model = chartEntryModel,
            startAxis = rememberStartAxis(
                valueFormatter = { value, _ -> value.toInt().toString() }
            ),
            bottomAxis = rememberBottomAxis(
                valueFormatter = { value, _ ->
                    val index = value.toInt()
                    labels.getOrNull(index) ?: ""
                }
            ),
            modifier = Modifier
                .width(chartWidth.coerceAtLeast(400.dp))
                .height(220.dp)
                .background(Color.White)
        )
    }
}