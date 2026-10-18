param(
    [string]$AdminUrl = "http://localhost:8084"
)

$policies = @(
    @{
        name = "ha-colas-operacionales"
        pattern = "^cola\."
        applyTo = "queues"
        priority = 11
        definition = @{
            "ha-mode" = "all"
            "ha-sync-mode" = "automatic"
        }
    },
    @{
        name = "retencion-dlq"
        pattern = "^cola\..*\.dlq$"
        applyTo = "queues"
        priority = 21
        definition = @{
            "message-ttl" = 604800000
            "max-length" = 10000
            overflow = "drop-head"
            "ha-mode" = "all"
            "ha-sync-mode" = "automatic"
        }
    }
)

foreach ($policy in $policies) {
    $body = $policy | ConvertTo-Json -Depth 8 -Compress
    Invoke-RestMethod -Method Post -Uri "$AdminUrl/policies" `
        -ContentType "application/json" -Body $body | Out-Null
    Write-Host "Politica aplicada: $($policy.name)"
}
