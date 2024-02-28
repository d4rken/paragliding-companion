package eu.darken.pgc.flights.core.igc

open class IGCException(
    override val cause: Throwable? = null,
    override val message: String? = null,
) : Exception()