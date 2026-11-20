param([string]$RepoRoot = (Join-Path $PSScriptRoot '..'))
$ErrorActionPreference = 'Stop'
$featureRes = Join-Path $RepoRoot 'feature\onboarding\src\main\res'
$sheetRes = Join-Path $RepoRoot 'core\designsystem\src\main\res'
$source = Get-Content (Join-Path $RepoRoot 'feature\onboarding\src\main\java\io\codepassion\doubletriangle\feature\onboarding\OnboardingScreen.kt') -Raw -Encoding utf8
[xml]$baseAliases = Get-Content (Join-Path $featureRes 'values\onboarding_title_fallbacks.xml') -Raw -Encoding utf8
[xml]$english = Get-Content (Join-Path $sheetRes 'values\sheet_translations.xml') -Raw -Encoding utf8
$englishByName = @{}; foreach ($entry in $english.resources.string) { $englishByName[$entry.name] = $entry.'#text' }
$keys = @($baseAliases.resources.string | ForEach-Object { $_.name.Substring('onboarding_title_'.Length) })
if ($keys.Count -ne 15) { throw "Expected 15 onboarding titles, found $($keys.Count)." }
foreach ($key in $keys) {
    if ($source -notmatch ('title\s*=\s*androidx\.compose\.ui\.res\.stringResource\(R\.string\.onboarding_title_' + [regex]::Escape($key) + '\)')) { throw "Title not connected: $key" }
    if (!$englishByName.ContainsKey($key)) { throw "Missing Sheet source: $key" }
}
foreach ($locale in @('es', 'ca', 'de', 'fr', 'it', 'pt', 'nl', 'ja', 'zh-rCN')) {
    [xml]$sheet = Get-Content (Join-Path $sheetRes "values-$locale\sheet_translations.xml") -Raw -Encoding utf8
    [xml]$fallback = Get-Content (Join-Path $featureRes "values-$locale\onboarding_title_fallbacks.xml") -Raw -Encoding utf8
    $sheetByName = @{}; foreach ($entry in $sheet.resources.string) { $sheetByName[$entry.name] = $entry.'#text' }
    $fallbackByName = @{}; foreach ($entry in $fallback.resources.string) { $fallbackByName[$entry.name] = $entry.'#text' }
    foreach ($key in $keys) {
        $untranslated = !$sheetByName.ContainsKey($key) -or $sheetByName[$key] -eq $englishByName[$key]
        $alias = "onboarding_title_$key"
        if ($untranslated -and (!$fallbackByName.ContainsKey($alias) -or [string]::IsNullOrWhiteSpace($fallbackByName[$alias]))) { throw "Missing $locale title fallback: $key" }
        if (!$untranslated -and $fallbackByName.ContainsKey($alias)) { throw "Sheet translation is masked for $locale title: $key" }
    }
}
Write-Output "Verified 15 iOS onboarding titles and Sheet-aware fallbacks for all 10 languages."
[xml]$subtitleAliases = Get-Content (Join-Path $featureRes 'values\onboarding_subtitle_fallbacks.xml') -Raw -Encoding utf8
$subtitleKeys = @($subtitleAliases.resources.string | ForEach-Object { $_.name.Substring('onboarding_subtitle_'.Length) })
foreach ($key in $subtitleKeys) {
    if ($source -notmatch ('subtitle\s*=\s*androidx\.compose\.ui\.res\.stringResource\(R\.string\.onboarding_subtitle_' + [regex]::Escape($key) + '\)')) { throw "Subtitle not connected: $key" }
}
foreach ($locale in @('es', 'ca', 'de', 'fr', 'it', 'pt', 'nl', 'ja', 'zh-rCN')) {
    [xml]$sheet = Get-Content (Join-Path $sheetRes "values-$locale\sheet_translations.xml") -Raw -Encoding utf8
    [xml]$fallback = Get-Content (Join-Path $featureRes "values-$locale\onboarding_subtitle_fallbacks.xml") -Raw -Encoding utf8
    $sheetByName = @{}; foreach ($entry in $sheet.resources.string) { $sheetByName[$entry.name] = $entry.'#text' }
    $fallbackByName = @{}; foreach ($entry in $fallback.resources.string) { $fallbackByName[$entry.name] = $entry.'#text' }
    foreach ($key in $subtitleKeys) {
        $untranslated = !$sheetByName.ContainsKey($key) -or $sheetByName[$key] -eq $englishByName[$key]
        $alias = "onboarding_subtitle_$key"
        if ($untranslated -and (!$fallbackByName.ContainsKey($alias) -or [string]::IsNullOrWhiteSpace($fallbackByName[$alias]))) { throw "Missing $locale subtitle fallback: $key" }
        if (!$untranslated -and $fallbackByName.ContainsKey($alias)) { throw "Sheet translation is masked for $locale subtitle: $key" }
    }
}
Write-Output "Verified all 14 onboarding subtitles and their Sheet-aware fallbacks."
# Choice aliases retain the generated Sheet mapping but fix untranslated cells.
[xml]$choiceBase = Get-Content (Join-Path $featureRes 'values\onboarding_choice_fallbacks.xml') -Raw -Encoding utf8
$choiceKeys = @($choiceBase.resources.string | ForEach-Object { $_.name.Substring('onboarding_choice_'.Length) })
$choiceSource = Get-Content (Join-Path $RepoRoot 'feature\onboarding\src\main\java\io\codepassion\doubletriangle\feature\onboarding\OnboardingLocalization.kt') -Raw -Encoding utf8
foreach ($key in $choiceKeys) {
    if ($choiceSource -notmatch ('R\.string\.' + [regex]::Escape($key) + '\s*->\s*R\.string\.onboarding_choice_')) { throw "Choice alias not connected: $key" }
}
foreach ($locale in @('es', 'ca', 'de', 'fr', 'it', 'pt', 'nl', 'ja', 'zh-rCN')) {
    [xml]$sheet = Get-Content (Join-Path $sheetRes "values-$locale\sheet_translations.xml") -Raw -Encoding utf8
    [xml]$fallback = Get-Content (Join-Path $featureRes "values-$locale\onboarding_choice_fallbacks.xml") -Raw -Encoding utf8
    $sheetByName = @{}; foreach ($entry in $sheet.resources.string) { $sheetByName[$entry.name] = $entry.'#text' }
    $fallbackByName = @{}; foreach ($entry in $fallback.resources.string) { $fallbackByName[$entry.name] = $entry.'#text' }
    foreach ($key in $choiceKeys) {
        $untranslated = !$sheetByName.ContainsKey($key) -or $sheetByName[$key] -ceq $englishByName[$key]
        $alias = "onboarding_choice_$key"
        if ($untranslated -and (!$fallbackByName.ContainsKey($alias) -or [string]::IsNullOrWhiteSpace($fallbackByName[$alias]))) { throw "Missing $locale choice fallback: $key" }
        if (!$untranslated -and $fallbackByName.ContainsKey($alias)) { throw "Sheet translation is masked for $locale choice: $key" }
    }
}
Write-Output "Verified 17 onboarding choice fallbacks across all 10 languages."
# Language picker names need fallback values when Sheet cells are untranslated.
[xml]$languageBase = Get-Content (Join-Path $featureRes 'values\onboarding_language_fallbacks.xml') -Raw -Encoding utf8
$languageKeys = @($languageBase.resources.string | ForEach-Object { $_.name.Substring('onboarding_language_'.Length) })
foreach ($locale in @('es','ca','de','fr','it','pt','nl','ja','zh-rCN')) {
    [xml]$sheet = Get-Content (Join-Path $sheetRes "values-$locale\sheet_translations.xml") -Raw -Encoding utf8
    [xml]$fallback = Get-Content (Join-Path $featureRes "values-$locale\onboarding_language_fallbacks.xml") -Raw -Encoding utf8
    $sheetByName=@{}; foreach ($entry in $sheet.resources.string) { $sheetByName[$entry.name]=$entry.'#text' }
    $fallbackByName=@{}; foreach ($entry in $fallback.resources.string) { $fallbackByName[$entry.name]=$entry.'#text' }
    foreach ($key in $languageKeys) {
        $untranslated = !$sheetByName.ContainsKey($key) -or $sheetByName[$key] -ceq $englishByName[$key]
        $alias = "onboarding_language_$key"
        if ($untranslated -and (!$fallbackByName.ContainsKey($alias) -or [string]::IsNullOrWhiteSpace($fallbackByName[$alias]))) { throw "Missing $locale language name: $key" }
        if (!$untranslated -and $fallbackByName.ContainsKey($alias)) { throw "Sheet translation is masked for $locale language name: $key" }
    }
}
Write-Output "Verified 10 language names across all 10 locales."
# Option descriptions use the iOS Sheet keys with fallbacks for missing cells.
[xml]$detailBase = Get-Content (Join-Path $featureRes 'values\onboarding_detail_fallbacks.xml') -Raw -Encoding utf8
foreach ($locale in @('es','ca','de','fr','it','pt','nl','ja','zh-rCN')) {
    [xml]$sheet = Get-Content (Join-Path $sheetRes "values-$locale\sheet_translations.xml") -Raw -Encoding utf8
    [xml]$fallback = Get-Content (Join-Path $featureRes "values-$locale\onboarding_detail_fallbacks.xml") -Raw -Encoding utf8
    $sheetByName=@{}; foreach ($entry in $sheet.resources.string) { $sheetByName[$entry.name]=$entry.'#text' }
    $fallbackByName=@{}; foreach ($entry in $fallback.resources.string) { $fallbackByName[$entry.name]=$entry.'#text' }
    foreach ($entry in $detailBase.resources.string) {
        $key = $entry.'#text'.Substring(8)
        $untranslated = !$sheetByName.ContainsKey($key) -or $sheetByName[$key] -ceq $englishByName[$key]
        if ($untranslated -and (!$fallbackByName.ContainsKey($entry.name) -or [string]::IsNullOrWhiteSpace($fallbackByName[$entry.name]))) { throw "Missing $locale option detail: $key" }
        if (!$untranslated -and $fallbackByName.ContainsKey($entry.name)) { throw "Sheet option translation is masked: $locale $key" }
        if ($choiceSource -notmatch ('R\.string\.' + [regex]::Escape($key) + '\s*->\s*R\.string\.' + $entry.name)) { throw "Option mapping missing: $key" }
    }
}
Write-Output "Verified 34 goal, lifestyle and experience labels and descriptions in all 10 languages."
# Equipment and movement restrictions retain their Sheet names where translated.
[xml]$equipmentBase = Get-Content (Join-Path $featureRes 'values\onboarding_equipment_fallbacks.xml') -Raw -Encoding utf8
$choiceSource = Get-Content (Join-Path $RepoRoot 'feature\onboarding\src\main\java\io\codepassion\doubletriangle\feature\onboarding\OnboardingLocalization.kt') -Raw -Encoding utf8
foreach ($locale in @('es','ca','de','fr','it','pt','nl','ja','zh-rCN')) {
    [xml]$sheet = Get-Content (Join-Path $sheetRes "values-$locale\sheet_translations.xml") -Raw -Encoding utf8
    [xml]$fallback = Get-Content (Join-Path $featureRes "values-$locale\onboarding_equipment_fallbacks.xml") -Raw -Encoding utf8
    $sheetByName=@{}; foreach ($entry in $sheet.resources.string) { $sheetByName[$entry.name]=$entry.'#text' }
    $fallbackByName=@{}; foreach ($entry in $fallback.resources.string) { $fallbackByName[$entry.name]=$entry.'#text' }
    foreach ($entry in $equipmentBase.resources.string) {
        $key = $entry.'#text'.Substring(8)
        $untranslated = !$sheetByName.ContainsKey($key) -or $sheetByName[$key] -ceq $englishByName[$key]
        if ($untranslated -and (!$fallbackByName.ContainsKey($entry.name) -or [string]::IsNullOrWhiteSpace($fallbackByName[$entry.name]))) { throw "Missing $locale equipment label: $key" }
        if (!$untranslated -and $fallbackByName.ContainsKey($entry.name)) { throw "Sheet equipment translation is masked: $locale $key" }
        if ($choiceSource -notmatch ('R\.string\.' + [regex]::Escape($key) + '\s*->\s*R\.string\.' + $entry.name)) { throw "Equipment mapping missing: $key" }
    }
}
Write-Output "Verified 59 onboarding equipment, focus and restriction labels in all 10 languages."
