# ============================================
# E2E Testing Script - CORRECTED VERSION
# ============================================

param(
    [string]$GatewayUrl = "http://localhost:8080",
    [string]$AuthServiceUrl = "http://localhost:8081",
    [string]$Login = "maratperovitch@gmail.com",  # Changed from $Email
    [string]$Password = "123456"
)

# Color helpers
function Write-Success { Write-Host $args[0] -ForegroundColor Green }
function Write-Error { Write-Host $args[0] -ForegroundColor Red }
function Write-Warning { Write-Host $args[0] -ForegroundColor Yellow }

$testResults = @()
$jwtToken = $null

# ============================================
# STEP 1: Test API Gateway Health
# ============================================
Write-Host "`n=== STEP 1: Test API Gateway Health ===" -ForegroundColor Cyan

try {
    $response = Invoke-RestMethod -Uri "$GatewayUrl/actuator/health" -Method Get
    Write-Success "✓ Gateway Health Check: OK (Status 200)"
    Write-Host "  Status: $($response.status)"
    $testResults += @{ Test = "Gateway Health"; Status = "PASS" }
}
catch {
    Write-Error "✗ Gateway Health Check FAILED"
    Write-Error "Fix: Ensure API Gateway is running on port 8080"
    $testResults += @{ Test = "Gateway Health"; Status = "FAIL" }
    exit
}

# ============================================
# STEP 2: Test Authentication Service Health
# ============================================
Write-Host "`n=== STEP 2: Test Authentication Service Health ===" -ForegroundColor Cyan

try {
    $authHealthResponse = Invoke-RestMethod -Uri "$AuthServiceUrl/actuator/health" -Method Get
    Write-Success "✓ Auth Service Health:  OK (Status 200)"
    Write-Host "  Status: $($authHealthResponse.status)"
    $testResults += @{ Test = "Auth Service Health"; Status = "PASS" }
}
catch {
    Write-Error "✗ Auth Service Health FAILED"
    Write-Error "Fix: Ensure Authentication Service is running on port 8081"
    $testResults += @{ Test = "Auth Service Health"; Status = "FAIL" }
    exit
}

# ============================================
# STEP 3: Authenticate via Authentication Service (Direct)
# ============================================
Write-Host "`n=== STEP 3: Authenticate Directly (Bypass Gateway) ===" -ForegroundColor Cyan

# IMPORTANT: Use 'login' field, NOT 'email'
$authBody = @{
    login    = $Login      # This is the key field name!
    password = $Password
} | ConvertTo-Json

Write-Host "  Sending LoginRequest with:  login='$Login', password='$Password'" -ForegroundColor Yellow

try {
    $authResponse = Invoke-RestMethod -Uri "$AuthServiceUrl/auth/login" `
        -Method Post `
        -ContentType "application/json" `
        -Body $authBody

    $jwtToken = $authResponse.accessToken

    if ($jwtToken) {
        Write-Success "✓ Authentication Successful (Status 200)"
        Write-Host "  JWT Token: $($jwtToken.Substring(0, 50))..." -ForegroundColor Green
        Write-Host "  User ID: $($authResponse.userId)" -ForegroundColor Green
        $testResults += @{ Test = "Authentication (Direct)"; Status = "PASS" }
    }
    else {
        Write-Error "✗ Authentication returned empty token"
        $testResults += @{ Test = "Authentication (Direct)"; Status = "FAIL" }
        exit
    }
}
catch {
    $statusCode = $_.Exception.Response.StatusCode.Value
    Write-Error "✗ Authentication FAILED (Status $statusCode)"
    Write-Host "  Error Details: $($_.Exception.Message)" -ForegroundColor Yellow
    Write-Warning "`n  Debugging Tips:"
    Write-Host "  1. Check Auth Service Logs:"
    Write-Host "     docker logs -f authentication-service --tail 50"
    Write-Host "  2. Verify user exists in database:"
    Write-Host '     docker exec java-internship-postgres psql -U postgres -d innowisedb -c "SELECT * FROM user_credentials;"'
    $testResults += @{ Test = "Authentication (Direct)"; Status = "FAIL" }
    exit
}

# ============================================
# STEP 4:  Authenticate via API Gateway
# ============================================
Write-Host "`n=== STEP 4: Authenticate via API Gateway ===" -ForegroundColor Cyan

$authBodyGateway = @{
    login    = $Login      # Same field name through gateway
    password = $Password
} | ConvertTo-Json

