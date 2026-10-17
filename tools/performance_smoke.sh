#!/usr/bin/env bash
set -Eeuo pipefail
mkdir -p performance-results
# Keep diagnostics if UI Automator is briefly unavailable on SwiftShader CI.
trap 'code=$?; echo "Smoke failed at line $LINENO (exit $code)" >&2;   adb devices -l >&2 || true;   adb logcat -d -t 500 2>/dev/null | grep -E "FATAL EXCEPTION|ANR in|UiAutomation|UiAutomator"     > performance-results/last-errors.txt || true; exit "$code"' ERR
capture_ui() {
  local label="$1"
  local remote="/sdcard/${label}.xml"
  local target="performance-results/${label}.xml"
  for retry in 1 2 3 4; do
    if adb shell uiautomator dump "$remote" > "performance-results/${label}-attempt.log" 2>&1 &&
       adb pull "$remote" "$target" >/dev/null 2>&1 &&
       grep -q '<hierarchy' "$target"; then
      return 0
    fi
    sleep 2
  done
  echo "Accessibility hierarchy capture failed: $label" >&2
  cat "performance-results/${label}-attempt.log" >&2 || true
  return 1
}
capture_screenshot() {
  local label="$1"
  # PNG images are supporting evidence, not a dependency of frame metrics.
  adb exec-out screencap -p > "performance-results/${label}.png" 2>/dev/null ||
    rm -f "performance-results/${label}.png"
}
APK="app/build/outputs/apk/release/app-release.apk"
# Read the actual isolated QA package from the built APK. Never assume
# that an ephemeral CI key can replace an owner's installed application.
PKG=$("$ANDROID_HOME/build-tools/36.0.0/aapt" dump badging "$APK" |
  sed -n "s/^package: name='\([^']*\)'.*/\1/p" | head -1)
test -n "$PKG"
ACT="ru.madarij.nativeapp.MainActivity"
test -s "$APK"
adb shell wm size 432x960
adb shell wm density 160
adb install -r "$APK"
adb shell pm path "$PKG" | grep -q 'package:'
adb shell input keyevent KEYCODE_HOME
{
  echo "Android emulator: Pixel 6, Android API 35"
  echo "Build: optimized release; R8 mapping verified in CI"
  echo "Cold process launch: force-stop, then am start -W"
} > performance-results/measurements.txt
for n in 1 2 3 4 5; do
  adb shell am force-stop "$PKG"
  sleep 2
  echo "---- Cold start $n ----" >> performance-results/measurements.txt
  adb shell am start -W -n "$PKG/$ACT" >> performance-results/measurements.txt
  sleep 3
done
# The navigation tests use the same viewport across all runs. Keep Android
# system animations enabled: an artificially instant UI hides jank.
adb shell dumpsys gfxinfo "$PKG" reset >/dev/null
echo "---- HOME scroll ----" >> performance-results/measurements.txt
adb shell input swipe 220 765 220 290 400
adb shell input swipe 220 300 220 770 400
echo "---- CONTENTS tap and scroll ----" >> performance-results/measurements.txt
# Bottom navigation lies at y=870..934 for the fixed 432x960 viewport.
adb shell input tap 129 906
sleep 3
capture_ui contents
capture_screenshot contents
adb shell input swipe 210 780 210 300 500
adb shell input swipe 210 310 210 780 500
sleep 2
# Resolve a real visible Read action from the Contents accessibility tree.
# A fixed Home coordinate fails when the scroll state is restored.
capture_ui contents-scrolled
python3 - <<'PY'
from xml.etree import ElementTree as ET
import re
import subprocess
import time

