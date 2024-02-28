package eu.darken.pgc.flights.core.database

import androidx.room.TypeConverter
import eu.darken.pgc.flights.core.Flight

class FlightIdTypeConverter {
    @TypeConverter
    fun fromAddress(id: Flight.Id): String = id.value

    @TypeConverter
    fun toAddress(id: String) = Flight.Id(id)
}