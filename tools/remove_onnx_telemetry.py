"""Remove the Android telemetry transport from the bundled ONNX AAR, in place.

The native inference libraries and model format remain unchanged. The app also
calls OrtEnvironment.setTelemetry(false) before creating any inference session.
Run this again if the bundled AAR is replaced during a dependency update.
"""
from io import BytesIO
from pathlib import Path
import re
import zipfile

root = Path(__file__).resolve().parents[1]
aar = root / "app/libs/onnxruntime-android-1.29.0-minimal-captcha.aar"
result = BytesIO()
with zipfile.ZipFile(aar) as source, zipfile.ZipFile(result, "w") as target:
    for entry in source.infolist():
        data = source.read(entry.filename)
        if entry.filename == "classes.jar":
            jar_result = BytesIO()
            with zipfile.ZipFile(BytesIO(data)) as jar, zipfile.ZipFile(jar_result, "w") as clean:
                for item in jar.infolist():
                    if item.filename.startswith(("ai/onnxruntime/TelemetryInitializer", "ai/onnxruntime/telemetry/")):
                        continue
                    clean.writestr(item, jar.read(item.filename))
            data = jar_result.getvalue()
        elif entry.filename == "AndroidManifest.xml":
            manifest = data.decode("utf-8")
            manifest = re.sub(r'\s*<provider\s+android:name="ai.onnxruntime.TelemetryInitializer"[^>]*/>', "", manifest)
            manifest = re.sub(r'\s*<uses-permission[^>]*/>', "", manifest)
            data = manifest.encode("utf-8")
        elif entry.filename == "proguard.txt":
            data = b"\n".join(line for line in data.splitlines() if b"TelemetryInitializer" not in line and b"ai.onnxruntime.telemetry" not in line)
        target.writestr(entry, data)
aar.write_bytes(result.getvalue())
print("Removed Android ONNX telemetry initializer and HTTP transport; retained native inference libraries.")
