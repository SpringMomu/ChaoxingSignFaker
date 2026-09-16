$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    $signingDirectory = Join-Path $PSScriptRoot 'signing'
    $signingProperties = Join-Path $signingDirectory 'local-release.properties'
    $signingKeystore = Join-Path $signingDirectory 'local-release.jks'
    if (!(Test-Path -LiteralPath $signingProperties)) {
        if (Test-Path -LiteralPath $signingKeystore) {
            throw 'Signing key exists without its password file. Restore local-release.properties before continuing.'
        }
        New-Item -ItemType Directory -Path $signingDirectory -Force | Out-Null
        $env:CSF_LOCAL_KEY_PASSWORD = [Guid]::NewGuid().ToString('N')
        try {
            & keytool -genkeypair -keystore $signingKeystore -storetype PKCS12 -alias local-release -keyalg RSA -keysize 3072 -validity 10000 -storepass:env CSF_LOCAL_KEY_PASSWORD -keypass:env CSF_LOCAL_KEY_PASSWORD -dname 'CN=ChaoxingSignFaker Local Release' -noprompt
            if ($LASTEXITCODE -ne 0) { throw 'Could not generate local signing key.' }
            Set-Content -LiteralPath $signingProperties -Encoding ascii -Value "storePassword=$env:CSF_LOCAL_KEY_PASSWORD"
        } finally {
            Remove-Item Env:CSF_LOCAL_KEY_PASSWORD -ErrorAction SilentlyContinue
        }
    }
    & .\gradlew.bat :app:assembleRelease --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Release build failed.' }
} finally {
    Pop-Location
}
