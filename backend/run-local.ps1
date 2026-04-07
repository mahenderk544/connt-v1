# Run Spring Boot with the "local" profile (uses application-local.yml if present).
# Create application-local.yml from src/main/resources/application-local.example.yml first.
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
$env:SPRING_PROFILES_ACTIVE = "local"
if (-not (Test-Path "src/main/resources/application-local.yml")) {
    Write-Host "Missing src/main/resources/application-local.yml" -ForegroundColor Yellow
    Write-Host "Copy application-local.example.yml to application-local.yml and fill in RDS URL, password, JWT secret." -ForegroundColor Yellow
    exit 1
}
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
