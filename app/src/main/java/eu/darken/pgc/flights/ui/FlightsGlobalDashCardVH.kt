package eu.darken.pgc.flights.ui

import android.view.ViewGroup
import eu.darken.pgc.R
import eu.darken.pgc.common.lists.binding
import eu.darken.pgc.databinding.FlightsDashGlobalItemBinding
import eu.darken.pgc.flights.core.FlightStats
import eu.darken.pgc.main.ui.dashboard.DashboardAdapter


class FlightsGlobalDashCardVH(parent: ViewGroup) :
    DashboardAdapter.BaseVH<FlightsGlobalDashCardVH.Item, FlightsDashGlobalItemBinding>(
        R.layout.flights_dash_global_item,
        parent
    ) {

    override val viewBinding = lazy { FlightsDashGlobalItemBinding.bind(itemView) }

    override val onBindData: FlightsDashGlobalItemBinding.(
        item: Item,
        payloads: List<Any>
    ) -> Unit = binding { item ->
        val stats = item.stats
        globalCount.text = getString(R.string.flights_dash_global_count_msg, stats.globalCount)
        globalHours.text = getString(R.string.flights_dash_global_hours_msg, stats.flightHours.toMinutes() / 60f)
        globalLocations.text = getString(R.string.flights_dash_global_locations_msg, stats.locations)

        importAction.apply {
            setOnClickListener { item.onView() }
        }
    }

    data class Item(
        val stats: FlightStats.Stats,
        val onView: () -> Unit
    ) : DashboardAdapter.Item {
        override val stableId: Long = this.javaClass.hashCode().toLong()
    }

}