# Guion de demostración EP4 (5–10 minutos)

Usa las terminales y servicios locales preparados en
[GUIA_EJECUCION.md](./GUIA_EJECUCION.md). No muestres contraseñas, tokens,
archivos `.env`, connection strings ni la pantalla de login de Management UI.

## contexto (0:00-0:45)

- **Terminal:** PowerShell, raíz del repositorio.
- **Comando:** `docker compose ps`
- **Debe verse:** PostgreSQL y ambos RabbitMQ en ejecución. Explica que todos
  los servicios y la infraestructura corren en local.
- **No mostrar:** valores del `.env`, login de Microsoft ni credenciales.

## [EP4-01/02/03] dos nodos, cluster_status, Management UI y Docker Compose en ejecución, todo en local

- **Terminal:** PowerShell, raíz del repositorio.
- **Comandos:**
  ```powershell
  docker exec pedidos360-rabbitmq1 rabbitmq-diagnostics cluster_status
  docker compose ps rabbitmq1 rabbitmq2
  ```
- **Debe verse:** los nodos `rabbit@rabbitmq1` y `rabbit@rabbitmq2`. En
  `http://localhost:15672`, muestra Overview y Nodes. La segunda consola está
  en `http://localhost:15673`.
- **No mostrar:** página de login, usuario/contraseña, `.env` o tokens.

## [EP4-04/05/06] tres colas, sus DLQ, Direct Exchange y Topic Exchange

- **Terminal:** navegador local, Management UI.
- **Comando:** abrir `http://localhost:15672/#/queues`.
- **Debe verse:** `cola.notificaciones`, `cola.cocina-preparacion`,
  `cola.facturacion-documentos` y sus tres DLQ correspondientes. En Exchanges,
  muestra `pedidos.dlx` como Direct, `pedidos.direct` como Direct y
  `pedidos.topic` como Topic.
- **No mostrar:** login, secretos ni filas de colas ajenas al ejercicio.

## [EP4-07] productor, exchange, cola, consumer y mensaje procesado

- **Terminal:** PowerShell con Notifications, Orders y broker iniciados.
- **Comando:** `.\scripts\ep4\Publicar-MensajesDemo.ps1 -Count 1`
- **Debe verse:** el mensaje enrutado por el exchange y los logs de consumo.
  En Management, Ready y Unacked regresan a cero luego del ACK.
- **No mostrar:** el diálogo de credenciales, contraseña ni tokens.

## [EP4-08] fallo controlado, DLQ y log

- **Terminal:** Notifications y Rabbit Management UI.
- **Comando:** publica con `.\scripts\ep4\Publicar-MensajesDemo.ps1 -Count 1`
  y mira la terminal de Notifications.
- **Debe verse:** `log.error` del NACK y, con consumer DLQ habilitado, el mensaje
  leído y confirmado en `cola.notificaciones.dlq`. La cabecera `x-death` tiene
  conteo 1. Con `DLQ_CONSUMER_ENABLED=false`, el mensaje queda visible en DLQ.
- **No mostrar:** credenciales, payloads con datos reales o login de UI.

## [EP4-09] administrador

- **Terminal:** PowerShell.
- **Comandos:**
  ```powershell
  '{"name":"demo.ep4.queue"}' | Set-Content -NoNewline -Encoding ascii .\admin-request.json
  curl.exe -i -X POST http://localhost:8084/queues -H "Content-Type: application/json" --data-binary "@admin-request.json"
  '{"name":"demo.ep4.exchange","type":"direct"}' | Set-Content -NoNewline -Encoding ascii .\admin-request.json
  curl.exe -i -X POST http://localhost:8084/exchanges -H "Content-Type: application/json" --data-binary "@admin-request.json"
  '{"exchangeName":"demo.ep4.exchange","queueName":"demo.ep4.queue","routingKey":"demo.ep4"}' | Set-Content -NoNewline -Encoding ascii .\admin-request.json
  curl.exe -i -X POST http://localhost:8084/bindings -H "Content-Type: application/json" --data-binary "@admin-request.json"
  curl.exe -i -X DELETE http://localhost:8084/bindings -H "Content-Type: application/json" --data-binary "@admin-request.json"
  curl.exe -i -X DELETE http://localhost:8084/exchanges/demo.ep4.exchange
  curl.exe -i -X DELETE http://localhost:8084/queues/demo.ep4.queue
  Remove-Item .\admin-request.json
  ```
- **Debe verse:** respuestas 201 al crear y 204 al borrar. Puedes mostrar el
  listado con `curl.exe http://localhost:8084/policies`.
- **No mostrar:** claves, tokens ni recursos de la topología operacional.

## [EP4-10] Ready, Unacked, Consumers y DLQ

- **Terminal:** PowerShell para lanzar el generador; navegador para las colas.
- **Comando:** `.\scripts\ep4\Publicar-MensajesDemo.ps1 -Count 15`
- **Debe verse:** configura temporalmente `CONSUMER_DELAY_MS=15000` y
  `DLQ_CONSUMER_ENABLED=false` en Notifications antes de iniciarlo. En
  Management, muestra Ready, Unacked, Consumers y mensajes acumulándose en la
  DLQ. Después reactiva el consumer y muestra ACK y descenso de conteos.
- **No mostrar:** contraseñas ni pantalla de login.

## [EP4-11] TTL y max-length

- **Terminal:** PowerShell con rabbit-admin activo y navegador Management.
- **Comando:** aplica una política de prueba de 30 segundos y límite 2:
  ```powershell
  $policy = @{
    name = 'demo-retencion'
    pattern = '^cola\.notificaciones\.dlq$'
    applyTo = 'queues'
    priority = 50
    definition = @{
      'message-ttl' = 30000
      'max-length' = 2
      overflow = 'drop-head'
      'ha-mode' = 'all'
      'ha-sync-mode' = 'automatic'
    }
  } | ConvertTo-Json -Depth 5
  Invoke-RestMethod -Method Post -Uri http://localhost:8084/policies -ContentType 'application/json' -Body $policy
  ```
  Luego, con consumer DLQ apagado, publica más de dos fallos controlados.
- **Debe verse:** nunca más de dos mensajes en la DLQ y la cola queda vacía
  tras esperar 30 segundos.
- **No mostrar:** claves, tokens ni `.env`.

## cierre con resiliencia y escalabilidad

- **Terminal:** PowerShell.
- **Comandos:**
  ```powershell
  docker stop pedidos360-rabbitmq2
  .\scripts\ep4\Publicar-MensajesDemo.ps1 -Count 1
  docker start pedidos360-rabbitmq2
  docker exec pedidos360-rabbitmq1 rabbitmq-diagnostics cluster_status
  ```
- **Debe verse:** publicación y procesamiento con el nodo 2 detenido, y los dos
  nodos en `cluster_status` tras reiniciarlo. La política de colas clásicas
  permite probar también la caída del nodo 1; realiza esa prueba solo después
  de confirmar que todas las colas están replicadas:
  ```powershell
  docker stop pedidos360-rabbitmq1
  .\scripts\ep4\Publicar-MensajesDemo.ps1 -Count 1 -ManagementUrl http://localhost:15673
  docker start pedidos360-rabbitmq1
  docker exec pedidos360-rabbitmq2 rabbitmq-diagnostics cluster_status
  ```
- **No mostrar:** credenciales, tokens o `.env`. No elimines contenedores ni
  volúmenes; al terminar, detén procesos con `Ctrl+C` y usa `docker stop`.
