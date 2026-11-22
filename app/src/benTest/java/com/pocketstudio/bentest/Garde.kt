package com.pocketstudio.bentest

import android.app.ActivityOptions
import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.view.inspector.WindowInspector
import java.io.File

/**
 * The run's guard against the app dying under it (Benjamin, 2026-11-22: « si l'appli plante pendant un
 * scénario, le robot le note comme échec de ce scénario, relance le studio et continue »). The robot runs in
 * the studio's process, so a crash kills it too; the logbook already names the step begun and never ended
 * ([Passage.recover] notes it as a crash with Android's cause). The guard adds what dies with the process:
 * - an exception nobody caught: the screen's picture and tags taken as the app dies (under the names
 *   [Scenarios.apresPlantage] reads), then the run's relaunch armed a few seconds later;
 * - a death no code hears (the engine's native crash, an ANR, the system killing the app): an alarm armed at
 *   each step's start, past its longest time, and cancelled at its end, relaunches BenStudio Test, which picks
 *   the run up by itself ([REPRENDRE]).
 */
internal object Garde {
    /** The extra of BenStudio Test's relaunch: the run is picked up without a touch. */
    const val REPRENDRE = "bentest_reprendre"

    /** After a step's longest time, how long the process may still be alive before the alarm relaunches the run. */
    private const val MARGE_MS = 90_000L
    private const val APRES_MORT_MS = 3_000L

    @Volatile private var app: Application? = null
    @Volatile private var captures: File? = null
    @Volatile private var step: String? = null
    @Volatile private var previous: Thread.UncaughtExceptionHandler? = null

    private val handler = Thread.UncaughtExceptionHandler { thread, throwable ->
        val dying = step
        if (dying != null) {
            runCatching { indices(dying) }
            runCatching { armer(APRES_MORT_MS) }
        }
        previous?.uncaughtException(thread, throwable)
    }

    /** Before a run: the handler in front of the app's own (CrashReporter's, which writes the trace, then Android's). */
    fun installer(app: Application, captures: File) {
        this.app = app
        this.captures = captures
        val current = Thread.getDefaultUncaughtExceptionHandler()
        if (current === handler) return
        previous = current
        Thread.setDefaultUncaughtExceptionHandler(handler)
    }

    /** A step begins ([name], [maxMs]) or the run is over (null): the relaunch armed past its time, or cancelled. */
    fun etape(name: String?, maxMs: Long = 0) {
        step = name
        runCatching { if (name != null) armer(maxMs + MARGE_MS) else annuler() }
    }

    /** The screen as the app dies, under the names of a failure's clues. */
    private fun indices(step: String) {
        val dir = captures ?: return
        dir.mkdirs()
        // On the main thread when it is the one dying (its looper is gone, read it in place), else posted for a short while.
        val main = LooperMainThread(timeoutMs = 2_000)
        runCatching {
            val tags = InAppDriver(main).ecran()
            val known = tags.filter { it in EssaiEclair.REPERES }
            File(dir, "echec-$step-tags.txt").writeText(
                buildString {
                    appendLine("pris quand l'appli est morte")
                    appendLine("repères : " + known.ifEmpty { listOf("aucun") }.joinToString(", "))
                    appendLine("tags à l'écran (${tags.size}) :")
                    (known + (tags - known.toSet())).forEach { appendLine(it) }
                },
            )
        }
        runCatching { main.run { Captures.windows(WindowInspector.getGlobalWindowViews(), File(dir, "echec-$step.jpg")) } }
    }

    private fun intent(app: Application): PendingIntent {
        val i = Intent(app, BenTestActivity::class.java).putExtra(REPRENDRE, true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        // Android 14+: the relaunch is ours to allow (an activity started by an alarm, the app in the background).
        val options = if (Build.VERSION.SDK_INT >= 34) {
            ActivityOptions.makeBasic().setPendingIntentCreatorBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED).toBundle()
        } else null
        return PendingIntent.getActivity(app, CODE, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT, options)
    }

    private fun armer(inMs: Long) {
        val app = app ?: return
        val am = app.getSystemService(AlarmManager::class.java) ?: return
        val at = System.currentTimeMillis() + inMs
        val exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent(app))
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent(app))
    }

    private fun annuler() {
        val app = app ?: return
        app.getSystemService(AlarmManager::class.java)?.cancel(intent(app))
    }

    private const val CODE = 0x6245
}
