package com.pocketstudio.app.rhythm

import androidx.compose.ui.graphics.Color

/** Pad names (French, short enough for a pad) and colours, in DrumKit voice order. */
object DrumKitInfo {
    val names = listOf(
        "Kick", "Kick 2", "Caisse", "Caisse 2",
        "Clap", "Charley", "Charley ouvert", "Tom grave",
        "Tom médium", "Tom aigu", "Rim", "Cloche",
        "Cymbale", "Shaker", "Conga", "Zap",
    )

    /** One colour family per instrument group, like the coloured channels of FL Studio. */
    val colors = listOf(
        Color(0xFFFF8A65), Color(0xFFFF8A65), Color(0xFFFFD54F), Color(0xFFFFD54F),
        Color(0xFFFFD54F), Color(0xFF81D4FA), Color(0xFF81D4FA), Color(0xFFA5D6A7),
        Color(0xFFA5D6A7), Color(0xFFA5D6A7), Color(0xFFCE93D8), Color(0xFFCE93D8),
        Color(0xFF81D4FA), Color(0xFFCE93D8), Color(0xFFA5D6A7), Color(0xFFF48FB1),
    )

    /** A ready-to-play groove so the first Play makes music, not silence. */
    val starterHits: Map<Int, List<Int>> = mapOf(
        0 to listOf(0, 4, 8, 12),
        2 to listOf(4, 12),
        5 to listOf(0, 2, 4, 6, 8, 10, 12, 14),
        6 to listOf(14),
    )
}
