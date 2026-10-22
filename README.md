# osi-back

API REST del **Lab Modernización**: gestión de requerimientos de modernización tecnológica (registro, estimación, aprobación, auditoría y reportes).

- Spring Boot 4.1 · Java 17 · Spring Security (JWT) · Spring Data JPA / Hibernate 7
- Base de datos Oracle (esquema `APRENDIZAJE`)
- Lo consume el frontend [`osi-front`](../osi-front)

## Diagramas y contratos

- [Diagrama de arquitectura](docs/diagrama-arquitectura.md)
- [Diagrama entidad-relación](docs/diagrama-entidad-relacion.md)
- [contrato API](docs/contrato-api.yaml)

---

## Requisitos

| Herramienta | Versión |
|---|---|
| JDK | 17 o superior |
| Maven | 3.9+ (o el wrapper `mvnw`) |
| Oracle | Acceso de red a `10.0.0.19:1521` (servicio `bdsph01`) |

> **macOS con XAMPP instalado:** `./mvnw` falla con *"Bad CPU type in executable"* porque toma el `curl` de XAMPP. Usa `mvn` (Homebrew) o `PATH=/usr/bin:$PATH sh mvnw ...`.

## Puesta en marcha

### 1. Base de datos

Las tablas ya existen en el esquema. Los scripts e imágenes de referencia están en [`sql/`](sql) y en [`../tablas/`](../tablas).

| Tabla | Uso |
|---|---|
| `LAB_USUARIO` | Usuarios, rol y contraseña (hash BCrypt) |
| `LAB_APLICACION` | Catálogo de aplicaciones |
| `LAB_REQUERIMIENTO` | Requerimientos y su estado |
| `LAB_ESTIMACION` | Estimación de horas por requerimiento |
| `LAB_AUDITORIA` | Historial de operaciones |
| `LAB_REFRESH_TOKEN` | Sesiones (refresh tokens). Script: [`../tablas/LAB_REFRESH_TOKEN.sql`](../tablas/LAB_REFRESH_TOKEN.sql) |

`spring.jpa.hibernate.ddl-auto=none`: la aplicación **no crea ni modifica tablas**. Cualquier cambio de estructura se hace con scripts SQL.

Restricciones de Oracle que el código respeta:

- `CK_LAB_USUARIO_ROL`: `ROL IN ('ANALISTA','COORDINADOR','APROBADOR')`
- `CK_LAB_REQ_ESTADO`: `ESTADO IN ('REGISTRADO','ESTIMADO','APROBADO','RECHAZADO')`
- `CK_LAB_REQ_PRIORIDAD`: `PRIORIDAD IN ('BAJA','MEDIA','ALTA')`

### 2. Arrancar

```bash
mvn spring-boot:run
```

Queda en **http://localhost:8091** (el 8080 lo usa nginx en desarrollo; se cambia con `SERVER_PORT`).

### 3. Primer usuario (solo si `LAB_USUARIO` no tiene ningún COORDINADOR)

Registrar usuarios exige un COORDINADOR, así que el primero se crea al arrancar con dos variables de entorno:

```bash
ADMIN_USERNAME=admin ADMIN_PASSWORD='UnaClaveSegura123' mvn spring-boot:run
```

En el log aparece `Se creó el usuario coordinador inicial 'admin'`. Si ya existe un COORDINADOR, las variables se ignoran. Los demás usuarios se registran con `POST /api/usuarios`.

No dejes la contraseña escrita en `application.properties`: ese archivo se sube a git.

## Configuración

Todo está en [`application.properties`](src/main/resources/application.properties). Lo sensible o lo que cambia por entorno se lee de variables de entorno:

| Variable | Por defecto | Descripción |
|---|---|---|
| `SERVER_PORT` | `8091` | Puerto HTTP |
| `JWT_SECRET` | clave de desarrollo | Clave HMAC del JWT (mín. 32 caracteres). **Obligatorio cambiarla en producción** |
| `JWT_EXPIRATION` | `15m` | Vida del access token |
| `REFRESH_EXPIRATION` | `7d` | Vida del refresh token |
| `REFRESH_COOKIE_SECURE` | `true` | Cookie solo por HTTPS. Poner `false` solo en desarrollo con Safari |
| `CORS_ORIGINS` | `http://localhost:4200` | Orígenes permitidos (separados por coma) |
| `STORAGE_DIR` | `uploads` | Carpeta de archivos adjuntos (en producción, una ruta persistente) |
| `SWAGGER_ENABLED` | `true` | Swagger UI y `/v3/api-docs`. Poner `false` en producción |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | vacío | Coordinador inicial (ver arriba) |

Las credenciales de Oracle están hoy en el archivo; en producción conviene moverlas a variables de entorno.

## Autenticación

Esquema **access token + refresh token**:

