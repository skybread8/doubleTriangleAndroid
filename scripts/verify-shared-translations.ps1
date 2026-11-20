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

# iOS has four body metric names absent from the Sheet. Validate the local
# fallback set separately from the generated shared catalogue.
$bodyMetricRoot = Join-Path $PSScriptRoot '..\feature\onboarding\src\main\res'
[xml]$bodyMetricBase = Get-Content (Join-Path $bodyMetricRoot 'values\body_metric_labels.xml') -Raw -Encoding utf8
$bodyMetricNames = @($bodyMetricBase.resources.string | ForEach-Object { $_.name })
foreach ($directory in $onboardingLocales.Values) {
    [xml]$localizedBodyMetrics = Get-Content (Join-Path $bodyMetricRoot "$directory\body_metric_labels.xml") -Raw -Encoding utf8
    $names = @($localizedBodyMetrics.resources.string | ForEach-Object { $_.name })
    if (@(Compare-Object $bodyMetricNames $names).Count -or @($localizedBodyMetrics.resources.string | Where-Object { [string]::IsNullOrWhiteSpace($_.'#text') }).Count) {
        throw "Incomplete body metric fallbacks in $directory"
    }
}
Write-Output "Body metrics: $($bodyMetricNames.Count) fallback keys available in all 10 locales."

# Set-style and mesocycle names absent from the Sheet have local fallbacks.
$workoutLabelRoot = Join-Path $PSScriptRoot '..\feature\workout\src\main\res'
[xml]$workoutLabelBase = Get-Content (Join-Path $workoutLabelRoot 'values\workout_local_labels.xml') -Raw -Encoding utf8
$workoutLabelNames = @($workoutLabelBase.resources.string | ForEach-Object { $_.name })
foreach ($directory in $onboardingLocales.Values) {
    [xml]$localizedWorkoutLabels = Get-Content (Join-Path $workoutLabelRoot "$directory\workout_local_labels.xml") -Raw -Encoding utf8
    $names = @($localizedWorkoutLabels.resources.string | ForEach-Object { $_.name })
    if (@(Compare-Object $workoutLabelNames $names).Count -or @($localizedWorkoutLabels.resources.string | Where-Object { [string]::IsNullOrWhiteSpace($_.'#text') }).Count) {
        throw "Incomplete workout label fallbacks in $directory"
    }
}
Write-Output "Workout labels: $($workoutLabelNames.Count) fallback keys available in all 10 locales."

# Background notification and accessibility fallbacks fill only untranslated Sheet cells.
$backgroundNames = $null
foreach ($directory in @('values-ja', 'values-nl', 'values-zh-rCN')) {
    $fallback = Join-Path $ResRoot "$directory\background_local_fallbacks.xml"
    [xml]$local = Get-Content -LiteralPath $fallback -Raw -Encoding utf8
    [xml]$sheet = Get-Content -LiteralPath (Join-Path $ResRoot "$directory\sheet_translations.xml") -Raw -Encoding utf8
    $sheetNames = @($sheet.resources.string | ForEach-Object { $_.name })
    $names = @($local.resources.string | ForEach-Object { $_.name })
    if (@($names | Where-Object { $_ -in $sheetNames -or $_ -notin $reference }).Count) { throw "Background fallback conflicts with the Sheet in $directory" }
    if (@($local.resources.string | Where-Object { [string]::IsNullOrWhiteSpace($_.'#text') }).Count) { throw "Empty background fallback in $directory" }
    if ($null -eq $backgroundNames) { $backgroundNames = $names }
    elseif (@(Compare-Object $backgroundNames $names).Count) { throw "Inconsistent background fallback keys in $directory" }
}
Write-Output "Background and accessibility: $($backgroundNames.Count) Sheet gaps covered locally in ja, nl and zh."
# Android-only profile settings copy must be present in every supported locale.
$settingsRoot = Join-Path $PSScriptRoot '..\feature\onboarding\src\main\res'
[xml]$settingsBase = Get-Content (Join-Path $settingsRoot 'values\android_settings_labels.xml') -Raw -Encoding utf8
$settingsNames = @($settingsBase.resources.string | ForEach-Object { $_.name })
foreach ($directory in $onboardingLocales.Values) {
    [xml]$settings = Get-Content (Join-Path $settingsRoot "$directory\android_settings_labels.xml") -Raw -Encoding utf8
    $names = @($settings.resources.string | ForEach-Object { $_.name })
    if (@(Compare-Object $settingsNames $names).Count -or @($settings.resources.string | Where-Object { [string]::IsNullOrWhiteSpace($_.'#text') }).Count) {
        throw "Incomplete Android settings translations in $directory"
    }
}
Write-Output "Android settings: $($settingsNames.Count) local labels available in all 10 locales."
# These aliases use the Sheet normally and replace cells that are absent or
# still contain the English source in the affected locales.
[xml]$settingsGapBase = Get-Content (Join-Path $settingsRoot 'values\settings_sheet_gaps.xml') -Raw -Encoding utf8
foreach ($directory in @('values-ja', 'values-nl', 'values-zh-rCN')) {
    [xml]$local = Get-Content (Join-Path $settingsRoot "$directory\settings_sheet_gaps.xml") -Raw -Encoding utf8
    [xml]$sheet = Get-Content (Join-Path $ResRoot "$directory\sheet_translations.xml") -Raw -Encoding utf8
    [xml]$english = Get-Content (Join-Path $ResRoot 'values\sheet_translations.xml') -Raw -Encoding utf8
    $sheetValues = @{}; foreach ($entry in $sheet.resources.string) { $sheetValues[$entry.name] = $entry.'#text' }
    $englishValues = @{}; foreach ($entry in $english.resources.string) { $englishValues[$entry.name] = $entry.'#text' }
    foreach ($entry in $local.resources.string) {
        $name = $entry.name.Substring('settings_sheet_'.Length)
        if (!$englishValues.ContainsKey($name) -or [string]::IsNullOrWhiteSpace($entry.'#text')) { throw "Invalid settings Sheet fallback $name in $directory" }
        if ($sheetValues.ContainsKey($name) -and $sheetValues[$name] -ne $englishValues[$name]) { throw "Sheet now translates $name in $directory; remove the local fallback" }
    }
    if (@(Compare-Object @($settingsGapBase.resources.string | ForEach-Object { $_.name }) @($local.resources.string | ForEach-Object { $_.name })).Count) { throw "Inconsistent settings Sheet fallbacks in $directory" }
}
Write-Output "Settings Sheet gaps: 8 localized aliases checked in ja, nl and zh."