try {
    $authResponseGateway = Invoke-RestMethod -Uri "$GatewayUrl/auth/login" `
        -Method Post `
        -ContentType "application/json" `
        -Body $authBodyGateway

    $jwtTokenGateway = $authResponseGateway.accessToken

    Write-Success "✓ Gateway Authentication Successful (Status 200)"
    Write-Host "  JWT Token received from Gateway" -ForegroundColor Green
    $testResults += @{ Test = "Authentication (Gateway)"; Status = "PASS" }
}
catch {
    $statusCode = $_.Exception.Response.StatusCode.Value
    Write-Error "✗ Gateway Authentication FAILED (Status $statusCode)"
    Write-Warning "  Note: Direct auth works, but gateway routing failed"
    $testResults += @{ Test = "Authentication (Gateway)"; Status = "FAIL" }
}

# Continue only if authenticated
if (-not $jwtToken) {
    Write-Error "Cannot proceed without valid JWT token"
    exit
}

# Set headers for authenticated requests
$headers = @{
    "Authorization" = "Bearer $jwtToken"
    "Content-Type"  = "application/json"
}

# ============================================
# STEP 5: Get User Profile
# ============================================
Write-Host "`n=== STEP 5: Get User Profile ===" -ForegroundColor Cyan

try {
    $userResponse = Invoke-RestMethod -Uri "$GatewayUrl/user/me" -Method Get -Headers $headers

    Write-Success "✓ User Profile Retrieved (Status 200)"
    Write-Host "  User ID: $($userResponse.id)" -ForegroundColor Green
    Write-Host "  Name: $($userResponse.name) $($userResponse.surname)" -ForegroundColor Green
    Write-Host "  Email:  $($userResponse.email)" -ForegroundColor Green
    $testResults += @{ Test = "Get User Profile"; Status = "PASS" }
}
catch {
    $statusCode = $_.Exception.Response.StatusCode.Value
    Write-Error "✗ Get User Profile FAILED (Status $statusCode)"

    if ($statusCode -eq 401) {
        Write-Warning "Token may be invalid. Re-authenticate."
    }
    elseif ($statusCode -eq 500) {
        Write-Host "Debug User Service: docker logs -f user-service --tail 100" -ForegroundColor Yellow
    }

    $testResults += @{ Test = "Get User Profile"; Status = "FAIL" }
}

# ============================================
# STEP 6: Create Order
# ============================================
Write-Host "`n=== STEP 6: Create Order ===" -ForegroundColor Cyan

$orderBody = @{
    userId     = 1
    status     = "CREATED"
    orderItems = @(
        @{ productId = 101; quantity = 2 },
        @{ productId = 102; quantity = 1 }
    )
} | ConvertTo-Json -Depth 3

try {
    $orderResponse = Invoke-RestMethod -Uri "$GatewayUrl/orders" `
        -Method Post `
        -Headers $headers `
        -Body $orderBody `
        -ContentType "application/json"

    Write-Success "✓ Order Created (Status 201)"
    Write-Host "  Order ID: $($orderResponse.id)" -ForegroundColor Green
    Write-Host "  Status: $($orderResponse.status)" -ForegroundColor Green
    Write-Host "  Creation Date: $($orderResponse.creationDate)" -ForegroundColor Green
    $testResults += @{ Test = "Create Order"; Status = "PASS" }

    $orderId = $orderResponse.id
}
catch {
    $statusCode = $_.Exception.Response.StatusCode.Value
    Write-Error "✗ Create Order FAILED (Status $statusCode)"

    if ($statusCode -eq 500) {
        Write-Host "Debug Order Service:" -ForegroundColor Yellow
        Write-Host "  docker logs -f order-service --tail 100" -ForegroundColor Yellow
    }

    $testResults += @{ Test = "Create Order"; Status = "FAIL" }
}

# ============================================
# STEP 7: Get Order by ID
# ============================================
if ($orderId) {
    Write-Host "`n=== STEP 7: Get Order by ID ===" -ForegroundColor Cyan

    try {
        $getOrderResponse = Invoke-RestMethod -Uri "$GatewayUrl/orders/$orderId" `
            -Method Get `
            -Headers $headers

        Write-Success "✓ Order Retrieved (Status 200)"
        Write-Host "  Order ID: $($getOrderResponse.id)" -ForegroundColor Green
        Write-Host "  Status: $($getOrderResponse.status)" -ForegroundColor Green
        $testResults += @{ Test = "Get Order by ID"; Status = "PASS" }
    }
    catch {
        $statusCode = $_.Exception.Response.StatusCode.Value
        Write-Error "✗ Get Order FAILED (Status $statusCode)"
        $testResults += @{ Test = "Get Order by ID"; Status = "FAIL" }
    }
}

# ============================================
# STEP 8: Get All User Orders (Paginated)
# ============================================
Write-Host "`n=== STEP 8: Get All User Orders (Paginated) ===" -ForegroundColor Cyan

try {
    $allOrdersResponse = Invoke-RestMethod -Uri "$GatewayUrl/orders/user?page=0&size=10" `
        -Method Get `
        -Headers $headers

    Write-Success "✓ User Orders Retrieved (Status 200)"
    Write-Host "  Total Orders: $($allOrdersResponse.totalElements)" -ForegroundColor Green
    $testResults += @{ Test = "Get All User Orders"; Status = "PASS" }
}
catch {
    $statusCode = $_.Exception.Response.StatusCode.Value
    Write-Error "✗ Get All User Orders FAILED (Status $statusCode)"
    $testResults += @{ Test = "Get All User Orders"; Status = "FAIL" }
}

# ============================================
# STEP 9: Security Test - Invalid Token
# ============================================
Write-Host "`n=== STEP 9: Security Test - Invalid Token ===" -ForegroundColor Cyan

$invalidHeaders = @{
    "Authorization" = "Bearer invalid_token_xyz"
    "Content-Type"  = "application/json"
}

try {
    $invalidResponse = Invoke-RestMethod -Uri "$GatewayUrl/orders" `
        -Method Get `
        -Headers $invalidHeaders -ErrorAction Stop

    Write-Warning "⚠ SECURITY ISSUE: Invalid token was accepted!"
    $testResults += @{ Test = "Invalid Token Rejection"; Status = "FAIL" }
}
catch {
    $statusCode = $_.Exception.Response.StatusCode.Value
    if ($statusCode -eq 401) {
        Write-Success "✓ Invalid Token Properly Rejected (Status 401)"
        $testResults += @{ Test = "Invalid Token Rejection"; Status = "PASS" }
    }
    else {
        Write-Error "✗ Unexpected status for invalid token (Status $statusCode)"
        $testResults += @{ Test = "Invalid Token Rejection"; Status = "FAIL" }
    }
}

# ============================================
# STEP 10: Security Test - Missing Auth Header
# ============================================
Write-Host "`n=== STEP 10: Security Test - Missing Auth Header ===" -ForegroundColor Cyan

try {
    $noAuthResponse = Invoke-RestMethod -Uri "$GatewayUrl/orders" `
        -Method Get -ErrorAction Stop

    Write-Warning "⚠ SECURITY ISSUE: Request without token was accepted!"
    $testResults += @{ Test = "Missing Auth Header Rejection"; Status = "FAIL" }
}
catch {
    $statusCode = $_.Exception.Response.StatusCode.Value
    if ($statusCode -eq 401) {
        Write-Success "✓ Missing Auth Header Properly Rejected (Status 401)"
        $testResults += @{ Test = "Missing Auth Header Rejection"; Status = "PASS" }
    }
    else {
        Write-Error "✗ Unexpected status for missing auth (Status $statusCode)"
        $testResults += @{ Test = "Missing Auth Header Rejection"; Status = "FAIL" }
    }
}

# ============================================
# Test Summary
# ============================================
Write-Host "`n$('='*60)" -ForegroundColor Cyan
Write-Host "TEST SUMMARY" -ForegroundColor Cyan
Write-Host "$('='*60)" -ForegroundColor Cyan

$passCount = ($testResults | Where-Object { $_.Status -eq "PASS" }).Count
$failCount = ($testResults | Where-Object { $_.Status -eq "FAIL" }).Count
$totalTests = $testResults.Count

$testResults | Format-Table -Property @(
    @{Label="Test Name"; Expression={$_.Test}; Width=35},
    @{Label="Result"; Expression={$_.Status}; Width=10}
) -AutoSize

Write-Host "`nResults: $passCount PASSED | $failCount FAILED (out of $totalTests tests)" -ForegroundColor Cyan

if ($failCount -eq 0) {
    Write-Success "`n✓ All tests passed! System is healthy."
}
else {
    Write-Error "`n✗ Some tests failed. Review logs above for details."
}