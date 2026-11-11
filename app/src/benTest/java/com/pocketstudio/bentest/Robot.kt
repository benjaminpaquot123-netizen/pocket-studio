package com.pocketstudio.bentest

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.inspector.WindowInspector
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import com.pocketstudio.app.AppInfo
import com.pocketstudio.app.BuildConfig
import com.pocketstudio.app.MainActivity
import com.pocketstudio.app.export.WavExporter
import com.pocketstudio.app.journey.Journey
import com.pocketstudio.app.report.CrashReporter
import com.pocketstudio.app.report.JournalJson
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.engine.NativeEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * BenStudio Test's robot in the app's process: it runs the essai éclair on a thread of its own
 * (the screens are reached through [InAppDriver]), writes everything in the logbook, and hands the
 * report to Benjamin, who sends it himself.
 */
object Robot {
    data class Etat(
        val running: Boolean = false,
        /** The step being done, as Benjamin reads it. */
        val step: String? = null,
        /** A question waiting for Benjamin's answer. */
        val question: String? = null,
        /** The run stopped in this step (the app died): it is noted when the run goes on. */
        val interrompu: String? = null,
        /** « 6 réussies, 1 ratée… » once the run is over. */
        val verdict: String? = null,
        val message: String? = null,
    )

    private val _etat = MutableStateFlow(Etat())
    val etat: StateFlow<Etat> = _etat
    private val answers = LinkedBlockingQueue<Boolean>()

    fun dir(context: Context) = File(context.filesDir, "bentest")
    fun carnet(context: Context) = Carnet(File(dir(context), "carnet.tsv"))
    private fun captures(context: Context) = File(dir(context), "captures")

    /** The steps' names and labels (the work is only done by a run). */
    private fun etapes(context: Context): List<Etape> = EssaiEclair(Journey(InAppDriver(LooperMainThread())), NativeSonde, NoAppli, carnet(context), captures(context)).etapes()

    /** What the screen shows when it opens: a run over, or one the app died in. */
    fun look(context: Context) {
        if (_etat.value.running) return
        val c = carnet(context)
        val lines = c.lines()
        val steps = etapes(context)
        val finished = lines.any { it.kind == Carnet.FINI }
        val ended = lines.filter { it.kind == Carnet.REUSSI || it.kind == Carnet.ECHEC || it.kind == Carnet.PLANTAGE }.map { it.step }.toSet()
        val dead = lines.lastOrNull { it.kind == Carnet.DEBUT && it.step !in ended }?.step
        _etat.value = Etat(
            interrompu = if (!finished) dead?.let { d -> steps.firstOrNull { it.name == d }?.label ?: d } else null,
            verdict = if (finished) Rapport.bilan(lines, steps).verdict() else null,
        )
    }

    /** Starts (or picks up) the essai éclair. */
    fun start(app: Application) {
        if (_etat.value.running) return
        _etat.value = Etat(running = true)
        val tracker = Tracker.install(app)
        tracker.keepScreenOn = true
        thread(name = "BenStudio Test robot") {
            val main = LooperMainThread()
            val driver = InAppDriver(main)
            val c = carnet(app)
            val essai = EssaiEclair(Journey(driver), NativeSonde, AppliReelle(app, main, tracker), c, captures(app).apply { mkdirs() })
            val passage = Passage(c, essai.etapes(), crashText = { CrashReporter.pendingCrash() })
            runCatching {
                passage.run { e ->
                    driver.deadlineMs = System.currentTimeMillis() + e.maxMs
                    _etat.update { it.copy(step = e.label) }
                }
            }.onFailure { c.add(Carnet.NOTE, "robot", "arrêt : $it") }
            tracker.keepScreenOn = false
            _etat.value = Etat(verdict = Rapport.bilan(c.lines(), passage.etapes).verdict())
            bringBack(app)
        }
    }

    /** Benjamin's answer to the question on screen. */
    fun answer(yes: Boolean) {
        answers.offer(yes)
        _etat.update { it.copy(question = null) }
    }

    /** Forgets the last run (its logbook, its pictures). */
    fun reset(context: Context) {
        if (_etat.value.running) return
        carnet(context).clear()
        captures(context).deleteRecursively()
        _etat.value = Etat()
    }

    internal fun ask(app: Application, question: String, maxMs: Long): Boolean? {
        answers.clear()
        _etat.update { it.copy(question = question) }
        bringBack(app)
        return answers.poll(maxMs, TimeUnit.MILLISECONDS).also { _etat.update { s -> s.copy(question = null) } }
    }

    private fun bringBack(app: Application) {
        runCatching { app.startActivity(Intent(app, BenTestActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)) }
    }

