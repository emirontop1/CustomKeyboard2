package com.customkeyboard.service

import android.content.Context
import android.content.SharedPreferences
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import com.customkeyboard.settings.PrefsManager
import com.customkeyboard.settings.BannedWordsFilter
import com.customkeyboard.settings.AutoConvertManager

class KeyboardService : InputMethodService() {

    private var keyboardView: KeyboardUIView? = null
    private lateinit var prefs: PrefsManager
    private lateinit var bannedFilter: BannedWordsFilter
    private lateinit var autoConvert: AutoConvertManager

    override fun onCreate() {
        super.onCreate()
        prefs = PrefsManager(this)
        bannedFilter = BannedWordsFilter(this)
        autoConvert = AutoConvertManager(this)
    }

    override fun onCreateInputView(): View {
        keyboardView = KeyboardUIView(this)
        return keyboardView!!
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        keyboardView?.resetState()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        keyboardView?.onInputViewStarted()
    }

    // ── Called by KeyboardUIView ────────────────────────────────

    fun onKeyPress(char: Char) {
        val ic = currentInputConnection ?: return
        val text = char.toString()
        ic.commitText(text, 1)
        updateSuggestions()
    }

    fun onSpacePressed() {
        val ic = currentInputConnection ?: return
        ic.commitText(" ", 1)
        updateSuggestions()
    }

    fun onDeletePressed() {
        val ic = currentInputConnection ?: return
        val selected = ic.getSelectedText(0)
        if (selected?.isNotEmpty() == true) {
            ic.commitText("", 1)
        } else {
            ic.deleteSurroundingText(1, 0)
        }
        updateSuggestions()
    }

    fun onDeleteWordPressed() {
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(100, 0)?.toString() ?: return
        val trimmed = before.trimEnd()
        val lastSpace = trimmed.lastIndexOf(' ')
        val deleteCount = if (lastSpace < 0) trimmed.length else trimmed.length - lastSpace
        if (deleteCount > 0) {
            ic.deleteSurroundingText(deleteCount, 0)
        }
        updateSuggestions()
    }

    fun onEnterPressed() {
        val ic = currentInputConnection ?: return
        val editorInfo = currentInputEditorInfo ?: run {
            ic.commitText("\n", 1)
            return
        }
        val action = editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION
        when (action) {
            EditorInfo.IME_ACTION_SEARCH -> ic.performEditorAction(EditorInfo.IME_ACTION_SEARCH)
            EditorInfo.IME_ACTION_SEND   -> ic.performEditorAction(EditorInfo.IME_ACTION_SEND)
            EditorInfo.IME_ACTION_GO     -> ic.performEditorAction(EditorInfo.IME_ACTION_GO)
            EditorInfo.IME_ACTION_NEXT   -> ic.performEditorAction(EditorInfo.IME_ACTION_NEXT)
            EditorInfo.IME_ACTION_DONE   -> ic.performEditorAction(EditorInfo.IME_ACTION_DONE)
            else -> ic.commitText("\n", 1)
        }
    }

    fun onAutoConvertCommit(shortcut: String) {
        val ic = currentInputConnection ?: return
        val expansion = autoConvert.get(shortcut) ?: return
        
        // Delete the shortcut text
        val before = ic.getTextBeforeCursor(100, 0)?.toString() ?: ""
        val toDelete = shortcut.length
        if (before.endsWith(shortcut)) {
            ic.deleteSurroundingText(toDelete, 0)
        }
        
        // Commit expansion
        val filtered = bannedFilter.filter(expansion)
        ic.commitText(filtered, 1)
        updateSuggestions()
    }

    fun onSuggestionTap(word: String) {
        val ic = currentInputConnection ?: return
        
        // Replace current partial with suggestion
        val before = ic.getTextBeforeCursor(100, 0)?.toString() ?: ""
        val lastSpace = before.lastIndexOf(' ')
        val partial = if (lastSpace < 0) before else before.substring(lastSpace + 1)
        
        if (partial.isNotEmpty()) {
            ic.deleteSurroundingText(partial.length, 0)
        }
        
        val filtered = bannedFilter.filter(word)
        ic.commitText("$filtered ", 1)
        keyboardView?.clearSuggestions()
    }

    fun shouldCapitalize(): Boolean {
        val before = currentInputConnection?.getTextBeforeCursor(3, 0)?.toString() ?: return true
        if (before.isEmpty()) return true
        val trimmed = before.trimEnd()
        return trimmed.isEmpty() || trimmed.endsWith('.') || trimmed.endsWith('!') || trimmed.endsWith('?')
    }

    private fun updateSuggestions() {
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(50, 0)?.toString() ?: ""
        val lastSpace = before.lastIndexOf(' ')
        val partial = if (lastSpace < 0) before else before.substring(lastSpace + 1)
        
        if (partial.length >= 2) {
            val suggestions = listOf(
                "hello", "help", "how", "happy", "home", "hope", "have", "history",
                "merhaba", "nasılsın", "teşekkürler", "evet", "hayır", "lütfen",
                "geliyorum", "gidiyorum", "nerede", "zaman"
            ).filter { it.startsWith(partial.lowercase()) }.take(3)
            
            keyboardView?.showSuggestions(suggestions)
        } else {
            keyboardView?.clearSuggestions()
        }
    }

    fun getPrefs() = prefs
    fun getAutoConvert() = autoConvert
    fun getBannedFilter() = bannedFilter
}
