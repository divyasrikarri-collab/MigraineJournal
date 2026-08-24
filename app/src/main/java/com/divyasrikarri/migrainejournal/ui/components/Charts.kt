package com.divyasrikarri.migrainejournal.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.divyasrikarri.migrainejournal.data.model.FrequencyBucket
import com.divyasrikarri.migrainejournal.data.model.PainPoint

/**
 * Compose-native charts drawn with layout and [Canvas]. The two charts the spec asks for are
 * simple enough that a charting dependency would cost more than it saves, and this keeps the
 * rendering theme-aware for free.
 */

/** Vertical bar chart of migraine counts per bucket. */
@Composable
fun FrequencyBarChart(
    buckets: List<FrequencyBucket>,
    modifier: Modifier = Modifier,
    barHeight: Dp = 160.dp
) {
    if (buckets.isEmpty()) {
        EmptyState("Not enough data to chart yet.")
        return
    }
    val maxCount = buckets.maxOf { it.count }.coerceAtLeast(1)
    // With many buckets, thin the x-axis labels so they stay readable.
    val labelStride = when {
        buckets.size <= 8 -> 1
        buckets.size <= 16 -> 2
        buckets.size <= 32 -> 4
        else -> 6
    }
    val description = buckets.joinToString(", ") { "${it.label}: ${it.count}" }

    Column(
        modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Migraine frequency chart. $description" }
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(barHeight),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            buckets.forEach { bucket ->
                val fraction = bucket.count.toFloat() / maxCount
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (bucket.count > 0 && buckets.size <= 16) {
                        Text(
                            text = bucket.count.toString(),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            // Keep a hairline for empty buckets so gaps stay visible.
                            .fillMaxHeight(if (bucket.count == 0) 0.012f else fraction * 0.88f)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(
                                if (bucket.count == 0) {
                                    MaterialTheme.colorScheme.outlineVariant
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                    )
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            buckets.forEachIndexed { index, bucket ->
                Text(
                    text = if (index % labelStride == 0) bucket.label else "",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** Line chart of pain level (fixed 0..10 axis) over successive migraines. */
@Composable
fun PainTrendChart(
    points: List<PainPoint>,
    modifier: Modifier = Modifier
) {
    if (points.size < 2) {
        EmptyState("Log at least two migraines to see a pain trend.")
        return
    }
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    val description = "Pain level trend across ${points.size} migraines, " +
        "from ${points.first().label} to ${points.last().label}."

    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            Column(
                Modifier
                    .width(24.dp)
                    .height(180.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                listOf("10", "5", "0").forEach { label ->
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Canvas(
                Modifier
                    .weight(1f)
                    .height(180.dp)
                    .padding(start = 6.dp)
                    .semantics { contentDescription = description }
            ) {
                val height = size.height
                val width = size.width
                val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))

                // Grid at 0, 5 and 10.
                listOf(0f, 0.5f, 1f).forEach { fraction ->
                    val y = height * fraction
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f,
                        pathEffect = if (fraction == 0.5f) dash else null
                    )
                }

                val stepX = if (points.size > 1) width / (points.size - 1) else width
                fun yFor(level: Float) = height - (level.coerceIn(0f, 10f) / 10f) * height

                val linePath = Path()
                points.forEachIndexed { index, point ->
                    val x = stepX * index
                    val y = yFor(point.painLevel)
                    if (index == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
                }

                val fillPath = Path().apply {
                    addPath(linePath)
                    lineTo(stepX * (points.size - 1), height)
                    lineTo(0f, height)
                    close()
                }
                drawPath(fillPath, color = fillColor)
                drawPath(linePath, color = lineColor, style = Stroke(width = 3f))

                points.forEachIndexed { index, point ->
                    drawCircle(
                        color = lineColor,
                        radius = 3.5f,
                        center = Offset(stepX * index, yFor(point.painLevel))
                    )
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 30.dp, top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                points.first().label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                points.last().label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
