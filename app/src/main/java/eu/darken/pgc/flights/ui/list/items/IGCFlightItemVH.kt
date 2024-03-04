package eu.darken.pgc.flights.ui.list.items

import android.view.ViewGroup
import androidx.core.view.isVisible
import eu.darken.pgc.R
import eu.darken.pgc.common.lists.binding
import eu.darken.pgc.databinding.FlightsListIgcItemBinding
import eu.darken.pgc.flights.core.igc.IGCFlight
import eu.darken.pgc.flights.ui.list.FlightsListAdapter
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale


class IGCFlightItemVH(parent: ViewGroup) :
    FlightsListAdapter.BaseVH<IGCFlightItemVH.Item, FlightsListIgcItemBinding>(
        R.layout.flights_list_igc_item,
        parent
    ) {

    override val viewBinding = lazy { FlightsListIgcItemBinding.bind(itemView) }

    override val onBindData: FlightsListIgcItemBinding.(
        item: Item,
        payloads: List<Any>
    ) -> Unit = binding { item ->
        val flight = item.flight
        flightTime.text = item.flight.flightAt?.format(dayTimeFormatter)
        flightLocation.text = flight.location ?: getString(R.string.flights_list_item_unknown_location)
        flightDuration.text = flight.duration?.let { dur ->
            getString(R.string.flights_list_item_duration, dur.toMinutes(), dur.toSecondsPart())
        }
        flightDistance.text = flight.distance?.toString()
        gliderType.text = flight.gliderType
        trackerType.text = flight.trackerType

        qualityIndicator.isVisible = flight.duration?.isZero == true

        root.apply {
            setOnClickListener { item.onClick() }
        }
    }

    data class Item(
        override val flight: IGCFlight,
        val onClick: () -> Unit,
    ) : FlightsListAdapter.Item {
        override val flightDay: String?
            get() = flight.flightAt?.format(dayFormatter)
    }

    companion object {
        private val dayTimeFormatter: DateTimeFormatter by lazy {
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.getDefault())
        }
        private val dayFormatter: DateTimeFormatter by lazy {
            DateTimeFormatter.ofPattern("yyyy.MM").withLocale(Locale.getDefault())
        }
    }

}