param(
    [Parameter(Mandatory = $true)][string]$ZipPath
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.IO.Compression.FileSystem

$temp = Join-Path $env:TEMP "copypath-verify-$([Guid]::NewGuid().ToString('N'))"
New-Item -ItemType Directory -Force -Path $temp | Out-Null
try {
    [System.IO.Compression.ZipFile]::ExtractToDirectory($ZipPath, $temp)
    $artifactsXml = Join-Path $temp "artifacts.xml"
    if (-not (Test-Path $artifactsXml)) {
        throw "artifacts.xml missing in zip"
    }
    [xml]$doc = Get-Content $artifactsXml
    $entries = @{}
    [System.IO.Compression.ZipFile]::OpenRead($ZipPath).Entries | ForEach-Object {
        $entries[$_.FullName] = $true
    }
    foreach ($artifact in $doc.repository.artifacts.artifact) {
        $classifier = $artifact.GetAttribute("classifier")
        $id = $artifact.GetAttribute("id")
        $version = $artifact.GetAttribute("version")
        $relative = if ($classifier -eq "osgi.bundle") {
            "plugins/${id}_${version}.jar"
        } elseif ($classifier -eq "org.eclipse.update.feature") {
            "features/${id}_${version}.jar"
        } else {
            throw "Unknown classifier $classifier"
        }
        if (-not $entries.ContainsKey($relative)) {
            throw "Zip entry not found: $relative (from artifacts.xml)"
        }
        if ($relative -match '\\') {
            throw "Zip entry uses backslash: $relative"
        }
    }
    Write-Output "Zip verification OK: $ZipPath"
}
finally {
    Remove-Item -Recurse -Force $temp -ErrorAction SilentlyContinue
}
