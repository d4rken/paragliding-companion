package eu.darken.pgc.importer.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.RECEIVER_EXPORTED
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
import eu.darken.pgc.common.lists.differ.update
import eu.darken.pgc.common.lists.setupDefaults
import eu.darken.pgc.common.uix.Fragment3
import eu.darken.pgc.common.viewbinding.viewBinding
import eu.darken.pgc.databinding.ImporterFragmentBinding
import javax.inject.Inject

@AndroidEntryPoint
class ImporterFragment : Fragment3(R.layout.importer_fragment) {

    override val vm: ImporterFragmentVM by viewModels()
    override val ui: ImporterFragmentBinding by viewBinding()

    @Inject lateinit var usbManager: UsbManager
    private lateinit var pickerLauncher: ActivityResultLauncher<Intent>
    @Inject lateinit var adapter: ImporterAdapter

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            log(TAG, INFO) { "onReceive($context,$intent)" }
            if (ACTION_USB_PERMISSION != intent.action) return

            vm.importUsb()
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

        ui.list.setupDefaults(adapter, dividers = false)
        vm.state.observe2(ui) { state ->
            adapter.update(state.items)
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
