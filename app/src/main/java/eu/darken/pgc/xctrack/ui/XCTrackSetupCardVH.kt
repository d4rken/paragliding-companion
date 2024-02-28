package eu.darken.pgc.xctrack.ui

import android.view.ViewGroup
import androidx.core.view.isVisible
import eu.darken.pgc.R
import eu.darken.pgc.common.lists.binding
import eu.darken.pgc.databinding.DashboardXctrackSetupItemBinding
import eu.darken.pgc.main.ui.dashboard.DashboardAdapter
import eu.darken.pgc.xctrack.core.XCTrackManager


class XCTrackSetupCardVH(parent: ViewGroup) :
    DashboardAdapter.BaseVH<XCTrackSetupCardVH.Item, DashboardXctrackSetupItemBinding>(
        R.layout.dashboard_xctrack_setup_item,
        parent
    ) {

    override val viewBinding = lazy { DashboardXctrackSetupItemBinding.bind(itemView) }

    override val onBindData: DashboardXctrackSetupItemBinding.(
        item: Item,
        payloads: List<Any>
    ) -> Unit = binding { item ->
        dismissAction.apply {
            isVisible = false
            setOnClickListener { item.onDismiss() }
        }

        continueSetupAction.apply {
            setOnClickListener { item.onContinue() }
        }
    }

    data class Item(
        val state: XCTrackManager.State,
        val onDismiss: () -> Unit,
        val onContinue: () -> Unit
    ) : DashboardAdapter.Item {
        override val stableId: Long = this.javaClass.hashCode().toLong()
    }

}