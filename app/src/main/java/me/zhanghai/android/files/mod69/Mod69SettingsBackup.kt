package me.zhanghai.android.files.mod69

import android.content.Context
import android.net.Uri
import androidx.preference.PreferenceManager
import org.json.JSONArray
import org.json.JSONObject

/** 69 mod: export / import all settings (bookmarks, storages, view options…) as a JSON file. */
object Mod69SettingsBackup {
    private fun files(context: Context) = listOf(
        PreferenceManager.getDefaultSharedPreferences(context),
        context.getSharedPreferences("mod69_recent", Context.MODE_PRIVATE),
        context.getSharedPreferences("mod69_pick", Context.MODE_PRIVATE),
    )

    private val names = listOf("default", "mod69_recent", "mod69_pick")

    fun export(context: Context, uri: Uri) {
        val root = JSONObject().put("app", "69-Material Files").put("version", 1)
        files(context).forEachIndexed { i, prefs ->
            val o = JSONObject()
            prefs.all.forEach { (k, v) ->
                val e = JSONObject()
                when (v) {
                    is Boolean -> e.put("t", "b").put("v", v)
                    is Int -> e.put("t", "i").put("v", v)
                    is Long -> e.put("t", "l").put("v", v)
                    is Float -> e.put("t", "f").put("v", v.toDouble())
                    is String -> e.put("t", "s").put("v", v)
                    is Set<*> -> e.put("t", "ss").put("v", JSONArray(v.map { it.toString() }))
                    else -> return@forEach
                }
                o.put(k, e)
            }
            root.put(names[i], o)
        }
        context.contentResolver.openOutputStream(uri)?.use { it.write(root.toString(1).toByteArray()) }
            ?: error("cannot write")
    }

    fun import(context: Context, uri: Uri) {
        val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: error("cannot read")
        val root = JSONObject(text)
        files(context).forEachIndexed { i, prefs ->
            val o = root.optJSONObject(names[i]) ?: return@forEachIndexed
            val ed = prefs.edit()
            o.keys().forEach { k ->
                val e = o.getJSONObject(k)
                when (e.optString("t")) {
                    "b" -> ed.putBoolean(k, e.getBoolean("v"))
                    "i" -> ed.putInt(k, e.getInt("v"))
                    "l" -> ed.putLong(k, e.getLong("v"))
                    "f" -> ed.putFloat(k, e.getDouble("v").toFloat())
                    "s" -> ed.putString(k, e.getString("v"))
                    "ss" -> {
                        val a = e.getJSONArray("v")
                        ed.putStringSet(k, (0 until a.length()).map { a.getString(it) }.toSet())
                    }
                }
            }
            ed.commit()
        }
    }
}
