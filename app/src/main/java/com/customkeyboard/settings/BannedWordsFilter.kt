package com.customkeyboard.settings

import android.content.Context
import org.json.JSONArray

class BannedWordsFilter(private val context: Context) {

    private val prefs = context.getSharedPreferences(PrefsKeys.PREFS_NAME, Context.MODE_PRIVATE)

    // Varsayılan yasaklı kelimeler (örnek — kendi kelimelerinizi ekleyebilirsiniz)
    private val defaultBanned = setOf(
        // Spam/noise
        "spam", "xxx", "qqq", "www",
        // Kültüre göre filtrelenebilir
        // Başa "***" eklenecek
    )

    fun filter(text: String): String {
        val customBanned = getCustomBanned()
        val allBanned = defaultBanned + customBanned

        var result = text
        allBanned.forEach { banned ->
            result = result.replace(banned, "*".repeat(banned.length), ignoreCase = true)
        }
        return result
    }

    fun getCustomBanned(): Set<String> {
        val json = prefs.getString(PrefsKeys.BANNED_WORDS, "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getString(it) }.toSet()
        } catch (e: Exception) { emptySet() }
    }

    fun addBanned(word: String) {
        val custom = getCustomBanned().toMutableSet()
        custom.add(word.lowercase())
        saveBanned(custom)
    }

    fun removeBanned(word: String) {
        val custom = getCustomBanned().toMutableSet()
        custom.remove(word.lowercase())
        saveBanned(custom)
    }

    fun clearAll() {
        saveBanned(emptySet())
    }

    private fun saveBanned(words: Set<String>) {
        val arr = JSONArray()
        words.forEach { arr.put(it) }
        prefs.edit().putString(PrefsKeys.BANNED_WORDS, arr.toString()).apply()
    }
}
