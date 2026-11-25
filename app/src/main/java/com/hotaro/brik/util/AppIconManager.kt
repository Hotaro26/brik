package com.hotaro.brik.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.hotaro.brik.ui.theme.ThemePreferences

data class AppIconOption(
    val id: String,
    val name: String,
    val subtitle: String,
    val alias: String,
    val primaryColor: Long,
    val secondaryColor: Long,
    val darkColor: Long,
    val holeColor: Long,
    val bgColor: Long
)

object AppIconManager {
    val icons = listOf(
        AppIconOption(
            id = "dynamic",
            name = "Dynamic",
            subtitle = "Material You dynamic adaptive colors (Default)",
            alias = "com.hotaro.brik.LauncherAlias",
            primaryColor = 0xFF4D77FF,
            secondaryColor = 0xFF2952E3,
            darkColor = 0xFF1836A8,
            holeColor = 0xFF0B1B54,
            bgColor = 0xFF090D18
        ),
        AppIconOption(
            id = "brik",
            name = "Brik Terracotta",
            subtitle = "Classic warm clay & black",
            alias = "com.hotaro.brik.LauncherBrik",
            primaryColor = 0xFFE55B2B,
            secondaryColor = 0xFFB83E15,
            darkColor = 0xFF8C2A0A,
            holeColor = 0xFF4A1404,
            bgColor = 0xFF000000
        ),
        AppIconOption(
            id = "classic",
            name = "Classic Green",
            subtitle = "Islamic emerald & mint",
            alias = "com.hotaro.brik.LauncherEmerald",
            primaryColor = 0xFF00E676,
            secondaryColor = 0xFF00C853,
            darkColor = 0xFF1B5E20,
            holeColor = 0xFF02280C,
            bgColor = 0xFF05140A
        ),
        AppIconOption(
            id = "cherry",
            name = "Cherry Red",
            subtitle = "Rich ruby & crimson",
            alias = "com.hotaro.brik.LauncherCherry",
            primaryColor = 0xFFFF1744,
            secondaryColor = 0xFFD50000,
            darkColor = 0xFF880E4F,
            holeColor = 0xFF3E000B,
            bgColor = 0xFF150406
        ),
        AppIconOption(
            id = "navy",
            name = "Navy Blue",
            subtitle = "Deep imperial oceanic blue",
            alias = "com.hotaro.brik.LauncherMidnight",
            primaryColor = 0xFF00E5FF,
            secondaryColor = 0xFF0091EA,
            darkColor = 0xFF0064B0,
            holeColor = 0xFF002244,
            bgColor = 0xFF080E1A
        ),
        AppIconOption(
            id = "lavender",
            name = "Lavender",
            subtitle = "Royal amethyst & violet",
            alias = "com.hotaro.brik.LauncherAmethyst",
            primaryColor = 0xFFD500F9,
            secondaryColor = 0xFFAA00FF,
            darkColor = 0xFF7200CA,
            holeColor = 0xFF2C004F,
            bgColor = 0xFF12071F
        ),
        AppIconOption(
            id = "pink",
            name = "Pink",
            subtitle = "Warm rose & blossom",
            alias = "com.hotaro.brik.LauncherPink",
            primaryColor = 0xFFFF4081,
            secondaryColor = 0xFFF50057,
            darkColor = 0xFFC51162,
            holeColor = 0xFF4A0022,
            bgColor = 0xFF18030B
        ),
        AppIconOption(
            id = "mocha",
            name = "Mocha",
            subtitle = "Roasted espresso & golden honey",
            alias = "com.hotaro.brik.LauncherSolar",
            primaryColor = 0xFFFFB300,
            secondaryColor = 0xFFFF8F00,
            darkColor = 0xFFE65100,
            holeColor = 0xFF3E1A00,
            bgColor = 0xFF140C02
        ),
        AppIconOption(
            id = "catppuccin",
            name = "Catppuccin",
            subtitle = "Modern pastel harmony",
            alias = "com.hotaro.brik.LauncherCatppuccin",
            primaryColor = 0xFF89B4FA,
            secondaryColor = 0xFFCBA6F7,
            darkColor = 0xFF1E3A6E,
            holeColor = 0xFF11111B,
            bgColor = 0xFF181825
        ),
        AppIconOption(
            id = "monochrome",
            name = "Monochrome",
            subtitle = "Minimalist pure black & white",
            alias = "com.hotaro.brik.LauncherMonochrome",
            primaryColor = 0xFFFFFFFF,
            secondaryColor = 0xFFDDDDDD,
            darkColor = 0xFFAAAAAA,
            holeColor = 0xFF333333,
            bgColor = 0xFF121212
        )
    )

    fun setAppIcon(context: Context, option: AppIconOption) {
        ThemePreferences.setAppIcon(context, option.id)
        val pm = context.packageManager
        val isHidden = StealthUtils.isAppHidden(context)

        // First enable the target alias so launcher always has an active component
        val targetComp = ComponentName(context, option.alias)
        if (!isHidden) {
            try {
                pm.setComponentEnabledSetting(
                    targetComp,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Then disable all other aliases
        icons.forEach { icon ->
            if (icon.id != option.id) {
                val comp = ComponentName(context, icon.alias)
                try {
                    pm.setComponentEnabledSetting(
                        comp,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
