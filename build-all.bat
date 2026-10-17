@echo off
setlocal

call ms-pedidos360-catalog\mvnw.cmd -f ms-pedidos360-catalog\pom.xml clean package
if errorlevel 1 exit /b %errorlevel%

call ms-pedidos360-catalog\mvnw.cmd -f ms-pedidos360-orders\pom.xml clean package
if errorlevel 1 exit /b %errorlevel%

call ms-pedidos360-catalog\mvnw.cmd -f ms-pedidos360-notifications\pom.xml clean package
if errorlevel 1 exit /b %errorlevel%

call ms-pedidos360-catalog\mvnw.cmd -f ms-pedidos360-bff\pom.xml clean package
if errorlevel 1 exit /b %errorlevel%

call ms-pedidos360-catalog\mvnw.cmd -f ms-pedidos360-rabbit-admin\pom.xml clean package
if errorlevel 1 exit /b %errorlevel%

echo Todos los microservicios compilaron correctamente.
