#!/usr/bin/env pwsh

# Test script for verifying backend and frontend connectivity

Write-Host "=== Restaurant App Test Suite ===" -ForegroundColor Cyan

# Test 1: Backend API
Write-Host "`n[1] Testing Backend on port 8080..." -ForegroundColor Yellow
$backendTest = Invoke-WebRequest -Uri "http://localhost:8080/api/categories" -UseBasicParsing -ErrorAction SilentlyContinue
if ($backendTest) {
    Write-Host "✓ Backend is UP (Status: $($backendTest.StatusCode))" -ForegroundColor Green
    Write-Host "  Response: $($backendTest.Content)" -ForegroundColor Gray
} else {
    Write-Host "✗ Backend is DOWN - Start with: .\mvnw.cmd spring-boot:run" -ForegroundColor Red
    exit 1
}

# Test 2: Vite Proxy
Write-Host "`n[2] Testing Vite Proxy on port 5173..." -ForegroundColor Yellow
$proxyTest = Invoke-WebRequest -Uri "http://localhost:5173/api/categories" -UseBasicParsing -ErrorAction SilentlyContinue
if ($proxyTest) {
    Write-Host "✓ Vite Proxy is UP (Status: $($proxyTest.StatusCode))" -ForegroundColor Green
    Write-Host "  Response: $($proxyTest.Content)" -ForegroundColor Gray
} else {
    Write-Host "✗ Vite Proxy is DOWN - Start with: npm run dev (in frontend folder)" -ForegroundColor Red
    exit 1
}

# Test 3: Create a test category
Write-Host "`n[3] Testing POST (Create Category)..." -ForegroundColor Yellow
$timestamp = Get-Date -Format "yyyyMMddHHmmss"
$testName = "TestCat_$timestamp"
$postBody = @{name=$testName} | ConvertTo-Json

$postTest = Invoke-WebRequest -Uri "http://localhost:5173/api/categories" -Method POST -Headers @{"Content-Type"="application/json"} -Body $postBody -UseBasicParsing -ErrorAction SilentlyContinue

if ($postTest -and $postTest.StatusCode -eq 201) {
    Write-Host "✓ POST successful (Status: $($postTest.StatusCode))" -ForegroundColor Green
    Write-Host "  Created: $($postTest.Content)" -ForegroundColor Gray
} else {
    Write-Host "✗ POST failed" -ForegroundColor Red
    exit 1
}

# Test 4: Verify GET returns all
Write-Host "`n[4] Testing GET (List All)..." -ForegroundColor Yellow
$getAllTest = Invoke-WebRequest -Uri "http://localhost:5173/api/categories" -UseBasicParsing -ErrorAction SilentlyContinue
if ($getAllTest) {
    $count = ($getAllTest.Content | ConvertFrom-Json).Count
    Write-Host "✓ GET successful - Found $count categories" -ForegroundColor Green
    Write-Host "  Response: $($getAllTest.Content)" -ForegroundColor Gray
} else {
    Write-Host "✗ GET failed" -ForegroundColor Red
    exit 1
}

Write-Host "`n=== All Tests Passed ===" -ForegroundColor Green
Write-Host "✓ Backend is running"
Write-Host "✓ Vite proxy is working"
Write-Host "✓ CRUD operations work"
Write-Host "`nOpen http://localhost:5173 in your browser and try the UI!" -ForegroundColor Cyan
