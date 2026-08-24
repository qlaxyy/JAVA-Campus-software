[CmdletBinding()]
param()

$ErrorActionPreference = 'Continue'
$failures = [System.Collections.Generic.List[string]]::new()

function Get-ToolOutput {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Command,
        [string[]]$Arguments = @()
    )

    try {
        $resolvedCommand = Get-Command $Command -ErrorAction SilentlyContinue
        if ($null -eq $resolvedCommand) {
            return $null
        }
        $output = & $resolvedCommand.Source @Arguments 2>&1 |
            ForEach-Object { $_.ToString() } |
            Out-String
        if ($LASTEXITCODE -ne 0) {
            return $null
        }
        return $output.Trim()
    }
    catch {
        return $null
    }
}

Write-Host 'JAVA Virtual Campus - environment check'
Write-Host 'Expected: Temurin JDK 8, Maven 3.9.16, Git, UTF-8'
Write-Host ''

$javaOutput = Get-ToolOutput -Command 'java' -Arguments @('-version')
if ($null -eq $javaOutput) {
    $failures.Add('java was not found. Install Temurin JDK 8 and configure JAVA_HOME/PATH.')
}
elseif ($javaOutput -notmatch 'version "1\.8\.0_') {
    $failures.Add('java is not JDK 8. Actual: ' + ($javaOutput -split "`r?`n")[0])
}
else {
    Write-Host ('[OK] Java: ' + ($javaOutput -split "`r?`n")[0])
}

$javacOutput = Get-ToolOutput -Command 'javac' -Arguments @('-version')
if ($null -eq $javacOutput -or $javacOutput -notmatch 'javac 1\.8\.0_') {
    $failures.Add('javac is not JDK 8 or is missing from PATH.')
}
else {
    Write-Host ('[OK] Compiler: ' + $javacOutput)
}

if ([string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
    $failures.Add('JAVA_HOME is not set.')
}
elseif (-not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
    $failures.Add('JAVA_HOME is not a valid JDK directory: ' + $env:JAVA_HOME)
}
else {
    Write-Host ('[OK] JAVA_HOME: ' + $env:JAVA_HOME)
}

$mavenOutput = Get-ToolOutput -Command 'mvn' -Arguments @('-version')
if ($null -eq $mavenOutput) {
    $failures.Add('Maven was not found. Install Apache Maven 3.9.16.')
}
else {
    $mavenLines = $mavenOutput -split "`r?`n"
    if ($mavenOutput -notmatch 'Apache Maven 3\.9\.16') {
        $failures.Add('Maven is not version 3.9.16. Actual: ' + $mavenLines[0])
    }
    if ($mavenOutput -notmatch 'Java version: 1\.8\.0_') {
        $failures.Add('Maven is not using JDK 8. Check JAVA_HOME and the IDE Maven Runner.')
    }
    if ($mavenOutput -match 'Apache Maven 3\.9\.16' -and $mavenOutput -match 'Java version: 1\.8\.0_') {
        Write-Host ('[OK] Maven: ' + $mavenLines[0])
    }
}

$gitOutput = Get-ToolOutput -Command 'git' -Arguments @('--version')
if ($null -eq $gitOutput) {
    $failures.Add('Git was not found.')
}
else {
    Write-Host ('[OK] ' + $gitOutput)
}

Write-Host ''
if ($failures.Count -gt 0) {
    Write-Host 'Environment check FAILED:' -ForegroundColor Red
    foreach ($failure in $failures) {
        Write-Host ('- ' + $failure) -ForegroundColor Red
    }
    exit 1
}

Write-Host 'Environment check PASSED.' -ForegroundColor Green
exit 0
