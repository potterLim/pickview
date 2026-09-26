param(
    [string]$Maven = "mvn",
    [string]$Repository = "",
    [switch]$SkipBuild
)
$ErrorActionPreference = "Stop"
$projectRoot = Split-Path $PSScriptRoot -Parent
Push-Location (Join-Path $projectRoot "backend")
try {
    if (-not $SkipBuild) {
        $buildArguments = @("-q", "-Dmaven.compiler.fork=true", "-DskipTests", "package")
        if ($Repository) { $buildArguments += "-Dmaven.repo.local=$Repository" }
        & $Maven @buildArguments
        if ($LASTEXITCODE -ne 0) { throw "Backend build failed" }
    }
    node (Join-Path $projectRoot "scripts/prepare-media.mjs")
    if ($LASTEXITCODE -ne 0) { throw "Demo media preparation failed" }
    java -jar target/pickview-api-0.1.0.jar --spring.profiles.active=local
} finally { Pop-Location }
