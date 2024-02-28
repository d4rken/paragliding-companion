package eu.darken.pgc.main.ui.dashboard

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
import eu.darken.pgc.flights.ui.FlightsGlobalDashCardVH
import eu.darken.pgc.importer.ui.ImporterDashCardVH
import eu.darken.pgc.xctrack.ui.XCTrackSetupCardVH
import javax.inject.Inject


class DashboardAdapter @Inject constructor() :
    ModularAdapter<DashboardAdapter.BaseVH<DashboardAdapter.Item, ViewBinding>>(),
    HasAsyncDiffer<DashboardAdapter.Item> {

    override val asyncDiffer: AsyncDiffer<*, Item> = setupDiffer()

    override fun getItemCount(): Int = data.size

    init {
        addMod(DataBinderMod(data))
        addMod(TypedVHCreatorMod({ data[it] is XCTrackSetupCardVH.Item }) { XCTrackSetupCardVH(it) })
        addMod(TypedVHCreatorMod({ data[it] is ImporterDashCardVH.Item }) { ImporterDashCardVH(it) })
        addMod(TypedVHCreatorMod({ data[it] is FlightsGlobalDashCardVH.Item }) { FlightsGlobalDashCardVH(it) })
    }

    abstract class BaseVH<D : Item, B : ViewBinding>(
        @LayoutRes layoutId: Int,
        parent: ViewGroup
    ) : VH(layoutId, parent), BindableVH<D, B>

    interface Item : DifferItem {
        override val payloadProvider: ((DifferItem, DifferItem) -> DifferItem?)
            get() = { old, new ->
                if (new::class.isInstance(old)) new else null
            }
    }
}