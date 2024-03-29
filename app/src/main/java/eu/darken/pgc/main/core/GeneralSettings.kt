package eu.darken.pgc.main.core

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
import eu.darken.pgc.common.theming.ThemeMode
import eu.darken.pgc.common.theming.ThemeStyle
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeneralSettings @Inject constructor(
    @ApplicationContext private val context: Context,
    moshi: Moshi,
) : PreferenceScreenData {

    private val Context.dataStore by preferencesDataStore(name = "settings_core")

    override val dataStore: DataStore<Preferences>
        get() = context.dataStore

    val isAutoReportingEnabled = dataStore.createValue("debug.bugreport.automatic.enabled", true)

    val themeMode = dataStore.createValue("core.ui.theme.mode", ThemeMode.SYSTEM, moshi)
    val themeStyle = dataStore.createValue("core.ui.theme.style", ThemeStyle.DEFAULT, moshi)

    override val mapper = PreferenceStoreMapper(
        isAutoReportingEnabled,
        themeMode,
        themeStyle,
    )

    companion object {
        internal val TAG = logTag("Core", "Settings")
    }
}