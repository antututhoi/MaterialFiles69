/*
 * Copyright (c) 2018 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.settings

import android.os.Build
import android.os.Bundle
import me.zhanghai.android.files.R
import me.zhanghai.android.files.theme.custom.CustomThemeHelper
import me.zhanghai.android.files.theme.custom.ThemeColor
import me.zhanghai.android.files.theme.night.NightMode
import me.zhanghai.android.files.theme.night.NightModeHelper
import me.zhanghai.android.files.ui.PreferenceFragmentCompat

class SettingsPreferenceFragment : PreferenceFragmentCompat() {
    private lateinit var localePreference: LocalePreference

    // 69 mod
    private val mod69Export = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) mod69Toast(runCatching { me.zhanghai.android.files.mod69.Mod69SettingsBackup.export(requireContext(), uri); "Settings saved" })
    }
    private val mod69Import = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) mod69Toast(runCatching { me.zhanghai.android.files.mod69.Mod69SettingsBackup.import(requireContext(), uri); "Settings loaded – restart the app" })
    }

    private fun mod69Toast(r: Result<String>) {
        android.widget.Toast.makeText(
            requireContext(), r.getOrElse { it.message ?: it.toString() }, android.widget.Toast.LENGTH_LONG
        ).show()
    }

    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.settings)
        // 69 mod
        preferenceScreen.findPreference<androidx.preference.Preference>("mod69_export_settings")?.setOnPreferenceClickListener {
            mod69Export.launch("69-material-files-settings.json"); true
        }
        preferenceScreen.findPreference<androidx.preference.Preference>("mod69_import_settings")?.setOnPreferenceClickListener {
            mod69Import.launch(arrayOf("application/json", "text/plain", "application/octet-stream")); true
        }
        preferenceScreen.findPreference<androidx.preference.Preference>("mod69_empty_bin")?.setOnPreferenceClickListener {
            me.zhanghai.android.files.mod69.Mod69Bin.emptyBin(requireContext()); true
        }
        preferenceScreen.findPreference<androidx.preference.Preference>("mod69_open_bin")?.setOnPreferenceClickListener {
            val dir = me.zhanghai.android.files.mod69.Mod69Bin.root.apply { mkdirs() }
            startActivity(
                me.zhanghai.android.files.filelist.FileListActivity.createViewIntent(java8.nio.file.Paths.get(dir.absolutePath))
            )
            true
        }

        localePreference = preferenceScreen.findPreference(getString(R.string.pref_key_locale))!!
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            localePreference.setApplicationLocalesPre33 = { locales ->
                val activity = requireActivity() as SettingsActivity
                activity.setApplicationLocalesPre33(locales)
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val viewLifecycleOwner = viewLifecycleOwner
        // The following may end up passing the same lambda instance to the observer because it has
        // no capture, and result in an IllegalArgumentException "Cannot add the same observer with
        // different lifecycles" if activity is finished and instantly started again. To work around
        // this, always use an instance method reference.
        // https://stackoverflow.com/a/27524543
        //Settings.THEME_COLOR.observe(viewLifecycleOwner) { CustomThemeHelper.sync() }
        //Settings.MATERIAL_DESIGN_3.observe(viewLifecycleOwner) { CustomThemeHelper.sync() }
        //Settings.NIGHT_MODE.observe(viewLifecycleOwner) { NightModeHelper.sync() }
        //Settings.BLACK_NIGHT_MODE.observe(viewLifecycleOwner) { CustomThemeHelper.sync() }
        Settings.THEME_COLOR.observe(viewLifecycleOwner, this::onThemeColorChanged)
        Settings.MATERIAL_DESIGN_3.observe(viewLifecycleOwner, this::onMaterialDesign3Changed)
        Settings.NIGHT_MODE.observe(viewLifecycleOwner, this::onNightModeChanged)
        Settings.BLACK_NIGHT_MODE.observe(viewLifecycleOwner, this::onBlackNightModeChanged)
    }

    private fun onThemeColorChanged(themeColor: ThemeColor) {
        CustomThemeHelper.sync()
    }

    private fun onMaterialDesign3Changed(isMaterialDesign3: Boolean) {
        CustomThemeHelper.sync()
    }

    private fun onNightModeChanged(nightMode: NightMode) {
        NightModeHelper.sync()
    }

    private fun onBlackNightModeChanged(blackNightMode: Boolean) {
        CustomThemeHelper.sync()
    }

    override fun onResume() {
        super.onResume()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Refresh locale preference summary because we aren't notified for an external change
            // between system default and the locale that's the current system default.
            localePreference.notifyChanged()
        }
    }
}
