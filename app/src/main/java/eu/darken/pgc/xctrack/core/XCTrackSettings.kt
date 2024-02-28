package eu.darken.pgc.xctrack.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.pgc.common.datastore.PreferenceScreenData
import eu.darken.pgc.common.datastore.PreferenceStoreMapper
import eu.darken.pgc.common.datastore.createValue
import eu.darken.pgc.common.debug.logging.logTag
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class XCTrackSettings @Inject constructor(
    @ApplicationContext private val context: Context,
    moshi: Moshi,
) : PreferenceScreenData {

    private val Context.dataStore by preferencesDataStore(name = "settings_xctrack")

    override val dataStore: DataStore<Preferences>
        get() = context.dataStore

    val isSetupDismissed = dataStore.createValue("xctrack.setup.dismissed", false)

    override val mapper = PreferenceStoreMapper(

    )

    companion object {
        internal val TAG = logTag("XCTrack", "Settings")
    }
}