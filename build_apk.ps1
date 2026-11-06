# Automated APK Builder Script for Countdown Lock Screen
$ErrorActionPreference = "Stop"

$sdk = "$env:LOCALAPPDATA\Android\Sdk"
$buildTools = "$sdk\build-tools\36.0.0"
$platform = "$sdk\platforms\android-37.0\android.jar"
$javaBin = "C:\Program Files\Java\jdk-21\bin"
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"

$base = Split-Path -Parent $MyInvocation.MyCommand.Path
if (-not $base) { $base = "C:\Users\SB_WS_LT_0009\akshay\CountdownApp" }
$app = "$base\app"
$src = "$app\src\main"
$res = "$src\res"
$assets = "$src\assets"
$manifest = "$src\AndroidManifest.xml"

$buildDir = "$app\build\cli"
if (Test-Path $buildDir) { Remove-Item -Recurse -Force $buildDir }
New-Item -ItemType Directory -Path "$buildDir\compiled_res" -Force | Out-Null
New-Item -ItemType Directory -Path "$buildDir\gen" -Force | Out-Null
New-Item -ItemType Directory -Path "$buildDir\classes" -Force | Out-Null
New-Item -ItemType Directory -Path "$buildDir\dex" -Force | Out-Null
New-Item -ItemType Directory -Path "$base\dist" -Force | Out-Null

Write-Host ">>> [1/6] Compiling resources with aapt2..." -ForegroundColor Cyan
& "$buildTools\aapt2.exe" compile --dir "$res" -o "$buildDir\compiled_res.zip"

Write-Host ">>> [2/6] Linking resources with aapt2..." -ForegroundColor Cyan
& "$buildTools\aapt2.exe" link -I "$platform" `
    --manifest "$manifest" `
    -A "$assets" `
    -o "$buildDir\app-unaligned.apk" `
    --java "$buildDir\gen" `
    --auto-add-overlay `
    "$buildDir\compiled_res.zip"

Write-Host ">>> [3/6] Compiling Java sources with javac..." -ForegroundColor Cyan
$javaFiles = Get-ChildItem -Path "$src\java", "$buildDir\gen" -Filter "*.java" -Recurse | Select-Object -ExpandProperty FullName
& "$javaBin\javac.exe" -cp "$platform" -d "$buildDir\classes" -source 17 -target 17 $javaFiles

Write-Host ">>> [4/6] Dexing with D8..." -ForegroundColor Cyan
$classFiles = Get-ChildItem -Path "$buildDir\classes" -Filter "*.class" -Recurse | Select-Object -ExpandProperty FullName
& "$buildTools\d8.bat" --lib "$platform" --output "$buildDir\dex" $classFiles

Write-Host ">>> [5/6] Packaging classes.dex into APK..." -ForegroundColor Cyan
Push-Location "$buildDir\dex"
& "$javaBin\jar.exe" -uf "$buildDir\app-unaligned.apk" classes.dex
Pop-Location

Write-Host ">>> [6/6] Zipaligning and Signing APK..." -ForegroundColor Cyan
& "$buildTools\zipalign.exe" -f -p 4 "$buildDir\app-unaligned.apk" "$buildDir\app-aligned.apk"

$keystore = "$base\debug.keystore"
if (-not (Test-Path $keystore)) {
    & "$javaBin\keytool.exe" -genkey -v -keystore "$keystore" -alias androiddebugkey `
        -keyalg RSA -keysize 2048 -validity 10000 -storepass android -keypass android `
        -dname "CN=Android Debug,O=Android,C=US"
}

$finalApk = "$base\dist\CountdownLockScreen.apk"
& "$buildTools\apksigner.bat" sign --ks "$keystore" --ks-pass pass:android --key-pass pass:android --out "$finalApk" "$buildDir\app-aligned.apk"

& "$buildTools\apksigner.bat" verify "$finalApk"
Write-Host "SUCCESS: APK created and verified at: $finalApk" -ForegroundColor Green
Get-Item "$finalApk" | Select-Object Name, Length, LastWriteTime
