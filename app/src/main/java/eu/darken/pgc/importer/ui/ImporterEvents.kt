package eu.darken.pgc.importer.ui

import android.content.Intent

sealed interface ImporterEvents {
    data class ShowPicker(val intent: Intent) : ImporterEvents
}