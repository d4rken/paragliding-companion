package eu.darken.pgc.common.error

import eu.darken.pgc.common.livedata.SingleLiveEvent

interface ErrorEventSource {
    val errorEvents: SingleLiveEvent<Throwable>
}