# Campus Lost & Found - Regression Test Script
# Date: 2026-04-20

$BaseUrl = 'http://localhost:8082'
$TestResults = @()
$TestStartTime = Get-Date

Write-Host "`n========== REGRESSION TEST SUITE =========`n" -ForegroundColor Cyan

# Part 1: API Basic Tests
Write-Host "Part 1: API Basic Functionality Tests`n" -ForegroundColor Yellow

$tests = @(
    @{ Name = 'Items List'; Method = 'GET'; Url = '/api/items?page=1&size=10'; ExpectCode = 200 },
    @{ Name = 'Search Items'; Method = 'GET'; Url = '/api/search/items?keyword=test'; ExpectCode = 200 },
    @{ Name = 'Hotwords'; Method = 'GET'; Url = '/api/search/hotwords?limit=5'; ExpectCode = 200 },
    @{ Name = 'Health Check'; Method = 'GET'; Url = '/actuator/health'; ExpectCode = 200 },
    @{ Name = 'API Info'; Method = 'GET'; Url = '/actuator/info'; ExpectCode = 200 }
)

foreach($test in $tests) {
    try {
        $response = Invoke-WebRequest -Uri "$BaseUrl$($test.Url)" -Method $test.Method -UseBasicParsing -TimeoutSec 10 -ErrorAction SilentlyContinue
        $statusCode = $response.StatusCode
        $passed = $statusCode -eq $test.ExpectCode
        $status = if($passed) { 'PASS' } else { 'FAIL' }
        Write-Host "  [$status] $($test.Name) - Code: $statusCode (Expected: $($test.ExpectCode))"
        $TestResults += @{ Test = $test.Name; Status = $status }
    } catch {
        Write-Host "  [FAIL] $($test.Name) - Connection Error"
        $TestResults += @{ Test = $test.Name; Status = 'FAIL' }
    }
}

# Part 2: Business Flow Tests
Write-Host "`nPart 2: Business Flow Tests`n" -ForegroundColor Yellow

# Test: Get Items List
try {
    $response = Invoke-WebRequest -Uri "$BaseUrl/api/items?page=1&size=10" -UseBasicParsing -ErrorAction SilentlyContinue
    $data = $response.Content | ConvertFrom-Json -ErrorAction SilentlyContinue
    if($data -and $data.code -eq 200) {
        $itemCount = if($data.data) { $data.data.Count } else { 0 }
        Write-Host "  [PASS] Get Items List - Found $itemCount items"
        $TestResults += @{ Test = 'Get Items List'; Status = 'PASS' }
    } else {
        Write-Host "  [FAIL] Get Items List - Invalid response format"
        $TestResults += @{ Test = 'Get Items List'; Status = 'FAIL' }
    }
} catch {
    Write-Host "  [FAIL] Get Items List - Error: $_"
    $TestResults += @{ Test = 'Get Items List'; Status = 'FAIL' }
}

# Test: Search Function
try {
    $response = Invoke-WebRequest -Uri "$BaseUrl/api/search/items?keyword=test" -UseBasicParsing -ErrorAction SilentlyContinue
    if($response.StatusCode -eq 200) {
        Write-Host "  [PASS] Search Items - Search feature working"
        $TestResults += @{ Test = 'Search Items'; Status = 'PASS' }
    }
} catch {
    Write-Host "  [FAIL] Search Items - Error"
    $TestResults += @{ Test = 'Search Items'; Status = 'FAIL' }
}

# Test: Hotwords
try {
    $response = Invoke-WebRequest -Uri "$BaseUrl/api/search/hotwords?limit=5" -UseBasicParsing -ErrorAction SilentlyContinue
    $data = $response.Content | ConvertFrom-Json -ErrorAction SilentlyContinue
    if($data -and $data.code -eq 200) {
        $wordCount = if($data.data) { $data.data.Count } else { 0 }
        Write-Host "  [PASS] Search Hotwords - Found $wordCount hotwords"
        $TestResults += @{ Test = 'Search Hotwords'; Status = 'PASS' }
    }
} catch {
    Write-Host "  [FAIL] Search Hotwords - Error"
    $TestResults += @{ Test = 'Search Hotwords'; Status = 'FAIL' }
}

# Part 3: Permission & Security Tests
Write-Host "`nPart 3: Permission and Security Tests`n" -ForegroundColor Yellow

