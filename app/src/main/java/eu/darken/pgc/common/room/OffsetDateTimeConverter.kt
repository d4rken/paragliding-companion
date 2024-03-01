package eu.darken.pgc.common.room

import androidx.room.TypeConverter
import java.time.OffsetDateTime

class OffsetDateTimeConverter {
    @TypeConverter
    fun fromValue(value: String?): OffsetDateTime? = value?.let { OffsetDateTime.parse(it) }

    @TypeConverter
    fun toValue(instant: OffsetDateTime?): String? = instant?.toString()
}