#!/bin/bash
# vide/ screenshot tool — captures all screens via ADB
# Usage: ./capture.sh [home|search|notifs|all]
# Requires: adb in PATH, device connected, vide/ as active launcher

set -e

DIR="$(cd "$(dirname "$0")" && pwd)"
DELAY=1.5  # seconds between actions

# Get screen resolution for tap coordinates
read WIDTH HEIGHT <<< $(adb shell wm size | grep -oP '\d+x\d+' | tr 'x' ' ')
echo "Device: ${WIDTH}x${HEIGHT}"

# Bottom bar tap targets (relative positions)
BOTTOM_Y=$((HEIGHT - 60))
SEARCH_X=$((WIDTH * 40 / 100))
NOTIF_X=$((WIDTH * 53 / 100))
BACK_X=$((WIDTH * 66 / 100))

snap() {
    local name="$1"
    sleep "$DELAY"
    adb shell screencap -p /sdcard/vide_screen.png
    adb pull /sdcard/vide_screen.png "$DIR/${name}.png" > /dev/null
    adb shell rm /sdcard/vide_screen.png
    echo "  -> $name.png"
}

go_home() {
    # Press home button to get back to vide/ launcher
    adb shell input keyevent KEYCODE_HOME
    sleep 0.5
}

capture_home() {
    echo "Home screen..."
    go_home
    snap "01_home"
}

capture_search() {
    echo "Search screen..."
    go_home
    adb shell input tap "$SEARCH_X" "$BOTTOM_Y"
    sleep 0.5
    # Type a sample query
    adb shell input text "spo"
    snap "02_search"
    # Back to home
    adb shell input tap "$BACK_X" "$BOTTOM_Y"
}

capture_notifs() {
    echo "Notifications screen..."
    go_home
    adb shell input tap "$NOTIF_X" "$BOTTOM_Y"
    snap "03_notifications"
    # Back to home
    adb shell input tap "$BACK_X" "$BOTTOM_Y"
}

case "${1:-all}" in
    home)   capture_home ;;
    search) capture_search ;;
    notifs) capture_notifs ;;
    all)
        capture_home
        capture_search
        capture_notifs
        echo "Done! Screenshots in $DIR/"
        ;;
    *)
        echo "Usage: $0 [home|search|notifs|all]"
        exit 1
        ;;
esac
