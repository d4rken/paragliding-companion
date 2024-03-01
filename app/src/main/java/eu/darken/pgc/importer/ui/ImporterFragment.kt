package eu.darken.pgc.importer.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.RECEIVER_EXPORTED
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import dagger.hilt.android.AndroidEntryPoint
import eu.darken.pgc.BuildConfig
import eu.darken.pgc.R
import eu.darken.pgc.common.debug.logging.Logging.Priority.ERROR
import eu.darken.pgc.common.debug.logging.Logging.Priority.INFO
import eu.darken.pgc.common.debug.logging.asLog
import eu.darken.pgc.common.debug.logging.log
import eu.darken.pgc.common.debug.logging.logTag
import eu.darken.pgc.common.uix.Fragment3
import eu.darken.pgc.common.viewbinding.viewBinding
import eu.darken.pgc.databinding.ImporterFragmentBinding
import eu.darken.pgc.importer.core.label
import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint
class ImporterFragment : Fragment3(R.layout.importer_fragment) {

    override val vm: ImporterFragmentVM by viewModels()
    override val ui: ImporterFragmentBinding by viewBinding()

    @Inject lateinit var usbManager: UsbManager
    private lateinit var pickerLauncher: ActivityResultLauncher<Intent>

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            log(TAG, INFO) { "onReceive($context,$intent)" }
            if (ACTION_USB_PERMISSION != intent.action) return

