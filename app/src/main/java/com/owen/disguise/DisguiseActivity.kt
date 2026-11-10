package com.owen.disguise

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.SystemClock
import android.view.Gravity
import android.widget.*
import android.text.Editable
import android.text.TextWatcher
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DisguiseActivity : Activity() {
    private val ink = Color.rgb(35, 31, 42)
    private val muted = Color.rgb(112, 106, 122)
    private val purple = Color.rgb(103, 80, 164)
    private val bg = Color.rgb(248, 248, 250)
    private lateinit var config: DisguiseConfig
    private lateinit var root: LinearLayout
    private val taps = mutableMapOf<String, Pair<Int, Long>>()
    private var selectedDate = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getStringExtra("config_id")
        config = Store.all(this).firstOrNull { it.id == id } ?: run {
            Toast.makeText(this, "This disguise no longer exists.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        showDisguise()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun label(value: String, size: Float = 15f, color: Int = ink, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun action(value: String, click: () -> Unit) = Button(this).apply {
        text = value
        isAllCaps = false
        setTextColor(Color.WHITE)
        backgroundTintList = android.content.res.ColorStateList.valueOf(purple)
        setOnClickListener { click() }
    }

    private fun screen(title: String, subtitle: String? = null, color: Int = bg) {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(22))
            setBackgroundColor(color)
        }
        root.addView(label(title, 27f, ink, true))
        if (subtitle != null) root.addView(label(subtitle, 13f, muted).apply {
            setPadding(0, dp(4), 0, dp(18))
        })
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun showDisguise() {
        when (config.disguiseType) {
            "Notes" -> showNotes()
            "Calculator" -> showCalculator()
            "Weather" -> showWeather()
            "Calendar" -> showCalendar()
            else -> showNotes()
        }
    }

    private fun showNotes() {
        screen(config.disguiseName, "All notes")
        val search = EditText(this).apply {
            hint = "Search notes"
            singleLine = true
            setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_search, 0, 0, 0)
        }
        root.addView(search)
        val editor = EditText(this).apply {
            hint = "Write a note..."
            minLines = 5
            gravity = Gravity.TOP
            setText(Store.secret(this@DisguiseActivity, config.id + "_notes"))
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(Color.WHITE)
        }
        root.addView(editor)
        root.addView(action("Save note") {
            Store.secret(this, config.id + "_notes", editor.text.toString())
            Toast.makeText(this, "Note saved on this phone.", Toast.LENGTH_SHORT).show()
        })
        root.addView(label("Tip: enter your configured phrase in Search notes to open this disguise's private space.", 12f, muted).apply {
            setPadding(0, dp(20), 0, dp(8))
        })
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrEmpty() && Store.matches(config, s.toString())) openPrivateSpace()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun showCalculator() {
        screen(config.disguiseName, "Calculator")
        val display = EditText(this).apply {
            hint = "0"
            textSize = 32f
            gravity = Gravity.END
            singleLine = true
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setPadding(dp(12), dp(16), dp(12), dp(16))
            setBackgroundColor(Color.WHITE)
        }
        root.addView(display)
        val rows = listOf(
            listOf("7", "8", "9", "÷"),
            listOf("4", "5", "6", "×"),
            listOf("1", "2", "3", "−"),
            listOf("0", ".", "C", "+")
        )
        rows.forEach { row ->
            root.addView(LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                row.forEach { key ->
                    addView(Button(this@DisguiseActivity).apply {
                        text = key
                        textSize = 20f
                        isAllCaps = false
                        setOnClickListener {
                            when (key) {
                                "C" -> display.setText("")
                                else -> display.append(when (key) { "÷" -> "/"; "×" -> "*"; "−" -> "-"; else -> key })
                            }
                        }
                    }, LinearLayout.LayoutParams(0, dp(58), 1f))
                }
            })
        }
        root.addView(action("=   Calculate") {
            val entered = display.text.toString().replace(" ", "")
            if (entered.isNotBlank() && Store.matches(config, entered)) {
                openPrivateSpace()
            } else {
                try {
                    display.setText(ExpressionParser(entered).parse().toString())
                } catch (_: Exception) {
                    Toast.makeText(this, "Check the calculation.", Toast.LENGTH_SHORT).show()
                }
            }
        })
        root.addView(label("The calculator works normally. The configured code is checked only when you press equals.", 12f, muted).apply {
            setPadding(0, dp(14), 0, 0)
        })
    }

    private fun showWeather() {
        screen(config.disguiseName, "Weekly forecast")
        root.addView(label("King George", 20f, ink, true))
        root.addView(label("Partly cloudy  •  68°", 16f, muted).apply { setPadding(0, dp(8), 0, dp(16)) })
        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
        val temps = listOf("71° / 54°", "69° / 52°", "68° / 51°", "72° / 55°", "70° / 53°")
        days.forEachIndexed { index, day ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(14), dp(14), dp(14))
                setBackgroundColor(Color.WHITE)
            }
            card.addView(label(day, 16f, ink, true), LinearLayout.LayoutParams(0, -2, 1f))
            card.addView(label(if (index % 2 == 0) "☀   " + temps[index] else "☁   " + temps[index], 14f, muted))
            card.setOnClickListener { tripleTap(day) }
            val p = LinearLayout.LayoutParams(-1, -2)
            p.topMargin = dp(7)
            root.addView(card, p)
        }
        root.addView(label("Forecast shown here is sample content. Triple-click your selected weekday to unlock.", 12f, muted).apply {
            setPadding(0, dp(16), 0, 0)
        })
    }

    private fun showCalendar() {
        selectedDate = ""
        screen(config.disguiseName, "Calendar")
        val dateButton = action("Choose a date") {
            val now = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                selectedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
                showCalendarSelected()
            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
        }
        root.addView(dateButton)
        root.addView(label("Choose a date to see its events.", 14f, muted).apply { setPadding(0, dp(16), 0, 0) })
    }

    private fun showCalendarSelected() {
        screen(config.disguiseName, selectedDate)
        root.addView(action("Change date") { showCalendar() })
        if (selectedDate == config.meta1) {
            val event = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(16), dp(14), dp(16))
                setBackgroundColor(Color.WHITE)
            }
            event.addView(label(config.meta2, 17f, ink, true))
            event.addView(label("Event", 12f, muted))
            event.setOnClickListener { tripleTapCalendarEvent() }
            root.addView(event)
            root.addView(label("Triple-click the event to unlock.", 12f, muted).apply { setPadding(0, dp(12), 0, 0) })
        } else {
            root.addView(label("No events for this date.", 14f, muted).apply { setPadding(0, dp(18), 0, 0) })
        }
    }

    private fun tripleTap(day: String) {
        val now = SystemClock.elapsedRealtime()
        val previous = taps[day]
        val count = if (previous == null || now - previous.second > 850) 1 else previous.first + 1
        taps[day] = count to now
        if (count >= 3) {
            taps[day] = 0 to now
            if (day == config.meta1) openPrivateSpace()
        }
    }

    private fun tripleTapCalendarEvent() {
        val now = SystemClock.elapsedRealtime()
        val previous = taps["event"]
        val count = if (previous == null || now - previous.second > 850) 1 else previous.first + 1
        taps["event"] = count to now
        if (count >= 3) {
            taps["event"] = 0 to now
            if (selectedDate == config.meta1) openPrivateSpace()
        }
    }

    private fun openPrivateSpace() {
        screen("Private space", "Unlocked for ${config.disguiseName}")
        root.addView(label("Private note", 16f, ink, true))
        val note = EditText(this).apply {
            hint = "Write something private for this disguise..."
            minLines = 5
            gravity = Gravity.TOP
            setText(Store.secret(this@DisguiseActivity, config.id))
            setBackgroundColor(Color.WHITE)
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        root.addView(note)
        root.addView(action("Save private note") {
            Store.secret(this, config.id, note.text.toString())
            Toast.makeText(this, "Saved locally on this phone.", Toast.LENGTH_SHORT).show()
        })
        root.addView(action("Open original ${config.targetName}") {
            val launch = packageManager.getLaunchIntentForPackage(config.targetPackage)
            if (launch != null) startActivity(launch)
            else Toast.makeText(this, "The original app could not be opened.", Toast.LENGTH_LONG).show()
        })
        root.addView(action("Back to disguise") { showDisguise() })
        root.addView(label("This is a separate DISGUISE screen. It does not replace the original app's internal interface.", 12f, muted).apply {
            setPadding(0, dp(18), 0, 0)
        })
    }
}

private class ExpressionParser(private val input: String) {
    private var pos = 0
    fun parse(): Double {
        val value = expression()
        if (pos != input.length) throw IllegalArgumentException("Unexpected input")
        return value
    }
    private fun expression(): Double {
        var value = term()
        while (pos < input.length && (input[pos] == '+' || input[pos] == '-')) {
            val op = input[pos++]
            val right = term()
            value = if (op == '+') value + right else value - right
        }
        return value
    }
    private fun term(): Double {
        var value = factor()
        while (pos < input.length && (input[pos] == '*' || input[pos] == '/')) {
            val op = input[pos++]
            val right = factor()
            value = if (op == '*') value * right else value / right
        }
        return value
    }
    private fun factor(): Double {
        if (pos < input.length && input[pos] == '(') {
            pos++
            val value = expression()
            if (pos >= input.length || input[pos++] != ')') throw IllegalArgumentException("Missing )")
            return value
        }
        val start = pos
        if (pos < input.length && input[pos] == '-') pos++
        while (pos < input.length && (input[pos].isDigit() || input[pos] == '.')) pos++
        if (start == pos) throw IllegalArgumentException("Expected number")
        return input.substring(start, pos).toDouble()
    }
}
