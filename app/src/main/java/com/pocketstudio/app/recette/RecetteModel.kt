package com.pocketstudio.app.recette

import android.content.Context
import org.json.JSONObject

enum class CheckStatus { Untested, Pass, Fail }

data class CheckItem(val id: String, val title: String, val hint: String)

data class Checklist(val id: String, val milestone: String, val items: List<CheckItem>)

data class CheckResult(val status: CheckStatus = CheckStatus.Untested, val comment: String = "")

object ChecklistParser {
    fun parse(json: String): Checklist {
        val root = JSONObject(json)
        val items = root.getJSONArray("items")
        return Checklist(
            id = root.getString("id"),
            milestone = root.getString("milestone"),
            items = List(items.length()) { i ->
                val item = items.getJSONObject(i)
                CheckItem(item.getString("id"), item.getString("title"), item.optString("hint"))
            },
        )
    }

    fun reportLines(checklist: Checklist, results: Map<String, CheckResult>): List<String> =
        checklist.items.map { item ->
            val result = results[item.id] ?: CheckResult()
            val mark = when (result.status) {
                CheckStatus.Pass -> "✅"
                CheckStatus.Fail -> "❌"
                CheckStatus.Untested -> "⬜"
            }
            val comment = result.comment.trim().replace("\n", " ")
            "- $mark ${item.title}" + if (comment.isNotEmpty()) " : $comment" else ""
        }
}

/** Loads the milestone checklist from assets and persists the user's answers. */
class RecetteRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("recette", Context.MODE_PRIVATE)

    fun load(): Checklist =
        ChecklistParser.parse(context.assets.open("recette.json").bufferedReader().use { it.readText() })

    fun changelog(): String =
        runCatching { context.assets.open("changelog.txt").bufferedReader().use { it.readText().trim() } }
            .getOrDefault("")

    fun results(checklist: Checklist): Map<String, CheckResult> = checklist.items.associate { item ->
        val key = "${checklist.id}:${item.id}"
        item.id to CheckResult(
            status = CheckStatus.entries.getOrElse(prefs.getInt("$key:status", 0)) { CheckStatus.Untested },
            comment = prefs.getString("$key:comment", "") ?: "",
        )
    }

    fun save(checklist: Checklist, itemId: String, result: CheckResult) {
        val key = "${checklist.id}:$itemId"
        prefs.edit()
            .putInt("$key:status", result.status.ordinal)
            .putString("$key:comment", result.comment)
            .apply()
    }
}
