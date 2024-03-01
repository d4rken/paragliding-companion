package eu.darken.pgc.flights.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import eu.darken.pgc.common.room.DurationConverter
import eu.darken.pgc.common.room.InstantConverter
import eu.darken.pgc.common.room.OffsetDateTimeConverter
import eu.darken.pgc.flights.core.igc.IGCFlightEntity
import eu.darken.pgc.flights.core.igc.IGCFlightsDao

@Database(
    entities = [
        IGCFlightEntity::class,
    ],
    version = 1,
    autoMigrations = [
//        AutoMigration(1, 2)
    ],
    exportSchema = true,
)
@TypeConverters(
    InstantConverter::class,
    DurationConverter::class,
    OffsetDateTimeConverter::class,
    FlightIdTypeConverter::class,
)
abstract class FlightsRoomDb : RoomDatabase() {
    abstract fun flightsIgc(): IGCFlightsDao
}