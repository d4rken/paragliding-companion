package eu.darken.pgc.importer.ui.items

import android.net.Uri
import android.view.ViewGroup
import androidx.core.view.isVisible
import eu.darken.pgc.R
import eu.darken.pgc.common.ca.CaString
import eu.darken.pgc.common.lists.binding
import eu.darken.pgc.databinding.ImporterManualItemBinding
import eu.darken.pgc.importer.ui.ImporterAdapter
import kotlin.math.roundToInt


class ManualCardVH(parent: ViewGroup) :
    ImporterAdapter.BaseVH<ManualCardVH.Item, ImporterManualItemBinding>(
        R.layout.importer_manual_item,
        parent
    ) {

    override val viewBinding = lazy { ImporterManualItemBinding.bind(itemView) }

    override val onBindData: ImporterManualItemBinding.(
        item: Item,
        payloads: List<Any>
    ) -> Unit = binding { item ->
        when (val manual = item.state) {
            is Item.ManualImportState.Start -> {
                primaryInfo.text = getString(R.string.importer_manual_import_start_desc1)
                secondaryInfo.text = getString(R.string.importer_manual_import_start_desc2)
            }

            is Item.ManualImportState.Progress -> {
                primaryInfo.text = getString(R.string.importer_progress_label)
                secondaryInfo.text = manual.progressMsg.get(context)

                progressIndicator.apply {
                    isIndeterminate = manual.max == -1
                    progress = manual.current
                    max = manual.max
                }
                progressLabel.text =
                    "${((manual.current.toDouble() / manual.max.toDouble()) * 100).roundToInt()}%"
            }

            is Item.ManualImportState.Result -> {
                primaryInfo.text = getString(
                    R.string.importer_manual_import_result_msg,
                    manual.success.size,
                    manual.skipped.size,
                    manual.failed.size
                )

                secondaryInfo.text = manual.failed
                    .takeIf { it.isNotEmpty() }
                    ?.joinToString("\n---\n") { "${it.first}: ${it.second}" }

            }
        }

        progressIndicator.isVisible = item.state is Item.ManualImportState.Progress
        progressLabel.isVisible = item.state is Item.ManualImportState.Progress

        cancelAction.apply {
            isVisible = item.state is Item.ManualImportState.Progress
            setOnClickListener { item.onCancel() }
        }
        importAction.apply {
            isVisible = item.state is Item.ManualImportState.Start
            setOnClickListener { item.onImport() }
        }
    }

    data class Item(
        val state: ManualImportState,
        val onImport: () -> Unit,
        val onCancel: () -> Unit,
    ) : ImporterAdapter.Item {
        override val stableId: Long = this.javaClass.hashCode().toLong()

        sealed interface ManualImportState {
            data class Start(
                val idle: Boolean = true
            ) : ManualImportState

            data class Progress(
                val current: Int = 0,
                val max: Int = -1,
                val progressMsg: CaString,
            ) : ManualImportState

            data class Result(
                val success: List<Uri>,
                val skipped: List<Uri>,
                val failed: List<Pair<Uri, Exception>>,
            ) : ManualImportState
        }
    }

}