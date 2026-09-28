param(
    [string]$IosCatalogPath = (Join-Path $PSScriptRoot '..\..\doubleTriangle\Packages\WildforceShared\Sources\WildforceShared\Resources\Localizable.xcstrings'),
    [string]$AndroidAppPath = (Join-Path $PSScriptRoot '..\app')
)

$ErrorActionPreference = 'Stop'
$ios = Get-Content -Raw $IosCatalogPath | ConvertFrom-Json -AsHashtable
$mapping = Get-Content -Raw (Join-Path $AndroidAppPath 'src\main\assets\ios_localization_catalog.json') | ConvertFrom-Json -AsHashtable
$iosKeys = @($ios.strings.Keys | Sort-Object)
$mappedKeys = @($mapping.entries | ForEach-Object { $_.iosKey } | Sort-Object)
$missing = Compare-Object $iosKeys $mappedKeys -PassThru | Where-Object { $_ -in $iosKeys }
$unexpected = Compare-Object $iosKeys $mappedKeys -PassThru | Where-Object { $_ -in $mappedKeys }
if ($missing -or $unexpected -or $iosKeys.Count -ne $mappedKeys.Count) {
    throw "Catalog mismatch. Missing: $($missing.Count); unexpected: $($unexpected.Count)."
}

[xml]$baseResources = Get-Content -Raw (Join-Path $AndroidAppPath 'src\main\res\values\ios_catalog.xml')
$resourceNames = @($baseResources.resources.string | ForEach-Object { $_.name })
$mappedResourceNames = @($mapping.entries | ForEach-Object { $_.androidResource })
if ((Compare-Object $resourceNames $mappedResourceNames) -or $resourceNames.Count -ne $mappedResourceNames.Count) {
    throw 'The Android base resource catalog does not match the mapping manifest.'
}

Write-Output "Verified $($iosKeys.Count) iOS keys and Android resources."
