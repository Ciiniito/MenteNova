# Base de datos local de MenteNova

Cada integrante trabaja con su **propio PostgreSQL local**: `application.properties` apunta a
`localhost:5432/mentenova` con el usuario `mentenova_app`. Esta carpeta deja esa base lista y al
día con un solo comando.

## Uso rápido (Windows)

Requisitos: PostgreSQL 15 o superior (`winget install --id PostgreSQL.PostgreSQL.18 -e`) y la
clave del usuario `postgres` que elegiste al instalarlo.

Desde la raíz del proyecto:

```powershell
powershell -ExecutionPolicy Bypass -File database\actualizar_base_local.ps1
```

Ejecútalo la primera vez y **después de cada `git pull`**. Solo aplica lo que falta, así que
puedes correrlo las veces que quieras.

| Paso | Qué hace |
|---|---|
| 1 | Verifica la conexión con PostgreSQL. |
| 2 | Crea el usuario `mentenova_app` y la base `mentenova` si no existen. Si la clave del usuario no coincide con `application.properties`, la corrige. |
| 3 | Deja a `mentenova_app` como dueño de las tablas. Hace falta si las creaste como `postgres`, por ejemplo desde pgAdmin. |
| 4 | Crea las 15 tablas del APF2 (`mentenova_schema.sql`) si la base está vacía. |
| 5 | Aplica los scripts nuevos de `database/cambios/`, cada uno una sola vez. |
| 6 | Carga los datos de demostración si aún no están. |

Después levanta la aplicación y entra a `http://localhost:8080/mentenova/login`.

### Opciones

| Opción | Uso |
|---|---|
| `-SinDatosDemo` | No carga los datos de demostración. |
| `-Recrear` | Borra y vuelve a crear la base (pide confirmación). Sirve para regenerar los datos demo con fechas actuales o para empezar de cero. |
| `-ClavePostgres <clave>` | Pasa la clave de `postgres` sin que la pida. |
| `-Puerto`, `-BaseDatos`, `-UsuarioApp`, `-ClaveApp` | Si tu instalación usa otros valores. Por defecto: `5432`, `mentenova`, `mentenova_app`, `mentenova_app`. |

## Usuarios de demostración

Contraseña de todos: `Demo1234!`

| Correo | Rol |
|---|---|
| `admin@demo.mentenova.local` | Administrador |
| `recepcion@demo.mentenova.local` | Recepcionista |
| `psicologo1@demo.mentenova.local` … `psicologo4@demo.mentenova.local` | Psicólogo |
| `paciente@demo.mentenova.local` | Paciente |

Los datos demo crean unas 585 citas con sus pagos, desde 90 días antes hasta 21 días después de
la fecha en que se cargan.

`src/main/resources/data.sql` no hace falta: el DDL ya inserta los roles, y el hash de su
administrador (`admin@mentenova.local`) no es un BCrypt válido.

## Si tu módulo necesita cambiar la base

1. Crea un archivo en `database/cambios/` con un número correlativo y un nombre descriptivo, por
   ejemplo `001_agregar_campo_x_a_cita.sql`.
2. Escribe solo el cambio (`ALTER TABLE`, `CREATE TABLE`, `INSERT` de catálogos, etc.), **sin**
   `BEGIN`/`COMMIT`. El script lo ejecuta en una transacción y lo registra en la tabla
   `control_cambios`; si falla, no queda nada a medias.
3. No edites un script que ya subiste: para corregirlo, crea otro. El script avisa si un archivo
   ya aplicado fue modificado.
4. Súbelo junto con tu código. Tus compañeros solo vuelven a ejecutar `actualizar_base_local.ps1`.

No modifiques `mentenova_schema.sql`: es la base del APF2 y solo se aplica en una base vacía.

## Archivos

| Archivo | Contenido |
|---|---|
| `actualizar_base_local.ps1` | Script único de instalación y actualización (Windows). |
| `mentenova_schema.sql` | DDL completo del APF2 (sección 6.2). |
| `permisos_usuario_app.sql` | Asigna a `mentenova_app` las tablas y funciones; lo usa el script. |
| `reportes_datos_demo.sql` | Datos de demostración: usuarios por rol, citas y pagos. |
| `cambios/` | Cambios incrementales de la base. |

## Sin Windows o a mano

```bash
psql -U postgres -c "CREATE ROLE mentenova_app LOGIN PASSWORD 'mentenova_app';"
psql -U postgres -c "CREATE DATABASE mentenova OWNER mentenova_app;"
psql -U mentenova_app -d mentenova -f database/mentenova_schema.sql
psql -U mentenova_app -d mentenova -f database/reportes_datos_demo.sql
```

Después aplica, en orden, los archivos de `database/cambios/` que aún no tengas.

## Problemas frecuentes

| Mensaje | Solución |
|---|---|
| `No se pudo conectar como postgres` | El servicio de PostgreSQL está detenido o la clave es incorrecta. |
| `la ejecución de scripts está deshabilitada en este sistema` | Ejecuta el comando tal cual, con `-ExecutionPolicy Bypass`. |
| `La base tiene N de 15 tablas del APF2` | La base está incompleta. Si es de pruebas, usa `-Recrear`. |
| La aplicación falla con `permission denied for table` | Ejecuta el script: el paso 3 corrige los dueños de las tablas. |
| El puerto 8080 está ocupado | `mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"` |

Si más adelante el equipo prefiere que la aplicación aplique los cambios sola al arrancar, el paso
natural es Flyway. Requiere cambiar `pom.xml` y `application.properties`, por eso no se incluyó.
