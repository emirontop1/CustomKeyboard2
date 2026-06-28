package com.customkeyboard.settings

import android.content.Context
import org.json.JSONObject

class AutoConvertManager(private val context: Context) {

    private val prefs = context.getSharedPreferences(PrefsKeys.PREFS_NAME, Context.MODE_PRIVATE)

    // Varsayılan kısayollar
    private val defaultShortcuts = mapOf(
        "tşk" to "Teşekkürler!",
        "mslm" to "Merhaba, nasılsın?",
        "tmm" to "Tamam, anlaşıldı.",
        "gly" to "Geliyorum!",
        "gdy" to "Gidiyorum!",
        "nrdşn" to "Neredesin?",
        "ok" to "Okey!",
        "ty" to "Thank you!",
        "np" to "No problem!",
        "lol" to "Hahaha!",
        "omw" to "On my way!",
        "brb" to "Be right back!"
    )

    fun getAll(): Map<String, String> {
        val json = prefs.getString(PrefsKeys.AUTO_CONVERT, null)
        return if (json.isNullOrEmpty()) {
            defaultShortcuts
        } else {
            try {
                val obj = JSONObject(json)
                obj.keys().asSequence().associateWith { obj.getString(it) }
            } catch (e: Exception) {
                defaultShortcuts
            }
        }
    }

    fun get(shortcut: String): String? {
        return getAll()[shortcut.lowercase()]
    }

    fun add(shortcut: String, expansion: String) {
        val map = getAll().toMutableMap()
        map[shortcut.lowercase()] = expansion
        save(map)
    }

    fun remove(shortcut: String) {
        val map = getAll().toMutableMap()
        map.remove(shortcut.lowercase())
        save(map)
    }

    fun resetToDefaults() {
        save(defaultShortcuts)
    }

    private fun save(map: Map<String, String>) {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v) }
        prefs.edit().putString(PrefsKeys.AUTO_CONVERT, obj.toString()).apply()
    }
}