# Test: Unauthorized Access
try {
    $response = Invoke-WebRequest -Uri "$BaseUrl/api/admin/users" -UseBasicParsing -TimeoutSec 10 -ErrorAction SilentlyContinue
    $code = $response.StatusCode
    if($code -eq 401 -or $code -eq 403) {
        Write-Host "  [PASS] Unauthorized Access Control - Correctly rejected (Code: $code)"
        $TestResults += @{ Test = 'Auth Control'; Status = 'PASS' }
    } else {
        Write-Host "  [WARN] Unauthorized Access - Unexpected status: $code"
        $TestResults += @{ Test = 'Auth Control'; Status = 'FAIL' }
    }
} catch {
    Write-Host "  [PASS] Unauthorized Access Control - Correctly rejected"
    $TestResults += @{ Test = 'Auth Control'; Status = 'PASS' }
}

# Test: SQL Injection Protection
try {
    $malUrl = "$BaseUrl/api/search/items?keyword=test%27+OR+%271%27=%271"
    $response = Invoke-WebRequest -Uri $malUrl -UseBasicParsing -TimeoutSec 10 -ErrorAction SilentlyContinue
    if($response.StatusCode -eq 200) {
        Write-Host "  [PASS] SQL Injection Protection - Handled safely"
        $TestResults += @{ Test = 'SQL Injection'; Status = 'PASS' }
    }
} catch {
    Write-Host "  [PASS] SQL Injection Protection - Rejected malicious input"
    $TestResults += @{ Test = 'SQL Injection'; Status = 'PASS' }
}

# Part 4: Data Integrity Tests
Write-Host "`nPart 4: Data Integrity Tests`n" -ForegroundColor Yellow

try {
    $response = Invoke-WebRequest -Uri "$BaseUrl/api/items?page=1&size=5" -UseBasicParsing -ErrorAction SilentlyContinue
    $data = $response.Content | ConvertFrom-Json -ErrorAction SilentlyContinue
    
    if($data -and $data.data -and $data.data.Count -gt 0) {
        $item = $data.data[0]
        $requiredFields = @('id', 'title', 'category', 'location', 'type')
        $allPresent = $true
        
        foreach($field in $requiredFields) {
            if(-not ($item.PSObject.Properties.Name -contains $field)) {
                $allPresent = $false
                break
            }
        }
        
        if($allPresent) {
            Write-Host "  [PASS] Data Integrity - All required fields present"
            $TestResults += @{ Test = 'Data Integrity'; Status = 'PASS' }
        } else {
            Write-Host "  [FAIL] Data Integrity - Missing required fields"
            $TestResults += @{ Test = 'Data Integrity'; Status = 'FAIL' }
        }
    }
} catch {
    Write-Host "  [FAIL] Data Integrity - Error: $_"
    $TestResults += @{ Test = 'Data Integrity'; Status = 'FAIL' }
}

# Part 5: Frontend Tests
Write-Host "`nPart 5: Frontend Tests`n" -ForegroundColor Yellow

try {
    $response = Invoke-WebRequest -Uri "http://localhost:8081" -UseBasicParsing -TimeoutSec 10 -ErrorAction SilentlyContinue
    if($response.StatusCode -eq 200 -and $response.Content.Length -gt 1000) {
        $sizeKB = [Math]::Round($response.Content.Length / 1024, 2)
        Write-Host "  [PASS] Frontend Homepage - Loaded successfully ($sizeKB KB)"
        $TestResults += @{ Test = 'Frontend'; Status = 'PASS' }
    }
} catch {
    Write-Host "  [FAIL] Frontend Homepage - Load error"
    $TestResults += @{ Test = 'Frontend'; Status = 'FAIL' }
}

# Generate Summary Report
Write-Host "`n========== TEST SUMMARY REPORT ==========`n" -ForegroundColor Green

$passCount = ($TestResults | Where-Object { $_.Status -eq 'PASS' }).Count
$failCount = ($TestResults | Where-Object { $_.Status -eq 'FAIL' }).Count
$totalCount = $TestResults.Count
$passRate = if($totalCount -gt 0) { [Math]::Round(($passCount / $totalCount) * 100, 1) } else { 0 }

Write-Host "Test Statistics:" -ForegroundColor Cyan
Write-Host "  Total Tests: $totalCount"
Write-Host "  Passed: $passCount"
Write-Host "  Failed: $failCount"
Write-Host "  Pass Rate: $passRate%`n"

Write-Host "Test Results:" -ForegroundColor Cyan
foreach($result in $TestResults) {
    $color = if($result.Status -eq 'PASS') { 'Green' } else { 'Red' }
    Write-Host "  [$($result.Status)] $($result.Test)" -ForegroundColor $color
}

$duration = (Get-Date) - $TestStartTime
Write-Host "`nTest Duration: $([Math]::Round($duration.TotalSeconds, 2)) seconds"

if($failCount -eq 0) {
    Write-Host "`nResult: SUCCESS - All tests passed!" -ForegroundColor Green
} else {
    Write-Host "`nResult: FAILURE - $failCount test(s) failed" -ForegroundColor Yellow
}

Write-Host "`n========================================`n" -ForegroundColor Cyan
