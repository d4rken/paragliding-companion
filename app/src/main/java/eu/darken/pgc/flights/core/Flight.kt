package eu.darken.pgc.flights.core

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.time.Duration
import java.util.UUID

interface Flight {

    val id: Id

    val duration: Duration?
    val location: String?

    val sourceType: SourceType

    @Parcelize
    data class Id(
        val value: String = UUID.randomUUID().toString()
    ) : Parcelable

    enum class SourceType {
        XCTRACK,
        UNKNOWN
    }
}