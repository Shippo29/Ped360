param(
    [ValidateRange(1, 1000)]
    [int]$Count = 15,
    [string]$ManagementUrl = "http://localhost:15672"
)

function Read-SecretText([string]$Prompt) {
    $secureValue = Read-Host -Prompt $Prompt -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureValue)
    try {
        [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
        $secureValue.Dispose()
    }
}

$username = $env:RABBITMQ_USER
$password = $env:RABBITMQ_PASSWORD
if ([string]::IsNullOrWhiteSpace($username)) {
    $username = Read-SecretText "Usuario RabbitMQ"
}
if ([string]::IsNullOrWhiteSpace($password)) {
    $password = Read-SecretText "Contrasena RabbitMQ"
}
$encodedCredentials = [Convert]::ToBase64String(
    [Text.Encoding]::UTF8.GetBytes("$username`:$password"))
$headers = @{ Authorization = "Basic $encodedCredentials" }
$publishEndpoints = @(
    @{
        uri = "$ManagementUrl/api/exchanges/%2F/pedidos.topic/publish"
        routingKey = "pedido.notificacion.creado"
        forceFailure = $false
    },
    @{
        uri = "$ManagementUrl/api/exchanges/%2F/pedidos.direct/publish"
        routingKey = "pedido.cocina"
        forceFailure = $false
    },
    @{
        uri = "$ManagementUrl/api/exchanges/%2F/pedidos.topic/publish"
        routingKey = "pedido.notificacion.creado"
        forceFailure = $true
    }
)
$idBase = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds() * 1000

foreach ($endpoint in $publishEndpoints) {
    for ($index = 0; $index -lt $Count; $index++) {
        $message = @{
            pedidoId = $idBase + $index
            clienteId = "ep4-demo"
            estado = "ACEPTADO"
            mensaje = "Mensaje de demostracion EP4"
            forzarFalloParaDlq = $endpoint.forceFailure
        } | ConvertTo-Json -Compress
        $body = @{
            properties = @{ content_type = "application/json" }
            routing_key = $endpoint.routingKey
            payload = $message
            payload_encoding = "string"
        } | ConvertTo-Json -Depth 5 -Compress
        $result = Invoke-RestMethod -Method Post -Uri $endpoint.uri `
            -Headers $headers -ContentType "application/json" -Body $body
        if (-not $result.routed) {
            throw "RabbitMQ no enruto el mensaje para $($endpoint.routingKey)"
        }
    }
    $idBase += $Count
}

Write-Host "Mensajes de demo publicados: $($Count * $publishEndpoints.Count)"
