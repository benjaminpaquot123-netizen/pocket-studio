package com.pocketstudio.app

import com.pocketstudio.app.report.AppLog
import android.app.Application
import com.pocketstudio.app.report.CrashReporter

class PocketStudioApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashReporter.install(this)
        Metrics.init(this)
        AppLog.add("Démarrage ${AppInfo.versionLabel}")
    }
}
