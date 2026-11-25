package com.hotaro.brik.data.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable? = null,
    val isSystemApp: Boolean = false
)
