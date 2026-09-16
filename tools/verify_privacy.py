"""Check the known removed upload paths in sources and the built Release APK.

This is a static regression check, not a substitute for network capture on a device.
Run after: gradlew.bat :app:assembleRelease
"""
from io import BytesIO
from pathlib import Path
import re
import xml.etree.ElementTree as ET
import zipfile

root = Path(__file__).resolve().parents[1]
forbidden = [
    b"com/umeng/", b"com.umeng.", b"io/sentry/", b"io.sentry.",
    b"brainspark/stackbricks", b"brainspark.stackbricks",
    b"supabase.co", b"cdn.aquamarine5.", b"UMengHelper",
    b"com/google/mlkit/", b"com.google.mlkit.",
    b"ai/onnxruntime/TelemetryInitializer", b"ai.onnxruntime.TelemetryInitializer",
    b"ai/onnxruntime/telemetry/", b"ai.onnxruntime.telemetry.",
]

for path in (root / "app/src/main/java").rglob("*.kt"):
    data = path.read_bytes()
    for token in forbidden:
        assert token not in data, f"Removed upload code remains: {path}: {token!r}"

aar = root / "app/libs/onnxruntime-android-1.29.0-minimal-captcha.aar"
with zipfile.ZipFile(aar) as archive:
    assert b"TelemetryInitializer" not in archive.read("AndroidManifest.xml")
    with zipfile.ZipFile(BytesIO(archive.read("classes.jar"))) as classes:
        assert not any("telemetry" in name.lower() for name in classes.namelist())

manifest = next((root / "app/build/intermediates/merged_manifests/release").rglob("AndroidManifest.xml"))
ns = "{http://schemas.android.com/apk/res/android}"
tree = ET.parse(manifest)
assert tree.getroot().find("application").get(ns + "allowBackup") == "false"
for element in tree.iter():
    for value in element.attrib.values():
        assert not any(token in value.encode() for token in forbidden), value
permissions = {item.get(ns + "name") for item in tree.findall("uses-permission")}
assert not permissions.intersection({
    "android.permission.READ_PHONE_STATE",
    "android.permission.CHANGE_WIFI_STATE",
    "android.permission.REQUEST_INSTALL_PACKAGES",
    "com.google.android.gms.permission.AD_ID",
})

apk = root / "app/build/outputs/apk/release/app-release.apk"
with zipfile.ZipFile(apk) as archive:
    for name in archive.namelist():
        if re.fullmatch(r"classes\d*\.dex", name):
            data = archive.read(name)
            for token in forbidden:
                assert token not in data, f"Removed dependency remains in {name}: {token!r}"
    assert "assets/captcha.ort" in archive.namelist(), "Offline captcha model missing"
    assert "lib/arm64-v8a/libonnxruntime.so" in archive.namelist()
print("PASS: source, ONNX AAR, merged manifest, and Release DEX checks")
print("PASS: core offline model retained; cloud backup and removed permissions absent")
