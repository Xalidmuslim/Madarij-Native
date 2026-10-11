#!/usr/bin/env python3
"""Emulator check: home > contents > volume 2 > reader. Screenshots attached."""
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path
OUT=Path("volume2-visual-qa")
OUT.mkdir(exist_ok=True)
PKG="ru.madarij.nativeapp"

def adb(*args):
    return subprocess.run(["adb",*args],check=True,capture_output=True,text=True,timeout=40).stdout

def screenshot(name):
    with (OUT/(name+".png")).open("wb") as f:
        subprocess.run(["adb","exec-out","screencap","-p"],stdout=f,check=True,timeout=30)

def dump():
    adb("shell","uiautomator","dump","/sdcard/window.xml")
    return ET.fromstring(adb("exec-out","cat","/sdcard/window.xml"))

def tap(label,loops=4):
    for attempt in range(loops):
        for node in dump().iter("node"):
            props=node.attrib
            if label in props.get("text","") or label in props.get("content-desc",""):
                bounds=[int(n) for n in re.findall(r"\d+",props.get("bounds",""))]
                if len(bounds)==4:
                    adb("shell","input","tap",str((bounds[0]+bounds[2])//2),
                        str((bounds[1]+bounds[3])//2))
                    time.sleep(2)
                    return True
        adb("shell","input","swipe","220","770","220","310","350")
        time.sleep(1)
    return False

def assert_visible(text):
    html=ET.tostring(dump(),encoding="unicode")
    if text not in html:
        raise RuntimeError("Text not visible on Android: "+text)

adb("shell","wm","size","432x960")
adb("shell","wm","density","160")
adb("install","-r","app/build/outputs/apk/debug/app-debug.apk")
adb("shell","am","start","-W","-n",PKG+"/.MainActivity")
time.sleep(9)
screenshot("01-home")
if not tap("Содержание",2):
    adb("shell","input","tap","130","868")
    time.sleep(3)
screenshot("02-contents-v1")
if not tap("Том 2"):
    raise RuntimeError("Missing Volume 2 switcher")
assert_visible("Том 2")
screenshot("03-contents-v2")
if not tap("Взгляды на грех"):
    raise RuntimeError("Missing chapter group")
screenshot("04-open-group")
if not tap("О взглядах людей на грех"):
    raise RuntimeError("Missing chapter")
if not tap("Читать",2) and not tap("О взглядах людей на грех",1):
    raise RuntimeError("Cannot open Volume 2 reader")
time.sleep(3)
screenshot("05-reader-top")
visible=ET.tostring(dump(),encoding="unicode").lower()
if "предварительный" not in visible and "двенадцать" not in visible:
    raise RuntimeError("No preview author text or warning in reader")
adb("shell","input","swipe","225","780","225","355","490")
time.sleep(2)
screenshot("06-reader-scroll")
log=adb("logcat","-d","-t","1600")
if "FATAL EXCEPTION" in log or "ANR in "+PKG in log:
    raise RuntimeError("Crash or ANR while reading")
(OUT/"result.txt").write_text("Volume 2 reader and navigation smoke test passed.\n",encoding="utf-8")
print("Volume 2 reader Android smoke test succeeded.")
