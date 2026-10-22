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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon

/**
 * The assistant's styles as big families that open and close (Techno, House, Bass music, Hip-hop,
 * Autres), Benjamin's open at first; inside, each family's styles in his order. Shared by the
 * assistant's first step and « Nouveau projet › Partir d'un style ». [tag]: test tags
 * (`<tag>_family_<family>`), [style]: one style's card.
 */
@Composable
fun StyleFamilies(tag: String, style: @Composable (StarterWizard.Style) -> Unit, open: Set<String> = StarterWizard.OPEN_FAMILIES) {
    // Saved as a list (a set is not saveable as is).
    var opened by rememberSaveable { mutableStateOf(StarterWizard.FAMILIES.filter { it in open }) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StarterWizard.FAMILIES.forEach { family ->
            val styles = StarterWizard.stylesOf(family)
            if (styles.isEmpty()) return@forEach
            val isOpen = family in opened
            FamilyHeader(family, styles.size, isOpen, Modifier.testTag("${tag}_family_$family")) {
                opened = if (isOpen) opened - family else StarterWizard.FAMILIES.filter { it in opened || it == family }
            }
            AnimatedVisibility(isOpen, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { styles.forEach { style(it) } }
            }
        }
    }
}

/** A family's header: its name and how many styles, the whole width a target that opens or closes it. */
@Composable
private fun FamilyHeader(family: String, count: Int, open: Boolean, modifier: Modifier, toggle: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier.fillMaxWidth().heightIn(min = 52.dp).clip(shape)
            .background(if (open) Neon.Cyan.copy(alpha = 0.14f) else Neon.SurfaceHigh)
            .border(1.dp, if (open) Neon.Cyan.copy(alpha = 0.6f) else Neon.Outline, shape)
            .clickable(role = Role.Button, onClick = toggle)
            .semantics { stateDescription = if (open) tr("ouvert") else tr("fermé") }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(tr(family), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            color = if (open) Neon.Cyan else Neon.TextPrimary, modifier = Modifier.weight(1f))
        Text(tr("%d styles", count), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, modifier = Modifier.padding(end = 6.dp))
        Icon(if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null, tint = if (open) Neon.Cyan else Neon.TextSecondary)
    }
}
