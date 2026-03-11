package com.vide.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vide.model.CalendarEvent
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ClockWidget(
    events: List<CalendarEvent> = emptyList(),
    onEventClick: (CalendarEvent) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1000)
        }
    }

    val time = now.format(DateTimeFormatter.ofPattern("HH:mm"))
    val date = now.format(
        DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.FRENCH)
    ).uppercase()

    Column(modifier = modifier) {
        Text(
            text = time,
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        val listState = rememberLazyListState()
        val scope = rememberCoroutineScope()

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = date,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = if (events.isNotEmpty()) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        val idx = events
                            .indexOfFirst { it.endTime > System.currentTimeMillis() }
                            .coerceAtLeast(0)
                        scope.launch { listState.animateScrollToItem(idx) }
                    }
                ) else Modifier
            )

            if (events.isNotEmpty()) {
                Spacer(modifier = Modifier.weight(1f))

                val nowMillis = System.currentTimeMillis()
                val firstUpcomingIndex = events
                    .indexOfFirst { it.endTime > nowMillis }
                    .coerceAtLeast(0)

                LaunchedEffect(events) {
                    listState.scrollToItem(firstUpcomingIndex)
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .heightIn(max = 54.dp)
                        .padding(start = 16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    items(events, key = { "${it.id}_${it.startTime}" }) { event ->
                        val isPast = event.endTime < nowMillis
                        Text(
                            text = formatEvent(event, now),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = if (isPast) 0.4f else 1f
                            ),
                            textAlign = TextAlign.End,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onEventClick(event) }
                                )
                                .padding(vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatEvent(event: CalendarEvent, now: LocalDateTime): String {
    val today = now.toLocalDate()
    val eventDateTime = Instant.ofEpochMilli(event.startTime)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
    val eventDate = eventDateTime.toLocalDate()

    val datePrefix = when {
        eventDate == today -> ""
        eventDate == today.plusDays(1) -> "DEM. "
        eventDate.isBefore(today.plusDays(7)) -> eventDate.format(
            DateTimeFormatter.ofPattern("EEE", Locale.FRENCH)
        ).uppercase() + ". "
        else -> eventDate.format(
            DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH)
        ).uppercase() + " "
    }

    val timeStr = if (event.allDay) "" else eventDateTime.format(
        DateTimeFormatter.ofPattern("HH:mm")
    )

    return buildString {
        append(datePrefix)
        if (timeStr.isNotEmpty()) append("$timeStr ")
        append(event.title.uppercase())
    }.trim()
}
