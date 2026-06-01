package com.yamispeaker.client

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class DisplayMode { IP, HOSTNAME, BOTH }
enum class ThemeColor { GREEN, BLUE, PURPLE }

object AppPrefs {
    private var prefs: SharedPreferences? = null

    private val _displayMode = mutableStateOf(DisplayMode.BOTH)
    private val _themeColor = mutableStateOf(ThemeColor.GREEN)

    var displayMode: DisplayMode
        get() = _displayMode.value
        set(value) {
            _displayMode.value = value
            prefs?.edit()?.putInt("display_mode", value.ordinal)?.apply()
        }

    var themeColor: ThemeColor
        get() = _themeColor.value
        set(value) {
            _themeColor.value = value
            prefs?.edit()?.putInt("theme_color", value.ordinal)?.apply()
        }

    fun init(ctx: Context) {
        prefs = ctx.getSharedPreferences("yamispeaker", Context.MODE_PRIVATE)
        displayMode = DisplayMode.entries[prefs?.getInt("display_mode", 2) ?: 2]
        themeColor = ThemeColor.entries[prefs?.getInt("theme_color", 0) ?: 0]
    }
}
