# BsPlay Backend

API Spring Boot para Bingo de 90 bolas con PostgreSQL, Flyway, sesiones JWT, cartones digitales y físicos, premios, estadísticas y sincronización WebSocket/STOMP.

## Desarrollo

Requiere Java 21, Maven 3.9 y PostgreSQL 17.

```bash
mvn test
mvn spring-boot:run
```

Las variables necesarias están documentadas en `.env.example`. La API se publica en `http://localhost:8080/api/v1`, Swagger en `/swagger-ui.html` y salud en `/actuator/health`.

## Producción

El repositorio contiene `compose.production.yml` y `deploy/Caddyfile` para ejecutar Spring Boot, PostgreSQL y HTTPS automático mediante Caddy.

```bash
cp .env.production.example .env.production
# Configura contraseñas, JWT_SECRET y ACME_EMAIL.
docker compose --env-file .env.production -f compose.production.yml up -d --build
```

- API: `https://api.play.bsdev.me/api/v1`
- WebSocket: `wss://api.play.bsdev.me/ws`
- Origen permitido: `https://play.bsdev.me`

PostgreSQL permanece dentro de la red privada de Docker y Flyway aplica las migraciones al iniciar.

## Verificación

```bash
mvn test
docker compose --env-file .env.production.example -f compose.production.yml config --quiet
```
