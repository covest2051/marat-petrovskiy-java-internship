# Список Spring Boot сервисов в docker-compose
$services = @(
    "authentication-service",
    "user-service",
    "order-service",
    "payment-service",
    "api-gateway"
)

# Папка для сохранения логов
$logFolder = "C:\Temp\docker-logs"
if (!(Test-Path $logFolder)) {
    New-Item -ItemType Directory -Path $logFolder
}

foreach ($service in $services) {
    Write-Host "Собираем логи для $service..."

    # Получаем последние 500 строк логов и фильтруем ошибки
    $log = docker-compose logs --tail=500 $service | Select-String "ERROR|WARN|Exception|Caused by"

    # Сохраняем в файл
    $filePath = Join-Path $logFolder "$service-errors.log"
    $log | Out-File -FilePath $filePath -Encoding utf8

    Write-Host "Логи сохранены в $filePath"
}

Write-Host "Готово! Все ошибки собраны в $logFolder""