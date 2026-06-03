# Backend — Gestión de inventario (Spring Boot)

## Clonar y arrancar

1. **PostgreSQL** creado y base de datos vacía o con el esquema generado por Hibernate (`spring.jpa.hibernate.ddl-auto=update`).

2. Copia **`.env.example`** a **`.env`** y rellena al menos:
   - `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`
   - `JWT_SECRET` (cadena larga en Base64 o secreto seguro; el arranque fallará si está vacío)
   - `JWT_EXPIRATION_MS` (ej. `86400000` = 24 h)

3. **Obligatorio si la base ya existía antes de añadir el rol `ADMIN` en código**  
   Si al arrancar o al registrar aparece un error PostgreSQL del tipo  
   `violates check constraint "users_role_check"`  
   ejecuta **una sola vez** el script (desde la carpeta del backend):

   ```bash
   psql -h localhost -U TU_USUARIO -d TU_BASE -f scripts/fix_users_role_add_admin.sql
   ```

   Hibernate **no** actualiza solo los `CHECK` antiguos de PostgreSQL; sin este paso no se puede guardar el rol `ADMIN` (ni a veces operaciones que toquen esa columna).

4. Arranca el backend desde la carpeta `gestion-inventario-back` (o configura el IDE con **working directory** en esa carpeta) para que se cargue el **`.env`** (ver `DotEnvLoader`).

5. Front: `environment.apiUrl` debe apuntar a `http://localhost:8080/api` (o la URL donde corra el API).

---

## Variables de entorno: qué es cada bloque (no confundir)

| Grupo | Variables típicas | Para qué sirve |
|--------|-------------------|----------------|
| **Base de datos** | `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | Conexión JDBC a PostgreSQL. |
| **JWT** | `JWT_SECRET`, `JWT_EXPIRATION_MS` | Firma y caducidad de tokens de sesión. |
| **Correo (SMTP)** | `MAIL_USERNAME`, `MAIL_APP_PASSWORD`, `MAIL_FROM`, `MAIL_HOST`, … | **Cuenta que envía** los correos (recuperación de contraseña, etc.). En Gmail suele ser el correo + **contraseña de aplicación** de Google (no la contraseña de inicio de sesión web). |
| **Enlace en el correo de reset** | `PASSWORD_RESET_FRONTEND_URL` | URL base del Angular (ej. `http://localhost:4200`). El backend concatena `/reset-password/{token}` (y opcionalmente `?returnUrl=...` para volver al login admin). |
| **Bootstrap admin** | `ADMIN_BOOTSTRAP_EMAIL`, `ADMIN_BOOTSTRAP_PASSWORD`, `ADMIN_BOOTSTRAP_PROMOTE_SELLER` | Crea o promueve el **usuario en la tabla `users`** con rol `ADMIN` al arrancar (ver sección siguiente). **No** sustituye a `MAIL_*`: son cosas distintas. |

El **login del administrador** es el usuario en BD (`email` + `password_hash`); puede crearse con bootstrap o manualmente. El **correo que envía** el reset es el configurado en `MAIL_*`.

---

## Docker

Docker **ayuda** a repetir el mismo entorno (versiones de Java, Postgres, variables con `env_file` o `environment`). **No evita** errores de esquema ya existentes en PostgreSQL (por ejemplo `users_role_check` antiguo): en bases nuevas Hibernate crea el esquema; en bases viejas sigue haciendo falta el script `scripts/fix_users_role_add_admin.sql` o migraciones (Flyway/Liquibase). En Compose se puede montar un script en `docker-entrypoint-initdb.d` **solo la primera vez** que se crea el volumen de datos.

---

## Variables de “bootstrap” del administrador (`application.properties` / `.env`)

En `application.properties` verás líneas equivalentes a:

