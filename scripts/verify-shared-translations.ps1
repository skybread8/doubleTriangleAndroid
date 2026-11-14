param([string]$ResRoot = (Join-Path $PSScriptRoot '..\core\designsystem\src\main\res'))

$ErrorActionPreference = 'Stop'
$locales = @('values', 'values-ca', 'values-de', 'values-es', 'values-fr', 'values-it', 'values-ja', 'values-nl', 'values-pt', 'values-zh-rCN')
$reference = $null
foreach ($locale in $locales) {
    $path = Join-Path $ResRoot "$locale\sheet_translations.xml"
    if (!(Test-Path -LiteralPath $path)) { throw "Missing Sheet export: $path" }
    [xml]$resources = Get-Content -LiteralPath $path -Raw -Encoding utf8
    $empty = @($resources.resources.string | Where-Object { [string]::IsNullOrWhiteSpace($_.'#text') })
    if ($empty.Count) { throw "Empty Sheet resources in $path`: $(($empty | ForEach-Object { $_.name }) -join ', ')" }
    $errors = @($resources.resources.string | Where-Object { $_.'#text' -match '^#(?:ERROR|REF|N/A|VALUE|DIV/0|NAME|NUM|NULL)[!?]?$' })
    if ($errors.Count) { throw "Spreadsheet errors in $path`: $(($errors | ForEach-Object { $_.name }) -join ', ')" }
    $names = @($resources.resources.string | ForEach-Object { $_.name })
    if (($names | Select-Object -Unique).Count -ne $names.Count) { throw "Duplicate Sheet resource in $path" }
    if ($locale -eq 'values') {
        $reference = $names
        $referenceSet = [System.Collections.Generic.HashSet[string]]::new([string[]]$reference, [System.StringComparer]::Ordinal)
    }
    else {
        $extra = @($names | Where-Object { !$referenceSet.Contains($_) })
        if ($extra.Count) { throw "Unknown Sheet resources in $path`: $($extra -join ', ')" }
        $iosPath = Join-Path $ResRoot "$locale\ios_sheet_fallbacks.xml"
        if (Test-Path -LiteralPath $iosPath) {
            [xml]$iosFallback = Get-Content -LiteralPath $iosPath -Raw -Encoding utf8
            $sheetSet = [System.Collections.Generic.HashSet[string]]::new([string[]]$names, [System.StringComparer]::Ordinal)
            $overlap = @($iosFallback.resources.string | Where-Object { $sheetSet.Contains($_.name) })
            if ($overlap.Count) { throw "iOS fallbacks override Sheet translations in $iosPath" }
        }
    }
}

$fallbackPath = Join-Path $ResRoot 'values\android_translation_fallbacks.xml'
if (Test-Path -LiteralPath $fallbackPath) {
    [xml]$fallbacks = Get-Content -LiteralPath $fallbackPath -Raw -Encoding utf8
    $missing = @($fallbacks.resources.string | Where-Object {
        $_.'#text' -like '@string/*' -and !$referenceSet.Contains($_.'#text'.Substring(8))
    })
    if ($missing.Count) { throw "Fallback aliases reference missing Sheet keys: $(($missing | ForEach-Object { $_.name }) -join ', ')" }
    $fallbackEntries = @($fallbacks.resources.string | Where-Object { $_.'#text' -notlike '@string/*' })
    $translated = $null
    foreach ($directory in @('values-en', 'values-ca', 'values-de', 'values-fr', 'values-it', 'values-ja', 'values-nl', 'values-pt', 'values-zh-rCN')) {
        $file = Join-Path $ResRoot "$directory\android_fallback_translations.xml"
        if (!(Test-Path -LiteralPath $file)) { throw "Missing Android fallback translations: $file" }
        [xml]$localized = Get-Content -LiteralPath $file -Raw -Encoding utf8
        $names = @($localized.resources.string | ForEach-Object { $_.name })
        if (@($names | Select-Object -Unique).Count -ne $names.Count) { throw "Duplicate fallback translation in $file" }
        $fallbackNames = @($fallbackEntries | ForEach-Object { $_.name })
        $unexpected = @($names | Where-Object { $_ -notin $fallbackNames })
        if ($unexpected.Count) { throw "Unknown or aliased fallback translations in $file`: $($unexpected -join ', ')" }
        if (@($localized.resources.string | Where-Object { [string]::IsNullOrWhiteSpace($_.'#text') }).Count) { throw "Empty fallback translation in $file" }
        if ($null -eq $translated) { $translated = $names }
        elseif (@(Compare-Object $translated $names).Count) { throw "Inconsistent fallback translation keys in $file" }
    }
    $pending = $fallbackEntries.Count - $translated.Count
    Write-Output "Sheet: $($reference.Count) base keys, $($locales.Count) locales; $($translated.Count) fallback texts translated locally, $pending still need translation."
} else {
    Write-Output "Sheet: $($reference.Count) base keys, $($locales.Count) locales."
}

# These iOS onboarding keys have no matching Sheet row. Keep the local
# translations complete until the Sheet supplies those exact keys.
$onboardingFallbacks = Get-Content (Join-Path $PSScriptRoot 'onboarding-local-fallbacks.json') -Raw -Encoding utf8 | ConvertFrom-Json -AsHashtable
[xml]$onboardingBase = Get-Content (Join-Path $ResRoot 'values\ios_onboarding_fallbacks.xml') -Raw -Encoding utf8
$onboardingNames = @($onboardingBase.resources.string | ForEach-Object { $_.name })
$onboardingLocales = [ordered]@{ ca='values-ca'; de='values-de'; es='values-es'; fr='values-fr'; it='values-it'; ja='values-ja'; nl='values-nl'; pt='values-pt'; 'zh-Hans'='values-zh-rCN' }
foreach ($locale in $onboardingLocales.Keys) {
    if (!$onboardingFallbacks.ContainsKey($locale)) { throw "Missing onboarding fallback locale: $locale" }
    [xml]$localized = Get-Content (Join-Path $ResRoot "$($onboardingLocales[$locale])\ios_onboarding_fallbacks.xml") -Raw -Encoding utf8
    $localizedByName = @{}; foreach ($entry in $localized.resources.string) { $localizedByName[$entry.name] = $entry.'#text' }
    foreach ($name in $onboardingNames) {
        if (!$localizedByName.ContainsKey($name) -or [string]::IsNullOrWhiteSpace($localizedByName[$name])) { throw "Missing onboarding translation $name in $locale" }
    }
}
Write-Output "Onboarding: $($onboardingNames.Count) iOS fallback keys available in all 10 locales."
