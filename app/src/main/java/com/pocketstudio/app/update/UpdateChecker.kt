package com.pocketstudio.app.update

import com.pocketstudio.app.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(val versionCode: Int, val versionName: String, val apkUrl: String, val notes: String)

/** Asks GitHub for the latest stable release and compares it with the installed build. */
object UpdateChecker {
    const val APK_NAME = "pocket-studio.apk"

    suspend fun check(): UpdateInfo? = withContext(Dispatchers.IO) {
        if (AppInfo.channel == "dev") return@withContext null
        runCatching {
            val connection = URL("https://api.github.com/repos/${AppInfo.repo}/releases/latest")
                .openConnection() as HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            parse(body)
        }.getOrNull()?.takeIf { it.versionCode > AppInfo.versionCode }
    }

    /** Tags look like "v0.1.42": the last number is the versionCode. */
    fun parse(json: String): UpdateInfo? {
        val root = JSONObject(json)
        val tag = root.optString("tag_name")
        val code = tag.substringAfterLast('.').toIntOrNull() ?: return null
        val assets = root.optJSONArray("assets") ?: return null
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            if (asset.optString("name") == APK_NAME) {
                return UpdateInfo(
                    versionCode = code,
                    versionName = tag.removePrefix("v"),
                    apkUrl = asset.getString("browser_download_url"),
                    notes = root.optString("body").take(600),
                )
            }
        }
        return null
    }
}
