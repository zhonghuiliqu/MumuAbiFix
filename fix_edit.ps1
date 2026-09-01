param([string]$Path)
$ErrorActionPreference = 'Stop'
$lines = Get-Content -Path $Path -Encoding UTF8
$changed = 0
for ($i = 0; $i -lt $lines.Count; $i++) {
  $line = $lines[$i]
  $m = [regex]::Match($line, '^\s*(\S+)\s+(\S+)')
  if ($m.Success) {
    $pkg = $m.Groups[1].Value -replace '\\\.', '.'
    if ($pkg -like 'com.bilibili.priconne*') {
      $new = $m.Groups[1].Value + ' x86_64' + $line.Substring($m.Groups[2].Index + $m.Groups[2].Length)
      $lines[$i] = $new
      $changed++
    }
  }
}
if ($changed -gt 0) { Set-Content -Path $Path -Value $lines -Encoding UTF8 }
Write-Output "CHANGED=$changed"
