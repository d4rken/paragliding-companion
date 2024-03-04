package eu.darken.pgc.flights.ui.list

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.reddit.indicatorfastscroll.FastScrollItemIndicator
import dagger.hilt.android.AndroidEntryPoint
import eu.darken.pgc.R
import eu.darken.pgc.common.debug.logging.logTag
import eu.darken.pgc.common.lists.differ.update
import eu.darken.pgc.common.lists.setupDefaults
import eu.darken.pgc.common.uix.Fragment3
import eu.darken.pgc.common.viewbinding.viewBinding
import eu.darken.pgc.databinding.FlightsListFragmentBinding
import javax.inject.Inject

@AndroidEntryPoint
class FlightsListFragment : Fragment3(R.layout.flights_list_fragment) {

    override val vm: FlightsListFragmentVM by viewModels()
    override val ui: FlightsListFragmentBinding by viewBinding()

    @Inject lateinit var flightsAdapter: FlightsListAdapter
    private var currentSortMode = FlightSortMode.FLIGHT_AT

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ui.toolbar.apply {
            setupWithNavController(findNavController())
            setOnMenuItemClickListener {
                when (it.itemId) {
                    else -> super.onOptionsItemSelected(it)
                }
            }
        }

        ui.list.setupDefaults(flightsAdapter)
        ui.apply {
            val itemLabler: (Int) -> FastScrollItemIndicator? = { pos ->
                val getRowItem: (Int) -> FlightsListAdapter.Item? = {
                    flightsAdapter.data.getOrNull(pos)
                }
                val lbl = when (currentSortMode) {
                    FlightSortMode.FLIGHT_AT -> getRowItem(pos)?.flightDay
                }
                FastScrollItemIndicator.Text(lbl ?: "?")
            }
            val showIndicator: (FastScrollItemIndicator, Int, Int) -> Boolean = { indicator, index, size ->
                size in 2..32
            }
            fastscroller.setupWithRecyclerView(ui.list, itemLabler, showIndicator, true)
            fastscrollerThumb.setupWithFastScroller(ui.fastscroller)
        }
        vm.state.observe2(ui) { state ->
            list.isVisible = state.items != null
            fastscroller.isVisible = list.isVisible
            progress.isVisible = state.items == null
            flightsAdapter.update(state.items)
        }

        super.onViewCreated(view, savedInstanceState)
    }

    companion object {
        internal val TAG = logTag("Flights", "List", "Fragment")
    }
}
