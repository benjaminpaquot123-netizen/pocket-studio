package com.pocketstudio.app.ui

import android.content.Context
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketstudio.app.learn.spot
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon

/**
 * Phase 36: the five spaces of the app, in the bottom bar (the rail lying down). Each one opens
 * one or more screens ([AppTab]): Structure the song's arrangement (the timeline and its parts),
 * Sons every track's editor (drums, acid, synths, audio) with the tracks as chips on top, Mix the
 * console, Live the performance page out of the creation path, Plus the rest (projects, settings,
 * help, MIDI, latency, tools).
 */
enum class Space(val icon: ImageVector) {
    Structure(Icons.Filled.Timeline),
    Sons(Icons.Filled.Piano),
    Mix(Icons.Filled.Tune),
    Live(Icons.Filled.Apps),
    Plus(Icons.Filled.MoreHoriz);

    val label: String get() = when (this) {
        Structure -> tr("Structure")
        Sons -> tr("Sons")
        Mix -> tr("Mix")
        Live -> tr("Live")
        Plus -> tr("Plus")
    }

    /** What the space is for, in the « ? » labels and for a screen reader. */
    val hint: String get() = when (this) {
        Structure -> tr("Ordre des parties")
        Sons -> tr("Tes pistes")
        Mix -> tr("Volumes")
        Live -> tr("Jouer en direct")
        Plus -> tr("Projets, aide")
    }
}

/** The tabs of the Sons space (the editors), in the chips' order. */
val SONS_TABS = listOf(AppTab.Rythme, AppTab.Acide, AppTab.Synthe, AppTab.Audio)

/** The space a screen belongs to. */
val AppTab.space: Space get() = when (this) {
    AppTab.Morceau, AppTab.Envoyer -> Space.Structure
    AppTab.Rythme, AppTab.Acide, AppTab.Synthe, AppTab.Audio -> Space.Sons
    AppTab.Mixeur -> Space.Mix
    AppTab.Live -> Space.Live
    AppTab.Outils -> Space.Plus
}

/**
 * The four tabs of the Mode simple, in everyday words: the song first (where one lands), the
 * drums, the keyboard, and « Envoyer » (the « Fini ? » page: checks, Partager, WAV).
 */
enum class SimpleTab(val icon: ImageVector, val tab: AppTab) {
    Morceau(Icons.AutoMirrored.Filled.QueueMusic, AppTab.Morceau),
    Rythme(Icons.Filled.GridView, AppTab.Rythme),
    Clavier(Icons.Filled.Piano, AppTab.Synthe),
    Envoyer(Icons.AutoMirrored.Filled.Send, AppTab.Envoyer);

    val label: String get() = when (this) {
        Morceau -> tr("Mon morceau")
        Rythme -> tr("Rythme")
        Clavier -> tr("Clavier")
        Envoyer -> tr("Envoyer")
    }

    val hint: String get() = when (this) {
        Morceau -> tr("Parties et pistes")
        Rythme -> tr("Batterie")
        Clavier -> tr("Jouer, enregistrer")
        Envoyer -> tr("Partager")
    }

    companion object {
        fun of(tab: AppTab): SimpleTab? = entries.firstOrNull { it.tab == tab }
    }
}

/**
 * Where the navigation was left, kept on the phone (the screens' own state is saved with the
 * activity): the last editor of the Sons space (it opens there again) and the Structure view.
 */
class NavPrefs(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var lastSons: AppTab
        get() = prefs.getString(SONS, null)?.let { n -> SONS_TABS.firstOrNull { it.name == n } } ?: AppTab.Rythme
        set(v) { if (v in SONS_TABS) prefs.edit().putString(SONS, v.name).apply() }

    /** The Structure space shows the parts' list (true) rather than the timeline. */
    var partsView: Boolean
        get() = prefs.getBoolean(PARTS, false)
        set(v) { prefs.edit().putBoolean(PARTS, v).apply() }

    companion object {
        const val PREFS = "nav"
        private const val SONS = "sons"
        private const val PARTS = "parts_view"
    }
}

/** One entry of the bottom bar (or the rail): [key] names its testTag (« tab_<key> »). */
class NavEntry(val key: String, val label: String, val hint: String, val icon: ImageVector, val selected: Boolean, val onClick: () -> Unit)

/**
 * The bottom bar: four or five entries, each with its word (never under 11 sp, DESIGN.md); a very
 * narrow bar (a big text size) keeps only the chosen one's word.
 */
@Composable
fun SpaceBar(entries: List<NavEntry>) = BoxWithConstraints {
    val perTab = maxWidth / entries.size.coerceAtLeast(1)
    val tight = perTab < 64.dp
    val iconsOnly = perTab < 52.dp
    NavigationBar(
        containerColor = lerp(Neon.Background, Color.Black, 0.12f),
        modifier = Modifier.drawBehind { drawLine(Color.White.copy(alpha = 0.07f), Offset.Zero, Offset(size.width, 0f), 1f) }.testTag("space_bar"),
    ) {
        entries.forEach { e ->
            NavigationBarItem(
                selected = e.selected,
                onClick = e.onClick,
                icon = { Icon(e.icon, contentDescription = if (iconsOnly) e.label else null) },
                alwaysShowLabel = !iconsOnly,
                label = {
                    // Narrow (a big text size, long Dutch words): two short lines rather than « Mijn num… ».
                    Text(
                        e.label, maxLines = 2, softWrap = true, overflow = TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = if (tight) MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 0.sp, lineHeight = 12.sp)
                        else MaterialTheme.typography.labelMedium.copy(lineHeight = 14.sp),
                    )
                },
                modifier = Modifier.testTag("tab_${e.key}").spot("tab_${e.key}", e.hint),
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Neon.OnAccent,
                    selectedTextColor = Neon.Cyan,
                    indicatorColor = Neon.Cyan,
                    unselectedIconColor = Neon.TextSecondary,
                    unselectedTextColor = Neon.TextSecondary,
                ),
            )
        }
    }
}

/** The same entries on the side, lying down (the screen keeps its height); it scrolls if the phone is very low. */
@Composable
fun SpaceRail(entries: List<NavEntry>) {
    NavigationRail(containerColor = lerp(Neon.Background, Color.Black, 0.12f), modifier = Modifier.testTag("space_rail")) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            entries.forEach { e ->
                NavigationRailItem(
                    selected = e.selected,
                    onClick = e.onClick,
                    icon = { Icon(e.icon, contentDescription = null) },
                    label = { Text(e.label, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.testTag("tab_${e.key}").spot("tab_${e.key}", e.hint),
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = Neon.OnAccent,
                        selectedTextColor = Neon.Cyan,
                        indicatorColor = Neon.Cyan,
                        unselectedIconColor = Neon.TextSecondary,
                        unselectedTextColor = Neon.TextSecondary,
                    ),
                )
            }
        }
    }
}
