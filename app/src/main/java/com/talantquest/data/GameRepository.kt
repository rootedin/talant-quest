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

    fun getLastEventScanTime(tagId: String): Long = prefs.getLong("event_time_$tagId", 0L)
    fun setLastEventScanTime(tagId: String) =
        prefs.edit().putLong("event_time_$tagId", System.currentTimeMillis()).apply()

    fun isEventOnCooldown(tagId: String): Boolean {
        val last = getLastEventScanTime(tagId)
        return last != 0L && System.currentTimeMillis() - last < COOLDOWN_MS
    }

    fun getEventCooldownMinutes(tagId: String): Int {
        val remaining = COOLDOWN_MS - (System.currentTimeMillis() - getLastEventScanTime(tagId))
        return if (remaining <= 0) 0 else (remaining / 60_000).toInt() + 1
    }

    fun resetAll() = prefs.edit().clear().apply()

    companion object {
        private const val COOLDOWN_MS = 15 * 60 * 1000L
    }
}
