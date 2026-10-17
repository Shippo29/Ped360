# Pedidos360: mensajería RabbitMQ (EP3)

## Levantar RabbitMQ

Desde la raíz:

```powershell
docker compose up -d rabbitmq
```

La aplicación de administración está en [http://localhost:15672](http://localhost:15672) (`guest` / `guest`); AMQP escucha en `localhost:5672`.

Orders usa `RABBIT_HOST` (`localhost`), `RABBIT_PORT` (`5672`), `RABBIT_USER` (`guest`) y `RABBIT_PASSWORD` (`guest`). Los nombres de exchange, DLX, routing key y colas también se pueden cambiar con `RABBIT_EXCHANGE`, `RABBIT_DLX`, `RABBIT_ROUTING_KEY`, `RABBIT_DLQ_ROUTING_KEY`, `RABBIT_QUEUE_NOTIFICACIONES`, `RABBIT_QUEUE_INVENTARIO` y `RABBIT_QUEUE_DLQ`. Para usar el Postgres del compose, levántalo con `docker compose up -d postgres` y define `DB_PORT=5433` al ejecutar Orders (el puerto `5432` del contenedor se publica como `5433` en el host).

Rabbit-admin usa las mismas variables de conexión y consulta el tipo del exchange existente mediante la API de Management (`RABBIT_MANAGEMENT_URL`, por defecto `http://localhost:15672`).

## Ejecutar y probar el flujo

Inicia `ms-pedidos360-orders` con `.\mvnw.cmd spring-boot:run` y un usuario local con acceso a la base de datos, Catálogo y un token válido de Entra ID. Al iniciar, Orders declara las colas, exchanges y bindings. El servicio rabbit-admin se ejecuta con `.\mvnw.cmd spring-boot:run` desde `ms-pedidos360-rabbit-admin`; queda en el puerto 8083 y no implementa JWT, por lo que debe usarse solo para pruebas locales.

El test optativo `PedidoServiceRabbitIntegrationTest` usa el RabbitMQ real, una base H2 y un mock de Catálogo para validar `cambiarEstado(ACEPTADO)` sin token de Entra. Con RabbitMQ activo, ejecútalo desde el directorio de Orders:

```powershell
.\mvnw.cmd '-Dtest=PedidoServiceRabbitIntegrationTest' '-Dpedidos.rabbit.it=true' test
```

Con un pedido existente y autorización de Operador/Admin, acepta el pedido (el `id` debe reemplazarse por uno real):

```powershell
curl.exe -X PUT http://localhost:8080/api/orders/1/status `
  -H "Authorization: Bearer <TOKEN_ENTRA_ID>" `
  -H "Content-Type: application/json" `
  -d "{\"nuevoEstado\":\"ACEPTADO\"}"
```

Orders descuenta el stock con el flujo actual, persiste el estado y publica un JSON a `pedidos.exchange` con routing key `pedido.aceptado`. Las colas de notificaciones e inventario consumen el evento; los consumidores confirman con ACK. Un mensaje rechazado sin requeue pasa a `pedidos.dlq`, donde se registra con nivel `ERROR`.

### Provocar el fallo y revisar la DLQ

En la consola RabbitMQ, abre **Exchanges > pedidos.exchange > Publish message** y publica con routing key `pedido.aceptado` y payload:

```json
{"pedidoId":999,"estado":"ACEPTADO","prueba":"FALLAR"}
```

`NotificacionConsumer` lanza el fallo controlado cuando el texto contiene `FALLAR`, hace NACK sin requeue y el mensaje termina en `pedidos.dlq`. `InventarioConsumer` procesa y confirma su propia copia del evento. Revisa los logs de Orders para ver el error de notificación y el registro del mensaje en la DLQ.

## API de administración (localhost:8083)

Los ejemplos usan PowerShell y `curl.exe`. Escribe el cuerpo JSON en un archivo ASCII temporal para evitar las reglas de escape de argumentos nativos de PowerShell; el mismo cuerpo puede pegarse directamente en Postman. El administrador no requiere token.

Crear una cola durable (201):

```powershell
'{"name":"demo.queue"}' | Set-Content -NoNewline -Encoding ascii .\admin-request.json
curl.exe -i -X POST http://localhost:8083/queues -H "Content-Type: application/json" --data-binary "@admin-request.json"
```

Eliminar cola (204):

```powershell
curl.exe -i -X DELETE http://localhost:8083/queues/demo.queue
```

Crear exchange (201; `type`: `direct`, `topic`, `fanout` o `headers`):

```powershell
'{"name":"demo.exchange","type":"topic"}' | Set-Content -NoNewline -Encoding ascii .\admin-request.json
curl.exe -i -X POST http://localhost:8083/exchanges -H "Content-Type: application/json" --data-binary "@admin-request.json"
```

Eliminar exchange (204):

```powershell
curl.exe -i -X DELETE http://localhost:8083/exchanges/demo.exchange
```

Crear binding (201; Rabbit-admin consulta el tipo real del exchange en la API de administración RabbitMQ y exige `routingKey` para `direct`/`topic`. `exchangeName` y `queueName` deben existir):

```powershell
'{"exchangeName":"demo.exchange","queueName":"demo.queue","routingKey":"pedido.*"}' | Set-Content -NoNewline -Encoding ascii .\admin-request.json
curl.exe -i -X POST http://localhost:8083/bindings -H "Content-Type: application/json" --data-binary "@admin-request.json"
```

Eliminar binding (204, enviando el mismo cuerpo):

```powershell
'{"exchangeName":"demo.exchange","queueName":"demo.queue","routingKey":"pedido.*"}' | Set-Content -NoNewline -Encoding ascii .\admin-request.json
curl.exe -i -X DELETE http://localhost:8083/bindings -H "Content-Type: application/json" --data-binary "@admin-request.json"
```

Los nombres se validan para aceptar letras, números, punto, guion y guion bajo. Los datos inválidos responden `400` con un mensaje descriptivo.

La consulta del tipo real del exchange y la existencia de la cola para validar un binding usa la API de Management. Por ello rabbit-admin debe tener acceso a esa API y sus credenciales deben tener permisos de lectura de metadatos y administración.

Al terminar, borra el archivo temporal con `Remove-Item .\admin-request.json`.
