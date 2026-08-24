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

    private fun routeFor(intent: Intent?): String? =
        when (intent?.getStringExtra(EXTRA_DESTINATION)) {
            DESTINATION_CHECK_IN -> Routes.checkIn()
            else -> null
        }

    companion object {
        const val EXTRA_DESTINATION = "com.divyasrikarri.migrainejournal.extra.DESTINATION"
        const val DESTINATION_CHECK_IN = "check_in"
    }
}
