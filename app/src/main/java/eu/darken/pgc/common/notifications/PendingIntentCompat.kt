package eu.darken.pgc.common.notifications

import android.app.PendingIntent
import eu.darken.pgc.common.hasApiLevel

object PendingIntentCompat {
    val FLAG_IMMUTABLE: Int = if (hasApiLevel(31)) {
        PendingIntent.FLAG_IMMUTABLE
    } else {
        0
    }
}