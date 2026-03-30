$ErrorActionPreference = "SilentlyContinue"

Write-Host "Stopping Java app processes..."
Get-Process java, javaw | Stop-Process -Force

Write-Host "Stopping SQL container..."
docker stop eproject-sql | Out-Null

Write-Host "Done."
