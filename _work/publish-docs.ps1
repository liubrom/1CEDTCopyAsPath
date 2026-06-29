param(
    [string]$RepositoryPath = (Join-Path (Split-Path -Parent $PSScriptRoot) "dist\repository"),
    [string]$DocsPath = (Join-Path (Split-Path -Parent $PSScriptRoot) "docs")
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path (Join-Path $RepositoryPath "content.xml"))) {
    throw "P2 repository not found: $RepositoryPath. Run build-plugin.ps1 first."
}

Remove-Item -Recurse -Force $DocsPath -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $DocsPath | Out-Null
Copy-Item -Path (Join-Path $RepositoryPath "*") -Destination $DocsPath -Recurse -Force
New-Item -ItemType File -Force -Path (Join-Path $DocsPath ".nojekyll") | Out-Null
Write-Output "Published p2 site to $DocsPath"
