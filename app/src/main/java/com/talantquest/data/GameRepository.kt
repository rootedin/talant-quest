package com.talantquest.data

import android.content.Context
import android.content.SharedPreferences

class GameRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("talant_quest", Context.MODE_PRIVATE)

    fun getTeamName(): String = prefs.getString("team_name", "") ?: ""
    fun setTeamName(name: String) = prefs.edit().putString("team_name", name).apply()
    fun hasTeamName(): Boolean = getTeamName().isNotBlank()

    fun getTalant(): Int = prefs.getInt("talant", 0)
    fun setTalant(amount: Int) = prefs.edit().putInt("talant", maxOf(0, amount)).apply()
    fun addTalant(amount: Int) = setTalant(getTalant() + amount)

    fun isTagUsed(tagId: String): Boolean = prefs.getBoolean("used_$tagId", false)
    fun markTagUsed(tagId: String) = prefs.edit().putBoolean("used_$tagId", true).apply()

    fun getTtsVolume(): Float = prefs.getFloat("tts_volume", 1.0f)
    fun setTtsVolume(v: Float) = prefs.edit().putFloat("tts_volume", v).apply()

    fun resetAll() = prefs.edit().clear().apply()
}
