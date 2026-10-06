[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$Apk)
. "$PSScriptRoot\environment.ps1"
Add-Type -AssemblyName System.IO.Compression.FileSystem
$apkPath = (Resolve-Path -LiteralPath $Apk).Path
$archive = [IO.Compression.ZipFile]::OpenRead($apkPath)
$native = @()
try {
    foreach ($entry in $archive.Entries | Where-Object FullName -match '^lib/.+\.so$') {
        $stream = $entry.Open()
        $memory = [IO.MemoryStream]::new()
        try { $stream.CopyTo($memory); $bytes=$memory.ToArray() } finally { $stream.Dispose(); $memory.Dispose() }
        if ($bytes[0] -ne 0x7f -or $bytes[1] -ne 0x45 -or $bytes[2] -ne 0x4c -or $bytes[3] -ne 0x46 -or $bytes[5] -ne 1) { throw "Unrecognized ELF: $($entry.FullName)" }
        $is64 = $bytes[4] -eq 2
        if ($is64) {
            $phOffset = [BitConverter]::ToUInt64($bytes,32)
            $phSize = [BitConverter]::ToUInt16($bytes,54)
            $phCount = [BitConverter]::ToUInt16($bytes,56)
        } else {
            $phOffset = [BitConverter]::ToUInt32($bytes,28)
            $phSize = [BitConverter]::ToUInt16($bytes,42)
            $phCount = [BitConverter]::ToUInt16($bytes,44)
        }
        $alignment=@()
        for ($i=0;$i -lt $phCount;$i++) {
            $offset=[int]($phOffset+$i*$phSize)
            if ([BitConverter]::ToUInt32($bytes,$offset) -eq 1) {
                if ($is64) { $alignment += [BitConverter]::ToUInt64($bytes,$offset+48) }
                else { $alignment += [BitConverter]::ToUInt32($bytes,$offset+28) }
            }
        }
        $native += [pscustomobject]@{path=$entry.FullName;size=$entry.Length;elf64=$is64;loadSegmentAlignments=$alignment;supports16KiBAlignment=($alignment.Count -gt 0 -and @($alignment | Where-Object {$_ -lt 16384}).Count -eq 0)}
    }
    $entryPoints=@($archive.Entries | Where-Object FullName -match '^META-INF/xposed/' | ForEach-Object FullName)
    $sourceBuildInfo = [ordered]@{}
    $buildInfoEntry = $archive.GetEntry('assets/build-info.properties')
    if ($null -ne $buildInfoEntry) {
        $reader = [IO.StreamReader]::new($buildInfoEntry.Open())
        try {
            foreach ($line in ($reader.ReadToEnd() -split "`n")) {
                if ($line -match '^([^=]+)=(.*)$') { $sourceBuildInfo[$Matches[1]] = $Matches[2].Trim() }
            }
        } finally { $reader.Dispose() }
    }
} finally { $archive.Dispose() }
$baseName=[IO.Path]::GetFileNameWithoutExtension($apkPath)
$alignmentOutput=& "$env:ANDROID_HOME\build-tools\37.0.0\zipalign.exe" -c -P 16 -v 4 $apkPath
$alignmentExit=$LASTEXITCODE
$alignmentOutput | Set-Content -LiteralPath "$env:SETAPPFULL_ROOT\reports\$baseName-zipalign.txt"
$signatureOutput=& "$env:ANDROID_HOME\build-tools\37.0.0\apksigner.bat" verify --verbose --print-certs $apkPath 2>&1
$signatureExit=$LASTEXITCODE
$signatureOutput | Set-Content -LiteralPath "$env:SETAPPFULL_ROOT\reports\$baseName-signature.txt"
$report=[ordered]@{apk=$apkPath;sha256=(Get-FileHash -LiteralPath $apkPath -Algorithm SHA256).Hash;zipalignPassed=($alignmentExit -eq 0);signatureVerified=($signatureExit -eq 0);sourceBuildInfo=$sourceBuildInfo;moduleMetadata=$entryPoints;nativeLibraries=$native}
$report | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath "$env:SETAPPFULL_ROOT\reports\$baseName-package-audit.json" -Encoding utf8
$report | ConvertTo-Json -Depth 8
if ($alignmentExit -ne 0 -or $signatureExit -ne 0 -or @($native | Where-Object {$_.elf64 -and -not $_.supports16KiBAlignment}).Count -gt 0) { throw 'APK signature, ZIP alignment, or 64-bit native ELF alignment validation failed.' }
