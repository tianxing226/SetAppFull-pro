[CmdletBinding()]
param()
. "$PSScriptRoot\environment.ps1"
$avdManager = "$env:ANDROID_HOME\cmdline-tools\23.0\bin\avdmanager.bat"
$definitions = @(
    @{ Name = 'SetAppFull_API36_phone'; Package = 'system-images;android-36;default;x86_64'; Device = 'medium_phone'; Width = '720'; Height = '1280'; Density = '320' },
    @{ Name = 'SetAppFull_API37_phone'; Package = 'system-images;android-37.0;google_apis;x86_64'; Device = 'medium_phone'; Width = '720'; Height = '1280'; Density = '320' },
    @{ Name = 'SetAppFull_API36_tablet'; Package = 'system-images;android-36;default;x86_64'; Device = 'medium_tablet'; Width = '1280'; Height = '800'; Density = '160' },
    @{ Name = 'SetAppFull_API37_tablet'; Package = 'system-images;android-37.0;google_apis;x86_64'; Device = 'medium_tablet'; Width = '1280'; Height = '800'; Density = '160' }
)
foreach ($definition in $definitions) {
    $avdPath = Join-Path $env:ANDROID_AVD_HOME ($definition.Name + '.avd')
    if (-not (Test-Path -LiteralPath "$avdPath\config.ini")) {
        'no' | & $avdManager create avd --name $definition.Name --package $definition.Package --device $definition.Device --path $avdPath
        if ($LASTEXITCODE -ne 0) { throw "Failed to create $($definition.Name)" }
        $properties = [ordered]@{}
        Get-Content -LiteralPath "$avdPath\config.ini" | ForEach-Object {
            if ($_ -match '^([^=]+)=(.*)$') { $properties[$Matches[1]] = $Matches[2] }
        }
        $properties['hw.cpu.ncore'] = '2'
        $properties['hw.ramSize'] = '2048'
        $properties['hw.lcd.width'] = $definition.Width
        $properties['hw.lcd.height'] = $definition.Height
        $properties['hw.lcd.density'] = $definition.Density
        $properties['hw.keyboard'] = 'yes'
        $properties['hw.gpu.enabled'] = 'yes'
        $properties['hw.gpu.mode'] = 'swiftshader'
        $properties['disk.dataPartition.size'] = '4G'
        $properties['fastboot.forceColdBoot'] = 'yes'
        $properties['showDeviceFrame'] = 'no'
        $properties['skin.dynamic'] = 'yes'
        $properties['skin.name'] = "$($definition.Width)x$($definition.Height)"
        $properties.GetEnumerator() | ForEach-Object { "$($_.Key)=$($_.Value)" } |
            Set-Content -LiteralPath "$avdPath\config.ini" -Encoding utf8
    }
    Write-Output "$($definition.Name): $avdPath"
}
& $avdManager list avd
if ($LASTEXITCODE -ne 0) { throw 'AVD listing failed.' }