& (Join-Path $PSScriptRoot 'verify-onboarding-titles.ps1')
# Shared labels with untranslated Sheet cells must use the Sheet where it has a
# translated value, and any local replacements must retain formatting arguments.
[xml]$uiBase = Get-Content (Join-Path $ResRoot 'values\ui_sheet_fallbacks.xml') -Raw -Encoding utf8
[xml]$uiEnglish = Get-Content (Join-Path $ResRoot 'values\sheet_translations.xml') -Raw -Encoding utf8
$uiEnglishByName = @{}; foreach ($entry in $uiEnglish.resources.string) { $uiEnglishByName[$entry.name] = $entry.'#text' }
foreach ($locale in @('es','ca','de','fr','it','pt','nl','ja','zh-rCN')) {
    [xml]$uiSheet = Get-Content (Join-Path $ResRoot "values-$locale\sheet_translations.xml") -Raw -Encoding utf8
    [xml]$uiLocal = Get-Content (Join-Path $ResRoot "values-$locale\ui_sheet_fallbacks.xml") -Raw -Encoding utf8
    $uiSheetByName = @{}; foreach ($entry in $uiSheet.resources.string) { $uiSheetByName[$entry.name] = $entry.'#text' }
    $uiLocalByName = @{}; foreach ($entry in $uiLocal.resources.string) { $uiLocalByName[$entry.name] = $entry.'#text' }
    foreach ($alias in $uiBase.resources.string) {
        $key = $alias.name.Substring('ui_sheet_'.Length)
        $untranslated = !$uiSheetByName.ContainsKey($key) -or $uiSheetByName[$key] -ceq $uiEnglishByName[$key]
        if ($untranslated -and (!$uiLocalByName.ContainsKey($alias.name) -or [string]::IsNullOrWhiteSpace($uiLocalByName[$alias.name]))) { throw "Missing UI fallback $key in $locale" }
    }
    foreach ($entry in $uiLocal.resources.string) {
        $key = $entry.name.Substring('ui_sheet_'.Length)
        if (!$uiEnglishByName.ContainsKey($key) -or [string]::IsNullOrWhiteSpace($entry.'#text')) { throw "Invalid UI fallback $key in $locale" }
        if ($uiSheetByName.ContainsKey($key) -and $uiSheetByName[$key] -ne $uiEnglishByName[$key]) { throw "Sheet now translates $key in $locale; remove its local fallback" }
        $sourceArgs = @([regex]::Matches($uiEnglishByName[$key], '%\d+\$[ds]') | ForEach-Object { $_.Value } | Sort-Object)
        $localArgs = @([regex]::Matches($entry.'#text', '%\d+\$[ds]') | ForEach-Object { $_.Value } | Sort-Object)
        if (@(Compare-Object $sourceArgs $localArgs).Count) { throw "UI format arguments differ for $key in $locale" }
    }
}
Write-Output "Common UI: $(@($uiBase.resources.string).Count) Sheet aliases checked for locale overlap and format arguments."
# Four iOS workout day-type names have no Sheet rows.
[xml]$daytypeBase = Get-Content (Join-Path $workoutLabelRoot 'values\workout_daytype_labels.xml') -Raw -Encoding utf8
$daytypeNames = @($daytypeBase.resources.string | ForEach-Object { $_.name })
foreach ($directory in $onboardingLocales.Values) {
    [xml]$localized = Get-Content (Join-Path $workoutLabelRoot "$directory\workout_daytype_labels.xml") -Raw -Encoding utf8
    $names = @($localized.resources.string | ForEach-Object { $_.name })
    if (@(Compare-Object $daytypeNames $names).Count -or @($localized.resources.string | Where-Object { [string]::IsNullOrWhiteSpace($_.'#text') }).Count) { throw "Incomplete workout day-type fallbacks in $directory" }
}
Write-Output "Workout day types: $($daytypeNames.Count) local labels available in all 10 locales."

# FAQ content is Android-specific and has no corresponding Sheet entries.
$faqRoot = Join-Path $PSScriptRoot '..\feature\onboarding\src\main\res'
foreach ($directory in $locales) {
    [xml]$faq = Get-Content (Join-Path $faqRoot "$directory\onboarding_faq.xml") -Raw -Encoding utf8
    if (@($faq.resources.string).Count -ne 7 -or @($faq.resources.string | Where-Object { [string]::IsNullOrWhiteSpace($_.'#text') }).Count) { throw "Incomplete FAQ headings in $directory" }
    foreach ($name in @('faq_questions', 'faq_answers')) {
        $array = @($faq.resources.'string-array' | Where-Object { $_.name -eq $name })
        if ($array.Count -ne 1 -or @($array[0].item).Count -ne 23 -or @($array[0].item | Where-Object { [string]::IsNullOrWhiteSpace($_) }).Count) { throw "Incomplete FAQ $name in $directory" }
    }
}
Write-Output 'FAQ: 23 questions and answers available in all 10 locales.'
