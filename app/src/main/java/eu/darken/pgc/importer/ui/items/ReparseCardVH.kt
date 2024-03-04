package eu.darken.pgc.importer.ui.items

import android.view.ViewGroup
import androidx.core.view.isVisible
import eu.darken.pgc.R
import eu.darken.pgc.common.ca.CaString
import eu.darken.pgc.common.lists.binding
import eu.darken.pgc.databinding.ImporterReparserItemBinding
import eu.darken.pgc.importer.ui.ImporterAdapter
import kotlin.math.roundToInt


class ReparseCardVH(parent: ViewGroup) :
    ImporterAdapter.BaseVH<ReparseCardVH.Item, ImporterReparserItemBinding>(
        R.layout.importer_reparser_item,
        parent
    ) {

    override val viewBinding = lazy { ImporterReparserItemBinding.bind(itemView) }

    override val onBindData: ImporterReparserItemBinding.(
        item: Item,
        payloads: List<Any>
    ) -> Unit = binding { item ->
        when (val reparser = item.state) {
            is Item.ReparserState.Start -> {
                reparseCard.isVisible = false
            }

            is Item.ReparserState.Progress -> {
                reparseCard.isVisible = true
                reparsePrimary.text = getString(R.string.importer_reparse_progress)
                reparseSecondary.text = reparser.progressMsg.get(context)

                reparseProgress.apply {
                    isIndeterminate = reparser.max == -1
                    progress = reparser.current
                    max = reparser.max
                    isVisible = true
                }
                reparseProgressLabel.text =
                    "${((reparser.current.toDouble() / reparser.max.toDouble()) * 100).roundToInt()}%"
            }

            is Item.ReparserState.Result -> {
                reparseCard.isVisible = true
                reparseProgress.isVisible = false
                reparsePrimary.text = getString(R.string.importer_reparse_changes, reparser.changes)
                reparseSecondary.text = null
            }
        }
    }

    data class Item(
        val state: ReparserState,
    ) : ImporterAdapter.Item {
        override val stableId: Long = this.javaClass.hashCode().toLong()

        sealed interface ReparserState {
            data class Start(
                val idle: Boolean = true
            ) : ReparserState


            data class Progress(
                val current: Int = 0,
                val max: Int = -1,
                val progressMsg: CaString,
            ) : ReparserState

            data class Result(
                val changes: Int,
            ) : ReparserState
        }
    }

}