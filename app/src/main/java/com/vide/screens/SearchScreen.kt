package com.vide.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vide.components.AppItem
import com.vide.model.AppInfo
import com.vide.model.ContactAction
import com.vide.model.ContactActionType
import com.vide.ui.theme.InterFontFamily

@Composable
fun SearchScreen(
    query: String,
    placeholder: String = "",
    apps: List<AppInfo>,
    contactActions: List<ContactAction>,
    onQueryChange: (String) -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onContactAction: (ContactAction) -> Unit,
    onGoogleSearch: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val searchTextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 20.sp,
        fontWeight = FontWeight.Light,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    val appItemStyle = MaterialTheme.typography.bodyLarge.copy(
        fontWeight = FontWeight.Light,
        letterSpacing = (-0.5).sp
    )

    var expandedContact by remember { mutableStateOf<String?>(null) }
    val groupedContacts = remember(contactActions) {
        contactActions.groupBy { it.contactName }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 24.dp)
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            textStyle = searchTextStyle.copy(
                color = MaterialTheme.colorScheme.onBackground
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 16.dp)
                .focusRequester(focusRequester),
            decorationBox = { innerTextField ->
                Box {
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder.ifEmpty { "rechercher..." },
                            style = searchTextStyle
                        )
                    }
                    innerTextField()
                }
            },
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground)
        )

        @Suppress("DEPRECATION")
        Divider(
            color = MaterialTheme.colorScheme.onBackground,
            thickness = 1.dp
        )

        LazyColumn(
            modifier = Modifier.padding(top = 32.dp)
        ) {
            // Google search
            if (query.isNotBlank()) {
                item(key = "google_search") {
                    Text(
                        text = "rechercher \"$query\" sur google",
                        style = appItemStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onGoogleSearch(query) }
                            )
                            .padding(vertical = 10.dp)
                    )
                }
            }

            // Contact results (grouped by name)
            groupedContacts.forEach { (name, actions) ->
                val isExpanded = expandedContact == name
                val hasCall = actions.any { it.actionType == ContactActionType.CALL }
                val hasChat = actions.any { it.actionType == ContactActionType.SMS || it.actionType == ContactActionType.WHATSAPP }
                val hasEmail = actions.any { it.actionType == ContactActionType.EMAIL }

                item(key = "contact_$name") {
                    Column {
                        // Contact row: name + indicator icons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        expandedContact = if (isExpanded) null else name
                                    }
                                )
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = name,
                                style = appItemStyle,
                                color = if (isExpanded)
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                else
                                    MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.weight(1f)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (hasCall) {
                                    Icon(
                                        imageVector = Icons.Filled.Phone,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                if (hasChat) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                if (hasEmail) {
                                    Icon(
                                        imageVector = Icons.Filled.Email,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Expanded actions
                        if (isExpanded) {
                            actions.forEach { action ->
                                Text(
                                    text = "— ${action.actionLabel}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = {
                                                onContactAction(action)
                                                expandedContact = null
                                            }
                                        )
                                        .padding(start = 16.dp, top = 6.dp, bottom = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // App results
            items(apps, key = { it.packageName }) { app ->
                AppItem(
                    label = app.label,
                    onClick = { onAppClick(app) },
                    onLongClick = { onAppLongClick(app) },
                    textStyle = appItemStyle
                )
            }
        }
    }
}
