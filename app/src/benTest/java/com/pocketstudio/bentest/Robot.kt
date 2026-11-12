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
        /** « 6 réussies, 1 ratée… » once the run is over (« 34 verts, 2 oranges, 1 rouge » for « Tout tester »). */
        val verdict: String? = null,
        /** The run of the logbook: [ECLAIR] or [TOUT]. */
        val passage: String = ECLAIR,
        val message: String? = null,
    )

    /** The essai éclair (T0) and « Tout tester » (T1, niveau 1). */
    const val ECLAIR = "eclair"
    const val TOUT = "tout"

    private val _etat = MutableStateFlow(Etat())
    val etat: StateFlow<Etat> = _etat
    private val answers = LinkedBlockingQueue<Boolean>()

    fun dir(context: Context) = File(context.filesDir, "bentest")
    fun carnet(context: Context) = Carnet(File(dir(context), "carnet.tsv"))
    private fun captures(context: Context) = File(dir(context), "captures")

    /** Which run the logbook holds (kept beside it: a run picked up after a crash is the same one). */
    private fun passageFile(context: Context) = File(dir(context), "passage.txt")
    fun passage(context: Context): String = runCatching { passageFile(context).readText().trim() }.getOrNull()?.takeIf { it == TOUT || it == ECLAIR } ?: ECLAIR

    /** The steps' names and labels (the work is only done by a run). */
    private fun etapes(context: Context, passage: String = passage(context)): List<Etape> {
        val j = Journey(InAppDriver(LooperMainThread()))
        return if (passage == TOUT) ToutTester(j, NativeSonde, NoAppli, carnet(context), captures(context)).etapes()
        else EssaiEclair(j, NativeSonde, NoAppli, carnet(context), captures(context)).etapes()
    }

    private fun verdict(lines: List<Carnet.Ligne>, steps: List<Etape>, passage: String): String =
        if (passage == TOUT) Rapport.bilanFeux(lines, steps).verdict() else Rapport.bilan(lines, steps).verdict()

    /** What the screen shows when it opens: a run over, or one the app died in. */
    fun look(context: Context) {
        if (_etat.value.running) return
        val c = carnet(context)
        val lines = c.lines()
        val passage = passage(context)
        val steps = etapes(context, passage)
        val finished = lines.any { it.kind == Carnet.FINI }
        val ended = lines.filter { it.kind == Carnet.REUSSI || it.kind == Carnet.ECHEC || it.kind == Carnet.PLANTAGE }.map { it.step }.toSet()
        val dead = lines.lastOrNull { it.kind == Carnet.DEBUT && it.step !in ended }?.step
        _etat.value = Etat(
            interrompu = if (!finished) dead?.let { d -> steps.firstOrNull { it.name == d }?.label ?: d } else null,
            verdict = if (finished) verdict(lines, steps, passage) else null,
            passage = passage,
        )
    }

    /** Starts (or picks up, [passage] null) the essai éclair or « Tout tester ». */
    fun start(app: Application, passage: String? = null) {
        if (_etat.value.running) return
        if (passage != null) runCatching { dir(app).mkdirs(); passageFile(app).writeText(passage) }
        val which = passage(app)
        _etat.value = Etat(running = true, passage = which)
        val tracker = Tracker.install(app)
        tracker.keepScreenOn = true
        val sorties = Sorties(carnet(app), ramener = { bringStudioBack(app, tracker) })
        tracker.sorties = sorties
        Robot.sorties = sorties
        thread(name = "BenStudio Test robot") {
            val main = LooperMainThread()
            val driver = InAppDriver(main)
            val c = carnet(app)
            driver.note = { c.add(Carnet.NOTE, "robot", it) }
            val appli = AppliReelle(app, main, tracker)
            val steps = if (which == TOUT) ToutTester(Journey(driver), NativeSonde, appli, c, captures(app).apply { mkdirs() }, ecran = driver::ecran).etapes()
            else EssaiEclair(Journey(driver), NativeSonde, appli, c, captures(app).apply { mkdirs() }, ecran = driver::ecran).etapes()
            val passage = Passage(c, steps, crashText = { CrashReporter.pendingCrash() })
            runCatching {
                passage.run { e ->
                    driver.deadlineMs = System.currentTimeMillis() + e.maxMs
                    sorties.etape = e.name
                    _etat.update { it.copy(step = e.label) }
                }
            }.onFailure { c.add(Carnet.NOTE, "robot", "arrêt : $it") }
            sorties.etape = null
            tracker.sorties = null
            tracker.keepScreenOn = false
            appli.orienter(null)
            _etat.value = Etat(verdict = verdict(c.lines(), passage.etapes, which), passage = which)
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
        passageFile(context).delete()
        captures(context).deleteRecursively()
        _etat.value = Etat()
    }

    internal fun ask(app: Application, question: String, maxMs: Long): Boolean? {
        answers.clear()
        _etat.update { it.copy(question = question) }
        // Our own screen over the studio: the studio stopping is wanted, not an exit.
        sorties?.voulue = true
        bringBack(app)
        return answers.poll(maxMs, TimeUnit.MILLISECONDS).also {
            sorties?.voulue = false
            _etat.update { s -> s.copy(question = null) }
        }
    }

    @Volatile
    private var sorties: Sorties? = null

    /** The studio back in front after the app left (another app opened): the task to the front, else its launcher activity reordered to front. */
    private fun bringStudioBack(app: Application, tracker: Tracker) {
        val moved = runCatching {
            val am = app.getSystemService(android.app.ActivityManager::class.java)
            val id = tracker.studio?.taskId ?: error("no studio")
            am.moveTaskToFront(id, 0)
        }.isSuccess
        if (!moved) bringBack(app)
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
        val passage = passage(context)
        val steps = etapes(context, passage)
        val about = about(context)
        val texts = buildList {
            add("rapport.md" to Rapport.markdown(lines, steps, about, tout = passage == TOUT))
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
        if (!NativeEngine.isAvailable) return Sonde.Lecture(0, 0f, 0f, false, false, false)
        val s = NativeEngine.streamInfo()
        // The transport as the engine has it (its step is -1 when stopped), not as the screen shows it.
        // The peak is the loudest since the last read, and the app's own screens read it too (Metrics keeps theirs).
        val peak = maxOf(s.peak, com.pocketstudio.app.Metrics.streamInfo.value.peak)
        // The short-term level and the live gain (T1's fade alert): the loudness meters' own fields (reading
        // them also takes their « peak since the last reading », which only the Mix's meter shows).
        val loud = FloatArray(NativeEngine.LOUDNESS)
        val ok = runCatching { NativeEngine.loudnessMeters(loud) }.isSuccess
        val lufs = if (ok) loud[NativeEngine.LOUDNESS_SHORT_TERM].takeIf { it > NativeEngine.LOUDNESS_NONE + 1f } ?: Float.NaN else Float.NaN
        val drive = if (ok) loud[NativeEngine.LOUDNESS_DRIVE] else Float.NaN
        val lane = runCatching { com.pocketstudio.app.report.HealthMonitor.probe?.invoke()?.masterLane }.getOrNull()
        return Sonde.Lecture(s.xruns, NativeEngine.cpuLoadNow(), peak, s.nonFinite, s.running, playing = NativeEngine.currentStep() >= 0, lufs = lufs, drive = drive, lane = lane)
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

    /** The run's watch on the app leaving the front. */
    @Volatile
    var sorties: Sorties? = null

    override fun onActivityResumed(activity: Activity) {
        if (activity is MainActivity) {
            studio = activity
            sorties?.retour()
            keep(activity)
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (studio === activity) studio = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) {
        if (activity !is MainActivity) return
        val on = activity.getSystemService(android.os.PowerManager::class.java)?.isInteractive ?: true
        sorties?.arret(ecranAllume = on, rotation = activity.isChangingConfigurations)
    }
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
        Appli.ExportVu(e.running, e.files, e.error, e.seconds)
    }

    override fun capture(file: File): Boolean = main.run { Captures.windows(WindowInspector.getGlobalWindowViews(), file) }

    override fun demander(question: String, maxMs: Long): Boolean? = Robot.ask(app, question, maxMs)

    override fun video(): Appli.VideoVu = main.run {
        val a = tracker.studio as? ComponentActivity ?: return@run Appli.VideoVu(true, null, null)
        val vm = ViewModelProvider(a)[com.pocketstudio.app.export.video.VideoExportViewModel::class.java]
        val v = vm.state.value
        Appli.VideoVu(v.running, v.file, v.error, vm.choice.value.seconds)
    }

    override fun relire(file: File): Relecture.Relu? = Relecture.lire(app, file, File(app.cacheDir, "bentest-relu"))

    override fun chaleur(): Appli.Chaleur? = runCatching {
        val battery = app.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val tenths = battery?.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE) ?: Int.MIN_VALUE
        val power = app.getSystemService(android.os.PowerManager::class.java)
        Appli.Chaleur(
            batterieC = if (tenths == Int.MIN_VALUE) null else tenths / 10f,
            statut = power?.currentThermalStatus,
            marge = if (Build.VERSION.SDK_INT >= 30) power?.getThermalHeadroom(10) else null,
        )
    }.getOrNull()

    @Volatile
    private var latencyFailure = ""

    override fun latence(): Double? {
        val start = NativeEngine.startLatencyTest()
        if (start != com.pocketstudio.engine.LatencyStart.Ok) {
            latencyFailure = "départ $start"
            return null
        }
        var waited = 0
        while (NativeEngine.latencyState() != com.pocketstudio.engine.LatencyState.Done && waited < 4_000) {
            Thread.sleep(50)
            waited += 50
        }
        val r = NativeEngine.latencyResult()
        if (!r.valid) latencyFailure = "${r.failure}, niveau ${r.inputPeak}"
        return r.latencyMs.takeIf { r.valid }
    }

    override fun latenceEchec(): String = latencyFailure

    override fun memoireMo(): Int? = runCatching {
        val info = android.os.Debug.MemoryInfo()
        android.os.Debug.getMemoryInfo(info)
        info.totalPss / 1024
    }.getOrNull()

    private val frames = java.util.concurrent.atomic.AtomicInteger()
    private val slowFrames = java.util.concurrent.atomic.AtomicInteger()
    private var frameListener: android.view.Window.OnFrameMetricsAvailableListener? = null
    private val frameThread by lazy { android.os.HandlerThread("BenStudio Test images").apply { start() } }

    override fun imagesDebut() = main.run {
        val a = tracker.studio ?: return@run
        frames.set(0)
        slowFrames.set(0)
        val l = android.view.Window.OnFrameMetricsAvailableListener { _, metrics, _ ->
            // Android vitals' slow frame: over 16 ms to draw.
            if (metrics.getMetric(android.view.FrameMetrics.FIRST_DRAW_FRAME) == 1L) return@OnFrameMetricsAvailableListener
            frames.incrementAndGet()
            if (metrics.getMetric(android.view.FrameMetrics.TOTAL_DURATION) > SLOW_FRAME_NS) slowFrames.incrementAndGet()
        }
        frameListener = l
        a.window.addOnFrameMetricsAvailableListener(l, android.os.Handler(frameThread.looper))
    }

    override fun imagesFin(): Pair<Int, Int>? = main.run {
        val l = frameListener ?: return@run null
        frameListener = null
        runCatching { tracker.studio?.window?.removeOnFrameMetricsAvailableListener(l) }
        frames.get() to slowFrames.get()
    }

    override fun orienter(couche: Boolean?) = main.run {
        tracker.studio?.requestedOrientation = when (couche) {
            true -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            false -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            null -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        Unit
    }

    override fun ecranAllume(): Boolean = app.getSystemService(android.os.PowerManager::class.java)?.isInteractive ?: true

    /** The app's own « Écran toujours allumé pendant la lecture » (on by default) before the robot turned it off. */
    @Volatile
    private var appKeptOn: Boolean? = null

    override fun garderEcran(on: Boolean) {
        // The app keeps the screen on while music plays: its setting off too, and back as it was.
        if (!on) {
            if (appKeptOn == null) appKeptOn = com.pocketstudio.app.settings.SettingsStore.state.value.keepScreenOn
            main.run { com.pocketstudio.app.settings.SettingsStore.update(app) { it.copy(keepScreenOn = false) } }
        } else {
            appKeptOn?.let { was -> main.run { com.pocketstudio.app.settings.SettingsStore.update(app) { it.copy(keepScreenOn = was) } } }
            appKeptOn = null
        }
        tracker.keepScreenOn = on
    }

    override fun arreterLecture() = main.run {
        val a = tracker.studio as? ComponentActivity ?: return@run
        ViewModelProvider(a)[RhythmViewModel::class.java].stop()
    }

    private companion object {
        const val SLOW_FRAME_NS = 16_700_000L
    }
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
