package eu.darken.pgc.flights.core.igc

data class IGCFile(
    val aRecord: IGCParser.ARecord?,
) {
    val manufacturerCode: String?
        get() = aRecord?.manufacturerCode

    val loggerCode: String?
        get() = aRecord?.loggerCode
}