param(
    [string]$Exporter = (Join-Path $PSScriptRoot '..\..\wildforce-back\scripts\export-android-translations.ps1'),
    [string]$ResRoot = (Join-Path $PSScriptRoot '..\core\designsystem\src\main\res'),
    [switch]$SkipDownload
)

$ErrorActionPreference = 'Stop'
if (!$SkipDownload) {
    & $Exporter -AndroidResPath $ResRoot
}

$locales = @('values-ca', 'values-de', 'values-es', 'values-fr', 'values-it', 'values-ja', 'values-nl', 'values-pt', 'values-zh-rCN')
foreach ($locale in $locales) {
    $path = Join-Path $ResRoot "$locale\sheet_translations.xml"
    if (!(Test-Path -LiteralPath $path)) { throw "Missing Sheet export: $path" }
    $text = Get-Content -LiteralPath $path -Raw -Encoding utf8
    # An empty override suppresses Android's normal fallback and renders blank.
    $text = [regex]::Replace($text, '(?m)^\s*<string\b[^>]*>\s*</string>\s*\r?\n', '')
    # Spreadsheet formula errors are not translations and must not reach UI.
    $text = [regex]::Replace($text, '(?m)^\s*<string\b[^>]*>\s*#(?:ERROR|REF|N/A|VALUE|DIV/0|NAME|NUM|NULL)[!?]?\s*</string>\s*\r?\n', '')
    [IO.File]::WriteAllText((Resolve-Path -LiteralPath $path), $text, [Text.UTF8Encoding]::new($false))
}

$basePath = Join-Path $ResRoot 'values\sheet_translations.xml'
$base = Get-Content -LiteralPath $basePath -Raw -Encoding utf8
# This row's English cell was empty in the 23 October export; its iOS key is
# "Create Profile". Once the Sheet supplies a value, preserve that value.
$base = [regex]::Replace($base, '(<string name="create_profile"[^>]*>)\s*(</string>)', '${1}Create Profile${2}')
[IO.File]::WriteAllText((Resolve-Path -LiteralPath $basePath), $base, [Text.UTF8Encoding]::new($false))

# In this Sheet entry the percent signs are literal, not format arguments.
# The exporter otherwise interprets the following word as a format conversion.
foreach ($locale in @('values') + $locales) {
    $path = Join-Path $ResRoot "$locale\sheet_translations.xml"
    $text = Get-Content -LiteralPath $path -Raw -Encoding utf8
    $text = [regex]::Replace($text, '<string name="the_power_should_come_60__from_legs__20__from_core__and_20__from_arms_"[^>]*>[^<]*</string>', {
        param($match)
        $line = $match.Value -replace '%\d+\$', '%'
        return $line.Replace('<string name="the_power_should_come_60__from_legs__20__from_core__and_20__from_arms_">', '<string name="the_power_should_come_60__from_legs__20__from_core__and_20__from_arms_" formatted="false">')
    })
    # Six plus-prefixed keys were exported as spreadsheet formulas (=+...).
    # Their visible text is the literal plus sign used by iOS.
    $text = [regex]::Replace($text, '(<string name="(?:_____in_this_period|_____since_first_log|_____vs_previous_best|__6mo|__15s|__30s)"[^>]*>)=\+', '${1}+')
    [IO.File]::WriteAllText((Resolve-Path -LiteralPath $path), $text, [Text.UTF8Encoding]::new($false))
}
& (Join-Path $PSScriptRoot 'sync-ios-sheet-fallbacks.ps1') -ResRoot $ResRoot
& (Join-Path $PSScriptRoot 'sync-sheet-tutorial-map.ps1')
& (Join-Path $PSScriptRoot 'sync-onboarding-sheet-map.ps1')
& (Join-Path $PSScriptRoot 'sync-android-fallback-translations.ps1') -ResRoot $ResRoot
& (Join-Path $PSScriptRoot 'verify-shared-translations.ps1') -ResRoot $ResRoot
