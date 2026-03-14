# Simulation Script for Fleet Flow
# This script seeds multiple couriers and creates random orders across Turkey hubs.

$baseUrl = "http://localhost:8080"
$couriersPerCity = 2
$ordersPerCity = 1

# City Hubs (Lat, Lon) - Expanded coverage
$hubs = @(
    @{ name = "istanbul"; lat = 41.0082; lon = 28.9784 },
    @{ name = "ankara";   lat = 39.9334; lon = 32.8597 },
    @{ name = "izmir";    lat = 38.4237; lon = 27.1428 },
    @{ name = "antalya";  lat = 36.8848; lon = 30.7056 },
    @{ name = "bursa";    lat = 40.1885; lon = 29.0610 },
    @{ name = "adana";    lat = 37.0000; lon = 35.3213 },
    @{ name = "trabzon";  lat = 41.0027; lon = 39.7168 },
    @{ name = "gaziantep";lat = 37.0662; lon = 37.3833 },
    @{ name = "eskisehir";lat = 39.7767; lon = 30.5206 },
    @{ name = "samsun";   lat = 41.2867; lon = 36.33 }
)

Write-Host "--- Seeding Couriers Across Turkey Hubs ---" -ForegroundColor Cyan
foreach ($hub in $hubs) {
    Write-Host "`nCity: $($hub.name)" -ForegroundColor White
    for ($i = 1; $i -le $couriersPerCity; $i++) {
        $cId = "kurye-$($hub.name)-$i"
        # Randomize within ~5km radius to ensure matching success (search radius is 5km)
        $lat = $hub.lat + (Get-Random -Minimum -0.02 -Maximum 0.02)
        $lon = $hub.lon + (Get-Random -Minimum -0.02 -Maximum 0.02)
        
        $body = @{
            courierId = $cId
            latitude = [Math]::Round($lat, 4)
            longitude = [Math]::Round($lon, 4)
            timestamp = [DateTimeOffset]::Now.ToUnixTimeMilliseconds()
        } | ConvertTo-Json

        Invoke-RestMethod -Uri "$baseUrl/courier/location" -Method Post -Body $body -ContentType "application/json"
        Write-Host "Created $cId at ($lat, $lon)"
    }
}

Write-Host "`n--- Creating Random Orders Across Hubs ---" -ForegroundColor Yellow
foreach ($hub in $hubs) {
    for ($j = 1; $j -le $ordersPerCity; $j++) {
        $custId = "musteri-$($hub.name)-$j"
        $lat = $hub.lat + (Get-Random -Minimum -0.01 -Maximum 0.01)
        $lon = $hub.lon + (Get-Random -Minimum -0.01 -Maximum 0.01)

        $body = @{
            customerId = $custId
            latitude = [Math]::Round($lat, 4)
            longitude = [Math]::Round($lon, 4)
        } | ConvertTo-Json

        try {
            $resp = Invoke-RestMethod -Uri "$baseUrl/matching/order" -Method Post -Body $body -ContentType "application/json"
            if ($resp.courierId) {
                Write-Host "Order #$($resp.id) in $($hub.name) -> Assigned to: $($resp.courierId)" -ForegroundColor Green
            } else {
                Write-Host "Order #$($resp.id) in $($hub.name) -> NO COURIER FOUND" -ForegroundColor Gray
            }
        } catch {
            Write-Host "Failed to create order in $($hub.name): $($_.Exception.Message)" -ForegroundColor Red
        }
    }
}

Write-Host "`nSimulation complete! Check the map across Turkey." -ForegroundColor Green
