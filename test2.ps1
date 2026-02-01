# Конфигурация
$GatewayUrl = "http://localhost:8085"
$Login = "maratperovitch@gmail.com"  # Фиксированный логин, как вы просили
$UserPassword = "123456"

Write-Host "--- Попытка регистрации пользователя: $Login ---" -ForegroundColor Cyan

# Формируем тело запроса. УБЕДИТЕСЬ, что имена полей (login) совпадают с DTO в Java
$regBody = @{
    login     = $Login
    password  = $UserPassword
    name      = "Marat"
    surname   = "Petrovskiy"
    role      = "ROLE_USER"
    birthDate = "1995-05-20"
} | ConvertTo-Json

try {
    $regResponse = Invoke-RestMethod -Uri "$GatewayUrl/auth/register" `
        -Method Post `
        -Body $regBody `
        -ContentType "application/json; charset=utf-8" `
        -ErrorAction Stop

    Write-Host "SUCCESS: Пользователь зарегистрирован!" -ForegroundColor Green
    $regResponse | ConvertTo-Json | Write-Host
} catch {
    Write-Host "ERROR: Ошибка регистрации!" -ForegroundColor Red
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "Status Code: $([int]$_.Exception.Response.StatusCode)" -ForegroundColor Yellow
        Write-Host "Response Body: $responseBody" -ForegroundColor White
    } else {
        Write-Host "Exception: $($_.Exception.Message)" -ForegroundColor Red
    }
}