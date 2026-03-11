package com.vide.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.vide.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val interFont = GoogleFont("Inter")

val InterFontFamily = FontFamily(
    Font(googleFont = interFont, fontProvider = provider, weight = FontWeight.Light),
    Font(googleFont = interFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = interFont, fontProvider = provider, weight = FontWeight.Medium),
)

val VideTypography = Typography(
    // Clock time: 72sp Light
    displayLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Light,
        fontSize = 72.sp,
        letterSpacing = (-2).sp
    ),
    // Home screen app name: 22sp Regular
    titleLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp
    ),
    // App name in search list: 18sp Regular
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp
    ),
    // Date below clock: 16sp Regular, wide tracking, uppercase
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 1.6.sp
    ),
    // Secondary label: 14sp Regular
    bodySmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    // Notification app name / timestamps: 13sp
    labelSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp
    ),
)
