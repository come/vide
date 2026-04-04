# vide/ screenshot tool — captures all screens via ADB
# Usage: .\capture.ps1 [home|search|notifs|all]
# Requires: adb in PATH, device connected, vide/ as active launcher

param([string]$Screen = "all")

$Dir = Split-Path -Parent $MyInvocation.MyCommand.Path
$Delay = 1.5

# Get screen resolution
$size = adb shell wm size | Select-String '\d+x\d+' | ForEach-Object { $_.Matches.Value }
$W, $H = $size -split 'x' | ForEach-Object { [int]$_ }

# Get density (dpi)
$dpi = [int]((adb shell wm density | Select-String '\d+' | ForEach-Object { $_.Matches.Value }) | Select-Object -First 1)
$density = $dpi / 160.0

Write-Host "Device: ${W}x${H} @ ${dpi}dpi (density=$density)"

# Convert dp to px
function Dp($dp) { [int]($dp * $density) }

# Bottom bar layout (from code):
# - windowInsetsPadding(navigationBars) pushes bar up ~48dp
# - bar height = 56dp, padding horizontal = 24dp
# - 3 icons (48dp each) arranged End in the row
$navBarH   = Dp 48
$barH      = Dp 56
$padH      = Dp 24
$iconSize  = Dp 48

# Center Y of the bottom bar
$barTopY   = $H - $navBarH - $barH
$centerY   = $barTopY + ($barH / 2)

# Icons X (right-aligned): back | notif | search (from right)
$backX     = $W - $padH - ($iconSize / 2)
$notifX    = $backX - $iconSize
$searchX   = $notifX - $iconSize

Write-Host "Tap targets: search=($searchX,$centerY) notif=($notifX,$centerY) back=($backX,$centerY)"

function Snap($name) {
    Start-Sleep -Seconds $Delay
    adb shell screencap -p /sdcard/vide_screen.png
    adb pull /sdcard/vide_screen.png "$Dir\$name.png" | Out-Null
    adb shell rm /sdcard/vide_screen.png
    Write-Host "  -> $name.png"
}

function GoHome {
    adb shell input keyevent KEYCODE_HOME
    Start-Sleep -Milliseconds 800
}

function CaptureHome {
    Write-Host "Home screen..."
    GoHome
    Snap "01_home"
}

function CaptureSearch {
    Write-Host "Search screen..."
    GoHome
    adb shell input tap $searchX $centerY
    Start-Sleep -Milliseconds 800
    adb shell input text "test%s"
    Snap "02_search"
    # Back
    adb shell input keyevent KEYCODE_BACK
}

function CaptureSearchContact {
    Write-Host "Search + contact expand..."
    GoHome
    adb shell input tap $searchX $centerY
    Start-Sleep -Milliseconds 800
    adb shell input text "test%s"
    Start-Sleep -Milliseconds 500
    # Tap 1st contact (2nd list item, after google search row)
    # statusbar(~48) + searchField(24+20+16=60) + divider(1) + listPadTop(32) + googleRow(~44) + contactCenter(20) = ~205dp
    $contactY = Dp 205
    adb shell input tap ($W / 2) $contactY
    Snap "02b_search_contact"
    # Back
    adb shell input keyevent KEYCODE_BACK
}

function CaptureNotifs {
    Write-Host "Notifications screen..."
    GoHome
    adb shell input tap $notifX $centerY
    Snap "03_notifications"
    # Back
    adb shell input keyevent KEYCODE_BACK
}

switch ($Screen) {
    "home"   { CaptureHome }
    "search" { CaptureSearch }
    "notifs" { CaptureNotifs }
    "contact" { CaptureSearchContact }
    "all" {
        CaptureHome
        CaptureSearch
        CaptureSearchContact
        CaptureNotifs
        Write-Host "Done! Screenshots in $Dir\"
    }
    default {
        Write-Host "Usage: .\capture.ps1 [home|search|notifs|all]"
    }
}
