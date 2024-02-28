package eu.darken.pgc.importer.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import dagger.hilt.android.AndroidEntryPoint
import eu.darken.pgc.R
import eu.darken.pgc.common.debug.logging.log
import eu.darken.pgc.common.debug.logging.logTag
import eu.darken.pgc.common.uix.Fragment3
import eu.darken.pgc.common.viewbinding.viewBinding
import eu.darken.pgc.databinding.ImporterFragmentBinding
import eu.darken.pgc.main.ui.dashboard.DashboardAdapter
import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint
class ImporterFragment : Fragment3(R.layout.importer_fragment) {

    override val vm: ImporterFragmentVM by viewModels()
    override val ui: ImporterFragmentBinding by viewBinding()

    @Inject lateinit var dashAdapter: DashboardAdapter

    private lateinit var pickerLauncher: ActivityResultLauncher<Intent>

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
                vm.startIngestion(uris)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ui.toolbar.apply {
            setupWithNavController(findNavController())
            setOnMenuItemClickListener {
                when (it.itemId) {


                    else -> super.onOptionsItemSelected(it)
                }
            }
        }

        ui.manualImportAction.setOnClickListener { vm.startSelection() }

        vm.state.observe2(ui) { state ->
            when (state) {
                is ImporterFragmentVM.State.Start -> {
                    manualImportPrimary.text = getString(R.string.importer_manual_import_start_desc1)
                    manualImportSecondary.text = getString(R.string.importer_manual_import_start_desc2)
                    manualImportProgress.isVisible = false
                    manualImportAction.isVisible = true
                }

                is ImporterFragmentVM.State.Progress -> {
                    manualImportPrimary.text = getString(R.string.importer_progress_label)
                    manualImportSecondary.text = state.progressMsg.get(requireContext())

                    manualImportProgress.apply {
                        isIndeterminate = state.max == -1
                        progress = state.current
                        max = state.max
                        isVisible = true
                    }
                    manualImportProgressLabel.text =
                        "${((state.current.toDouble() / state.max.toDouble()) * 100).roundToInt()}%"
                    manualImportAction.isVisible = false
                }

                is ImporterFragmentVM.State.Result -> {
                    manualImportPrimary.text = getString(
                        R.string.importer_manual_import_result_msg,
                        state.success.size,
                        state.skipped.size,
                        state.failed.size
                    )

                    manualImportSecondary.text = state.failed
                        .takeIf { it.isNotEmpty() }
                        ?.joinToString("\n---\n") { "${it.first}: ${it.second}" }
                    manualImportProgress.isVisible = false
                    manualImportAction.isVisible = true
                }
            }
        }

        vm.events.observe2(ui) { event ->
            when (event) {
                is ImporterEvents.ShowPicker -> {
                    pickerLauncher.launch(event.intent)
                }
            }
        }

        super.onViewCreated(view, savedInstanceState)
    }

    companion object {
        internal val TAG = logTag("Ingester", "Fragment")
    }
}
