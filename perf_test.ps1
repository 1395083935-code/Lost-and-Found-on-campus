# Performance Test Script - 4 Critical Paths P95 Analysis

$BaseUrl = 'http://localhost:8082'
$Results = @()

function Test-Endpoint {
    param([string]$Name, [string]$Url, [string]$Method = 'GET', $Body, [int]$Count = 100, [int]$Target)
    
    Write-Host "`n=== Testing: $Name ===" -ForegroundColor Cyan
    $timings = @()
    
    for($i = 0; $i -lt $Count; $i++) {
        $sw = [System.Diagnostics.Stopwatch]::StartNew()
        try {
            if($Method -eq 'GET') {
                $response = Invoke-WebRequest -Uri "$BaseUrl$Url" -UseBasicParsing -TimeoutSec 10 -ErrorAction SilentlyContinue
            } else {
                $bodyJson = $Body | ConvertTo-Json
                $response = Invoke-WebRequest -Uri "$BaseUrl$Url" -Method POST -Body $bodyJson -ContentType 'application/json' -UseBasicParsing -TimeoutSec 10 -ErrorAction SilentlyContinue
            }
        } catch { }
        $sw.Stop()
        $timings += $sw.ElapsedMilliseconds
        
        if(($i + 1) % 25 -eq 0) {
            Write-Host "    Progress: $($i+1)/$Count"
        }
    }
    
    $sorted = $timings | Sort-Object
    $min = $sorted[0]
    $max = $sorted[-1]
    $avg = [Math]::Round(($sorted | Measure-Object -Average).Average, 2)
    $p95_idx = [Math]::Max(0, [Math]::Floor($sorted.Count * 0.95) - 1)
    $p99_idx = [Math]::Max(0, [Math]::Floor($sorted.Count * 0.99) - 1)
    $p95 = $sorted[$p95_idx]
    $p99 = $sorted[$p99_idx]
    $pass = $p95 -le $Target
    
    $status = if($pass) { "PASS" } else { "FAIL" }
    Write-Host "  Min: ${min}ms, Max: ${max}ms, Avg: ${avg}ms" 
    Write-Host "  P95: ${p95}ms (Target: ${Target}ms) [$status]" -ForegroundColor $(if($pass) { 'Green' } else { 'Yellow' })
    Write-Host "  P99: ${p99}ms"
    
    return @{ Name=$Name; Count=$Count; Min=$min; Max=$max; Avg=$avg; P95=$p95; P99=$p99; Target=$Target; Pass=$pass }
}

# Run 4 tests
$r1 = Test-Endpoint -Name "Item List" -Url "/api/items?page=1&size=10" -Count 100 -Target 300
$r2 = Test-Endpoint -Name "Search Items" -Url "/api/search/items?keyword=test" -Count 100 -Target 300
$r3 = Test-Endpoint -Name "Login WeChat" -Url "/api/wechat/login" -Method "POST" -Body @{code='test'} -Count 50 -Target 200
$r4 = Test-Endpoint -Name "Publish Item" -Url "/api/items" -Method "POST" -Body @{title='Test';description='Desc';category='electronics';contactPhone='13800000000'} -Count 50 -Target 500

$Results = @($r1, $r2, $r3, $r4)

# Generate Report
Write-Host "`n`n========== PERFORMANCE TEST REPORT - P95 ANALYSIS =========`n" -ForegroundColor Cyan

foreach($r in $Results) {
    $status = if($r.Pass) { "PASS" } else { "FAIL" }
    Write-Host "Path: $($r.Name)"
    Write-Host "  Samples: $($r.Count)"
    Write-Host "  Response Time: Min=$($r.Min)ms | Avg=$($r.Avg)ms | Max=$($r.Max)ms"
    Write-Host "  P95: $($r.P95)ms (Target: $($r.Target)ms) [$status]" -ForegroundColor $(if($r.Pass) { 'Green' } else { 'Yellow' })
    Write-Host "  P99: $($r.P99)ms`n"
}

# Summary
$passCount = ($Results | Where-Object { $_.Pass }).Count
$totalCount = $Results.Count
$passRate = [Math]::Round(($passCount / $totalCount) * 100, 1)

Write-Host "========== SUMMARY =========`n" -ForegroundColor Cyan
Write-Host "Total Paths Tested: $totalCount"
Write-Host "Paths Passed P95 Target: $passCount"
Write-Host "Pass Rate: $passRate%`n"

if($passCount -eq $totalCount) {
    Write-Host "SUCCESS: All critical paths meet P95 performance targets!" -ForegroundColor Green
} elseif($passCount -gt 0) {
    Write-Host "WARNING: Some paths did not meet targets. Optimization recommended." -ForegroundColor Yellow
} else {
    Write-Host "CRITICAL: All paths failed P95 targets. Urgent optimization needed." -ForegroundColor Red
}

Write-Host "`nReport Generated: $(Get-Date)" -ForegroundColor Gray
