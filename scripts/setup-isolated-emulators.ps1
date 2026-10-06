[CmdletBinding()]
param()
. "$PSScriptRoot\environment.ps1"
$catalogSources = @(
    'https://dl.google.com/android/repository/sys-img/android/sys-img2-3.xml',
    'https://dl.google.com/android/repository/sys-img/google_apis/sys-img2-3.xml'
)
$wantedPackages = @(
    'system-images;android-36;default;x86_64',
    'system-images;android-37.0;google_apis;x86_64'
)
$metadata = foreach ($feed in $catalogSources) {
    [xml]$catalog = (Invoke-WebRequest -Uri $feed).Content
    foreach ($package in $catalog.SelectNodes('//*[local-name()="remotePackage"]')) {
        if ($package.path -in $wantedPackages) {
            foreach ($archive in $package.archives.archive) {
                [pscustomobject]@{
                    Package = $package.path
                    Revision = $package.revision.major
                    Source = $feed
                    Url = $feed.Substring(0, $feed.LastIndexOf('/') + 1) + $archive.complete.url
                    CompressedBytes = $archive.complete.size
                    Checksum = $archive.complete.checksum.InnerText
                    ChecksumType = $archive.complete.checksum.type
                }
            }
        }
    }
}
$metadata | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath "$env:SETAPPFULL_ROOT\reports\isolated-avd-download-metadata.json" -Encoding utf8
$androidCli = "$env:ANDROID_HOME\cmdline-tools\23.0\bin\android.exe"
& $androidCli --no-metrics --sdk=$env:ANDROID_HOME sdk install 'emulator@37.2.12' 'system-images/android-36/default/x86_64@2.0.0' 'system-images/android-37.0/google_apis/x86_64@6.0.0'
if ($LASTEXITCODE -ne 0) { throw 'Isolated emulator package installation failed.' }
& "$env:ANDROID_HOME\emulator\emulator.exe" -version
& "$env:ANDROID_HOME\emulator\emulator.exe" -accel-check
# An unavailable accelerator is expected here; no Windows features are changed.
exit 0
