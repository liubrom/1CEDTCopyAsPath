param(
    [Parameter(Mandatory = $true)][string]$SourceDir,
    [Parameter(Mandatory = $true)][string]$ZipPath
)

$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

$sourceFull = [System.IO.Path]::GetFullPath($SourceDir)
if (-not (Test-Path $sourceFull)) {
    throw "Source directory not found: $sourceFull"
}

if (Test-Path $ZipPath) {
    Remove-Item $ZipPath -Force
}

$zipDir = Split-Path $ZipPath -Parent
if ($zipDir -and -not (Test-Path $zipDir)) {
    New-Item -ItemType Directory -Force -Path $zipDir | Out-Null
}

$zip = [System.IO.Compression.ZipFile]::Open($ZipPath, [System.IO.Compression.ZipArchiveMode]::Create)
try {
    Get-ChildItem $sourceFull -Recurse -File | ForEach-Object {
        $relative = $_.FullName.Substring($sourceFull.Length).TrimStart('\', '/').Replace('\', '/')
        [void][System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $_.FullName, $relative)
    }
}
finally {
    $zip.Dispose()
}

Write-Output "Created zip with forward-slash paths: $ZipPath"
