# All changes are process-local. Dot-source this script before invoking Android tools.
$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path -Parent $PSScriptRoot
if (-not $env:SETAPPFULL_ROOT) { $env:SETAPPFULL_ROOT = Split-Path -Parent $repositoryRoot }
$env:JAVA_HOME = Join-Path $env:SETAPPFULL_ROOT 'tools\jdk\jdk-17.0.20.1+1'
$env:ANDROID_HOME = Join-Path $env:SETAPPFULL_ROOT 'tools\android-sdk'
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:ANDROID_USER_HOME = Join-Path $env:SETAPPFULL_ROOT 'cache\android-user'
$env:ANDROID_EMULATOR_HOME = Join-Path $env:SETAPPFULL_ROOT 'cache\android-emulator'
$env:ANDROID_AVD_HOME = Join-Path $env:SETAPPFULL_ROOT 'cache\android-avd'
$env:GRADLE_USER_HOME = Join-Path $env:SETAPPFULL_ROOT 'cache\gradle'
$env:GRADLE_RO_DEP_CACHE = $null
$env:TEMP = Join-Path $env:SETAPPFULL_ROOT 'tmp'
$env:TMP = $env:TEMP
$javaUserRoot = Join-Path $env:SETAPPFULL_ROOT 'cache\java-user'
$env:JAVA_TOOL_OPTIONS = "-Djava.io.tmpdir=`"$env:TEMP`" -Duser.home=`"$javaUserRoot`" -Dfile.encoding=UTF-8 -Duser.language=en -Duser.country=US"
$env:PATH = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\cmdline-tools\23.0\bin;$env:PATH"
@('cache\android-user', 'cache\android-emulator', 'cache\android-avd', 'cache\gradle', 'cache\java-user', 'cache\project', 'tmp', 'logs', 'build', 'signing') | ForEach-Object {
    New-Item -ItemType Directory -Path (Join-Path $env:SETAPPFULL_ROOT $_) -Force | Out-Null
}
