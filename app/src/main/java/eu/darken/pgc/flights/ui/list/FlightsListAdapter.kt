package eu.darken.pgc.flights.ui.list

import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.viewbinding.ViewBinding
import eu.darken.pgc.common.lists.BindableVH
import eu.darken.pgc.common.lists.differ.AsyncDiffer
import eu.darken.pgc.common.lists.differ.DifferItem
import eu.darken.pgc.common.lists.differ.HasAsyncDiffer
import eu.darken.pgc.common.lists.differ.setupDiffer
import eu.darken.pgc.common.lists.modular.ModularAdapter
import eu.darken.pgc.common.lists.modular.mods.DataBinderMod
import eu.darken.pgc.common.lists.modular.mods.TypedVHCreatorMod
import eu.darken.pgc.flights.core.Flight
import eu.darken.pgc.flights.ui.list.items.IGCFlightItemVH
import javax.inject.Inject


class FlightsListAdapter @Inject constructor() :
    ModularAdapter<FlightsListAdapter.BaseVH<FlightsListAdapter.Item, ViewBinding>>(),
    HasAsyncDiffer<FlightsListAdapter.Item> {

    override val asyncDiffer: AsyncDiffer<*, Item> = setupDiffer()

    override fun getItemCount(): Int = data.size

    init {
        addMod(DataBinderMod(data))
        addMod(TypedVHCreatorMod({ data[it] is IGCFlightItemVH.Item }) { IGCFlightItemVH(it) })
    }

    abstract class BaseVH<D : Item, B : ViewBinding>(
        @LayoutRes layoutId: Int,
        parent: ViewGroup
    ) : VH(layoutId, parent), BindableVH<D, B>

    interface Item : DifferItem {
        val flight: Flight

        val flightDay: String?

        override val stableId: Long
            get() = flight.id.hashCode().toLong()

        override val payloadProvider: ((DifferItem, DifferItem) -> DifferItem?)
            get() = { old, new ->
                if (new::class.isInstance(old)) new else null
            }
    }
}