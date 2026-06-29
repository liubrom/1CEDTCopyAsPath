param(
    [string]$Version = "1.0.0",
    [switch]$PublishDocs,
    [switch]$CopyToDownloads
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$bundleRoot = Join-Path $root "plugin\bundles\ru.cursor.edt.copypath.ui"
$bin = Join-Path $bundleRoot "bin"
$src = Join-Path $bundleRoot "src"
$p2 = if ($env:EDT_P2_POOL) { $env:EDT_P2_POOL } else { Join-Path $env:USERPROFILE ".p2\pool\plugins" }
$dist = Join-Path $root "dist"
$p2Root = Join-Path $env:TEMP "copy-as-path-p2-build"
$stage = Join-Path $p2Root "stage"
$repo = Join-Path $p2Root "repository"

function Get-Jar([string]$pattern) {
    Get-ChildItem $p2 -Filter $pattern | Sort-Object LastWriteTime -Descending | Select-Object -First 1 -ExpandProperty FullName
}

if (-not (Test-Path $p2)) {
    throw "EDT p2 pool not found: $p2. Set EDT_P2_POOL to ...\.p2\pool\plugins"
}

$classpath = @(
    Get-Jar "org.eclipse.ui_*.jar"
    Get-Jar "org.eclipse.ui.workbench_*.jar"
    Get-Jar "org.eclipse.core.runtime_*.jar"
    Get-Jar "org.eclipse.core.resources_*.jar"
    Get-Jar "org.eclipse.core.jobs_*.jar"
    Get-Jar "org.eclipse.core.commands_*.jar"
    Get-Jar "org.eclipse.core.expressions_*.jar"
    Get-Jar "org.eclipse.ui.ide_*.jar"
    Get-Jar "org.eclipse.jface_*.jar"
    Get-Jar "org.eclipse.swt_*.jar"
    Get-Jar "org.eclipse.swt.win32.win32.x86_64_*.jar"
    Get-Jar "org.eclipse.equinox.common_*.jar"
    Get-Jar "org.eclipse.osgi_*.jar"
    Get-Jar "org.eclipse.emf.ecore_*.jar"
    Get-Jar "org.eclipse.emf.common_*.jar"
    Get-Jar "com._1c.g5.v8.dt.core_*.jar"
    Get-Jar "com._1c.g5.v8.dt.metadata_*.jar"
    Get-Jar "com._1c.g5.v8.dt.bsl.model_*.jar"
    Get-Jar "com._1c.g5.v8.dt.mcore_*.jar"
    Get-Jar "com._1c.g5.v8.dt.navigator_*.jar"
) | Where-Object { $_ -and (Test-Path $_) } | Select-Object -Unique

if ($classpath.Count -lt 8) {
    throw "Classpath incomplete. Found $($classpath.Count) jars in $p2"
}

Remove-Item -Recurse -Force $bin -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $bin | Out-Null

$sources = Get-ChildItem $src -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
& (Join-Path $PSScriptRoot "generate-icons.ps1") | Out-Null
& javac -encoding UTF-8 -source 17 -target 17 -cp ($classpath -join ";") -d $bin $sources
if ($LASTEXITCODE -ne 0) { throw "javac failed" }

Copy-Item (Join-Path $bundleRoot "plugin.xml") $bin -Force
Copy-Item (Join-Path $bundleRoot "plugin.properties") $bin -Force
Copy-Item (Join-Path $bundleRoot "plugin_ru.properties") $bin -Force
Copy-Item (Join-Path $src "ru\cursor\edt\copypath\ui\internal\messages.properties") (Join-Path $bin "ru\cursor\edt\copypath\ui\internal\") -Force
Copy-Item (Join-Path $src "ru\cursor\edt\copypath\ui\internal\messages_ru.properties") (Join-Path $bin "ru\cursor\edt\copypath\ui\internal\") -Force
Copy-Item (Join-Path $bundleRoot "icons") $bin -Recurse -Force
New-Item -ItemType Directory -Force -Path (Join-Path $bin "META-INF") | Out-Null
$manifest = Get-Content (Join-Path $bundleRoot "META-INF\MANIFEST.MF") -Raw
$manifest = $manifest -replace "1\.0\.0\.qualifier", $Version
$manifestPath = Join-Path $bin "META-INF\MANIFEST.MF"
$utf8NoBom = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText($manifestPath, $manifest, $utf8NoBom)

$pluginsDir = Join-Path $stage "plugins"
$featuresDir = Join-Path $stage "features"
Remove-Item -Recurse -Force $stage -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $pluginsDir, $featuresDir | Out-Null

$pluginJar = Join-Path $pluginsDir "ru.cursor.edt.copypath.ui_$Version.jar"
if (Test-Path $pluginJar) { Remove-Item $pluginJar -Force }
Push-Location $bin
jar cfm $pluginJar META-INF\MANIFEST.MF .
Pop-Location

$featureWork = Join-Path $env:TEMP "copypath-feature-$Version"
Remove-Item -Recurse -Force $featureWork -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $featureWork | Out-Null
$featureXml = Get-Content (Join-Path $root "plugin\features\ru.cursor.edt.copypath.feature\feature.xml") -Raw
$featureXml = $featureXml -replace "1\.0\.0\.qualifier", $Version
[System.IO.File]::WriteAllText((Join-Path $featureWork "feature.xml"), $featureXml, $utf8NoBom)
Copy-Item (Join-Path $root "plugin\features\ru.cursor.edt.copypath.feature\feature.properties") $featureWork -Force
$featureJar = Join-Path $featuresDir "ru.cursor.edt.copypath.feature_$Version.jar"
Push-Location $featureWork
jar cf $featureJar feature.xml feature.properties
Pop-Location

Remove-Item -Recurse -Force $repo -ErrorAction SilentlyContinue
$repoPlugins = Join-Path $repo "plugins"
$repoFeatures = Join-Path $repo "features"
New-Item -ItemType Directory -Force -Path $repoPlugins, $repoFeatures | Out-Null
Copy-Item $pluginJar $repoPlugins -Force
Copy-Item $featureJar $repoFeatures -Force

& (Join-Path $PSScriptRoot "generate-p2-metadata.ps1") `
    -RepositoryPath $repo `
    -Version $Version `
    -PluginJar $pluginJar `
    -FeatureJar $featureJar

$distRepo = Join-Path $dist "repository"
Remove-Item -Recurse -Force $distRepo -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $dist, $distRepo | Out-Null
Copy-Item -Path (Join-Path $repo "*") -Destination $distRepo -Recurse -Force

$zipPath = Join-Path $dist "copy-as-path-plugin-$Version.zip"
$asciiZip = Join-Path $p2Root "copy-as-path-plugin-$Version.zip"
& (Join-Path $PSScriptRoot "pack-p2-zip.ps1") -SourceDir $repo -ZipPath $asciiZip
Copy-Item $asciiZip $zipPath -Force

if ($CopyToDownloads) {
    $downloadsZip = Join-Path $env:USERPROFILE "Downloads\copy-as-path-plugin-$Version.zip"
    Copy-Item $asciiZip $downloadsZip -Force
    Write-Output "Also copied to $downloadsZip"
}

& (Join-Path $PSScriptRoot "verify-p2-zip.ps1") -ZipPath $asciiZip

if ($PublishDocs) {
    & (Join-Path $PSScriptRoot "publish-docs.ps1") -RepositoryPath $distRepo
}

Write-Output "Built $zipPath"
Write-Output "Plugin version: $Version"
