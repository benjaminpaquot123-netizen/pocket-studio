package com.pocketstudio.app.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon

/**
 * The assistant's styles as big families that open and close (Techno, House, Bass music, Hip-hop,
 * Autres), all folded at first (except [open]); inside, each family's styles in his order. Shared by the
 * assistant's first step and « Nouveau projet › Partir d'un style ». [tag]: test tags
 * (`<tag>_family_<family>`), [style]: one style's card.
 */
@Composable
fun StyleFamilies(
    tag: String, style: @Composable (StarterWizard.Style) -> Unit, open: Set<String> = emptySet(),
    /** Phase 36, Mode simple: « Calme (Chill, Lo-fi) » first, open. */
    calm: Boolean = false,
) {
    val families = (if (calm) listOf(StarterWizard.CALM_FAMILY) else emptyList()) + StarterWizard.FAMILIES
    // Saved as a list (a set is not saveable as is).
    var opened by rememberSaveable { mutableStateOf(families.filter { it in open || (calm && it == StarterWizard.CALM_FAMILY) }) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        families.forEach { family ->
            val styles = if (family == StarterWizard.CALM_FAMILY) StarterWizard.calmStyles() else StarterWizard.stylesOf(family)
            if (styles.isEmpty()) return@forEach
            val isOpen = family in opened
            FamilyHeader(family, styles.size, isOpen, Modifier.testTag(if (family == StarterWizard.CALM_FAMILY) "${tag}_family_calm" else "${tag}_family_$family")) {
                opened = if (isOpen) opened - family else families.filter { it in opened || it == family }
            }
            AnimatedVisibility(isOpen, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { styles.forEach { style(it) } }
            }
        }
    }
}

/** A family's icon and colour (mid tones, readable on the dark and the light panels), apart from the accent and the "playing" green. */
internal fun familyLook(family: String): Pair<ImageVector, Color> = when (family) {
    "Techno" -> Icons.Filled.Bolt to Color(0xFF5BB8F5)
    "House" -> Icons.Filled.Album to Color(0xFFFFB74D)
    "Bass music" -> Icons.Filled.Speaker to Color(0xFFB388FF)
    "Hip-hop" -> Icons.Filled.Mic to Color(0xFFF06292)
    StarterWizard.CALM_FAMILY -> Icons.Filled.NightsStay to Color(0xFFB39DDB)
    else -> Icons.Filled.MusicNote to Color(0xFF80CBC4)
}

/** A family's header: its icon, name and how many styles, the whole width a target that opens or closes it. */
@Composable
private fun FamilyHeader(family: String, count: Int, open: Boolean, modifier: Modifier, toggle: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val (icon, color) = familyLook(family)
    Row(
        modifier.fillMaxWidth().heightIn(min = 56.dp).clip(shape)
            .background(if (open) color.copy(alpha = 0.16f) else Neon.SurfaceHigh)
            .border(1.dp, if (open) color.copy(alpha = 0.7f) else Neon.Outline, shape)
            .clickable(role = Role.Button, onClick = toggle)
            .semantics { stateDescription = if (open) tr("ouvert") else tr("fermé") }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = if (open) 0.35f else 0.22f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        }
        Text(if (family == StarterWizard.CALM_FAMILY) tr("Calme (Chill, Lo-fi)") else tr(family), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Neon.TextPrimary,
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 12.dp))
        Text(tr("%d styles", count), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, maxLines = 1, modifier = Modifier.padding(start = 8.dp, end = 4.dp))
        Icon(if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null, tint = if (open) color else Neon.TextSecondary)
    }
}
