package eu.darken.pgc.flights.core

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.time.Duration
import java.time.OffsetDateTime
import java.util.UUID

interface Flight {

    val id: Id

    val flightAt: OffsetDateTime?
    val distance: Long?
    val duration: Duration?
    val location: String?
    val gliderType: String?
    val trackerType: String?

    @Parcelize
    data class Id(
        val value: String = UUID.randomUUID().toString()
    ) : Parcelable
}