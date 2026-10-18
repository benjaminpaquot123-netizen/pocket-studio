package com.pocketstudio.app

import android.os.Build

object AppInfo {
    val versionName: String get() = BuildConfig.VERSION_NAME
    val versionCode: Int get() = BuildConfig.VERSION_CODE
    val channel: String get() = BuildConfig.RELEASE_CHANNEL
    val repo: String get() = BuildConfig.GITHUB_REPO

    val versionLabel: String get() = "$versionName ($channel)"

    val device: String
        get() = "${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
}