def visible_nodes(path):
    root=ET.parse(path).getroot()
    result=[]
    for n in root.iter('node'):
        label=(n.get('text','')+' '+n.get('content-desc','')).strip()
        m=re.fullmatch(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]',n.get('bounds',''))
        if m:
            x1,y1,x2,y2=map(int,m.groups())
            if 170 < y1 < 810:
                result.append((label,y1,(x1+x2)//2,(y1+y2)//2))
    return result

def reads(nodes):
    return [point for point in nodes if 'Читать' in point[0]]

nodes=visible_nodes('performance-results/contents-scrolled.xml')
targets=reads(nodes)
if not targets:
    # New design intentionally starts with semantic sections collapsed.
    # Expand the actual first-volume first title by its UI tree position.
    titles=[p for p in nodes if 'Введение и толкование' in p[0]]
    if not titles:
        raise SystemExit('No first section in Contents: cannot measure Reader')
    _,_,x,y=min(titles,key=lambda p:p[1])
    subprocess.run(['adb','shell','input','tap',str(x),str(y)],check=True)
    time.sleep(2)
    for attempt in range(4):
        result=subprocess.run(['adb','shell','uiautomator','dump',
                    '/sdcard/contents-expanded.xml'],
                    stdout=subprocess.PIPE,stderr=subprocess.STDOUT,
                    text=True,timeout=45)
        pull=subprocess.run(['adb','pull','/sdcard/contents-expanded.xml',
                    'performance-results/contents-expanded.xml'],
                    stdout=subprocess.PIPE,stderr=subprocess.STDOUT,
                    text=True,timeout=30) if result.returncode==0 else None
        if pull is not None and pull.returncode==0:
            break
        time.sleep(2)
    else:
        raise SystemExit('Could not acquire accessibility hierarchy after expanding Contents')
    targets=reads(visible_nodes('performance-results/contents-expanded.xml'))
if not targets:
    raise SystemExit('No visible Read control after opening first semantic section')
_,_,x,y=min(targets,key=lambda p:p[1])
open('performance-results/reader-target.txt','w').write(f'{x} {y}\n')
print('Reading from expanded Contents at',x,y)
PY
read -r READ_X READ_Y < performance-results/reader-target.txt
echo "---- READER tap and scroll ----" >> performance-results/measurements.txt
adb shell input tap "$READ_X" "$READ_Y"
sleep 4
capture_ui reader
capture_screenshot reader
# If either screen failed to navigate, abort rather than report invalid
# scrolling metrics as proof of Reader performance.
python3 - <<'PY'
from xml.etree import ElementTree as ET
import re
def nodes(path):
    root=ET.parse(path).getroot()
    for item in root.iter('node'):
        label=(item.get('text','')+' '+item.get('content-desc','')).strip()
        bounds=item.get('bounds','')
        m=re.search(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]',bounds)
        if m:
            yield (label,int(m.group(2)))
contents=list(nodes('performance-results/contents.xml'))
reader=list(nodes('performance-results/reader.xml'))
print('Contents header:',[(s,y) for s,y in contents if y<160][:12])
print('Reader header:',[(s,y) for s,y in reader if y<160][:12])
if not any('Оглавление' in s and y<160 for s,y in contents):
    raise SystemExit('Contents screen was not opened: QA route invalid')
if not any(('Том 1' in s or 'Том 2' in s) and y<175 for s,y in reader):
    raise SystemExit('Reader screen was not opened: QA route invalid')
PY
# Collect scroll and frame metrics while the verified reader is still open.
# Frame timing is reset only after reaching Reader, never on the Home menu.
adb shell dumpsys gfxinfo "$PKG" reset >/dev/null
adb shell input swipe 230 785 230 330 500
adb shell input swipe 230 350 230 785 500
sleep 1
adb shell dumpsys gfxinfo "$PKG" > performance-results/gfxinfo.txt
adb shell dumpsys meminfo "$PKG" > performance-results/meminfo.txt
adb shell am start -W -n "$PKG/$ACT" >> performance-results/measurements.txt
adb shell dumpsys gfxinfo "$PKG" framestats > performance-results/framestats.txt
grep -E "Total frames rendered|Janky frames|50th percentile|90th percentile|95th percentile|99th percentile" performance-results/gfxinfo.txt >> performance-results/measurements.txt || true
# BACK and final screenshot are secondary: never discard valid scroll metrics
# just because an unstable emulator temporarily loses its ADB connection.
adb shell input keyevent KEYCODE_BACK || true
sleep 1
adb shell screencap -p /sdcard/performance-final.png || true
adb pull /sdcard/performance-final.png performance-results/final.png >/dev/null || true
adb logcat -d -t 1500 | grep -E 'FATAL EXCEPTION|ANR in ru.madarij.nativeapp' > performance-results/crash-log.txt || true
if [ -s performance-results/crash-log.txt ]; then
  echo "Crash or ANR detected" >&2
  cat performance-results/crash-log.txt
  exit 1
fi
cat performance-results/measurements.txt
