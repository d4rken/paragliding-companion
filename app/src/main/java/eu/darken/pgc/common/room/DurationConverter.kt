package eu.darken.pgc.common.room

import androidx.room.TypeConverter
import java.time.Duration

class DurationConverter {
    @TypeConverter
    fun fromValue(value: String?): Duration? = value?.let { Duration.parse(it) }

    @TypeConverter
    fun toValue(instant: Duration?): String? = instant?.toString()
}