package com.divyasrikarri.migrainejournal.ui.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.divyasrikarri.migrainejournal.BuildConfig
import com.divyasrikarri.migrainejournal.data.model.VolumeUnit
import com.divyasrikarri.migrainejournal.notification.NotificationHelper
import com.divyasrikarri.migrainejournal.ui.AppViewModelProvider
import com.divyasrikarri.migrainejournal.ui.components.SectionCard
import com.divyasrikarri.migrainejournal.ui.components.SingleSelectChips
import com.divyasrikarri.migrainejournal.ui.components.TimePickerDialog
import com.divyasrikarri.migrainejournal.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val exporting by viewModel.exporting.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showTimePicker by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showPermissionRationale by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> viewModel.onNotificationPermissionResult(granted) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.Share ->
                    context.startActivity(Intent.createChooser(event.intent, event.message))
                is SettingsEvent.Message -> snackbarHostState.showSnackbar(event.text)
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionCard(title = "Daily reminder") {
                    SettingRow(
                        title = "Remind me to check in",
                        subtitle = "A single evening notification"
                    ) {
                        Switch(
                            checked = settings.reminderEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.setReminderEnabled(enabled)
                                if (enabled && !NotificationHelper.hasPermission(context)) {
                                    showPermissionRationale = true
                                }
                            }
                        )
                    }
                    SettingRow(
                        title = "Reminder time",
                        subtitle = "When the check-in notification arrives"
                    ) {
                        OutlinedButton(
                            onClick = { showTimePicker = true },
                            enabled = settings.reminderEnabled
                        ) {
                            Text(
                                DateUtils.formatTimeOfDay(
                                    settings.reminderHour,
                                    settings.reminderMinute
                                )
                            )
                        }
                    }
                    if (settings.reminderEnabled && !NotificationHelper.hasPermission(context)) {
                        Text(
                            "Notifications are blocked for this app, so the reminder cannot be " +
                                "delivered.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            item {
                SectionCard(title = "Tracking") {
                    SettingRow(
                        title = "Menstrual cycle day",
                        subtitle = "Adds a cycle-day field to the daily check-in"
                    ) {
                        Switch(
                            checked = settings.trackMenstrualCycle,
                            onCheckedChange = viewModel::setTrackMenstrualCycle
                        )
                    }
                }
            }

            item {
                SectionCard(title = "Units") {
                    Text("Water intake", style = MaterialTheme.typography.bodyMedium)
                    SingleSelectChips(
                        options = VolumeUnit.entries.map { it.label },
                        selected = settings.volumeUnit.label,
                        onSelect = { label ->
                            VolumeUnit.entries
                                .firstOrNull { it.label == label }
                                ?.let(viewModel::setVolumeUnit)
                        }
                    )
                    Text(
                        "Stored values are always millilitres; this only changes how they are " +
                            "shown.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                SectionCard(title = "Export") {
                    Text(
                        "Files are written to this app's own storage and only leave the device " +
                            "if you pick a target in the share sheet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = viewModel::exportPdf,
                            enabled = !exporting,
                            modifier = Modifier.weight(1f)
                        ) { Text("PDF summary") }
                        OutlinedButton(
                            onClick = viewModel::exportCsv,
                            enabled = !exporting,
                            modifier = Modifier.weight(1f)
                        ) { Text("CSV data") }
                    }
                }
            }

            item {
                SectionCard(title = "Data") {
                    Text(
                        "Everything is stored on this device only. There is no account, no " +
                            "sync and no analytics.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Clear all data") }
                }
            }

            item {
                SectionCard(title = "About") {
                    Text(
                        "Migraine Journal ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "A personal log, not a medical device. Nothing here is a diagnosis or " +
                            "treatment advice.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            initialHour = settings.reminderHour,
            initialMinute = settings.reminderMinute,
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                showTimePicker = false
                viewModel.setReminderTime(hour, minute)
            }
        )
    }

    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            title = { Text("Allow notifications?") },
            text = {
                Text(
                    "Migraine Journal sends one notification a day, at the time you choose, to " +
                        "prompt the daily check-in. It is never used for anything else."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionRationale = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationale = false }) { Text("Not now") }
            }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Delete everything?") },
            text = {
                Text(
                    "This permanently removes every migraine, daily check-in and food entry on " +
                        "this device. Export first if you want a copy."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    viewModel.clearAllData()
                }) { Text("Delete everything") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String? = null,
    trailing: @Composable () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        trailing()
    }
}
