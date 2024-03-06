package eu.darken.pgc.backup.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.pgc.common.datastore.PreferenceScreenData
import eu.darken.pgc.common.datastore.PreferenceStoreMapper
import eu.darken.pgc.common.debug.logging.logTag
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupSettings @Inject constructor(
    @ApplicationContext private val context: Context,
    moshi: Moshi,
) : PreferenceScreenData {

    private val Context.dataStore by preferencesDataStore(name = "settings_backup")

    override val dataStore: DataStore<Preferences>
        get() = context.dataStore

    override val mapper = PreferenceStoreMapper(

    )

    companion object {
        internal val TAG = logTag("Backup", "Settings")
    }
}