param(
    [string]$IosCatalog = (Join-Path $PSScriptRoot '..\..\doubleTriangle\Packages\WildforceShared\Sources\WildforceShared\Resources\Localizable.xcstrings'),
    [string]$ResRoot = (Join-Path $PSScriptRoot '..\core\designsystem\src\main\res')
)
$ErrorActionPreference = 'Stop'
$ios = Get-Content -LiteralPath $IosCatalog -Raw -Encoding utf8 | ConvertFrom-Json -AsHashtable
[xml]$base = Get-Content -LiteralPath (Join-Path $ResRoot 'values\sheet_translations.xml') -Raw -Encoding utf8
$baseByName = @{}; foreach ($entry in $base.resources.string) { $baseByName[$entry.name] = $entry.'#text' }
$keywords = @('abstract','assert','boolean','break','byte','case','catch','char','class','const','continue','default','do','double','else','enum','extends','final','finally','float','for','goto','if','implements','import','instanceof','int','interface','long','native','new','package','private','protected','public','return','short','static','strictfp','super','switch','synchronized','this','throw','throws','transient','try','void','volatile','while','true','false','null')
$formatPattern = '(?<!%)%(?<position>\d+\$)?(?<flags>[-+# 0,(<]*)(?<width>\d*)?(?<precision>\.\d+)?(?<length>hh|h|ll|l|z|t|j)?(?<type>[@diuoxXfFeEgGaAcCs])'
function Convert-Format([string]$value) {
    $state = [pscustomobject]@{ Next = 0 }
    return [regex]::Replace($value, $formatPattern, {
        param($match)
        $position = $match.Groups['position'].Value
        if (!$position) { $state.Next++; $position = "$($state.Next)`$" }
        $type = $match.Groups['type'].Value
        if ($type -eq '@') { $type = 's' } elseif ($type -eq 'i' -or $type -eq 'u') { $type = 'd' }
        return '%' + $position + $match.Groups['flags'].Value + $match.Groups['width'].Value + $match.Groups['precision'].Value + $type
    })
}
function Escape-Android([string]$value) {
    $value = $value.Replace('\','\\').Replace("`r`n",'\n').Replace("`n",'\n').Replace("'","\'").Replace('"','\"')
    return [System.Security.SecurityElement]::Escape($value)
}
function Get-FormatSignature([string]$value) {
    return (@([regex]::Matches($value, '(?<!%)%\d+\$[-+# 0,(<]*\d*(?:\.\d+)?[a-zA-Z]') | ForEach-Object { $_.Value } | Sort-Object) -join '|')
}
$candidates = @{}
foreach ($key in $ios.strings.Keys) {
    $name = [regex]::Replace($key.Replace('-','_').Replace(' ','_').ToLowerInvariant(), '[^a-z0-9_]', '_')
    if (!$name) { continue }
    if ([char]::IsDigit($name[0]) -or $name -in $keywords) { $name = "key_$name" }
    if (!$baseByName.ContainsKey($name)) { continue }
    $english = $ios.strings[$key].localizations.en.stringUnit.value
    if (!$english) { $english = $key }
    $baseValue = $baseByName[$name].Replace("\'", "'").Replace('\"','"').Replace('\\','\')
    if ((Convert-Format $english) -cne $baseValue) { continue }
    if (!$candidates.ContainsKey($name)) { $candidates[$name] = [System.Collections.Generic.List[string]]::new() }
    $candidates[$name].Add($key)
}
$locales = [ordered]@{ ca='values-ca'; de='values-de'; es='values-es'; fr='values-fr'; it='values-it'; ja='values-ja'; nl='values-nl'; pt='values-pt'; 'zh-Hans'='values-zh-rCN' }
$total = 0
foreach ($locale in $locales.Keys) {
    $directory = Join-Path $ResRoot $locales[$locale]
    [xml]$sheet = Get-Content -LiteralPath (Join-Path $directory 'sheet_translations.xml') -Raw -Encoding utf8
    $present = @{}; foreach ($entry in $sheet.resources.string) { $present[$entry.name] = $true }
    $lines = [System.Collections.Generic.List[string]]::new()
    $lines.Add('<?xml version="1.0" encoding="utf-8"?>')
    $lines.Add('<!-- Generated from iOS only where the shared Sheet has no translation. -->')
    $lines.Add('<resources>')
    foreach ($name in ($candidates.Keys | Sort-Object)) {
        if ($present.ContainsKey($name) -or $candidates[$name].Count -ne 1) { continue }
        $value = $ios.strings[$candidates[$name][0]].localizations[$locale].stringUnit.value
        if ([string]::IsNullOrWhiteSpace($value) -or $value -match '^#(?:ERROR|REF|N/A|VALUE|DIV/0|NAME|NUM|NULL)[!?]?$') { continue }
        $formatted = [regex]::IsMatch($value, $formatPattern)
        $value = Convert-Format $value
        if ((Get-FormatSignature $value) -cne (Get-FormatSignature $baseByName[$name])) { continue }
        $value = Escape-Android $value
        $attribute = if ($formatted) { '' } else { ' formatted="false"' }
        $lines.Add("    <string name=`"$name`"$attribute>$value</string>")
        $total++
    }
    $lines.Add('</resources>')
    [IO.File]::WriteAllLines((Join-Path $directory 'ios_sheet_fallbacks.xml'), $lines, [Text.UTF8Encoding]::new($false))
}
Write-Output "Generated $total iOS fallback translations for missing Sheet cells."
