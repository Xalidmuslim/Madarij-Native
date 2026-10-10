#!/usr/bin/env bash
set -euo pipefail
mkdir -p performance-results
APK="app/build/outputs/apk/release/app-release.apk"
PKG="ru.madarij.nativeapp.readerpolish"
ACT="ru.madarij.nativeapp.MainActivity"
test -s "$APK"
adb shell wm size 432x960
adb shell wm density 160
adb install -r "$APK"
adb shell input keyevent KEYCODE_HOME
{
  echo "Android emulator: Pixel 6, Android API 35"
  echo "Build: optimized release; R8 mapping verified in CI"
  echo "Cold process launch: force-stop, then am start -W"
} > performance-results/measurements.txt
for n in 1 2 3; do
  adb shell am force-stop "$PKG"
  sleep 2
  echo "---- Cold start $n ----" >> performance-results/measurements.txt
  adb shell am start -W -n "$PKG/$ACT" >> performance-results/measurements.txt
  sleep 3
done
adb shell dumpsys gfxinfo "$PKG" reset >/dev/null
echo "---- HOME scroll ----" >> performance-results/measurements.txt
adb shell input swipe 220 765 220 290 400
adb shell input swipe 220 300 220 770 400
echo "---- CONTENTS tap and scroll ----" >> performance-results/measurements.txt
adb shell input tap 130 867
sleep 2
adb shell input swipe 210 780 210 300 500
adb shell input swipe 210 310 210 780 500
echo "---- READER tap and scroll ----" >> performance-results/measurements.txt
adb shell input tap 358 532
sleep 2
adb shell input swipe 230 785 230 330 500
adb shell input swipe 230 350 230 785 500
adb shell input keyevent KEYCODE_BACK
sleep 1
adb shell input keyevent KEYCODE_BACK
sleep 1
adb shell screencap -p /sdcard/performance-final.png
adb pull /sdcard/performance-final.png performance-results/final.png >/dev/null
adb shell dumpsys gfxinfo "$PKG" > performance-results/gfxinfo.txt
adb shell dumpsys gfxinfo "$PKG" framestats > performance-results/framestats.txt
grep -E "Total frames rendered|Janky frames|50th percentile|90th percentile|95th percentile|99th percentile" performance-results/gfxinfo.txt >> performance-results/measurements.txt || true
adb logcat -d -t 1500 | grep -E 'FATAL EXCEPTION|ANR in ru.madarij.nativeapp' > performance-results/crash-log.txt || true
if [ -s performance-results/crash-log.txt ]; then
  echo "Crash or ANR detected" >&2
  cat performance-results/crash-log.txt
  exit 1
fi
cat performance-results/measurements.txt
