package eu.darken.pgc.importer.ui.items

import android.hardware.usb.UsbDevice
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import androidx.core.view.isVisible
import com.google.android.material.radiobutton.MaterialRadioButton
import eu.darken.pgc.R
import eu.darken.pgc.common.ca.CaString
import eu.darken.pgc.common.lists.binding
import eu.darken.pgc.databinding.ImporterUsbItemBinding
import eu.darken.pgc.importer.core.UsbImporter
import eu.darken.pgc.importer.core.label
import eu.darken.pgc.importer.ui.ImporterAdapter
import me.jahnen.libaums.core.fs.UsbFile
import kotlin.math.roundToInt


class UsbCardVH(parent: ViewGroup) :
    ImporterAdapter.BaseVH<UsbCardVH.Item, ImporterUsbItemBinding>(
        R.layout.importer_usb_item,
        parent
    ) {

    override val viewBinding = lazy { ImporterUsbItemBinding.bind(itemView) }

    override val onBindData: ImporterUsbItemBinding.(
        item: Item,
        payloads: List<Any>
    ) -> Unit = binding { item ->
        when (val usb = item.state) {
            is Item.UsbImportstate.Start -> {
                primaryInfo.text = getString(R.string.importer_usb_import_start_desc1)
                secondaryInfo.text = getString(R.string.importer_usb_import_start_desc2)

                deviceGroup.apply {
                    isVisible = true
                    removeAllViews()
                    var toCheck = -1
                    item.devices.forEach { device ->
                        val radioButton = MaterialRadioButton(context).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            id = View.generateViewId()
                            text = device.label
                            if (item.selectedDevice == device) toCheck = id
                        }
                        addView(radioButton)
                    }
                    check(toCheck)

                    setOnCheckedChangeListener { group, checkedId ->
                        val button: RadioButton = group.findViewById(checkedId) ?: return@setOnCheckedChangeListener
                        button.isChecked = !button.isChecked // Wait for list update
                        val selectedUsbDevice = item.devices.first { it.label == button.text.toString() }
                        item.onDeviceSelected(selectedUsbDevice)
                    }
                }
            }

            is Item.UsbImportstate.Progress -> {
                primaryInfo.text = getString(R.string.importer_progress_label)
                secondaryInfo.text = usb.progressMsg.get(context)

                progressIndicator.apply {
                    isIndeterminate = usb.max == -1
                    progress = usb.current
                    max = usb.max
                }
                progressLabel.text =
                    "${((usb.current.toDouble() / usb.max.toDouble()) * 100).roundToInt()}%"
            }

            is Item.UsbImportstate.Result -> {
                primaryInfo.text = getString(
                    R.string.importer_usb_import_result_msg,
                    usb.success.size,
                    usb.skipped.size,
                    usb.failed.size
                )

                secondaryInfo.text = usb.failed
                    .takeIf { it.isNotEmpty() }
                    ?.joinToString("\n---\n") { "${it.first}: ${it.second}" }
            }
        }
        progressIndicator.isVisible = item.state is Item.UsbImportstate.Progress
        progressLabel.isVisible = item.state is Item.UsbImportstate.Progress
        deviceGroup.isVisible = item.state is Item.UsbImportstate.Start

        cancelAction.apply {
            isVisible = item.state is Item.UsbImportstate.Progress
            setOnClickListener { item.onCancel() }
        }
        importAction.apply {
            isVisible = item.state is Item.UsbImportstate.Start
            isEnabled = item.selectedDevice != null
            setOnClickListener { item.onImport(item.selectedDevice!!) }
        }
    }

    data class Item(
        var selectedDevice: UsbDevice? = null,
        val devices: List<UsbDevice>,
        val state: UsbImportstate,
        val onImport: (UsbDevice) -> Unit,
        val onDeviceSelected: (UsbDevice) -> Unit,
        val onCancel: () -> Unit,
    ) : ImporterAdapter.Item {
        override val stableId: Long = this.javaClass.hashCode().toLong()

        sealed interface UsbImportstate {
            data class Start(
                val noop: Boolean = true,
            ) : UsbImportstate

            data class Progress(
                val current: Int = 0,
                val max: Int = -1,
                val progressMsg: CaString,
            ) : UsbImportstate

            data class Result(
                val result: UsbImporter.Result,
            ) : UsbImportstate {

                val success: Collection<UsbFile>
                    get() = result.success
                val skipped: Collection<UsbFile>
                    get() = result.skipped
                val failed: Collection<Pair<UsbFile, Exception>>
                    get() = result.failed
            }
        }
    }

}