@file:OptIn(ExperimentalFoundationApi::class)

package com.vide.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Composable
fun AppItem(
    label: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    modifier: Modifier = Modifier
) {
    Text(
        text = label,
        style = textStyle,
        color = textColor,
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 10.dp)
    )
}
