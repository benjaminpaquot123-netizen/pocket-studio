package com.pocketstudio.bentest

import java.io.File
import java.io.FileOutputStream

/**
 * BenStudio Test's logbook on the phone's disk (docs/PLAN-TEST-APK.md, « Le passage tient debout
 * tout seul »): one line per event, written and synced as it happens, so that what the robot did
 * outlives a crash of the app. A line: time, kind, step, text (tabs and line breaks escaped).
 */
class Carnet(val file: File, private val clock: () -> Long = System::currentTimeMillis) {
    data class Ligne(val atMs: Long, val kind: String, val step: String, val text: String)

    @Synchronized
    fun add(kind: String, step: String, text: String = "") {
        file.parentFile?.mkdirs()
        val line = listOf(clock().toString(), kind, step, text).joinToString("\t") { escape(it) } + "\n"
        FileOutputStream(file, true).use { out ->
            out.write(line.toByteArray(Charsets.UTF_8))
            // On the disk before the next move: the step that crashes the app is the one the logbook names.
            runCatching { out.fd.sync() }
        }
    }

    @Synchronized
    fun lines(): List<Ligne> = if (!file.isFile) emptyList() else file.readLines(Charsets.UTF_8).mapNotNull { raw ->
        // A line cut by the crash itself (half written) is left out.
        val f = raw.split('\t')
        if (f.size != 4) return@mapNotNull null
        val at = f[0].toLongOrNull() ?: return@mapNotNull null
        Ligne(at, unescape(f[1]), unescape(f[2]), unescape(f[3]))
    }

    @Synchronized
    fun clear() {
        file.delete()
    }

    companion object {
        const val DEBUT = "debut"
        const val REUSSI = "reussi"
        const val ECHEC = "echec"
        const val PLANTAGE = "plantage"
        const val MESURE = "mesure"
        const val NOTE = "note"

        /** A step whose timing is skewed (the app left the front): its lights are not counted. */
        const val FAUSSE = "fausse"
        const val FINI = "fini"
        /** A step's light: « vert;why », « orange;why »… (Feu.mot first). */
        const val FEU = "feu"

        /** An answer to a question of the banner: « verdict;capture;comment » (Oreille.kt). */
        const val REPONSE = "reponse"

        /** « Écoute au micro »'s numbers: « bruit;csv », « courbe;csv », « empreinte;style;… », « anomalie;… » (EcouteMicro.kt). */
        const val MICRO = "micro"

        private fun escape(s: String) = s.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n").replace("\r", "")

        private fun unescape(s: String): String = buildString(s.length) {
            var i = 0
            while (i < s.length) {
                val c = s[i]
                if (c == '\\' && i + 1 < s.length) {
                    when (s[i + 1]) {
                        't' -> append('\t')
                        'n' -> append('\n')
                        else -> append(s[i + 1])
                    }
                    i += 2
                } else {
                    append(c)
                    i++
                }
            }
        }
    }
}

/** A step of a run: its name in the logbook, what Benjamin reads, its longest time, and the work (its result in a few words). */
class Etape(val name: String, val label: String, val maxMs: Long, val work: (deadlineMs: Long) -> String)

/** A step went past its longest time. */
class EtapeTropLongue(message: String) : RuntimeException(message)

/**
 * A run of [etapes] written in [carnet], that picks up where it stopped: a step begun and never
 * ended is the one the app died in, noted as a crash ([crashText]: what the app kept of it), and the
 * run goes on with the next one. A failed step never stops the others.
 */
class Passage(
    private val carnet: Carnet,
    val etapes: List<Etape>,
    private val clock: () -> Long = System::currentTimeMillis,
    private val crashText: () -> String? = { null },
) {
    /** Steps begun and never ended (the app died in them), each noted once as a crash. Returns their names. */
    fun recover(): List<String> {
        val lines = carnet.lines()
        val ended = lines.filter { it.kind == Carnet.REUSSI || it.kind == Carnet.ECHEC || it.kind == Carnet.PLANTAGE }.map { it.step }.toSet()
        val crashed = lines.filter { it.kind == Carnet.DEBUT && it.step !in ended }.map { it.step }.distinct()
        crashed.forEach { carnet.add(Carnet.PLANTAGE, it, crashText()?.lineSequence()?.take(12)?.joinToString("\n").orEmpty()) }
        return crashed
    }

    /** The steps still to do (none ended, in its order). */
    fun remaining(): List<Etape> {
        val lines = carnet.lines()
        val done = lines.filter { it.kind == Carnet.REUSSI || it.kind == Carnet.ECHEC || it.kind == Carnet.PLANTAGE }.map { it.step }.toSet()
        return etapes.filter { it.name !in done }
    }

    /** True once every step is ended (the run is over). */
    fun finished(): Boolean = carnet.lines().any { it.kind == Carnet.FINI }

    /** Runs what is left, step by step; [onStep] hears each one as it begins. */
    fun run(onStep: (Etape) -> Unit = {}) {
        recover()
        for (e in remaining()) {
            onStep(e)
            carnet.add(Carnet.DEBUT, e.name)
            val start = clock()
            val deadline = start + e.maxMs
            val result = runCatching { e.work(deadline) }
            val took = clock() - start
            val skewed = carnet.lines().let { l -> l.drop(l.indexOfLast { it.step == e.name && it.kind == Carnet.DEBUT }.coerceAtLeast(0)) }.any { it.step == e.name && it.kind == Carnet.FAUSSE }
            result.fold(
                onSuccess = { text ->
                    if (took > e.maxMs && !skewed) carnet.add(Carnet.ECHEC, e.name, "trop longue : ${took / 1000} s (au plus ${e.maxMs / 1000} s). $text")
                    else carnet.add(Carnet.REUSSI, e.name, text)
                },
                onFailure = { t ->
                    if (t is PasseExterne) carnet.add(Carnet.REUSSI, e.name, "${Externe.RAISON} : ${t.tag}")
                    else carnet.add(Carnet.ECHEC, e.name, (if (t is EtapeTropLongue) "trop longue : " else "") + (t.message ?: t.javaClass.simpleName).take(400)) },
            )
        }
        if (!finished()) carnet.add(Carnet.FINI, "")
    }
}