            vm.importUsb(intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) ?: selectedUsbDevice)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            log(TAG) { "pickerLauncher: code=${result.resultCode} -> $result" }
            val uris = result?.data?.let { intent ->
                intent.clipData?.let {
                    mutableSetOf<Uri>().apply {
                        for (i in 0 until it.itemCount) {
                            add(it.getItemAt(i).uri)
                        }
                    }
                } ?: intent.data?.let { setOf(it) }
            }
            if (result.resultCode == Activity.RESULT_OK && uris != null) {
                vm.importManual(uris)
            }
        }
    }

    var selectedUsbDevice: UsbDevice? = null

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ui.toolbar.apply {
            setupWithNavController(findNavController())
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_reparse -> {
                        vm.reparse()
                        true
                    }

                    else -> super.onOptionsItemSelected(it)
                }
            }
        }

        vm.state.observe2(ui) { state ->
            when (val manual = state.manualImport) {
                is ImporterFragmentVM.ManualImportState.Start -> {
                    manualImportPrimary.text = getString(R.string.importer_manual_import_start_desc1)
                    manualImportSecondary.text = getString(R.string.importer_manual_import_start_desc2)
                    manualImportProgress.isVisible = false
                    manualImportAction.isVisible = true
                    manualImportAction.setOnClickListener { vm.startSelection() }
                }

                is ImporterFragmentVM.ManualImportState.Progress -> {
                    manualImportPrimary.text = getString(R.string.importer_progress_label)
                    manualImportSecondary.text = manual.progressMsg.get(requireContext())

                    manualImportProgress.apply {
                        isIndeterminate = manual.max == -1
                        progress = manual.current
                        max = manual.max
                        isVisible = true
                    }
                    manualImportProgressLabel.text =
                        "${((manual.current.toDouble() / manual.max.toDouble()) * 100).roundToInt()}%"
                    manualImportAction.isVisible = false
                }

                is ImporterFragmentVM.ManualImportState.Result -> {
                    manualImportPrimary.text = getString(
                        R.string.importer_manual_import_result_msg,
                        manual.success.size,
                        manual.skipped.size,
                        manual.failed.size
                    )

                    manualImportSecondary.text = manual.failed
                        .takeIf { it.isNotEmpty() }
                        ?.joinToString("\n---\n") { "${it.first}: ${it.second}" }
                    manualImportProgress.isVisible = false
                    manualImportAction.isVisible = true
                }
            }
            when (val reparser = state.reparserState) {
                is ImporterFragmentVM.ReparserState.Start -> {
                    reparseCard.isVisible = false
                }

                is ImporterFragmentVM.ReparserState.Progress -> {
                    reparseCard.isVisible = true
                    reparsePrimary.text = getString(R.string.importer_reparse_progress)
                    reparseSecondary.text = reparser.progressMsg.get(requireContext())

                    reparseProgress.apply {
                        isIndeterminate = reparser.max == -1
                        progress = reparser.current
                        max = reparser.max
                        isVisible = true
                    }
                    reparseProgressLabel.text =
                        "${((reparser.current.toDouble() / reparser.max.toDouble()) * 100).roundToInt()}%"
                }

                is ImporterFragmentVM.ReparserState.Result -> {
                    reparseCard.isVisible = true
                    reparseProgress.isVisible = false
                    reparsePrimary.text = getString(R.string.importer_reparse_changes, reparser.changes)
                    reparseSecondary.text = null
                }
            }
            when (val usb = state.usbImportState) {
                is ImporterFragmentVM.UsbImportstate.Start -> {
                    usbImportPrimary.text = getString(R.string.importer_usb_import_start_desc1)
                    usbImportSecondary.text = getString(R.string.importer_usb_import_start_desc2)
                    usbImportProgress.isVisible = false
                    usbImportAction.apply {
                        isEnabled = false
                        isVisible = true
                    }

                    usbImportDeviceGroup.apply {
                        isVisible = true
                        removeAllViews()
                        usb.devices.forEach { device ->
                            val radioButton = RadioButton(requireContext()).apply {
                                text = device.label
                                id = View.generateViewId()
                            }
                            addView(radioButton)
                        }

                        setOnCheckedChangeListener { group, checkedId ->
                            val button = findViewById<RadioButton>(checkedId)
                            selectedUsbDevice = usb.devices.first { it.label == button.text.toString() }
                            usbImportAction.isEnabled = selectedUsbDevice != null
                        }
                    }
                    usbCancelAction.isVisible = false
                    usbImportAction.setOnClickListener { vm.importUsb(selectedUsbDevice!!) }
                }

                is ImporterFragmentVM.UsbImportstate.Progress -> {
                    usbImportPrimary.text = getString(R.string.importer_progress_label)
                    usbImportSecondary.text = usb.progressMsg.get(requireContext())

                    usbImportProgress.apply {
                        isIndeterminate = usb.max == -1
                        progress = usb.current
                        max = usb.max
                        isVisible = true
                    }
                    usbImportProgressLabel.text =
                        "${((usb.current.toDouble() / usb.max.toDouble()) * 100).roundToInt()}%"
                    usbImportAction.isVisible = false
                    usbImportDeviceGroup.isVisible = false
                    usbCancelAction.apply {
                        isVisible = true
                        setOnClickListener { vm.cancelImportUsb() }
                    }
                }

                is ImporterFragmentVM.UsbImportstate.Result -> {
                    usbImportPrimary.text = getString(
                        R.string.importer_usb_import_result_msg,
                        usb.success.size,
                        usb.skipped.size,
                        usb.failed.size
                    )

                    usbImportSecondary.text = usb.failed
                        .takeIf { it.isNotEmpty() }
                        ?.joinToString("\n---\n") { "${it.first}: ${it.second}" }
                    usbImportProgress.isVisible = false
                    usbImportAction.apply {
                        isVisible = true
                        setOnClickListener { vm.importUsb(null) }
                    }
                    usbCancelAction.isVisible = false
                    usbImportDeviceGroup.isVisible = false
                }
            }
        }

        vm.events.observe2(ui) { event ->
            when (event) {
                is ImporterEvents.ShowPicker -> {
                    pickerLauncher.launch(event.intent)
                }

                is ImporterEvents.RequestUsbPermission -> {
                    val pi = PendingIntent.getBroadcast(
                        requireContext(),
                        0,
                        Intent(ACTION_USB_PERMISSION),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                    usbManager.requestPermission(event.device, pi)
                }
            }
        }

        try {
            ContextCompat.registerReceiver(
                requireContext(),
                usbReceiver,
                IntentFilter(ACTION_USB_PERMISSION),
                RECEIVER_EXPORTED,
            )
        } catch (e: Exception) {
            log(TAG, ERROR) { "Failed to register usbReceiver: ${e.asLog()}" }
        }

        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        try {
            requireContext().unregisterReceiver(usbReceiver)
        } catch (e: Exception) {
            log(TAG, ERROR) { "Failed to unregister usbReceiver: ${e.asLog()}" }
        }
        super.onDestroyView()
    }

    companion object {
        const val ACTION_USB_PERMISSION = "${BuildConfig.APPLICATION_ID}.USB_PERMISSION"
        internal val TAG = logTag("Importer", "Fragment")
    }
}
