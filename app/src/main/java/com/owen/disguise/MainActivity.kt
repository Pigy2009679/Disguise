package com.owen.disguise

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class InstalledApp(val name: String, val packageName: String)

class MainActivity : Activity() {
    private val purple = Color.rgb(103, 80, 164)
    private val ink = Color.rgb(35, 31, 42)
    private val muted = Color.rgb(112, 106, 122)
    private val bg = Color.rgb(247, 245, 252)
    private lateinit var root: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showHome()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun text(value: String, size: Float = 15f, color: Int = ink, bold: Boolean = false): TextView =
        TextView(this).apply {
            this.text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun button(label: String, onClick: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            setTextColor(Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(purple)
            setOnClickListener { onClick() }
        }

    private fun base(title: String, subtitle: String? = null): LinearLayout {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(20))
            setBackgroundColor(bg)
        }
        root.addView(text(title, 29f, ink, true))
        if (subtitle != null) root.addView(text(subtitle, 14f, muted).apply {
            setPadding(0, dp(5), 0, dp(16))
        })
        setContentView(ScrollView(this).apply { addView(root) })
        return root
    }

    private fun showHome() {
        val configs = Store.all(this)
        base("DISGUISE", "Create separate app experiences with their own appearance and unlock method.")
        root.addView(text("Your disguises", 19f, ink, true).apply { setPadding(0, dp(12), 0, dp(8)) })
        root.addView(text("${configs.size} configured", 13f, muted))
        root.addView(button("+  Create a disguise") { chooseOriginalApp() }.apply {
            setPadding(0, dp(4), 0, dp(4))
        })
        if (configs.isEmpty()) {
            root.addView(text("No disguises yet. Create one to get started.", 15f, muted).apply {
                setPadding(0, dp(24), 0, dp(12))
            })
        }
        configs.forEach { c ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(15), dp(13), dp(15), dp(13))
                setBackgroundColor(Color.WHITE)
            }
            card.addView(text(c.disguiseName, 18f, ink, true))
            card.addView(text("For ${c.targetName}  •  ${c.disguiseType}", 13f, muted))
            card.addView(text("Unlock: ${unlockDescription(c)}", 13f, muted))
            card.addView(LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(button("Preview") { openDisguise(c) }, LinearLayout.LayoutParams(0, dp(48), 1f))
                addView(button("Add shortcut") { pinShortcut(c) }, LinearLayout.LayoutParams(0, dp(48), 1f))
                addView(button("Remove") {
                    AlertDialog.Builder(this@MainActivity).setTitle("Remove disguise?")
                        .setMessage("This removes the DISGUISE entry and its private note. The original app stays installed.")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Remove") { _, _ ->
                            Store.remove(this@MainActivity, c.id)
                            showHome()
                        }.show()
                }, LinearLayout.LayoutParams(0, dp(48), 1f))
            })
            val params = LinearLayout.LayoutParams(-1, -2)
            params.topMargin = dp(12)
            root.addView(card, params)
        }
        root.addView(text("How it works", 18f, ink, true).apply { setPadding(0, dp(24), 0, dp(6)) })
        root.addView(text("A disguise is a separate shortcut that opens a screen made by DISGUISE. It does not replace or modify the original app. After the configured interaction, you enter a private space for that disguise.", 14f, muted))
        root.addView(text("Privacy note: this first version stores settings and private notes locally on this phone. Do not use it for highly sensitive information yet.", 12f, muted).apply { setPadding(0, dp(16), 0, 0) })
    }

    private fun installedApps(): List<InstalledApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(intent, 0)
            .mapNotNull {
                val pkg = it.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == packageName) return@mapNotNull null
                InstalledApp(it.loadLabel(packageManager).toString(), pkg)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.name.lowercase(Locale.getDefault()) }
    }

    private fun chooseOriginalApp() {
        val apps = installedApps()
        if (apps.isEmpty()) {
            AlertDialog.Builder(this).setMessage("No launchable apps were found.").setPositiveButton("OK", null).show()
            return
        }
        val labels = apps.map { "${it.name}\n${it.packageName}" }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Choose an installed app")
            .setItems(labels) { _, which -> chooseDisguise(apps[which]) }.show()
    }

    private fun chooseDisguise(app: InstalledApp) {
        val types = arrayOf("Notes", "Calculator", "Weather", "Calendar")
        AlertDialog.Builder(this).setTitle("What should ${app.name} look like?")
            .setItems(types) { _, which -> configure(app, types[which]) }.show()
    }

    private fun configure(app: InstalledApp, type: String) {
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
        }
        fun field(hint: String, value: String = "") = EditText(this).apply {
            this.hint = hint
            setText(value)
            isSingleLine = true
        }
        val disguiseName = field("Disguised app name", type)
        form.addView(text("Shortcut name", 13f, muted)); form.addView(disguiseName)

        var secretField: EditText? = null
        var daySpinner: Spinner? = null
        var dateField: EditText? = null
        var eventField: EditText? = null
        when (type) {
            "Notes" -> {
                form.addView(text("Secret phrase (typed into Notes search)", 13f, muted))
                secretField = field("Choose a secret phrase")
                secretField!!.inputType = 1
                form.addView(secretField)
            }
            "Calculator" -> {
                form.addView(text("Secret code (enter it, then press =)", 13f, muted))
                secretField = field("Example: 2580")
                form.addView(secretField)
            }
            "Weather" -> {
                form.addView(text("Triple-click this weekday to unlock", 13f, muted))
                daySpinner = Spinner(this)
                daySpinner!!.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
                    listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday"))
                form.addView(daySpinner)
            }
            "Calendar" -> {
                form.addView(text("Unlock date (YYYY-MM-DD)", 13f, muted))
                dateField = field("2026-11-18", SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
                form.addView(dateField)
                form.addView(text("Event name to triple-click", 13f, muted))
                eventField = field("School Project")
                form.addView(eventField)
            }
        }
        AlertDialog.Builder(this).setTitle("Customize $type")
            .setView(form)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Create") { _, _ ->
                val name = disguiseName.text.toString().trim().ifBlank { type }
                var meta1 = ""
                var meta2 = ""
                var rawSecret = ""
                when (type) {
                    "Notes", "Calculator" -> rawSecret = secretField?.text?.toString()?.trim().orEmpty()
                    "Weather" -> meta1 = daySpinner?.selectedItem?.toString() ?: "Wednesday"
                    "Calendar" -> {
                        meta1 = dateField?.text?.toString()?.trim().orEmpty()
                        meta2 = eventField?.text?.toString()?.trim().orEmpty()
                        if (!meta1.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) || meta2.isBlank()) {
                            Toast.makeText(this, "Enter a valid date and event name.", Toast.LENGTH_LONG).show()
                            return@setPositiveButton
                        }
                    }
                }
                if ((type == "Notes" || type == "Calculator") && rawSecret.length < 3) {
                    Toast.makeText(this, "Choose a secret with at least 3 characters.", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                val salt = Store.newSalt()
                val config = DisguiseConfig(
                    targetPackage = app.packageName,
                    targetName = app.name,
                    disguiseType = type,
                    disguiseName = name,
                    unlockSalt = salt,
                    unlockHash = if (rawSecret.isNotEmpty()) Store.hash(salt, rawSecret) else "",
                    meta1 = meta1,
                    meta2 = meta2
                )
                Store.save(this, config)
                Toast.makeText(this, "Disguise created. Add its shortcut to your home screen.", Toast.LENGTH_LONG).show()
                showHome()
            }.show()
    }

    private fun unlockDescription(c: DisguiseConfig): String = when (c.disguiseType) {
        "Notes" -> "secret phrase"
        "Calculator" -> "secret calculation/code"
        "Weather" -> "triple-click ${c.meta1}"
        "Calendar" -> "triple-click ${c.meta2} on ${c.meta1}"
        else -> "not configured"
    }

    private fun openDisguise(c: DisguiseConfig) {
        startActivity(Intent(this, DisguiseActivity::class.java).putExtra("config_id", c.id))
    }

    private fun pinShortcut(c: DisguiseConfig) {
        if (Build.VERSION.SDK_INT < 26) {
            Toast.makeText(this, "Pinned shortcuts require Android 8.0 or newer.", Toast.LENGTH_LONG).show()
            return
        }
        val manager = getSystemService(ShortcutManager::class.java)
        val icon = Icon.createWithBitmap(shortcutBitmap(c.disguiseType))
        val intent = Intent(this, DisguiseActivity::class.java)
            .setAction(Intent.ACTION_VIEW).putExtra("config_id", c.id)
        val shortcut = ShortcutInfo.Builder(this, c.id)
            .setShortLabel(c.disguiseName.take(20))
            .setLongLabel("${c.disguiseName} disguise for ${c.targetName}")
            .setIcon(icon).setIntent(intent).build()
        if (manager.isRequestPinShortcutSupported) {
            manager.requestPinShortcut(shortcut, null)
        } else {
            Toast.makeText(this, "Your launcher does not support pinned shortcuts.", Toast.LENGTH_LONG).show()
        }
    }

    private fun shortcutBitmap(type: String): Bitmap {
        val bitmap = Bitmap.createBitmap(144, 144, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val colors = mapOf("Notes" to 0xFF4D78B8.toInt(), "Calculator" to 0xFF555A68.toInt(),
            "Weather" to 0xFF3D91B5.toInt(), "Calendar" to 0xFFB65B69.toInt())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors[type] ?: purple }
        canvas.drawRoundRect(0f, 0f, 144f, 144f, 30f, 30f, paint)
        paint.color = Color.WHITE
        paint.textSize = 68f
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textAlign = Paint.Align.CENTER
        val letter = when (type) { "Notes" -> "N"; "Calculator" -> "÷"; "Weather" -> "W"; else -> "C" }
        canvas.drawText(letter, 72f, 96f, paint)
        return bitmap
    }
}
