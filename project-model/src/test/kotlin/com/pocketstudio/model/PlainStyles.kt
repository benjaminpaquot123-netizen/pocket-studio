package com.pocketstudio.model

import com.pocketstudio.model.StarterWizard.Role

/**
 * A style stripped of every format v2 field (lot gr-l1b): the new roles' lines, layers, kit sounds, the vocal
 * sample, the loop, the mix, the body, the doubled chords and the texture. The tests that pin the old behaviour
 * run on it, so each styles' team can convert its recipes without breaking them.
 */
internal object PlainStyles {
    val V2_ROLES = setOf(Role.Snap, Role.Rim, Role.Conga, Role.Shaker)

    fun plain(style: StarterWizard.Style) = style.copy(
        lines = style.lines.filter { it.role !in V2_ROLES }.map { it.copy(layers = emptyList()) },
        kitSounds = emptyMap(), vocalSample = null, loop = null, mix = null, body = false,
        melody = style.melody.copy(chordDouble = null, texture = null),
    )

    fun plain(id: String) = plain(StarterWizard.style(id)!!)
}
