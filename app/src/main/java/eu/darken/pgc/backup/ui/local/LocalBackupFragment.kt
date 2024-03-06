package eu.darken.pgc.backup.ui.local

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
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
import eu.darken.pgc.databinding.BackupLocalSaveFragmentBinding

@AndroidEntryPoint
class LocalBackupFragment : Fragment3(R.layout.backup_local_save_fragment) {

    override val vm: LocalBackupFragmentVM by viewModels()
    override val ui: BackupLocalSaveFragmentBinding by viewBinding()

    private lateinit var pickerLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            log(TAG) { "pickerLauncher: code=${result.resultCode} -> $result" }
            val uri = result?.data?.data
            if (result.resultCode == Activity.RESULT_OK && uri != null) {
                vm.startBackup(uri)
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

        ui.backupAction.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            pickerLauncher.launch(intent)
        }

        vm.state.observe2(ui) { state ->
            backupAction.isVisible = state is LocalBackupFragmentVM.State.Start
            progress.isVisible = state is LocalBackupFragmentVM.State.Progress
            toolbar.setNavigationIcon(
                if (state is LocalBackupFragmentVM.State.Progress) {
                    R.drawable.ic_cancel_24
                } else {
                    R.drawable.ic_arrow_back_24
                }
            )
            when (state) {
                is LocalBackupFragmentVM.State.Start -> {
                    primaryInfo.text = getString(R.string.backup_local_select_path_desc)
                }

                is LocalBackupFragmentVM.State.Progress -> {
                    primaryInfo.text = state.progressMsg.get(requireContext())
                    if (state.max != -1) {
                        progress.progress = state.current
                        progress.max = state.max
                        progress.isIndeterminate = false
                    } else {
                        progress.isIndeterminate = true
                    }
                }

                is LocalBackupFragmentVM.State.Result -> {
                    primaryInfo.text = state.resultMsg.get(requireContext())
                }
            }
        }

        super.onViewCreated(view, savedInstanceState)
    }

    companion object {
        internal val TAG = logTag("Backup", "Local", "Fragment")
    }
}
