package com.owen.disguise

import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

data class DisguiseConfig(
    val id: String = UUID.randomUUID().toString(),
    val targetPackage: String,
    val targetName: String,
    val disguiseType: String,
    val disguiseName: String,
    val unlockSalt: String,
    val unlockHash: String,
    val meta1: String = "",
    val meta2: String = ""
)

object Store {
    private const val PREFS = "disguise_store"
    private const val CONFIGS = "configs"

    fun all(context: Context): MutableList<DisguiseConfig> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(CONFIGS, "[]") ?: "[]"
        val array = JSONArray(raw)
        val result = mutableListOf<DisguiseConfig>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            result.add(DisguiseConfig(
                id = o.optString("id"),
                targetPackage = o.optString("targetPackage"),
                targetName = o.optString("targetName"),
                disguiseType = o.optString("disguiseType"),
                disguiseName = o.optString("disguiseName"),
                unlockSalt = o.optString("unlockSalt"),
                unlockHash = o.optString("unlockHash"),
                meta1 = o.optString("meta1"),
                meta2 = o.optString("meta2")
            ))
        }
        return result
    }

    fun save(context: Context, config: DisguiseConfig) {
        val list = all(context)
        list.removeAll { it.id == config.id }
        list.add(config)
        write(context, list)
    }

    fun remove(context: Context, id: String) {
        write(context, all(context).filterNot { it.id == id })
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove("private_$id").apply()
    }

    private fun write(context: Context, configs: List<DisguiseConfig>) {
        val array = JSONArray()
        configs.forEach { c ->
            array.put(JSONObject().apply {
                put("id", c.id); put("targetPackage", c.targetPackage); put("targetName", c.targetName)
                put("disguiseType", c.disguiseType); put("disguiseName", c.disguiseName)
                put("unlockSalt", c.unlockSalt); put("unlockHash", c.unlockHash)
                put("meta1", c.meta1); put("meta2", c.meta2)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(CONFIGS, array.toString()).apply()
    }

    fun secret(context: Context, id: String, value: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("private_$id", value).apply()
    }

    fun secret(context: Context, id: String): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("private_$id", "") ?: ""

    fun newSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun hash(salt: String, value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest((salt + value).toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun matches(config: DisguiseConfig, value: String): Boolean =
        MessageDigest.isEqual(hash(config.unlockSalt, value).toByteArray(), config.unlockHash.toByteArray())
}
