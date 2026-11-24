param(
    [string]$ResRoot = (Join-Path $PSScriptRoot '..\core\designsystem\src\main\res'),
    [string]$OutputPath = (Join-Path $PSScriptRoot '..\docs\sheet-missing-cells.tsv'),
    [string]$UnchangedPath = (Join-Path $PSScriptRoot '..\docs\sheet-unchanged-candidates.tsv')
)
$ErrorActionPreference = 'Stop'
[xml]$base = Get-Content -LiteralPath (Join-Path $ResRoot 'values\sheet_translations.xml') -Raw -Encoding utf8
$locales = [ordered]@{ ca='values-ca'; de='values-de'; es='values-es'; fr='values-fr'; it='values-it'; ja='values-ja'; nl='values-nl'; pt='values-pt'; 'zh-Hans'='values-zh-rCN' }
$present = @{}
$values = @{}
foreach ($locale in $locales.Keys) {
    [xml]$sheet = Get-Content -LiteralPath (Join-Path $ResRoot "$($locales[$locale])\sheet_translations.xml") -Raw -Encoding utf8
    $set = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)
    $byName = @{}
    foreach ($entry in $sheet.resources.string) { if (![string]::IsNullOrWhiteSpace($entry.'#text')) { [void]$set.Add($entry.name); $byName[$entry.name] = [string]$entry.'#text' } }
    $present[$locale] = $set
    $values[$locale] = $byName
}
$lines = [System.Collections.Generic.List[string]]::new()
$lines.Add("recurso_android`tingles_exportado`tidiomas_sin_celda_exportada")
foreach ($entry in $base.resources.string) {
    $missing = @($locales.Keys | Where-Object { !$present[$_].Contains($entry.name) })
    if (!$missing.Count) { continue }
    $english = ([string]$entry.'#text' -replace '[\t\r\n]+', ' ').Trim()
    $lines.Add("$($entry.name)`t$english`t$($missing -join ', ')")
}
New-Item -ItemType Directory -Force -Path (Split-Path -Parent $OutputPath) | Out-Null
[IO.File]::WriteAllLines([IO.Path]::GetFullPath($OutputPath), $lines, [Text.UTF8Encoding]::new($false))
Write-Output "Sheet coverage: $($lines.Count - 1) base keys have missing exported locale cells; report: $OutputPath"

# Identical text is only a review candidate: names, units and borrowed words
# can legitimately be the same in English and another language.
$unchanged = [System.Collections.Generic.List[string]]::new()
$unchanged.Add("recurso_android`tingles_exportado`tidiomas_con_valor_identico")
foreach ($entry in $base.resources.string) {
    $english = [string]$entry.'#text'
    if ($english.Length -lt 4 -or $english -notmatch '[A-Za-z]{3}') { continue }
    $matching = @($locales.Keys | Where-Object { $present[$_].Contains($entry.name) -and [string]::Equals($english, $values[$_][$entry.name], [StringComparison]::Ordinal) })
    if (!$matching.Count) { continue }
    $safeEnglish = ($english -replace '[\t\r\n]+', ' ').Trim()
    $unchanged.Add("$($entry.name)`t$safeEnglish`t$($matching -join ', ')")
}
New-Item -ItemType Directory -Force -Path (Split-Path -Parent $UnchangedPath) | Out-Null
[IO.File]::WriteAllLines([IO.Path]::GetFullPath($UnchangedPath), $unchanged, [Text.UTF8Encoding]::new($false))
Write-Output "Sheet review: $($unchanged.Count - 1) keys contain identical candidate values; report: $UnchangedPath"
