package com.pocketstudio.app.sounds

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FrontHand
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Texture
import androidx.compose.material.icons.filled.TripOrigin
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Waves
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.pocketstudio.model.AcidSound
import com.pocketstudio.model.PlaitsPatch
import com.pocketstudio.model.SynthPatch
import kotlin.math.roundToInt

/** The icon and colour of a family of sounds: the same family looks the same on every screen. */
data class SoundLook(val icon: ImageVector, val color: Color)

object SoundLooks {
    private val BASS = Color(0xFFFF8A65)
    private val LEAD = Color(0xFFFFCA28)
    private val PAD = Color(0xFFBA68C8)
    private val PLUCK = Color(0xFF4DB6AC)
    private val KEYS = Color(0xFF4FC3F7)
    private val FX = Color(0xFFF06292)
    private val DRUMS = Color(0xFF81C784)
    private val VOICE = Color(0xFF9575CD)
    private val WIND = Color(0xFF90A4AE)

    /** By the family's name as the app shows it (synth library, Plaits, General MIDI, packs). */
    fun of(family: String): SoundLook = when (family.lowercase().trim()) {
        "basses", "basse", "bass" -> SoundLook(Icons.Filled.Waves, BASS)
        "basses 808", "808" -> SoundLook(Icons.Filled.Speaker, BASS)
        "leads", "lead", "synthé lead" -> SoundLook(Icons.Filled.Bolt, LEAD)
        "stabs et hoovers", "stab" -> SoundLook(Icons.Filled.FlashOn, LEAD)
        "nappes", "pad", "synthé nappe" -> SoundLook(Icons.Filled.Cloud, PAD)
        "pluck", "plucks" -> SoundLook(Icons.Filled.Grain, PLUCK)
        "claviers", "keys", "pianos", "orgues" -> SoundLook(Icons.Filled.Piano, KEYS)
        "percussions mélodiques" -> SoundLook(Icons.Filled.Notifications, KEYS)
        "effets", "fx", "effets synthé", "bruitages" -> SoundLook(Icons.Filled.AutoAwesome, FX)
        "voix", "vocal" -> SoundLook(Icons.Filled.RecordVoiceOver, VOICE)
        "batterie", "kits", "kits de batterie", "kicks", "kick" -> SoundLook(Icons.Filled.Adjust, DRUMS)
        "caisses claires", "snare" -> SoundLook(Icons.Filled.TripOrigin, DRUMS)
        "claps", "clap" -> SoundLook(Icons.Filled.FrontHand, DRUMS)
        "charleys fermés", "charleys ouverts", "hat", "open_hat" -> SoundLook(Icons.Filled.BlurOn, DRUMS)
        "percussions", "perc", "toms", "tom" -> SoundLook(Icons.Filled.Album, DRUMS)
        "analogiques" -> SoundLook(Icons.Filled.Tune, BASS)
        "fm" -> SoundLook(Icons.Filled.Hub, KEYS)
        "textures" -> SoundLook(Icons.Filled.Texture, PAD)
        "physiques" -> SoundLook(Icons.Filled.Vibration, PLUCK)
        "guitares", "cordes solo", "ensembles" -> SoundLook(Icons.Filled.LibraryMusic, PLUCK)
        "cuivres", "anches", "flûtes" -> SoundLook(Icons.Filled.Air, WIND)
        "ethnique" -> SoundLook(Icons.Filled.Public, VOICE)
        "favoris" -> SoundLook(Icons.Filled.Star, LEAD)
        "échantillon" -> SoundLook(Icons.Filled.GraphicEq, BASS)
        "classiques" -> SoundLook(Icons.Filled.Waves, LEAD)
        "sales" -> SoundLook(Icons.Filled.Bolt, FX)
        "doux" -> SoundLook(Icons.Filled.Cloud, PAD)
        else -> SoundLook(Icons.Filled.MusicNote, KEYS)
    }

    /** A few words on a synth patch: its wave, its unison, how it sounds over time. */
    fun describe(patch: SynthPatch): String {
        val wave = patch.get(SynthPatch.OSC1_WAVE)
        val parts = mutableListOf(if (wave < 0.25f) "Scie" else if (wave < 0.75f) "Carré" else "Triangle")
        val voices = 1 + (patch.get(SynthPatch.UNISON) * 6).roundToInt()
        if (voices > 1) parts += "$voices voix"
        parts += when {
            patch.get(SynthPatch.A_ATTACK) > 0.35f -> "monte doucement"
            patch.get(SynthPatch.A_SUSTAIN) < 0.2f -> "court"
            else -> "tenu"
        }
        return parts.joinToString(" · ")
    }

    fun describe(patch: PlaitsPatch): String = PlaitsPatch.MODELS.getOrElse(patch.model) { "" }

    fun describe(sound: AcidSound): String =
        (if (sound.waveform < 0.5f) "Scie" else "Carré") + if (sound.drive > 0.6f) " · saturé" else if (sound.resonance > 0.75f) " · piquant" else ""

    /** One line about each of Plaits' 24 models, in PlaitsPatch.MODELS order. */
    val PLAITS_HINTS = listOf(
        "Deux oscillateurs et un filtre", "Rond ou mordant, façon Casio CZ", "Les sons du DX7, série 1",
        "Les sons du DX7, série 2", "Les sons du DX7, série 3", "Une onde qui se promène",
        "Cordes des années 70", "Les jeux vidéo 8 bits", "Le synthé classique", "Doux à très saturé",
        "Cloches et métal", "Des voyelles qui chantent", "Orgue et harmoniques", "Sons numériques des années 80",
        "Un accord entier par touche", "Un robot qui parle", "Un essaim de scies, très large",
        "Souffle et vent filtrés", "Poussière et crépitements", "Guitare, harpe, cordes pincées",
        "Marimba, cloches, bois", "Kick analogique", "Caisse claire analogique", "Charley analogique",
    )
}
