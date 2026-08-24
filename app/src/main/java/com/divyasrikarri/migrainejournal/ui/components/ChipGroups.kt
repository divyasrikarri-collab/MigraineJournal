package com.divyasrikarri.migrainejournal.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Multi-select chip row. [selected] is the current set of chosen option labels. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MultiSelectChips(
    options: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option in selected,
                onClick = { onToggle(option) },
                label = { Text(option) }
            )
        }
    }
}

/** Single-select chip row. Tapping the selected chip clears the selection. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SingleSelectChips(
    options: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(if (option == selected) null else option) },
                label = { Text(option) }
            )
        }
    }
}

/** Row of tappable suggestion chips (recent medications, food autocomplete). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SuggestionChips(
    options: List<String>,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            AssistChip(onClick = { onClick(option) }, label = { Text(option) })
        }
    }
}
