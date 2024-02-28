package eu.darken.pgc.flights.core.igc

data class IGCFile(
    val aRecord: IGCParser.ARecord?,
    val header: IGCParser.HRecord?,
) {
    val manufacturerCode: String?
        get() = aRecord?.manufacturerCode

    val loggerCode: String?
        get() = aRecord?.loggerCode

    fun isValid(): Boolean {
        if (aRecord?.isValid() != true) return false
        if (header?.isValid() != true) return false
        return true
    }
}