# Fleet Flow - Start All Services
# This script starts the microservices in separate windows.

Write-Host "Checking infrastructure (Docker)..." -ForegroundColor Cyan
docker-compose up -d

Write-Host "Installing common-lib to local repository..." -ForegroundColor Cyan
mvn install -pl common-lib -DskipTests

Write-Host "Starting Discovery Server..." -ForegroundColor Yellow

Start-Process powershell -ArgumentList "-NoExit", "-Command", "mvn -pl discovery-server spring-boot:run"
Start-Sleep -Seconds 10

$services = @("api-gateway", "courier-service", "matching-service", "tracking-service")

foreach ($service in $services) {
    Write-Host "Starting $service..." -ForegroundColor Yellow
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "mvn -pl $service spring-boot:run"
}

Write-Host "All services are starting in separate windows." -ForegroundColor Green
