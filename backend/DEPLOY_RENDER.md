# Deploy Backend en Render

## Opcion recomendada (Blueprint)

1. Sube estos cambios al repositorio remoto.
2. En Render, entra a **Blueprints** y selecciona el repo.
3. Render detectara `render.yaml` y creara:
   - `celticket-backend` (Web Service Docker)
   - `celticket-postgres` (PostgreSQL)
   - `celticket-redis` (Key Value)
4. Completa variables sensibles pendientes:
   - `WOMPI_PUBLIC_KEY`
   - `WOMPI_PRIVATE_KEY`
   - `WOMPI_INTEGRITY_SECRET`
   - `WOMPI_REDIRECT_URL`

## Verificacion

1. Revisa logs del backend hasta ver arranque sin errores de DB/Redis.
2. Prueba `GET /api/events` en la URL publica del servicio.
3. Si usas frontend externo, actualiza base URL al backend de Render.

## Nota importante de migracion

Este backend fue ajustado para PostgreSQL en Render.
Si tienes datos en MySQL local, debes migrarlos/manualmente sembrarlos en PostgreSQL.
