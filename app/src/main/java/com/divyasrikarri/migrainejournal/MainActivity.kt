package com.divyasrikarri.migrainejournal

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.divyasrikarri.migrainejournal.ui.MigraineApp
import com.divyasrikarri.migrainejournal.ui.navigation.Routes
import com.divyasrikarri.migrainejournal.ui.theme.MigraineJournalTheme

class MainActivity : ComponentActivity() {

    /** Route requested by the launching intent (a reminder tap), consumed once by the NavHost. */
    private var pendingRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingRoute = routeFor(intent)

        setContent {
            MigraineJournalTheme {
                MigraineApp(
                    pendingRoute = pendingRoute,
                    onPendingRouteConsumed = { pendingRoute = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingRoute = routeFor(intent)
    }

    /**
     * Reads the launching intent's destination and clears it. Without the clear, the extra
     * survives on the same Intent across an activity recreation, so rotating the device would
     * bounce the user back into the check-in.
     */
    private fun routeFor(intent: Intent?): String? {
        val destination = intent?.getStringExtra(EXTRA_DESTINATION) ?: return null
        intent.removeExtra(EXTRA_DESTINATION)
        return when (destination) {
            DESTINATION_CHECK_IN -> Routes.checkIn()
            else -> null
        }
    }

    companion object {
        const val EXTRA_DESTINATION = "com.divyasrikarri.migrainejournal.extra.DESTINATION"
        const val DESTINATION_CHECK_IN = "check_in"
    }
}
