package com.vide.data

import android.content.Context
import android.provider.CalendarContract
import com.vide.model.CalendarEvent
import java.util.Calendar

object CalendarReader {

    fun getEvents(context: Context): List<CalendarEvent> {
        val ownCalendarIds = getOwnCalendarIds(context)
        if (ownCalendarIds.isEmpty()) return emptyList()

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        val endRange = System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000

        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(startOfDay.toString())
            .appendPath(endRange.toString())
            .build()

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY
        )

        val placeholders = ownCalendarIds.joinToString(",") { "?" }
        val selection = "${CalendarContract.Instances.CALENDAR_ID} IN ($placeholders)" +
            " AND ${CalendarContract.Instances.SELF_ATTENDEE_STATUS} != ${CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED}"
        val selectionArgs = ownCalendarIds.map { it.toString() }.toTypedArray()

        val events = mutableListOf<CalendarEvent>()
        try {
            context.contentResolver.query(
                uri, projection, selection, selectionArgs,
                "${CalendarContract.Instances.BEGIN} ASC"
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val title = cursor.getString(1)
                    if (title.isNullOrBlank()) continue
                    events.add(
                        CalendarEvent(
                            id = cursor.getLong(0),
                            title = title,
                            startTime = cursor.getLong(2),
                            endTime = cursor.getLong(3),
                            allDay = cursor.getInt(4) == 1
                        )
                    )
                }
            }
        } catch (_: SecurityException) {}

        return events
    }

    private fun getOwnCalendarIds(context: Context): Set<Long> {
        val ids = mutableSetOf<Long>()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.OWNER_ACCOUNT
        )

        try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                "${CalendarContract.Calendars.ACCOUNT_NAME} = ${CalendarContract.Calendars.OWNER_ACCOUNT}",
                null, null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    ids.add(cursor.getLong(0))
                }
            }
        } catch (_: SecurityException) {}

        return ids
    }
}
