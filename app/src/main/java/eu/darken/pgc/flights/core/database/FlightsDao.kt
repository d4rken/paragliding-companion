package eu.darken.pgc.flights.core.database

import eu.darken.pgc.flights.core.Flight

interface FlightsDao {

    fun getById(flightId: Flight.Id): FlightEntity?

    fun getBySha1(sha1: String): FlightEntity?

    suspend fun flightCount(): Int
}