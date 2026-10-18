# Guía de ejecución local EP4

Todo se ejecuta en el equipo local. No uses `docker compose down -v`, no borres
volúmenes y no arranques todos los servicios Maven al mismo tiempo.

## Preparación

Desde la raíz, prepara `.env` una sola vez y no lo compartas:

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
```

En cada terminal PowerShell de microservicio carga las variables sin imprimir
sus valores:

```powershell
Get-Content .env | ForEach-Object {
    if ($_ -match '^([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
        Set-Item -Path "Env:$($matches[1])" -Value $matches[2]
    }
}
$env:JAVA_HOME = 'C:\Users\Shippo29\.jdks\temurin-21'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

Si cambiaste de JDK, ejecuta primero `.\mvnw.cmd clean` dentro de cada módulo.
Las contraseñas y el tenant deben permanecer solo en el entorno local.

## Infraestructura Docker

Terminal 1 — PostgreSQL:

```powershell
docker compose up -d postgres
docker compose ps postgres
```

Terminal 2 — RabbitMQ 1:

```powershell
$old = docker inspect --format '{{.State.Running}}' pedidos360-rabbitmq 2>$null
if ($old -eq 'true') { docker stop pedidos360-rabbitmq }
docker compose up -d rabbitmq1
docker compose ps rabbitmq1
```

Terminal 3 — RabbitMQ 2; Compose lo espera hasta que el primero esté saludable:

```powershell
docker compose up -d rabbitmq2
docker compose ps rabbitmq1 rabbitmq2
docker exec pedidos360-rabbitmq1 rabbitmq-diagnostics cluster_status
```

Los nodos usan volúmenes nuevos. El volumen antiguo `pedidos360_rabbitdata` no
se monta, elimina ni reutiliza. PostgreSQL continúa usando su volumen existente.
No expongas capturas de la Management UI que muestren contraseñas o tokens.

## Aplicaciones — una terminal por servicio

Abre cada terminal en la raíz del proyecto. En cada una, carga `.env` y configura
Temurin 21 con los comandos de Preparación. Corre cada comando Maven en su
terminal y espera el mensaje `Started` y el puerto antes de abrir la siguiente.

Terminal 4 — Catalog:

```powershell
$env:DB_NAME = 'catalogodb'
Set-Location ms-pedidos360-catalog
.\mvnw.cmd clean
.\mvnw.cmd spring-boot:run
```

Espera `Started` y el puerto `8081`.

Terminal 5 — Orders:

```powershell
$env:DB_NAME = 'pedidosdb'
$env:RABBITMQ_ADDRESSES = 'localhost:5672,localhost:5673'
Set-Location ms-pedidos360-orders
.\mvnw.cmd clean
.\mvnw.cmd spring-boot:run
```

Espera `Started` y el puerto `8080`.

Terminal 6 — Notifications:

```powershell
$env:RABBITMQ_ADDRESSES = 'localhost:5672,localhost:5673'
$env:DLQ_CONSUMER_ENABLED = 'true'
$env:CONSUMER_DELAY_MS = '0'
Set-Location ms-pedidos360-notifications
.\mvnw.cmd clean
.\mvnw.cmd spring-boot:run
```

Espera `Started` y el puerto `8083`.

Terminal 7 — BFF:

```powershell
Set-Location ms-pedidos360-bff
.\mvnw.cmd clean
.\mvnw.cmd spring-boot:run
```

Espera `Started` y el puerto `8082`.

Terminal 8 — rabbit-admin:

```powershell
$env:RABBITMQ_ADDRESSES = 'localhost:5672,localhost:5673'
$env:RABBIT_MANAGEMENT_URLS = 'http://localhost:15672,http://localhost:15673'
Set-Location ms-pedidos360-rabbit-admin
.\mvnw.cmd clean
.\mvnw.cmd spring-boot:run
```

Espera `Started` y el puerto `8084`. Aplica replicación y retención:

```powershell
Set-Location ..
.\scripts\ep4\Aplicar-PoliticasRabbit.ps1
```

Terminal 9 — frontend Angular:

```powershell
Set-Location frontend-angular
npm start
```

Espera que Angular indique `localhost:4200`. No edites ni muestres archivos de
environment o configuración de autenticación del frontend.

## Demo de colas

Para producir Ready y Unacked de cocina, inicia Notifications con un retraso
temporal, por ejemplo `CONSUMER_DELAY_MS=15000`. Para acumular mensajes en DLQ,
usa `DLQ_CONSUMER_ENABLED=false`. Reinicia Notifications con esos valores y
ejecuta:

```powershell
Set-Location ..
.\scripts\ep4\Publicar-MensajesDemo.ps1 -Count 15
```

Al ejecutar Notifications en Docker Compose, pasa esos valores al contenedor
antes de recrearlo:

```powershell
$env:CONSUMER_DELAY_MS = '15000'
$env:DLQ_CONSUMER_ENABLED = 'false'
docker compose up -d --force-recreate notifications
Remove-Item Env:CONSUMER_DELAY_MS, Env:DLQ_CONSUMER_ENABLED
```

El script solicita credenciales de forma oculta y publica mensajes de
notificaciones normales, cocina y fallo controlado. El NACK deja un error en el
log aunque el consumer de DLQ esté apagado. Al reactivar el consumer, confirmará
los mensajes y dejará las DLQ en cero.

Para el guion cronometrado, los resultados esperados y lo que no debe aparecer
en pantalla, sigue [DEMO_EP4.md](./DEMO_EP4.md).

## Parada segura

Detén frontend y procesos Maven con `Ctrl+C`, uno por terminal. Después puedes
detener solo los contenedores sin eliminar almacenamiento:

```powershell
docker stop pedidos360-rabbitmq2 pedidos360-rabbitmq1 pedidos360-postgres
```

No uses `docker compose down -v`, `docker volume rm` ni borres los volúmenes.
