param(
    [string]$RepositoryPath,
    [string]$Version,
    [string]$PluginJar,
    [string]$FeatureJar
)

$ErrorActionPreference = "Stop"

$pluginName = "ru.cursor.edt.copypath.ui"
$featureName = "ru.cursor.edt.copypath.feature"
$featureGroupId = "$featureName.feature.group"
$featureJarId = "$featureName.feature.jar"
$categoryId = "ru.cursor.edt.copypath.category"
$categoryVersion = $Version

$pluginSize = (Get-Item $PluginJar).Length
$featureSize = (Get-Item $FeatureJar).Length
$timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()

function Get-Sha256([string]$path) {
    (Get-FileHash -Path $path -Algorithm SHA256).Hash.ToLower()
}

$pluginSha256 = Get-Sha256 $PluginJar
$featureSha256 = Get-Sha256 $FeatureJar

$artifacts = @"
<?xml version='1.0' encoding='UTF-8'?>
<?artifactRepository version='1.1.0'?>
<repository name='Copy As Path for 1C:EDT $Version' type='org.eclipse.equinox.p2.artifact.repository.simpleRepository' version='1'>
  <properties size='2'>
    <property name='p2.timestamp' value='$timestamp'/>
    <property name='p2.compressed' value='false'/>
  </properties>
  <mappings size='3'>
    <rule filter='(&amp; (classifier=osgi.bundle))' output='`${repoUrl}/plugins/`${id}_`${version}.jar'/>
    <rule filter='(&amp; (classifier=binary))' output='`${repoUrl}/binary/`${id}_`${version}'/>
    <rule filter='(&amp; (classifier=org.eclipse.update.feature))' output='`${repoUrl}/features/`${id}_`${version}.jar'/>
  </mappings>
  <artifacts size='2'>
    <artifact classifier='osgi.bundle' id='$pluginName' version='$Version'>
      <properties size='3'>
        <property name='artifact.size' value='$pluginSize'/>
        <property name='download.size' value='$pluginSize'/>
        <property name='download.checksum.sha-256' value='$pluginSha256'/>
      </properties>
    </artifact>
    <artifact classifier='org.eclipse.update.feature' id='$featureName' version='$Version'>
      <properties size='3'>
        <property name='artifact.size' value='$featureSize'/>
        <property name='download.size' value='$featureSize'/>
        <property name='download.checksum.sha-256' value='$featureSha256'/>
      </properties>
    </artifact>
  </artifacts>
</repository>
"@

