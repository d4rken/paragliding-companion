package eu.darken.pgc.backup.ui.settings

import androidx.annotation.Keep
import androidx.fragment.app.viewModels
import androidx.preference.Preference
import dagger.hilt.android.AndroidEntryPoint
import eu.darken.pgc.R
import eu.darken.pgc.backup.core.BackupSettings
import eu.darken.pgc.common.uix.PreferenceFragment2
import eu.darken.pgc.main.ui.settings.SettingsFragmentDirections
import javax.inject.Inject

@Keep
@AndroidEntryPoint
class BackupSettingsFragment : PreferenceFragment2() {

    private val vdc: BackupSettingsFragmentVM by viewModels()

    @Inject lateinit var backupSettings: BackupSettings

    override val settings: BackupSettings by lazy { backupSettings }
    override val preferenceFile: Int = R.xml.preferences_backup

    private val localBackupPref by lazy {
        findPreference<Preference>("backup.local.save")!!
    }

    override fun onPreferencesCreated() {
        localBackupPref.setOnPreferenceClickListener {
            SettingsFragmentDirections.actionSettingsContainerFragmentToBackupFragment().navigate()
            true
        }
        super.onPreferencesCreated()
    }

}