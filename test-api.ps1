$body = @{
    title = "test-anonymous"
    location = "lib"
    contactInfo = "13800138000"
    type = 0
    anonymous = $true
} | ConvertTo-Json

$response = Invoke-WebRequest -Uri "http://localhost:8082/api/items" `
    -Method POST `
    -ContentType "application/json" `
    -Body $body `
    -UseBasicParsing

$json = $response.Content | ConvertFrom-Json
Write-Host "Response Code: $($json.code)"
Write-Host "Response Message: $($json.msg)"

if ($json.data) {
    Write-Host "Item contactInfo is: $($json.data.contactInfo)"
    if ($null -eq $json.data.contactInfo) {
        Write-Host "✅ SUCCESS: contactInfo is hidden for anonymous post"
    } else {
        Write-Host "❌ FAIL: contactInfo should be null for anonymous post but got: $($json.data.contactInfo)"
    }
}
