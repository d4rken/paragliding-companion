package eu.darken.pgc.importer.ui

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
import eu.darken.pgc.importer.ui.items.ManualCardVH
import eu.darken.pgc.importer.ui.items.ReparseCardVH
import eu.darken.pgc.importer.ui.items.UsbCardVH
import javax.inject.Inject


class ImporterAdapter @Inject constructor() :
    ModularAdapter<ImporterAdapter.BaseVH<ImporterAdapter.Item, ViewBinding>>(),
    HasAsyncDiffer<ImporterAdapter.Item> {

    override val asyncDiffer: AsyncDiffer<*, Item> = setupDiffer()

    override fun getItemCount(): Int = data.size

    init {
        addMod(DataBinderMod(data))
        addMod(TypedVHCreatorMod({ data[it] is ReparseCardVH.Item }) { ReparseCardVH(it) })
        addMod(TypedVHCreatorMod({ data[it] is ManualCardVH.Item }) { ManualCardVH(it) })
        addMod(TypedVHCreatorMod({ data[it] is UsbCardVH.Item }) { UsbCardVH(it) })
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