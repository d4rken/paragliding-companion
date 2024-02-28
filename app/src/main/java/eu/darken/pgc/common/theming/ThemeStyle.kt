package eu.darken.pgc.common.theming

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import eu.darken.pgc.R
import eu.darken.pgc.common.preferences.EnumPreference

@JsonClass(generateAdapter = false)
enum class ThemeStyle(override val labelRes: Int) : EnumPreference<ThemeStyle> {
    @Json(name = "DEFAULT") DEFAULT(R.string.ui_theme_style_default_label),
    @Json(name = "MATERIAL_YOU") MATERIAL_YOU(R.string.ui_theme_style_materialyou_label),
    ;
}