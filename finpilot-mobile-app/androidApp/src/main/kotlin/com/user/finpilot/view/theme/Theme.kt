package com.user.finpilot.view.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val FinPilotPurple = Color(0xFF6C5CE7)
private val FinPilotPurpleLight = Color(0xFFEDE9FE)
private val FinPilotGreen = Color(0xFF2E7D32)
private val FinPilotBackground = Color(0xFFFBF7FF)
private val FinPilotSurfaceVariant = Color(0xFFE9E3F5)

private val LightColors = lightColorScheme(
    primary = FinPilotPurple,
    primaryContainer = FinPilotPurpleLight,
    onPrimaryContainer = FinPilotPurple,
    secondary = FinPilotGreen,
    background = FinPilotBackground,
    surface = Color.White,
    surfaceVariant = FinPilotSurfaceVariant,
)

@Composable
fun FinPilotTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography(),
        shapes = Shapes(
            small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        ),
        content = content,
    )
}