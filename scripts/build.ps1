[CmdletBinding()]
param(
    [string[]]$Tasks = @(':app:assembleDebug', ':probe:assembleDebug'),
    [switch]$Offline,
    [switch]$RefreshDependencies
)
. "$PSScriptRoot\environment.ps1"
$projectPath = Join-Path $env:SETAPPFULL_ROOT 'source\SetAppFull'
$gradle = Join-Path $env:SETAPPFULL_ROOT 'tools\gradle-9.3.1\bin\gradle.bat'
if (-not (Test-Path -LiteralPath $gradle)) { throw 'Run scripts/setup-toolchain.ps1 first.' }
$debugStore = Join-Path $env:SETAPPFULL_ROOT 'signing\debug.keystore'
if (-not (Test-Path -LiteralPath $debugStore)) {
    & "$env:JAVA_HOME\bin\keytool.exe" -genkeypair -keystore $debugStore -storepass android -keypass android -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=Android Debug,O=Android,C=US' -storetype JKS -noprompt
    if ($LASTEXITCODE -ne 0) { throw 'Could not create the isolated debug keystore.' }
}
if (($Tasks -join ' ') -match '(?i)release' -and -not (Test-Path -LiteralPath "$env:SETAPPFULL_ROOT\signing\release.properties")) {
    throw 'Release signing is absent: signing/release.properties is required for Release tasks.'
}
$logPath = Join-Path $env:SETAPPFULL_ROOT ('logs\build-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.log')
$arguments = @('--project-dir', $projectPath, '--project-cache-dir', "$env:SETAPPFULL_ROOT\cache\project", '--no-daemon', '--console=plain', '--stacktrace') + $Tasks
if ($Offline) { $arguments += '--offline' }
if ($RefreshDependencies) { $arguments += '--refresh-dependencies' }
& $gradle @arguments 2>&1 | Tee-Object -FilePath $logPath
$buildExit = $LASTEXITCODE
if ($buildExit -ne 0) { throw "Build failed (exit $buildExit). See $logPath" }
Write-Output "Build log: $logPath"