| | Access token | Refresh token |
|---|---|---|
| Formato | JWT firmado HS256 | Valor aleatorio opaco (en BD solo se guarda su hash SHA-256) |
| Vida | 15 minutos | 7 días |
| Cómo viaja | Header `Authorization: Bearer <token>` | Cookie `refresh_token` (`HttpOnly`, `Secure`, `SameSite=Strict`, `Path=/api/auth`) |
| Revocable | No (expira solo) | Sí (logout o robo detectado) |

Flujo:

1. `POST /api/auth/login` → devuelve el access token en el cuerpo y crea la cookie.
2. Cada petición lleva `Authorization: Bearer <token>`.
3. Al vencer (401), el cliente llama a `POST /api/auth/refresh`: recibe un access token nuevo y la cookie se **rota** (el refresh anterior queda usado).
4. `POST /api/auth/logout` revoca el refresh token y borra la cookie.

Protecciones:

- **Reutilización:** si un refresh ya rotado se vuelve a usar pasados 30 s (`app.refresh.reuse-grace`), se asume robo y se revocan **todas** las sesiones del usuario. Los 30 s cubren varias pestañas refrescando a la vez.
- **Usuarios inactivos** (`ACTIVO = 0`): no pueden iniciar sesión y pierden el acceso en el siguiente refresh (máx. 15 min).
- Los refresh tokens vencidos se borran cada día a las 03:00.

### Probar con curl / Postman

```bash
# 1. Login
curl -X POST http://localhost:8091/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"UnaClaveSegura123"}'

# 2. Usar el "token" de la respuesta
curl http://localhost:8091/api/auth/me -H "Authorization: Bearer <token>"
```

En Postman: pestaña **Authorization** → tipo **Bearer Token**.

### Swagger UI

Con el backend levantado: **http://localhost:8091/swagger-ui.html** (especificación OpenAPI en `/v3/api-docs`).

1. Ejecuta `POST /api/auth/login` desde Swagger (**Try it out**) y copia el `token` de la respuesta.
2. Pulsa **Authorize**, pega el token (sin escribir `Bearer`) y confirma.
3. Ya puedes probar cualquier endpoint. Cada uno indica en su descripción el **permiso requerido**, que se lee automáticamente de su `@PreAuthorize`.

El token dura 15 minutos; al vencer, repite el paso 1. En producción, desactivar Swagger con `SWAGGER_ENABLED=false`.

## Roles y permisos

Los endpoints exigen **permisos**, no roles (`@PreAuthorize("hasAuthority('...')")`). Qué permisos tiene cada rol se define en un solo lugar: [`Rol.java`](src/main/java/com/osinergmin/osinergmin/model/Rol.java).

| Permiso | ANALISTA | COORDINADOR | APROBADOR |
|---|:-:|:-:|:-:|
| `CATALOGO_VER` — ver catálogo, requerimientos y tablero | ✔ | ✔ | ✔ |
| `CATALOGO_GESTIONAR` — crear/editar/eliminar aplicaciones | | ✔ | |
| `REQUERIMIENTO_CREAR` | ✔ | ✔ | |
| `REQUERIMIENTO_MODIFICAR` — solicitud abierta | ✔ | ✔ | |
| `REQUERIMIENTO_ELIMINAR` | ✔ | ✔ | |
| `ESTIMACION_GUARDAR` | ✔ | | |
| `REQUERIMIENTO_APROBAR` — aprobar / rechazar | | | ✔ |
| `AUDITORIA_VER` | ✔ | ✔ | ✔ |
| `USUARIO_ADMINISTRAR` — registrar y listar usuarios | | ✔ | |

- El JWT solo lleva el claim `rol`. En cada petición, [`RolAuthoritiesConverter`](src/main/java/com/osinergmin/osinergmin/security/RolAuthoritiesConverter.java) lo convierte en `ROLE_<rol>` + sus permisos, así que un cambio en la matriz aplica sin reemitir tokens.
- El login y `/api/auth/me` devuelven la lista `permisos` para que el frontend adapte la interfaz.
- Algunas reglas dependen también del **estado** y las valida el servicio, no el permiso. Ejemplo: un requerimiento `APROBADO` no se puede modificar aunque se tenga `REQUERIMIENTO_MODIFICAR`.

**Cambiar la matriz:** editar `Rol.java` y actualizar [`RolTest`](src/test/java/com/osinergmin/osinergmin/model/RolTest.java). **Agregar un permiso:** añadirlo en `Permiso.java`, asignarlo en `Rol.java`, usarlo en `@PreAuthorize` y agregarlo al tipo `Permiso` del frontend.

## Endpoints

Todos bajo `/api`. Salvo los tres de autenticación marcados como públicos, todos requieren `Authorization: Bearer <token>`.