    /** The app's version and commit, the device. */
    fun about(context: Context): List<Pair<String, String>> {
        val id = context.resources.getIdentifier("bentest_commit", "string", context.packageName)
        return listOf(
            "Version" to "${AppInfo.versionName} (${BuildConfig.VERSION_CODE})",
            "Commit" to (if (id != 0) context.getString(id) else "?"),
            "Appareil" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "Android" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            "Date" to SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT).format(Date()),
        )
    }

    /** « Envoyer le rapport »: the zip written, then Android's share menu (nothing leaves by itself). */
    fun share(context: Context): Boolean {
        val zip = writeReport(context)
        return WavExporter.share(context, listOf(zip.zip), "Envoyer le rapport", subject = "BenStudio Test : rapport ${AppInfo.versionName}")
    }

    fun writeReport(context: Context): RapportZip.Ecrit {
        val c = carnet(context)
        val lines = c.lines()
        val steps = etapes(context)
        val about = about(context)
        val texts = buildList {
            add("rapport.md" to Rapport.markdown(lines, steps, about))
            add("mesures.json" to Rapport.json(lines, about))
            add("carnet.tsv" to (if (c.file.isFile) c.file.readText() else ""))
            add("journal.json" to runCatching { JournalJson.current(about) }.getOrDefault("{}"))
            CrashReporter.pendingCrash()?.let { add("plantage.txt" to it) }
        }
        val pictures = captures(context).listFiles()?.sortedBy { it.name }?.map { "captures/${it.name}" to it }.orEmpty()
        val out = File(File(context.cacheDir, "exports"), "rapport-benstudio-test-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(Date()) + ".zip")
        out.parentFile?.listFiles { f -> f.name.startsWith("rapport-benstudio-test-") }?.forEach { it.delete() }
        return RapportZip.write(out, texts, pictures)
    }
}

/** The engine's numbers (read only, from any thread: the app's own screens read them the same way). */
object NativeSonde : Sonde {
    override fun lire(): Sonde.Lecture {
        if (!NativeEngine.isAvailable) return Sonde.Lecture(0, 0f, 0f, false, false)
        val s = NativeEngine.streamInfo()
        return Sonde.Lecture(s.xruns, NativeEngine.cpuLoadNow(), s.peak, s.nonFinite, s.running)
    }
}

/** Only to read the steps' names. */
private object NoAppli : Appli {
    override fun ouvrirStudio() = Unit
    override fun export() = Appli.ExportVu(false, emptyList(), null)
    override fun capture(file: File) = false
    override fun demander(question: String, maxMs: Long): Boolean? = null
}

/** The app's activities as they come and go: the studio's, and its screen kept on while the robot works (its touches are not a finger's). */
internal class Tracker : Application.ActivityLifecycleCallbacks {
    @Volatile
    var studio: Activity? = null

    @Volatile
    var keepScreenOn = false
        set(value) {
            field = value
            studio?.let { a -> a.runOnUiThread { keep(a) } }
        }

    private fun keep(a: Activity) {
        if (keepScreenOn) a.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) else a.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onActivityResumed(activity: Activity) {
        if (activity is MainActivity) {
            studio = activity
            keep(activity)
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (studio === activity) studio = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    companion object {
        @Volatile
        private var installed: Tracker? = null

        fun install(app: Application): Tracker = installed ?: synchronized(this) {
            installed ?: Tracker().also {
                app.registerActivityLifecycleCallbacks(it)
                installed = it
            }
        }
    }
}

/** [Appli] on the phone. */
private class AppliReelle(private val app: Application, private val main: MainThread, private val tracker: Tracker) : Appli {
    override fun ouvrirStudio() {
        app.startActivity(Intent(app, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun export(): Appli.ExportVu = main.run {
        val a = tracker.studio as? ComponentActivity ?: return@run Appli.ExportVu(true, emptyList(), null)
        val e = ViewModelProvider(a)[RhythmViewModel::class.java].export.value
        Appli.ExportVu(e.running, e.files, e.error)
    }

    override fun capture(file: File): Boolean = main.run { Captures.windows(WindowInspector.getGlobalWindowViews(), file) }

    override fun demander(question: String, maxMs: Long): Boolean? = Robot.ask(app, question, maxMs)
}

/** A picture of the app's windows as they are on screen, half size, in JPEG. */
object Captures {
    fun windows(views: List<View>, file: File, scale: Float = 0.5f): Boolean = runCatching {
        val shown = views.filter { it.isAttachedToWindow && it.isShown && it.width > 0 && it.height > 0 }
        val base = shown.maxByOrNull { it.width.toLong() * it.height } ?: return false
        val origin = IntArray(2).also { base.getLocationOnScreen(it) }
        val bitmap = Bitmap.createBitmap((base.width * scale).toInt().coerceAtLeast(1), (base.height * scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(scale, scale)
        // The windows in their order (the activity first, its dialogs and sheets over it).
        for (v in shown) {
            val at = IntArray(2).also { v.getLocationOnScreen(it) }
            canvas.save()
            canvas.translate((at[0] - origin[0]).toFloat(), (at[1] - origin[1]).toFloat())
            v.draw(canvas)
            canvas.restore()
        }
        file.parentFile?.mkdirs()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 70, it) }
        bitmap.recycle()
        file.length() > 0
    }.getOrDefault(false)
}
