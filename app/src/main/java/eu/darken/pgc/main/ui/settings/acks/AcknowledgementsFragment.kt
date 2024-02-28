package eu.darken.pgc.main.ui.settings.acks

import androidx.annotation.Keep
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import eu.darken.pgc.R
import eu.darken.pgc.common.uix.PreferenceFragment2
import eu.darken.pgc.main.core.GeneralSettings
import javax.inject.Inject

@Keep
@AndroidEntryPoint
class AcknowledgementsFragment : PreferenceFragment2() {

    private val vm: AcknowledgementsFragmentVM by viewModels()

    override val preferenceFile: Int = R.xml.preferences_acknowledgements
    @Inject lateinit var debugSettings: GeneralSettings

    override val settings: GeneralSettings by lazy { debugSettings }

}