$content = @"
<?xml version='1.0' encoding='UTF-8'?>
<?metadataRepository version='1.2.0'?>
<repository name='Copy As Path for 1C:EDT $Version' type='org.eclipse.equinox.internal.p2.metadata.repository.LocalMetadataRepository' version='1'>
  <properties size='2'>
    <property name='p2.timestamp' value='$timestamp'/>
    <property name='p2.compressed' value='false'/>
  </properties>
  <units size='4'>
    <unit id='$pluginName' version='$Version'>
      <properties size='4'>
        <property name='org.eclipse.equinox.p2.name' value='Copy As Path for 1C:EDT'/>
        <property name='org.eclipse.equinox.p2.provider' value='Cursor'/>
        <property name='org.eclipse.equinox.p2.bundle.localization' value='plugin'/>
        <property name='df_LT.pluginName' value='Copy As Path for 1C:EDT'/>
      </properties>
      <provides size='5'>
        <provided namespace='org.eclipse.equinox.p2.iu' name='$pluginName' version='$Version'/>
        <provided namespace='osgi.bundle' name='$pluginName' version='$Version'/>
        <provided namespace='osgi.identity' name='$pluginName' version='$Version'>
          <properties size='1'>
            <property name='type' value='osgi.bundle'/>
          </properties>
        </provided>
        <provided namespace='org.eclipse.equinox.p2.eclipse.type' name='bundle' version='1.0.0'/>
        <provided namespace='org.eclipse.equinox.p2.localization' name='df_LT' version='1.0.0'/>
      </provides>
      <requires size='14'>
        <required namespace='osgi.bundle' name='org.eclipse.ui' range='0.0.0'/>
        <required namespace='osgi.bundle' name='org.eclipse.core.runtime' range='0.0.0'/>
        <required namespace='osgi.bundle' name='org.eclipse.core.resources' range='0.0.0'/>
        <required namespace='osgi.bundle' name='org.eclipse.ui.ide' range='0.0.0'/>
        <required namespace='osgi.bundle' name='org.eclipse.core.expressions' range='0.0.0'/>
        <required namespace='osgi.bundle' name='org.eclipse.emf.ecore' range='0.0.0'/>
        <required namespace='java.package' name='com._1c.g5.v8.dt.bsl.model' range='0.0.0'/>
        <required namespace='java.package' name='com._1c.g5.v8.dt.core.filesystem' range='0.0.0'/>
        <required namespace='java.package' name='com._1c.g5.v8.dt.core.platform' range='0.0.0'/>
        <required namespace='java.package' name='com._1c.g5.v8.dt.metadata.mdclass' range='0.0.0'/>
        <required namespace='java.package' name='org.eclipse.emf.common.util' range='0.0.0'/>
        <required namespace='java.package' name='org.eclipse.emf.ecore' range='0.0.0'/>
        <required namespace='java.package' name='org.eclipse.emf.ecore.resource' range='0.0.0'/>
        <requiredProperties namespace='osgi.ee' match='(&amp;(osgi.ee=JavaSE)(version=17))'/>
      </requires>
      <artifacts size='1'>
        <artifact classifier='osgi.bundle' id='$pluginName' version='$Version'/>
      </artifacts>
      <touchpoint id='org.eclipse.equinox.p2.osgi' version='1.0.0'/>
      <touchpointData size='1'>
        <instructions size='1'>
          <instruction key='manifest'>
            Bundle-SymbolicName: $pluginName;singleton:=true&#xA;Bundle-Version: $Version
          </instruction>
        </instructions>
      </touchpointData>
    </unit>
    <unit id='$featureJarId' version='$Version'>
      <properties size='5'>
        <property name='org.eclipse.equinox.p2.name' value='Copy As Path for 1C:EDT'/>
        <property name='org.eclipse.equinox.p2.description' value='Adds Copy as path command to 1C:EDT metadata navigator context menu'/>
        <property name='org.eclipse.equinox.p2.provider' value='Cursor'/>
        <property name='df_LT.featureName' value='Copy As Path for 1C:EDT'/>
        <property name='df_LT.providerName' value='Cursor'/>
      </properties>
      <provides size='3'>
        <provided namespace='org.eclipse.equinox.p2.iu' name='$featureJarId' version='$Version'/>
        <provided namespace='org.eclipse.equinox.p2.eclipse.type' name='feature' version='1.0.0'/>
        <provided namespace='org.eclipse.update.feature' name='$featureName' version='$Version'/>
      </provides>
      <filter>
        (org.eclipse.update.install.features=true)
      </filter>
      <artifacts size='1'>
        <artifact classifier='org.eclipse.update.feature' id='$featureName' version='$Version'/>
      </artifacts>
      <touchpoint id='org.eclipse.equinox.p2.osgi' version='1.0.0'/>
      <touchpointData size='1'>
        <instructions size='1'>
          <instruction key='zipped'>
            true
          </instruction>
        </instructions>
      </touchpointData>
      <licenses size='1'>
        <license uri='http://www.eclipse.org/legal/epl-2.0' url='http://www.eclipse.org/legal/epl-2.0'>
          Eclipse Public License 2.0
        </license>
      </licenses>
      <copyright>
        Copyright (c) 2026
      </copyright>
    </unit>
    <unit id='$featureGroupId' version='$Version' singleton='false'>
      <properties size='5'>
        <property name='org.eclipse.equinox.p2.name' value='Copy As Path for 1C:EDT'/>
        <property name='org.eclipse.equinox.p2.description' value='Adds Copy as path command to 1C:EDT metadata navigator context menu'/>
        <property name='org.eclipse.equinox.p2.provider' value='Cursor'/>
        <property name='org.eclipse.equinox.p2.type.group' value='true'/>
        <property name='df_LT.featureName' value='Copy As Path for 1C:EDT'/>
      </properties>
      <provides size='2'>
        <provided namespace='org.eclipse.equinox.p2.iu' name='$featureGroupId' version='$Version'/>
        <provided namespace='org.eclipse.equinox.p2.localization' name='df_LT' version='1.0.0'/>
      </provides>
      <requires size='2'>
        <required namespace='org.eclipse.equinox.p2.iu' name='$pluginName' range='[$Version,$Version]'/>
        <required namespace='org.eclipse.equinox.p2.iu' name='$featureJarId' range='[$Version,$Version]'>
          <filter>
            (org.eclipse.update.install.features=true)
          </filter>
        </required>
      </requires>
      <touchpoint id='null' version='0.0.0'/>
      <licenses size='1'>
        <license uri='http://www.eclipse.org/legal/epl-2.0' url='http://www.eclipse.org/legal/epl-2.0'>
          Eclipse Public License 2.0
        </license>
      </licenses>
      <copyright>
        Copyright (c) 2026
      </copyright>
    </unit>
    <unit id='$categoryId' version='$categoryVersion'>
      <properties size='3'>
        <property name='org.eclipse.equinox.p2.name' value='Copy As Path for 1C:EDT'/>
        <property name='org.eclipse.equinox.p2.description' value='Copy as path command for 1C:EDT metadata navigator'/>
        <property name='org.eclipse.equinox.p2.type.category' value='true'/>
      </properties>
      <provides size='1'>
        <provided namespace='org.eclipse.equinox.p2.iu' name='$categoryId' version='$categoryVersion'/>
      </provides>
      <requires size='1'>
        <required namespace='org.eclipse.equinox.p2.iu' name='$featureGroupId' range='[$Version,$Version]'/>
      </requires>
      <touchpoint id='null' version='0.0.0'/>
    </unit>
  </units>
</repository>
"@

$utf8NoBom = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText((Join-Path $RepositoryPath "artifacts.xml"), $artifacts, $utf8NoBom)
[System.IO.File]::WriteAllText((Join-Path $RepositoryPath "content.xml"), $content, $utf8NoBom)

$p2Index = @"
version=1
metadata.repository.factory.order=content.xml,\!
artifact.repository.factory.order=artifacts.xml,\!
"@
[System.IO.File]::WriteAllText((Join-Path $RepositoryPath "p2.index"), $p2Index, $utf8NoBom)

Write-Output "P2 metadata generated: $RepositoryPath"
