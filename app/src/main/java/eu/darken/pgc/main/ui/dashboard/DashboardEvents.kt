package eu.darken.pgc.main.ui.dashboard

import android.content.Intent

sealed interface DashboardEvents {
    data class GrantXCTrackAccess(val intent: Intent) : DashboardEvents
}