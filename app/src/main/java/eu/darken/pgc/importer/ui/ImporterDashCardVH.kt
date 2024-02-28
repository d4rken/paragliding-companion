package eu.darken.pgc.importer.ui

import android.view.ViewGroup
import eu.darken.pgc.R
import eu.darken.pgc.common.lists.binding
import eu.darken.pgc.databinding.ImporterDashboardItemBinding
import eu.darken.pgc.main.ui.dashboard.DashboardAdapter


class ImporterDashCardVH(parent: ViewGroup) :
    DashboardAdapter.BaseVH<ImporterDashCardVH.Item, ImporterDashboardItemBinding>(
        R.layout.importer_dashboard_item,
        parent
    ) {

    override val viewBinding = lazy { ImporterDashboardItemBinding.bind(itemView) }

    override val onBindData: ImporterDashboardItemBinding.(
        item: Item,
        payloads: List<Any>
    ) -> Unit = binding { item ->
        importAction.apply {
            setOnClickListener { item.onImport() }
        }
    }

    data class Item(
        val onImport: () -> Unit
    ) : DashboardAdapter.Item {
        override val stableId: Long = this.javaClass.hashCode().toLong()
    }

}