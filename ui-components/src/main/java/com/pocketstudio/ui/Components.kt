package com.pocketstudio.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Dark card with a thin neon outline, the basic container of every screen. */
@Composable
fun NeonCard(
    modifier: Modifier = Modifier,
    accent: Color = Neon.Outline,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Neon.Surface),
        border = BorderStroke(1.dp, accent),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

/** A card's title in small capitals; with [help], a faded "?" after it explains the term. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = Neon.Cyan, help: Help? = null) {
    if (help == null) {
        Text(text.uppercase(), modifier = modifier, style = MaterialTheme.typography.labelSmall, color = color)
    } else {
        WithHelp(help, modifier) { Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = color) }
    }
}

/** Label on the left, value on the right; used for technical readouts. */
@Composable
fun StatRow(label: String, value: String, valueColor: Color = Neon.TextPrimary) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary)
        Text(
            value,
            modifier = Modifier.weight(1f),
            style = MonoStyle,
            color = valueColor,
            textAlign = TextAlign.End,
        )
    }
}

/** Horizontal level meter, green to amber to red. [level] is linear 0..1. */
@Composable
fun LevelMeter(level: Float, modifier: Modifier = Modifier) {
    val track = Neon.SurfaceHigh
    val low = Neon.Acid
    val mid = Neon.Amber
    val high = Neon.Red
    Canvas(modifier = modifier.fillMaxWidth().height(10.dp)) {
        val radius = CornerRadius(size.height / 2, size.height / 2)
        drawRoundRect(color = track, cornerRadius = radius)
        val width = size.width * level.coerceIn(0f, 1f)
        if (width > 0f) {
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    0f to low, 0.7f to mid, 1f to high,
                    startX = 0f, endX = size.width,
                ),
                topLeft = Offset.Zero,
                size = Size(width, size.height),
                cornerRadius = radius,
            )
        }
    }
}
