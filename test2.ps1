# Конфигурация
$GatewayUrl = "http://localhost:8085"
$Login = "user_$(Get-Random)"  # Теперь используем login
$UserPassword = "password123"

Write-Host "--- 1. Попытка регистрации пользователя: $Login ---" -ForegroundColor Cyan

$regBody = @{
    login     = "$UniqueLogin@example.com"
    password  = "password123"
    name      = "Ivan"
    surname   = "Ivanov"
    birthDate = "1995-05-20"
} | ConvertTo-Json

try {
    $regResponse = Invoke-RestMethod -Uri "$GatewayUrl/auth/register" -Method Post -Body $regBody -ContentType "application/json" -ErrorAction Stop
    Write-Host "SUCCESS: Пользователь зарегистрирован." -ForegroundColor Green
    Write-Host $regResponse
} catch {
      Write-Host "ERROR: Ошибка регистрации!" -ForegroundColor Red
      if ($_.Exception.Response) {
          $statusCode = [int]$_.Exception.Response.StatusCode
          Write-Host "Status Code: $statusCode" -ForegroundColor Yellow
          $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
          $responseBody = $reader.ReadToEnd()
          Write-Host "Response Body: $responseBody" -ForegroundColor White
      } else {
          Write-Host "Exception Message: $($_.Exception.Message)" -ForegroundColor Red
      }
      exit
  }