$baseUrl = "http://localhost:8080"
$totalCouriers = 100
$updatesPerCourier = 10

Write-Host "--- Fleet Flow Zero-Dependency Secure Stress Test ---" -ForegroundColor Cyan

# Phase 1: Fetch Token
Write-Host "Fetching security token from Gateway..." -NoNewline
try {
    $authResp = Invoke-RestMethod -Uri "$baseUrl/auth/login?username=stress-bot" -Method Get
    $token = $authResp.token
    Write-Host " [OK]" -ForegroundColor Green
} catch {
    Write-Host " [FAILED]" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)"
    exit
}

Write-Host "Simulating $totalCouriers couriers with $updatesPerCourier updates each..."

$startTime = Get-Date
$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type"  = "application/json"
}

# Simple yet fast loop for compatibility (PS 5.1 +)
$results = for ($c = 1; $c -le $totalCouriers; $c++) {
    $courierId = "kurye-ps-$c"
    
    # We'll do updates in sequence for each courier to ensure compatibility
    # If the user has PS7, -Parallel is faster, but this works everywhere.
    for ($i = 1; $i -le $updatesPerCourier; $i++) {
        $lat = 41.0082 + (Get-Random -Minimum -100 -Maximum 100) / 1000.0
        $lon = 28.9784 + (Get-Random -Minimum -100 -Maximum 100) / 1000.0
        
        $body = @{
            courierId = $courierId
            latitude = $lat
            longitude = $lon
            timestamp = [DateTimeOffset]::Now.ToUnixTimeMilliseconds()
        } | ConvertTo-Json

        try {
            $resp = Invoke-RestMethod -Uri "$baseUrl/courier/location" -Method Post -Body $body -Headers $headers
            Write-Host "." -NoNewline
        } catch {
            Write-Host "E" -ForegroundColor Red -NoNewline
        }
    }
}

$endTime = Get-Date
$duration = ($endTime - $startTime).TotalSeconds
$totalRequests = $totalCouriers * $updatesPerCourier
$rps = [Math]::Round($totalRequests / $duration, 2)

Write-Host "`n`n--- Test Results ---" -ForegroundColor Green
Write-Host "Total Requests: $totalRequests"
Write-Host "Total Duration: $duration seconds"
Write-Host "Throughput: $rps req/sec"
Write-Host "Approx. Latency: $([Math]::Round(($duration / $totalRequests) * 1000, 2)) ms/req"
