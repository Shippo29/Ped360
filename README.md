# Pedidos360

Aplicación distribuida con Spring Boot 4.1.1, Java 21, Angular, PostgreSQL,
Microsoft Entra ID y RabbitMQ.

## Servicios y puertos

| Servicio | Puerto | Responsabilidad |
| --- | ---: | --- |
| Catalog | 8081 | Catálogo y stock |
| Orders | 8080 | Pedidos y publicación de eventos |
| BFF | 8082 | Entrada HTTP única y proxy transparente |
| Notifications | 8083 | Consumers de cocina, facturación, notificaciones y DLQ; historial en memoria |
| Rabbit-admin | 8084 | Administración REST de colas, exchanges y bindings |
| Frontend Angular/Nginx | 4200 (Docker) | Aplicación web |
| PostgreSQL | 5433 host / 5432 Docker | Bases `catalogodb` y `pedidosdb` |
| RabbitMQ 1 | 5672 AMQP / 15672 Management | Nodo principal |
| RabbitMQ 2 | 5673 AMQP / 15673 Management | Segundo nodo del clúster |

Orders y Catalog necesitan `ENTRA_TENANT_ID` válido para autenticar peticiones.
No se almacenan claves ni identificadores reales en el repositorio.

## RabbitMQ

Orders y Notifications declaran la misma topología durable:

| Tipo | Nombre | Enlace |
| --- | --- | --- |
| Direct exchange | `pedidos.direct` | `pedido.cocina` → `cola.cocina-preparacion`; `pedido.factura` → `cola.facturacion-documentos` |
| Topic exchange | `pedidos.topic` | `pedido.notificacion.*` → `cola.notificaciones` |
| Direct DLX | `pedidos.dlx` | Routing key independiente para cada DLQ |

Cada cola de negocio es durable y dirige los rechazos a su propia DLQ durable:
`cola.notificaciones.dlq`, `cola.cocina-preparacion.dlq` y
`cola.facturacion-documentos.dlq`. Un error registra `log.error` y ejecuta
`basicNack(tag, false, false)`, sin requeue. El consumer de DLQ confirma mensajes
por defecto; se desactiva para una demo con `DLQ_CONSUMER_ENABLED=false`.

Compose crea dos nodos RabbitMQ 3 con volúmenes nuevos separados y el mismo
`RABBITMQ_ERLANG_COOKIE`. No reutiliza ni elimina `pedidos360_rabbitdata`.
Las colas clásicas se replican mediante `ha-mode=all` y
`ha-sync-mode=automatic`; la política de retención incluye también HA porque
RabbitMQ aplica una sola política coincidente por cola. La retención limita las
DLQ a 10 000 mensajes y siete días, con `overflow=drop-head`. Ejecuta
`.\scripts\ep4\Aplicar-PoliticasRabbit.ps1` después de iniciar rabbit-admin.

Orders publica cada cambio una sola vez: al crear (`pedido.notificacion.creado`),
al aceptar envía un comando a cocina, otro a facturación y una notificación de
aceptación; al despachar publica `pedido.notificacion.despachado`. Un fallo de
publicación se registra con `log.error` y no revierte el estado ni el descuento
de stock. Kafka no forma parte de esta implementación; su uso queda pendiente.

Los nombres y routing keys son configurables en `pedidos360.rabbit` de los
`application.yml`, enlazados con `@ConfigurationProperties`. Rabbit-admin
administra infraestructura mediante `AmqpAdmin` y valida los bindings contra
la API de RabbitMQ Management.

## Ejecutar en local

Se requiere JDK 21 o superior; si cambias de JDK, ejecuta `mvnw clean` antes de compilar. También se requiere Node.js y Docker Desktop iniciado.
El wrapper Maven descarga Maven cuando hace falta.

1. Prepara configuración local sin versionar el archivo real:

   ```powershell
   if (-not (Test-Path .env)) { Copy-Item .env.example .env }
   # Edita .env con tus valores locales y el tenant/client ID de Entra existentes.
   git check-ignore .env
   ```

   `.env.example` contiene solo valores de muestra; sustituye sus placeholders.
   No uses ni publiques secretos reales en el repositorio.

2. Valida Compose y arranca solo PostgreSQL y RabbitMQ:

   ```powershell
   docker compose config
   docker compose ps
   docker compose up -d postgres
   docker compose up -d rabbitmq1
   docker compose up -d rabbitmq2
   docker compose ps
   ```

   No uses `docker compose down -v`. Postgres publica `5433:5432`.
   RabbitMQ está en `localhost:5672` y `localhost:5673`, y sus consolas en
   `http://localhost:15672` y `http://localhost:15673`. Comprueba el clúster con
   `docker exec pedidos360-rabbitmq1 rabbitmq-diagnostics cluster_status`.

Para arrancar toda la aplicación en contenedores, incluida la interfaz:

```powershell
docker compose up -d
docker compose ps
```

Compose inicia las dependencias antes que Catalog, Orders, Notifications, BFF,
Rabbit-admin y Frontend.

3. Compila y prueba con JDK 21 o superior:

   ```powershell
   .\build-all.bat
   Set-Location frontend-angular
   npm ci
   npm run build
   Set-Location ..
   ```

   Para iniciar servicios fuera de Docker, configura `JAVA_HOME` solo en cada
   terminal y usa `RABBITMQ_ADDRESSES=localhost:5672,localhost:5673`.

