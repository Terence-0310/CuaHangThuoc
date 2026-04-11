$ErrorActionPreference = "Stop"

Write-Host "Building project..." -ForegroundColor Cyan
mvn -q -DskipTests compile dependency:build-classpath "-Dmdep.outputFile=target/classpath.txt"
if ($LASTEXITCODE -ne 0) {
    throw "Build failed."
}

Write-Host "Running demand planning pipeline demo..." -ForegroundColor Cyan
$cp = Get-Content "target/classpath.txt"
java -cp "target/classes;$cp" tools.DemandPlanningRunner
if ($LASTEXITCODE -ne 0) {
    throw "Demand planning runner failed."
}

Write-Host "Demand planning pipeline completed." -ForegroundColor Green
