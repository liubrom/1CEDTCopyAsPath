param(
    [string]$StageDir,
    [string]$CategoryXml,
    [string]$RepositoryPath,
    [string]$EdtLauncher = "E:\EDT\_EDT\1C_EDT 2025.2\1cedt\1cedtc.exe"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $EdtLauncher)) {
    throw "EDT launcher not found: $EdtLauncher"
}
if (-not (Test-Path $CategoryXml)) {
    throw "category.xml not found: $CategoryXml"
}
if (-not (Test-Path $StageDir)) {
    throw "Stage directory not found: $StageDir"
}

function Convert-ToDirectoryFileUri([string]$path) {
    $full = [System.IO.Path]::GetFullPath($path)
    if (-not $full.EndsWith([System.IO.Path]::DirectorySeparatorChar)) {
        $full += [System.IO.Path]::DirectorySeparatorChar
    }
    return ([Uri]$full).AbsoluteUri
}

function Convert-ToFileFileUri([string]$path) {
    return ([Uri]([System.IO.Path]::GetFullPath($path))).AbsoluteUri
}

Remove-Item -Recurse -Force $RepositoryPath -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $RepositoryPath | Out-Null

$repoUri = Convert-ToDirectoryFileUri $RepositoryPath
$stagePath = [System.IO.Path]::GetFullPath($StageDir)
$categoryPath = [System.IO.Path]::GetFullPath($CategoryXml)

$commonArgs = @(
    "-nosplash"
    "-application", "org.eclipse.equinox.p2.publisher.FeaturesAndBundlesPublisher"
    "-metadataRepository", $repoUri
    "-artifactRepository", $repoUri
    "-source", $stagePath
    "-publishArtifacts"
    "-append"
    "-configs", "win32,win32,x86_64"
)

Write-Output "Publishing bundles and features"
Write-Output "  source: $stagePath"
Write-Output "  repo:   $repoUri"
$proc = Start-Process -FilePath $EdtLauncher -ArgumentList $commonArgs -Wait -PassThru -NoNewWindow
if ($proc.ExitCode -ne 0) {
    throw "FeaturesAndBundlesPublisher failed with exit code $($proc.ExitCode)"
}

$categoryUri = Convert-ToFileFileUri $CategoryXml

$categoryArgs = @(
    "-nosplash"
    "-application", "org.eclipse.equinox.p2.publisher.CategoryPublisher"
    "-metadataRepository", $repoUri
    "-artifactRepository", $repoUri
    "-categoryDefinition", $categoryUri
)

Write-Output "Publishing categories"
Write-Output "  category: $categoryUri"
$proc2 = Start-Process -FilePath $EdtLauncher -ArgumentList $categoryArgs -Wait -PassThru -NoNewWindow
if ($proc2.ExitCode -ne 0) {
    throw "CategoryPublisher failed with exit code $($proc2.ExitCode)"
}

$hasPlugins = Test-Path (Join-Path $RepositoryPath "plugins")
$hasFeatures = Test-Path (Join-Path $RepositoryPath "features")
if (-not $hasPlugins -or -not $hasFeatures) {
    throw "Repository is incomplete (plugins=$hasPlugins features=$hasFeatures) in $RepositoryPath"
}

if (-not (Test-Path (Join-Path $RepositoryPath "content.xml"))) {
    throw "content.xml was not created in $RepositoryPath"
}
if (-not (Test-Path (Join-Path $RepositoryPath "artifacts.xml"))) {
    throw "artifacts.xml was not created in $RepositoryPath"
}

# Как в repo.zip: только plain XML, без jar/xz метаданных (меньше проблем с jar:file: в zip).
Remove-Item (Join-Path $RepositoryPath "content.jar") -Force -ErrorAction SilentlyContinue
Remove-Item (Join-Path $RepositoryPath "artifacts.jar") -Force -ErrorAction SilentlyContinue
Remove-Item (Join-Path $RepositoryPath "content.xml.xz") -Force -ErrorAction SilentlyContinue
Remove-Item (Join-Path $RepositoryPath "artifacts.xml.xz") -Force -ErrorAction SilentlyContinue
Remove-Item (Join-Path $RepositoryPath "p2.index") -Force -ErrorAction SilentlyContinue

Write-Output "P2 repository published: $RepositoryPath"