## Probar el flujo de pedidos y DLQ

El endpoint protegido para avanzar el pedido requiere un token de Entra con rol
Operador o Admin. Con `$env:ACCESS_TOKEN` definido y un pedido válido:

```powershell
'{"nuevoEstado":"ACEPTADO"}' | Set-Content -NoNewline -Encoding ascii .\order-request.json
curl.exe -X PUT http://localhost:8080/api/orders/1/status `
  -H "Authorization: Bearer $env:ACCESS_TOKEN" `
  -H "Content-Type: application/json" --data-binary "@order-request.json"
Remove-Item .\order-request.json
```

La aceptación envía un mensaje a cocina, uno a facturación y uno a notificaciones.
Con los consumers ejecutándose, revisa ACK, conteos y errores:

```powershell
docker exec pedidos360-rabbitmq1 rabbitmqctl list_queues name messages consumers
docker compose logs notifications
```

Para forzar la ruta DLQ sobre un pedido existente:

```powershell
curl.exe -X POST http://localhost:8080/api/orders/1/simular-error-dlq `
  -H "Authorization: Bearer $env:ACCESS_TOKEN"
docker compose logs notifications
```

El mensaje de prueba lleva `forzarFalloParaDlq=true`; Notifications lo rechaza
sin reencolar y RabbitMQ lo envía a `cola.notificaciones.dlq`. El NACK registra
el error incluso con el consumer de DLQ desactivado.

Para poblar Ready, Unacked y las DLQ, configura `CONSUMER_DELAY_MS` y
`DLQ_CONSUMER_ENABLED` antes de iniciar Notifications y ejecuta
`.\scripts\ep4\Publicar-MensajesDemo.ps1 -Count 15`. El script solicita usuario y
contraseña RabbitMQ de forma oculta.

Las políticas de retención se gestionan también con Rabbit-admin:
`GET /policies`, `POST /policies` y `DELETE /policies/{name}`. La definición
acepta TTL, max-length, overflow drop-head y los parámetros de HA clásicos.

El historial en memoria se consulta por el BFF:

```powershell
curl.exe http://localhost:8082/api/notifications/history
```

El endpoint correspondiente del servicio es
`http://localhost:8083/api/notifications/history`.

Orders tiene un test de integración aislado de Entra y Catálogo (H2, cliente
mock y usuario Operador de prueba en MockMvc). Para ejercer RabbitMQ real,
levanta ambos nodos y Notifications primero, configura `RABBITMQ_ADDRESSES`,
`RABBITMQ_USER` y `RABBITMQ_PASSWORD`, y ejecuta:

```powershell
Set-Location ms-pedidos360-orders
.\mvnw.cmd '-Dtest=PedidoServiceRabbitIntegrationTest' '-Dpedidos.rabbit.it=true' test
Set-Location ..
```

El test externo está opt-in porque necesita un broker local. Sin un token de
Entra disponible, el test usa un usuario Operador simulado y cubre los endpoints
HTTP de aceptación y DLQ sin desactivar la autorización en producción.

## Rabbit-admin REST (localhost:8084)

Rabbit-admin no implementa JWT y debe exponerse solo en desarrollo local.
Los nombres aceptan letras, números, punto, guion y guion bajo; valores inválidos
responden `400`.

```powershell
# Cola durable
'{"name":"demo.queue"}' | Set-Content -NoNewline -Encoding ascii .\admin-request.json
curl.exe -i -X POST http://localhost:8084/queues `
  -H "Content-Type: application/json" --data-binary "@admin-request.json"
curl.exe -i -X DELETE http://localhost:8084/queues/demo.queue

# Exchange
'{"name":"demo.exchange","type":"topic"}' | Set-Content -NoNewline -Encoding ascii .\admin-request.json
curl.exe -i -X POST http://localhost:8084/exchanges `
  -H "Content-Type: application/json" --data-binary "@admin-request.json"

# Binding: la cola y el exchange deben existir
'{"exchangeName":"demo.exchange","queueName":"demo.queue","routingKey":"demo.*"}' `
  | Set-Content -NoNewline -Encoding ascii .\admin-request.json
curl.exe -i -X POST http://localhost:8084/bindings `
  -H "Content-Type: application/json" --data-binary "@admin-request.json"
curl.exe -i -X DELETE http://localhost:8084/bindings `
  -H "Content-Type: application/json" --data-binary "@admin-request.json"
curl.exe -i -X DELETE http://localhost:8084/exchanges/demo.exchange
curl.exe -i -X DELETE http://localhost:8084/queues/demo.queue
Remove-Item .\admin-request.json
```

## Variables de entorno

`.env.example` documenta `DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD`,
`RABBITMQ_ADDRESSES`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD`,
`RABBITMQ_ERLANG_COOKIE`, `RABBIT_MANAGEMENT_URL(S)`,
`DLQ_CONSUMER_ENABLED`, `CONSUMER_DELAY_MS`, `ENTRA_TENANT_ID`,
`AZURE_CLIENT_ID`, `ORDERS_SERVICE_URL`, `CATALOG_SERVICE_URL`,
`NOTIFICATIONS_SERVICE_URL` y `FRONTEND_ALLOWED_ORIGINS`. Compose usa DNS
interno para PostgreSQL y RabbitMQ y puertos publicados separados en el host.
