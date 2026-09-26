package com.baris.tvappswitcher

import android.graphics.drawable.Drawable

data class AppInfo(
    val name: String,
    val packageName: String,
    var icon: Drawable? = null
)
