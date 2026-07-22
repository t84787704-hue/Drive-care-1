package com.drivecare.app.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

object LocaleManager {

    fun getLocale(appLanguage: AppLanguage): Locale {
        return when (appLanguage) {
            AppLanguage.ENGLISH -> Locale("en")
            AppLanguage.URDU -> Locale("ur")
            AppLanguage.ARABIC -> Locale("ar")
            AppLanguage.HINDI -> Locale("hi")
            AppLanguage.CHINESE -> Locale("zh", "CN")
        }
    }

    fun applyLocale(context: Context, appLanguage: AppLanguage): Context {
        val locale = getLocale(appLanguage)
        Locale.setDefault(locale)

        val resources = context.resources
        val config = Configuration(resources.configuration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val localeList = LocaleList(locale)
            LocaleList.setDefault(localeList)
            config.setLocales(localeList)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLayoutDirection(locale)
        }

        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)

        return context.createConfigurationContext(config)
    }
}
