package com.vide.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.vide.model.NotificationInfo
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotificationItem(
    notification: NotificationInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val screenWidth = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val threshold = screenWidth * 0.3f
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val progress = (offsetX.value / threshold).coerceIn(0f, 1f)
    val itemAlpha = 1f - (progress * 0.6f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            .alpha(itemAlpha)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (offsetX.value > threshold) {
                                offsetX.animateTo(screenWidth, tween(200))
                                onDismiss()
                            } else {
                                offsetX.animateTo(0f, spring(dampingRatio = 0.6f))
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch { offsetX.animateTo(0f, spring()) }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        scope.launch {
                            offsetX.snapTo((offsetX.value + dragAmount).coerceAtLeast(0f))
                        }
                    }
                )
            }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 10.dp)
    ) {
        Text(
            text = buildString {
                if (notification.isImportant) append("● ")
                append(notification.appName)
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (notification.isImportant)
                MaterialTheme.colorScheme.onBackground
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = notification.content,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (notification.isImportant) FontWeight.Medium else FontWeight.Normal
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
        )
        Text(
            text = formatTimestamp(notification.timestamp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / 60_000
    val hours = minutes / 60
    return when {
        minutes < 1 -> "À l'instant"
        minutes < 60 -> "Il y a $minutes min"
        hours < 24 -> "Il y a ${hours}h"
        else -> "Il y a ${hours / 24}j"
    }
}
