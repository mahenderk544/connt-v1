# Prints a URL-safe Base64 secret (256 bits) suitable for connto.jwt.secret (HS256).
# Usage: .\scripts\generate-jwt-secret.ps1
# Copy the output into application-local.yml or set JWT_SECRET env var.

$bytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
# Base64Url without padding — safe for YAML/env without quoting issues
$s = [Convert]::ToBase64String($bytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')
Write-Host $s
Write-Host ""
Write-Host "Paste into application-local.yml under connto.jwt.secret (quote the value) or:" -ForegroundColor DarkGray
Write-Host ('$env:JWT_SECRET = "' + $s + '"') -ForegroundColor DarkGray
