[CmdletBinding()]
param()
. "$PSScriptRoot\environment.ps1"
function Get-VerifiedArchive([string]$Url, [string]$Name, [string]$Hash, [string]$Algorithm = 'SHA256') {
    $destination = Join-Path $env:SETAPPFULL_ROOT "cache\$Name"
    if (-not (Test-Path -LiteralPath $destination)) { Invoke-WebRequest -Uri $Url -OutFile $destination }
    if ((Get-FileHash -LiteralPath $destination -Algorithm $Algorithm).Hash -ne $Hash) { throw "Checksum mismatch: $Name" }
    return $destination
}
if (-not (Test-Path -LiteralPath "$env:JAVA_HOME\bin\java.exe")) {
    $archive = Get-VerifiedArchive 'https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.20.1%2B1/OpenJDK17U-jdk_x64_windows_hotspot_17.0.20.1_1.zip' 'OpenJDK17U-jdk_x64_windows_hotspot_17.0.20.1_1.zip' 'e53a79c3c3d86865bd7e787903884331068e71321714ffd44f145785affc7cb0'
    Expand-Archive -LiteralPath $archive -DestinationPath "$env:SETAPPFULL_ROOT\tools\jdk" -Force
}
if (-not (Test-Path -LiteralPath "$env:SETAPPFULL_ROOT\tools\gradle-9.3.1\bin\gradle.bat")) {
    $archive = Get-VerifiedArchive 'https://services.gradle.org/distributions/gradle-9.3.1-bin.zip' 'gradle-9.3.1-bin.zip' 'b266d5ff6b90eada6dc3b20cb090e3731302e553a27c5d3e4df1f0d76beaff06'
    Expand-Archive -LiteralPath $archive -DestinationPath "$env:SETAPPFULL_ROOT\tools" -Force
}
$androidCli = "$env:ANDROID_HOME\cmdline-tools\23.0\bin\android.exe"
if (-not (Test-Path -LiteralPath $androidCli)) {
    $archive = Get-VerifiedArchive 'https://dl.google.com/android/repository/commandlinetools-win-16111833_latest.zip' 'commandlinetools-win-16111833.zip' '57d04f2d75eb8e8fffc5000a987e5de4b5a63e9d' 'SHA1'
    $extractPath = "$env:SETAPPFULL_ROOT\tmp\cmdline-tools-bootstrap"
    Expand-Archive -LiteralPath $archive -DestinationPath $extractPath -Force
    New-Item -ItemType Directory -Path "$env:ANDROID_HOME\cmdline-tools" -Force | Out-Null
    $sourcePath = [IO.Path]::GetFullPath("$extractPath\cmdline-tools")
    $targetPath = [IO.Path]::GetFullPath("$env:ANDROID_HOME\cmdline-tools\23.0")
    if (-not $sourcePath.StartsWith("$env:SETAPPFULL_ROOT\") -or -not $targetPath.StartsWith("$env:SETAPPFULL_ROOT\")) { throw 'Unexpected extraction path.' }
    Move-Item -LiteralPath $sourcePath -Destination $targetPath
}
$requiredPackages = [ordered]@{
    'platforms/android-37.0@2.0.0' = 'platforms\android-37.0\android.jar'
    'build-tools/37.0.0@37.0.0' = 'build-tools\37.0.0\apksigner.bat'
    'platform-tools@37.0.1' = 'platform-tools\adb.exe'
}
$missingPackages = @($requiredPackages.Keys | Where-Object { -not (Test-Path -LiteralPath (Join-Path $env:ANDROID_HOME $requiredPackages[$_])) })
if ($missingPackages.Count -gt 0) {
    & $androidCli --no-metrics --sdk=$env:ANDROID_HOME sdk install @missingPackages
    if ($LASTEXITCODE -ne 0) { throw 'Android SDK installation failed.' }
}
& "$env:JAVA_HOME\bin\java.exe" -version
& "$env:SETAPPFULL_ROOT\tools\gradle-9.3.1\bin\gradle.bat" --version
