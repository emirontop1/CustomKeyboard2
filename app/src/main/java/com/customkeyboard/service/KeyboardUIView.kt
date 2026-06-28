package com.customkeyboard.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import com.customkeyboard.settings.PrefsKeys

@SuppressLint("ClickableViewAccessibility")
class KeyboardUIView(private val service: KeyboardService) : View(service) {

    private enum class Mode { LETTERS, NUMBERS, SYMBOLS }
    private var mode = Mode.LETTERS
    private var isShifted = false
    private var isCapsLock = false
    private var lastShiftTime = 0L

    private val keys = mutableListOf<Key>()
    private val suggestionKeys = mutableListOf<Key>()
    private var pressedKey: Key? = null
    private var suggestions = listOf<String>()

    private var keyboardWidth = 0
    private var keyboardHeight = 0
    private var keyHeight = 0f
    private var suggestionHeight = 0f
    private var padding = 0f

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val keyPressedPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val specialKeyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val smallTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val suggestionBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val suggestionTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        applyTheme()
    }

    private fun applyTheme() {
        val prefs = service.getPrefs()
        val isDark = prefs.isDarkTheme()
        val accentColor = prefs.getAccentColor()

        if (isDark) {
            bgPaint.color = Color.parseColor("#1C1C1E")
            keyPaint.color = Color.parseColor("#2C2C2E")
            keyPressedPaint.color = Color.parseColor("#545458")
            specialKeyPaint.color = Color.parseColor("#3A3A3C")
            textPaint.color = Color.WHITE
            smallTextPaint.color = Color.parseColor("#AEAEB2")
            suggestionBgPaint.color = Color.parseColor("#1C1C1E")
            suggestionTextPaint.color = accentColor
            dividerPaint.color = Color.parseColor("#3A3A3C")
        } else {
            bgPaint.color = Color.parseColor("#E5E5EA")
            keyPaint.color = Color.WHITE
            keyPressedPaint.color = Color.parseColor("#D1D5DB")
            specialKeyPaint.color = Color.parseColor("#D1D5DB")
            textPaint.color = Color.BLACK
            smallTextPaint.color = Color.parseColor("#6C757D")
            suggestionBgPaint.color = Color.parseColor("#F3F3F7")
            suggestionTextPaint.color = accentColor
            dividerPaint.color = Color.parseColor("#D1D5DB")
        }
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getMetrics(dm)

        keyboardWidth = dm.widthPixels
        val dp = dm.density

        keyHeight = 52 * dp
        suggestionHeight = 40 * dp
        padding = 3 * dp

        val rows = 5
        keyboardHeight = (suggestionHeight + rows * keyHeight + 6 * padding).toInt()

        setMeasuredDimension(keyboardWidth, keyboardHeight)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        keyboardWidth = w
        buildKeys()
    }

    private fun buildKeys() {
        keys.clear()
        val w = keyboardWidth.toFloat()
        val top = suggestionHeight + padding

        when (mode) {
            Mode.LETTERS -> buildLetterKeys(w, top)
            Mode.NUMBERS -> buildNumberKeys(w, top)
            Mode.SYMBOLS -> buildSymbolKeys(w, top)
        }
    }

    private fun buildLetterKeys(w: Float, top: Float) {
        val rows = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
        
        // Row 1: qwertyuiop
        val row1w = (w - 11 * padding) / 10
        rows[0].forEachIndexed { i, c ->
            keys += Key(c.toString(), KeyType.LETTER, padding + i * (row1w + padding), top + padding, row1w, keyHeight)
        }

        // Row 2: asdfghjkl (centered)
        val row2w = (w - 10 * padding) / 9
        val row2offset = (w - 9 * row2w - 8 * padding) / 2
        rows[1].forEachIndexed { i, c ->
            keys += Key(c.toString(), KeyType.LETTER, row2offset + i * (row2w + padding), top + keyHeight + 2 * padding, row2w, keyHeight)
        }

        // Row 3: Shift + zxcvbnm + Delete
        val shiftW = w * 0.12f
        val delW = w * 0.12f
        val row3w = (w - shiftW - delW - 4 * padding) / 7
        keys += Key("⇧", KeyType.SHIFT, padding, top + 2 * keyHeight + 3 * padding, shiftW, keyHeight)
        rows[2].forEachIndexed { i, c ->
            keys += Key(c.toString(), KeyType.LETTER, shiftW + 2 * padding + i * (row3w + padding), top + 2 * keyHeight + 3 * padding, row3w, keyHeight)
        }
        keys += Key("⌫", KeyType.DELETE, w - delW - padding, top + 2 * keyHeight + 3 * padding, delW, keyHeight)

        // Row 4: Numbers + , + space + . + ?
        val numW = w * 0.12f
        val spaceW = w * 0.45f
        var x = padding
        keys += Key("123", KeyType.NUMBERS, x, top + 3 * keyHeight + 4 * padding, numW, keyHeight); x += numW + padding
        keys += Key(",", KeyType.LETTER, x, top + 3 * keyHeight + 4 * padding, (w - 6 * padding - numW - spaceW - numW) / 2, keyHeight); x += (w - 6 * padding - numW - spaceW - numW) / 2 + padding
        keys += Key(" ", KeyType.SPACE, x, top + 3 * keyHeight + 4 * padding, spaceW, keyHeight); x += spaceW + padding
        keys += Key(".", KeyType.LETTER, x, top + 3 * keyHeight + 4 * padding, (w - 6 * padding - numW - spaceW - numW) / 2, keyHeight); x += (w - 6 * padding - numW - spaceW - numW) / 2 + padding
        keys += Key("↵", KeyType.ENTER, x, top + 3 * keyHeight + 4 * padding, numW, keyHeight)
    }

    private fun buildNumberKeys(w: Float, top: Float) {
        val row = "1234567890"
        val kw = (w - 11 * padding) / 10
        row.forEachIndexed { i, c ->
            keys += Key(c.toString(), KeyType.LETTER, padding + i * (kw + padding), top + padding, kw, keyHeight)
        }

        val row2 = "@#$%&-+()"
        val kw2 = (w - 10 * padding) / 9
        val offset = (w - 9 * kw2 - 8 * padding) / 2
        row2.forEachIndexed { i, c ->
            keys += Key(c.toString(), KeyType.LETTER, offset + i * (kw2 + padding), top + keyHeight + 2 * padding, kw2, keyHeight)
        }

        val row3 = "*/.,!?;:\""
        val kw3 = (w - 10 * padding) / 9
        row3.forEachIndexed { i, c ->
            keys += Key(c.toString(), KeyType.LETTER, padding + i * (kw3 + padding), top + 2 * keyHeight + 3 * padding, kw3, keyHeight)
        }

        // Row 4: ABC + space + delete + enter
        val abcW = w * 0.15f
        val delW = w * 0.12f
        val entW = w * 0.12f
        val spW = w - abcW - delW - entW - 4 * padding
        var x = padding
        keys += Key("ABC", KeyType.LETTERS, x, top + 3 * keyHeight + 4 * padding, abcW, keyHeight); x += abcW + padding
        keys += Key(" ", KeyType.SPACE, x, top + 3 * keyHeight + 4 * padding, spW, keyHeight); x += spW + padding
        keys += Key("⌫", KeyType.DELETE, x, top + 3 * keyHeight + 4 * padding, delW, keyHeight); x += delW + padding
        keys += Key("↵", KeyType.ENTER, x, top + 3 * keyHeight + 4 * padding, entW, keyHeight)
    }

    private fun buildSymbolKeys(w: Float, top: Float) {
        val row = "~`|•√π÷×¶∆"
        val kw = (w - 11 * padding) / 10
        row.forEachIndexed { i, c ->
            keys += Key(c.toString(), KeyType.LETTER, padding + i * (kw + padding), top + padding, kw, keyHeight)
        }

        val row2 = "£¢€¥°={}[]"
        val kw2 = (w - 11 * padding) / 10
        row2.forEachIndexed { i, c ->
            keys += Key(c.toString(), KeyType.LETTER, padding + i * (kw2 + padding), top + keyHeight + 2 * padding, kw2, keyHeight)
        }

        val row3 = "<>«»_…„""
        val kw3 = (w - 10 * padding) / 9
        row3.take(9).forEachIndexed { i, c ->
            keys += Key(c.toString(), KeyType.LETTER, padding + i * (kw3 + padding), top + 2 * keyHeight + 3 * padding, kw3, keyHeight)
        }
        val delW = w * 0.12f
        keys += Key("⌫", KeyType.DELETE, w - delW - padding, top + 2 * keyHeight + 3 * padding, delW, keyHeight)

        // Row 4: ABC + space + delete + enter
        val abcW = w * 0.15f
        val delW2 = w * 0.12f
        val entW = w * 0.12f
        val spW = w - abcW - delW2 - entW - 4 * padding
        var x = padding
        keys += Key("ABC", KeyType.LETTERS, x, top + 3 * keyHeight + 4 * padding, abcW, keyHeight); x += abcW + padding
        keys += Key(" ", KeyType.SPACE, x, top + 3 * keyHeight + 4 * padding, spW, keyHeight); x += spW + padding
        keys += Key("⌫", KeyType.DELETE, x, top + 3 * keyHeight + 4 * padding, delW2, keyHeight); x += delW2 + padding
        keys += Key("↵", KeyType.ENTER, x, top + 3 * keyHeight + 4 * padding, entW, keyHeight)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Suggestion strip
        canvas.drawRect(0f, 0f, width.toFloat(), suggestionHeight, suggestionBgPaint)
        dividerPaint.strokeWidth = 1f
        canvas.drawLine(0f, suggestionHeight, width.toFloat(), suggestionHeight, dividerPaint)

        suggestionKeys.forEachIndexed { i, key ->
            if (i > 0) canvas.drawLine(key.x, 0f, key.x, suggestionHeight, dividerPaint)
            suggestionTextPaint.textSize = 14f * resources.displayMetrics.density
            canvas.drawText(key.label, key.cx, key.cy + 5f, suggestionTextPaint)
        }

        // Keys
        val shifted = isShifted || isCapsLock
        keys.forEach { key ->
            val paint = when {
                key == pressedKey -> keyPressedPaint
                key.type == KeyType.LETTER && key.label.length == 1 -> keyPaint
                key.type == KeyType.SPACE -> keyPaint
                else -> specialKeyPaint
            }

            val rect = RectF(key.x + 2, key.y + 2, key.x + key.w - 2, key.y + key.h - 2)
            canvas.drawRoundRect(rect, 10f, 10f, paint)

            // Caps lock indicator
            if (key.type == KeyType.SHIFT && isCapsLock) {
                val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { 
                    color = service.getPrefs().getAccentColor()
                }
                canvas.drawRoundRect(rect, 10f, 10f, accentPaint)
            }

            val label = when {
                key.type == KeyType.LETTER && key.label.length == 1 && key.label[0].isLetter() ->
                    if (shifted) key.label.uppercase() else key.label
                key.type == KeyType.SPACE -> "SPACE"
                else -> key.label
            }

            textPaint.textSize = 16f * resources.displayMetrics.density
            canvas.drawText(label, key.cx, key.cy + 5f, textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                pressedKey = suggestionKeys.firstOrNull { it.contains(x, y) }
                    ?: keys.firstOrNull { it.contains(x, y) }
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                val key = pressedKey ?: return true
                pressedKey = null
                invalidate()
                vibrate()
                handleKey(key)
            }
            MotionEvent.ACTION_CANCEL -> {
                pressedKey = null
                invalidate()
            }
        }
        return true
    }

    private fun handleKey(key: Key) {
        when (key.type) {
            KeyType.LETTER -> {
                val text = if ((isShifted || isCapsLock) && key.label.length == 1 && key.label[0].isLetter())
                    key.label.uppercase() else key.label
                service.onKeyPress(text[0])
                if (isShifted && !isCapsLock) { isShifted = false; invalidate() }
            }
            KeyType.SPACE -> {
                service.onSpacePressed()
                if (isShifted && !isCapsLock) { isShifted = false; invalidate() }
            }
            KeyType.DELETE -> service.onDeletePressed()
            KeyType.ENTER -> service.onEnterPressed()
            KeyType.SHIFT -> handleShift()
            KeyType.NUMBERS -> { mode = Mode.NUMBERS; buildKeys() }
            KeyType.LETTERS -> { mode = Mode.LETTERS; buildKeys() }
            KeyType.SYMBOLS -> { mode = Mode.SYMBOLS; buildKeys() }
            KeyType.SUGGESTION -> service.onSuggestionTap(key.label)
        }
    }

    private fun handleShift() {
        val now = System.currentTimeMillis()
        when {
            isCapsLock -> { isCapsLock = false; isShifted = false }
            isShifted && now - lastShiftTime < 400 -> { isCapsLock = true }
            else -> { isShifted = !isShifted; lastShiftTime = now }
        }
        invalidate()
    }

    @Suppress("DEPRECATION")
    private fun vibrate() {
        if (!service.getPrefs().isHapticEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (service.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
                .defaultVibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            val v = service.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                v.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
            else v.vibrate(20)
        }
    }

    fun resetState() {
        isShifted = false
        isCapsLock = false
        mode = Mode.LETTERS
        suggestions = emptyList()
        buildKeys()
    }

    fun onInputViewStarted() {
        applyTheme()
        if (service.shouldCapitalize()) { isShifted = true }
        buildKeys()
    }

    fun showSuggestions(list: List<String>) {
        suggestions = list
        suggestionKeys.clear()
        val w = keyboardWidth.toFloat()
        val kw = w / list.size.coerceAtMost(3)
        list.take(3).forEachIndexed { i, s ->
            suggestionKeys += Key(s, KeyType.SUGGESTION, i * kw, 0f, kw, suggestionHeight)
        }
        invalidate()
    }

    fun clearSuggestions() {
        suggestions = emptyList()
        suggestionKeys.clear()
        invalidate()
    }

    enum class KeyType { LETTER, SHIFT, DELETE, ENTER, SPACE, NUMBERS, LETTERS, SYMBOLS, SUGGESTION }

    data class Key(val label: String, val type: KeyType, val x: Float, val y: Float, val w: Float, val h: Float) {
        val cx get() = x + w / 2f
        val cy get() = y + h / 2f
        fun contains(px: Float, py: Float) = px >= x && px < x + w && py >= y && py < y + h
    }
}
