package com.divyasrikarri.migrainejournal.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.divyasrikarri.migrainejournal.ui.theme.painColor
import com.divyasrikarri.migrainejournal.util.DateUtils
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Month grid where a day's fill intensity is that day's worst pain level, and a small dot
 * marks days with a completed check-in.
 */
@Composable
fun MonthCalendar(
    monthView: MonthView,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val firstOfMonth = monthView.month.withDayOfMonth(1)
    val daysInMonth = firstOfMonth.lengthOfMonth()
    // Monday-first grid; DayOfWeek.value is 1 (Mon) .. 7 (Sun).
    val leadingBlanks = firstOfMonth.dayOfWeek.value - 1
    val today = DateUtils.today()

    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous month"
                )
            }
            Text(
                DateUtils.monthLabel(monthView.month),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = onNextMonth) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next month"
                )
            }
        }

        Row(Modifier.fillMaxWidth()) {
            DayOfWeek.values().forEach { day ->
                Text(
                    text = day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        val cells = leadingBlanks + daysInMonth
        val rows = (cells + 6) / 7
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(rows) { row ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(7) { column ->
                        val index = row * 7 + column
                        val dayOfMonth = index - leadingBlanks + 1
                        if (dayOfMonth in 1..daysInMonth) {
                            val date = firstOfMonth.withDayOfMonth(dayOfMonth)
                            DayCell(
                                date = date,
                                pain = monthView.painByDay[date],
                                checkedIn = date in monthView.checkedInDays,
                                isToday = date == today,
                                isSelected = date == selectedDate,
                                onClick = { onSelectDate(date) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Box(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    pain: Int?,
    checkedIn: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = pain?.let { painColor(it) } ?: MaterialTheme.colorScheme.surfaceVariant
    val label = buildString {
        append(DateUtils.formatDate(date))
        append(if (pain != null) ", migraine, pain $pain out of 10" else ", no migraine")
        if (checkedIn) append(", check-in done")
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .then(
                if (isSelected) {
                    Modifier.border(
                        2.dp,
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(8.dp)
                    )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (pain != null) {
                    Color(0xFF1B1B1B)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            if (checkedIn) {
                Box(
                    Modifier
                        .padding(top = 2.dp)
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(
                            if (pain != null) {
                                Color(0xFF1B1B1B)
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                )
            }
        }
    }
}
