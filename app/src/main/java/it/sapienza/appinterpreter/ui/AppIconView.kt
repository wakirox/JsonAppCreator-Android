package it.sapienza.appinterpreter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.sapienza.appinterpreter.utils.AppIcon

/** A JSON app's icon as a circle of [size], drawn like its home-screen shortcut (see [AppIcon]). */
@Composable
fun AppIconView(name: String, icon: String?, size: Dp = 40.dp) {
    val emoji = AppIcon.emoji(icon)
    // The line height follows the text size: the theme's 24 sp would push the letter out of a small tile.
    val textSize = with(LocalDensity.current) { (size * if (emoji != null) 0.5f else 0.36f).toSp() }
    Box(
        Modifier.size(size).clip(CircleShape)
            .background(Color(if (emoji != null) AppIcon.softColor(name) else AppIcon.CREAM)),
        contentAlignment = Alignment.Center
    ) {
        if (emoji != null) {
            Text(emoji, fontSize = textSize, lineHeight = textSize)
        } else {
            Box(
                Modifier.size(
                    size * 0.62f
                ).clip(RoundedCornerShape(size * 0.18f)).background(Color(AppIcon.tileColor(name))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    AppIcon.letter(name),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = textSize,
                    lineHeight = textSize
                )
            }
        }
    }
}
