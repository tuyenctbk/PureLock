package com.example.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Custom Modifier extension for Android TV DPAD focusable elements.
 * Provides smooth scale transitions and vibrant focus border rings for 10-foot TV UI ergonomics.
 */
@Composable
fun Modifier.dpadFocusable(
    shape: Shape = RoundedCornerShape(12.dp),
    focusedBorderColor: Color = MaterialTheme.colorScheme.primary,
    focusedScale: Float = 1.05f,
    borderWidth: Dp = 2.5.dp
): Modifier {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) focusedScale else 1.0f,
        animationSpec = tween(durationMillis = 180),
        label = "DpadFocusScale"
    )

    return this
        .onFocusChanged { isFocused = it.isFocused }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .border(
            width = if (isFocused) borderWidth else 0.dp,
            color = if (isFocused) focusedBorderColor else Color.Transparent,
            shape = shape
        )
}
