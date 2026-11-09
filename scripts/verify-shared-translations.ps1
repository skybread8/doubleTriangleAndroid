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
    $pending = @($fallbacks.resources.string | Where-Object { $_.'#text' -notlike '@string/*' }).Count
    Write-Output "Sheet: $($reference.Count) base keys, $($locales.Count) locales; $pending Android fallbacks still need a verified Sheet key."
} else {
    Write-Output "Sheet: $($reference.Count) base keys, $($locales.Count) locales."
}
