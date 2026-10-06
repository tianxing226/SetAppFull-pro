[CmdletBinding()]
param(
    [ValidateSet(36, 37)][int]$Api = 36,
    [ValidateSet('phone', 'tablet')][string]$Device = 'phone',
    [ValidateSet(5580, 5582)][int]$Port = 5580,
    [ValidateSet('on', 'off')][string]$Acceleration = 'on',
    [string]$PrivateRamdisk
)
. "$PSScriptRoot\environment.ps1"
$avdName = "SetAppFull_API${Api}_${Device}"
if (-not (Test-Path -LiteralPath "$env:ANDROID_AVD_HOME\$avdName.avd\config.ini")) { throw 'Create the isolated AVD first.' }
# Android Emulator explicitly requires this opt-in for its unsupported, slow TCG mode.
# It is scoped to this process and its children, and never changes Windows features.
if ($Acceleration -eq 'off') { $env:ANDROID_I_WANT_MY_TCG = 'yes' }
$emulatorArguments = @('-avd', $avdName, '-port', $Port, '-accel', $Acceleration, '-gpu', 'swiftshader',
    '-no-window', '-no-audio', '-no-metrics', '-no-snapshot', '-no-boot-anim', '-cores', '2', '-memory', '2048',
    '-camera-back', 'none', '-camera-front', 'none', '-timezone', 'Asia/Shanghai', '-verbose')
if ($PrivateRamdisk) {
    $resolvedRamdisk = (Resolve-Path -LiteralPath $PrivateRamdisk -ErrorAction Stop).Path
    $privateAvdRoot = [IO.Path]::GetFullPath("$env:ANDROID_AVD_HOME\$avdName.avd\")
    if (-not $resolvedRamdisk.StartsWith($privateAvdRoot, [StringComparison]::OrdinalIgnoreCase)) {
        throw 'A modified ramdisk must be a private copy within this exact AVD directory.'
    }
    $emulatorArguments += @('-ramdisk', ('"' + $resolvedRamdisk + '"'))
}
$emulatorProcess = Start-Process -FilePath "$env:ANDROID_HOME\emulator\emulator.exe" -ArgumentList $emulatorArguments `
    -WorkingDirectory $env:SETAPPFULL_ROOT -WindowStyle Hidden `
    -RedirectStandardOutput "$env:SETAPPFULL_ROOT\logs\${avdName}-stdout.log" `
    -RedirectStandardError "$env:SETAPPFULL_ROOT\logs\${avdName}-stderr.log" -PassThru
[pscustomobject]@{ Name = $avdName; Api = $Api; Port = $Port; Serial = "emulator-$Port"; LauncherPid = $emulatorProcess.Id; PrivateRamdisk = $PrivateRamdisk } |
    ConvertTo-Json | Set-Content -LiteralPath "$env:SETAPPFULL_ROOT\reports\${avdName}-launch.json" -Encoding utf8
$emulatorProcess.Id
