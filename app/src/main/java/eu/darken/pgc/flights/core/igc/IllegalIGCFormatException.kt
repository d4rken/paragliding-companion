package eu.darken.pgc.flights.core.igc

class IllegalIGCFormatException(
    override val message: String?,
    override val cause: Throwable? = null,
) : IGCException()