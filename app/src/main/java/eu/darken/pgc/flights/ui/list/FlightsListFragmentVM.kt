package eu.darken.pgc.flights.ui.list

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.pgc.common.coroutine.DispatcherProvider
import eu.darken.pgc.common.debug.logging.logTag
import eu.darken.pgc.common.uix.ViewModel3
import eu.darken.pgc.flights.core.FlightRepo
import eu.darken.pgc.flights.core.igc.IGCFlight
import eu.darken.pgc.flights.ui.list.items.IGCFlightItemVH
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

@HiltViewModel
class FlightsListFragmentVM @Inject constructor(
    handle: SavedStateHandle,
    dispatcherProvider: DispatcherProvider,
    private val flightRepo: FlightRepo,
) : ViewModel3(dispatcherProvider = dispatcherProvider) {

    val state = flightRepo.data
        .map { flights ->
            val items = flights.flights
                .sortedByDescending { it.flightAt }
                .map { flight ->
                    when (flight) {
                        is IGCFlight -> IGCFlightItemVH.Item(
                            flight = flight,
                            onClick = {

                            }
                        )

                        else -> throw NotImplementedError("Type not implemented $flight")
                    }
                }
            State(
                items = items
            )
        }
        .onStart { emit(State()) }
        .asLiveData2()

    data class State(
        val items: List<FlightsListAdapter.Item>? = null,
    )

    companion object {
        internal val TAG = logTag("Flights", "List", "Fragment", "VM")
    }
}