[CmdletBinding()]
param(
    [Parameter(Mandatory, Position = 0, ValueFromRemainingArguments)]
    [string[]] $Modules,

    [switch] $Integration
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$rootPom = Join-Path $repoRoot "pom.xml"

$moduleList = @(
    $Modules |
        ForEach-Object { $_ -split "[,\s]+" } |
        ForEach-Object { $_.Trim() } |
        Where-Object { $_ } |
        Select-Object -Unique
)

if ($moduleList.Count -eq 0) {
    throw "Provide at least one Maven module."
}

[xml] $pom = Get-Content -Raw -LiteralPath $rootPom
$declaredModules = @($pom.project.modules.module | ForEach-Object { ([string] $_).Trim() })
$unknownModules = @($moduleList | Where-Object { $_ -notin $declaredModules })

if ($unknownModules.Count -gt 0) {
    throw "Unknown Maven module(s): $($unknownModules -join ', ')"
}

$mavenArguments = @(
    "-f", $rootPom,
    "-pl", ($moduleList -join ","),
    "-am"
)

if ($Integration) {
    $mavenArguments += @("-Pintegration", "verify")
}
else {
    $mavenArguments += "test"
}

$mode = if ($Integration) { "unit and integration" } else { "unit" }
Write-Host "Verifying Maven module(s) [$($moduleList -join ', ')] in $mode mode."

Push-Location $repoRoot
try {
    & mvn @mavenArguments
    $exitCode = $LASTEXITCODE
}
finally {
    Pop-Location
}

exit $exitCode