```properties
app.admin.bootstrap-email=${ADMIN_BOOTSTRAP_EMAIL:}
app.admin.bootstrap-password=${ADMIN_BOOTSTRAP_PASSWORD:}
app.admin.bootstrap-promote-seller=${ADMIN_BOOTSTRAP_PROMOTE_SELLER:false}
```

**Para qué sirven (todo es opcional):**

| Variable / propiedad | Qué hace |
|----------------------|-----------|
| **`ADMIN_BOOTSTRAP_EMAIL`** + **`ADMIN_BOOTSTRAP_PASSWORD`** | Al **arrancar** la aplicación, si **ambas** tienen valor y ese **correo no existe** en la tabla `users`, se **crea** un usuario con rol **ADMIN** y esa contraseña (guardada con BCrypt). Sirve para tener el primer admin en desarrollo sin insertar SQL a mano. |
| **`ADMIN_BOOTSTRAP_PROMOTE_SELLER`** (`true` / `false`) | Si es **`true`** y el correo del bootstrap **ya existe** como **vendedor (`SELLER`)**, en el arranque se **promueve** a **ADMIN** y se **reemplaza la contraseña** por la del `.env`. Útil si registraste ese correo como vendedor antes y quieres convertirlo en admin **una vez**. En producción suele dejarse en `false`. |

Si dejas **vacíos** `ADMIN_BOOTSTRAP_EMAIL` y `ADMIN_BOOTSTRAP_PASSWORD`, **no** se crea ni modifica ningún usuario por arranque.

**Seguridad:** no subas el `.env` a Git (está en `.gitignore`). En producción evita contraseñas débiles y valora desactivar el bootstrap o usar solo cuentas creadas por procedimientos controlados.

---

## Error 500 en `POST /api/auth/register`

Revisa el **log del servidor** (consola de Spring / IntelliJ). Causas frecuentes:

- Restricción **`users_role_check`** (ejecutar el script SQL de arriba).
- Base de datos **inaccesible** o credenciales **incorrectas** en `.env`.
- **`JWT_SECRET`** vacío o inválido.
- Correo **ya registrado** (debería devolver 409; si hay bug o envoltura genérica, puede verse como 500).

---

## Login (`POST /api/auth/login`)

- Además de `email` y `password`, el cuerpo puede incluir **`portal`**:
  - **`seller`** o campo omitido / vacío: solo se permite sesión para usuarios con rol **SELLER** (pantalla de vendedores).
  - **`admin`**: solo usuarios con rol **ADMIN** (pantalla de administración).
- Si el rol no coincide con el `portal` indicado, la API responde **401** con el mismo mensaje genérico que para credenciales incorrectas (no se indica explícitamente “no eres vendedor/admin”).

---

## Correo y recuperación de contraseña

- **Vendedores y administradores** usan los mismos endpoints `POST /api/auth/forgot-password` y `POST /api/auth/reset-password` (no hay rol distinto en el servidor para el reset).
- Si el correo **no** está en `users`, la API responde **404** con mensaje claro (no se envía correo). Si está registrado, responde **200** y se envía el correo con el enlace. *(Esto permite inferir si un correo existe en el sistema; se prioriza la claridad frente a la enumeración opaca.)*
- El front puede enviar un `returnUrl` permitido (`/admin/ingreso` o `/login`) para que, tras cambiar la clave, el usuario vuelva al login correspondiente; el enlace del correo incluye ese query de forma segura (lista blanca en backend).
- Detalle de `MAIL_*`: ver `.env.example` y comentarios en `application.properties`.

---

## Alta de administradores (`POST /api/admin/admins`)

- Requiere **JWT de un usuario con rol ADMIN** (misma seguridad que el resto de `/api/admin/**`).
- Cuerpo: mismo JSON que `POST /api/auth/register` (`RegisterRequest`: nombres, apellidos, dirección, teléfono, correo, contraseña).
- Crea el usuario con rol **ADMIN**; **no** devuelve token de sesión del nuevo usuario (el administrador que registra mantiene su sesión).
