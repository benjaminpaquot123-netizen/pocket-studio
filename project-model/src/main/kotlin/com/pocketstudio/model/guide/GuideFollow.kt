package com.pocketstudio.model.guide

import com.pocketstudio.model.Project

/**
 * G6 (docs/PLAN-GUIDE.md §4.5 « Son »): a song followed to the end of its parcours, card after card
 * in the Guide's own order, by someone who takes what each card offers: the first option that
 * changes the song ([Policy.ACCEPT]), or each card's first option as offered (« Garder » most of the
 * time, [Policy.FIRST]). What only a person can do here is left: an option that opens a screen, a
 * check to hear, one that needs the song measured (the balance); a card with nothing else is
 * skipped, as is a card met a second time (put off « plus tard », an alert that stays). Pure: for
 * the tests and the song bench (scripts/song-bench.sh --guide), never the app's screens.
 */
object GuideFollow {
    enum class Policy { ACCEPT, FIRST }

    /** The song at the end of its parcours and each decision, in order (« fx.bass:cleaner », « arr.bass:sauter »). */
    data class Followed(val project: Project, val decisions: List<String>)

    private val PERSON_ONLY = setOf(GuideAction.OPEN, GuideAction.LISTEN, GuideAction.PLAY)

    fun follow(start: Project, policy: Policy = Policy.ACCEPT, simple: Boolean = false, at: Long = 1L, limit: Int = 200): Followed {
        // An assistant's song the Guide sees for the first time: what each chain is now (as the app does, quietly).
        var p = if (start.style != null && start.guide.seen.isEmpty()) GuideActions.withBaseline(start) else start
        val decisions = ArrayList<String>()
        val met = HashSet<String>()
        repeat(limit) {
            val next = GuideAnalyzer.analyze(p, simple, at).next ?: return Followed(p, decisions)
            val card = next.card
            if (!met.add(card.id)) {
                // Met again: put off, or an alert whose fact stays: « C'est voulu » for an alert, « Sauter » otherwise.
                p = GuideActions.skip(p, card.id, at, forever = next.alert)
                decisions += "${card.id}:sauter"
                return@repeat
            }
            val usable = card.proposals.filter { o -> !p.guide.refused(card.id, o.id) && o.action.type !in PERSON_ONLY && !o.action.needsMeasure }
            val ordered = if (policy == Policy.ACCEPT) usable.filter { it.action.edits } + usable.filter { !it.action.edits } else usable
            for (o in ordered) {
                val out = GuideActions.tryApply(p, card.id, o.id, at)
                if (out.problem != null || out.project == p) continue
                p = out.project
                decisions += "${card.id}:${o.id}"
                return@repeat
            }
            p = GuideActions.skip(p, card.id, at, forever = next.alert)
            decisions += "${card.id}:sauter"
        }
        error("the parcours of ${start.style} did not end in $limit cards: ${decisions.takeLast(10)}")
    }
}
