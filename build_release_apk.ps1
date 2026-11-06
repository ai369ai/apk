# Automated Production Release APK Builder
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

$buildDir = "$app\build\release_cli"
if (Test-Path -LiteralPath $buildDir) { Remove-Item -LiteralPath $buildDir -Recurse -Force }
New-Item -ItemType Directory -Path "$buildDir\compiled_res" -Force | Out-Null
New-Item -ItemType Directory -Path "$buildDir\gen" -Force | Out-Null
New-Item -ItemType Directory -Path "$buildDir\classes" -Force | Out-Null
New-Item -ItemType Directory -Path "$buildDir\dex" -Force | Out-Null
New-Item -ItemType Directory -Path "$base\dist" -Force | Out-Null

Write-Host ">>> [1/6] Compiling resources with aapt2..." -ForegroundColor Cyan
& "$buildTools\aapt2.exe" compile --dir "$res" -o "$buildDir\compiled_res.zip"

Write-Host ">>> [2/6] Linking resources with aapt2 (Release mode)..." -ForegroundColor Cyan
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

Write-Host ">>> [4/6] Dexing with D8 in release mode..." -ForegroundColor Cyan
$classFiles = Get-ChildItem -Path "$buildDir\classes" -Filter "*.class" -Recurse | Select-Object -ExpandProperty FullName
& "$buildTools\d8.bat" --release --lib "$platform" --output "$buildDir\dex" $classFiles

Write-Host ">>> [5/6] Packaging classes.dex into APK cleanly with Python..." -ForegroundColor Cyan
python -c "import zipfile, sys; apk = sys.argv[1]; dex = sys.argv[2]; z = zipfile.ZipFile(apk, 'a', compression=zipfile.ZIP_DEFLATED); z.write(dex, 'classes.dex'); z.close()" "$buildDir\app-unaligned.apk" "$buildDir\dex\classes.dex"

Write-Host ">>> [6/6] Zipaligning and Signing Release APK..." -ForegroundColor Cyan
& "$buildTools\zipalign.exe" -f -p 4 "$buildDir\app-unaligned.apk" "$buildDir\app-aligned.apk"

$releaseKeystore = "$base\release.keystore"
$ksPass = "CountdownApp2026Secure!"
$keyAlias = "countdown_release_key"

if (-not (Test-Path $releaseKeystore)) {
    Write-Host ">>> Generating production release keypair..." -ForegroundColor Yellow
    & "$javaBin\keytool.exe" -genkey -v -keystore "$releaseKeystore" -alias "$keyAlias" `
        -keyalg RSA -keysize 2048 -validity 10000 -storepass "$ksPass" -keypass "$ksPass" `
        -dname "CN=Countdown Lock Screen,OU=Mobile Development,O=Countdown Studio,L=New York,ST=NY,C=US"
}

$finalReleaseApk = "$base\dist\CountdownLockScreen-release.apk"
& "$buildTools\apksigner.bat" sign `
    --ks "$releaseKeystore" `
    --ks-pass "pass:$ksPass" `
    --key-pass "pass:$ksPass" `
    --ks-key-alias "$keyAlias" `
    --v1-signing-enabled true `
    --v2-signing-enabled true `
    --v3-signing-enabled true `
    --out "$finalReleaseApk" `
    "$buildDir\app-aligned.apk"

Write-Host ">>> Verifying Release APK..." -ForegroundColor Cyan
& "$buildTools\apksigner.bat" verify --verbose "$finalReleaseApk"

Write-Host "SUCCESS: Production Release APK created at: $finalReleaseApk" -ForegroundColor Green
Get-Item "$finalReleaseApk" | Select-Object Name, Length, LastWriteTime