### Autenticación — `/api/auth`

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/login` | Público | `{username, password}` → access token + cookie |
| POST | `/refresh` | Público (cookie) | Nuevo access token; rota la cookie |
| POST | `/logout` | Público (cookie) | Revoca la sesión |
| GET | `/me` | Autenticado | Usuario actual con su rol y permisos |

### Usuarios — `/api/usuarios`

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| POST | `/` | `USUARIO_ADMINISTRAR` | Registrar `{username, password, rol}` (contraseña con BCrypt) |
| GET | `/` | `USUARIO_ADMINISTRAR` | Listar usuarios |

### Aplicaciones — `/api/aplicaciones`

| Método | Ruta | Permiso |
|---|---|---|
| GET | `/`, `/{id}` | Autenticado |
| POST | `/` | `CATALOGO_GESTIONAR` |
| PUT | `/{id}` | `CATALOGO_GESTIONAR` |
| DELETE | `/{id}` | `CATALOGO_GESTIONAR` |

### Requerimientos — `/api/requerimientos`

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| GET | `/?estado=` | Autenticado | Listar (filtro de estado opcional) |
| GET | `/{id}` | Autenticado | Detalle |
| POST | `/` | `REQUERIMIENTO_CREAR` | Registrar |
| PUT | `/{id}` | `REQUERIMIENTO_MODIFICAR` | Modificar (no si está `APROBADO`) |
| DELETE | `/{id}` | `REQUERIMIENTO_ELIMINAR` | Eliminar |
| POST | `/{id}/archivo` | `REQUERIMIENTO_MODIFICAR` | Adjuntar archivo (`multipart/form-data`, campo `archivo`, máx. 10 MB) |
| GET | `/{id}/archivo` | Autenticado | Descargar adjunto |
| PUT | `/{id}/estimacion` | `ESTIMACION_GUARDAR` | Guardar estimación `{hhDesarrollo}` |
| PATCH | `/{id}/decision` | `REQUERIMIENTO_APROBAR` | Aprobar o rechazar |
| GET | `/{id}/auditoria` | `AUDITORIA_VER` | Historial del requerimiento |

### Tablero, reportes y auditoría

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| GET | `/api/dashboard/resumen` | Autenticado | Indicadores del dashboard |
| GET | `/api/estimaciones?estado=` | Autenticado | Bandeja de estimación/aprobación (estado repetible) |
| GET | `/api/reportes/seguimiento?desde=&hasta=&aplicacion=` | Autenticado | Reporte de seguimiento |
| GET | `/api/auditoria?desde=&hasta=&usuario=` | `AUDITORIA_VER` | Historial global |

### Errores

Todas las respuestas de error son JSON:

```json
{ "timestamp": "...", "status": 400, "error": "Bad Request", "message": "Datos inválidos",
  "campos": { "username": "El usuario es obligatorio" } }
```

| Código | Cuándo |
|---|---|
| 400 | Validación fallida (`campos` indica cada error) o JSON inválido |
| 401 | Sin token, token inválido/vencido, credenciales incorrectas o sesión cerrada |
| 403 | Sin el permiso necesario, o usuario inactivo |
| 404 | Recurso inexistente |
| 409 | Conflicto (usuario duplicado, estado que no permite la operación) |
| 500 | Error interno: el detalle va al log, nunca al cliente |

## Estructura

```
src/main/java/com/osinergmin/osinergmin/
├── config/       SecurityConfig (rutas, JWT, CORS), OpenApiConfig (Swagger), AdminInicialConfig
├── controller/   Endpoints REST
├── dto/          Records de entrada/salida (nunca se exponen entidades)
├── exception/    Excepciones de negocio y manejadores globales de errores
├── model/        Entidades JPA y enums (Rol, Permiso, EstadoRequerimiento, ...)
├── repository/   Spring Data JPA
├── security/     JwtService, refresh token, conversión rol → permisos
├── service/      Lógica de negocio
└── util/         Utilidades (fechas, códigos de requerimiento)
```

## Tests

```bash
mvn test
```

- Los tests unitarios (`service/`, `model/RolTest`, `security/RolAuthoritiesConverterTest`) no necesitan base de datos.
- `OsinergminApplicationTests` levanta el contexto completo y **sí necesita conexión a Oracle**.
- Pendiente: `EstimacionServiceTest.requerimientoInexistenteDa404` falla porque espera el mensaje `REQ-0099` y el servicio responde `id 99`.

## Problemas comunes

| Síntoma | Causa / solución |
|---|---|
| `Port 8091 was already in use` | Ya hay una instancia corriendo (p. ej. desde VS Code). Detenerla o usar otro `SERVER_PORT` |
| 404 de nginx en `localhost:8080` | El backend corre en el **8091** |
| `ORA-12170` timeout al arrancar | Sin acceso de red a Oracle (revisar VPN) |
| `ORA-02290 ... CK_LAB_USUARIO_ROL` | Rol no permitido por la BD: solo `ANALISTA`, `COORDINADOR`, `APROBADOR` |
| La sesión no se mantiene al recargar en Safari | Safari no acepta cookies `Secure` en `http://localhost`: arrancar con `REFRESH_COOKIE_SECURE=false` |
