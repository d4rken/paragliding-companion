package eu.darken.pgc.backup.ui.settings

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.pgc.common.coroutine.DispatcherProvider
import eu.darken.pgc.common.debug.logging.logTag
import eu.darken.pgc.common.uix.ViewModel3
import javax.inject.Inject

@HiltViewModel
class BackupSettingsFragmentVM @Inject constructor(
    private val handle: SavedStateHandle,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel3(dispatcherProvider) {


    companion object {
        private val TAG = logTag("Settings", "Backup", "VM")
    }
}