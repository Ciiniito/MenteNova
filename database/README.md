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
| 6 | Carga los datos de demostración (`reportes_datos_demo.sql`) si aún no están y luego los scripts nuevos de `database/demo/`, cada uno una sola vez. |

Después levanta la aplicación y entra a `http://localhost:8080/mentenova/login`.

### Opciones

| Opción | Uso |
|---|---|
| `-SinDatosDemo` | No carga los datos de demostración. La base queda sin usuarios ni especialidades: ejecuta `data.sql` (ver más abajo) para tener el administrador inicial y crea las especialidades en `/especialidades`. |
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
| `paciente@demo.mentenova.local` | Paciente (vinculado a la ficha de María González Pérez) |
| `pedro.salinas@demo.mentenova.local` | Paciente con cuenta aún **sin vincular** a su ficha |

Los datos demo crean unas 585 citas con sus pagos, desde 90 días antes hasta 21 días después de
la fecha en que se cargan.

### Psicólogos, especialidades y disponibilidad

Los módulos de psicólogos, especialidades, disponibilidad y agenda usan tablas que ya están en
`mentenova_schema.sql`; no necesitan scripts adicionales. Los datos demo traen 5 especialidades y
4 psicólogos con horarios de lunes a sábado: de 09:00 a 14:00 presencial y de 15:00 a 20:00
virtual. No hay excepciones cargadas.

| `id_psicologo` | Cuenta | Psicólogo |
|---|---|---|
| 1 | `psicologo4@demo.mentenova.local` | Diego Ramos Chávez |
| 2 | `psicologo3@demo.mentenova.local` | Carmen Salas Paredes |
| 3 | `psicologo2@demo.mentenova.local` | Andrés Torres Vega |
| 4 | `psicologo1@demo.mentenova.local` | Laura Méndez Ríos |

### Casos de prueba de la gestión de pacientes (`demo/001_pacientes_casos_prueba.sql`)

| Paciente | Para probar |
|---|---|
| Pedro Salinas Rojas (DNI 70000025) | Vincular una cuenta: edita la ficha y escribe `pedro.salinas@demo.mentenova.local` en "Cuenta de acceso". |
| Lucas Paredes Soto | Paciente menor y sin documento. |
| Valeria Mendoza Pérez (CE 001234567) | Carné de extranjería. |
| James Carter (Pasaporte X1234567) | Pasaporte. |
| Rosa Inés Vargas Quiroz (DNI 70000026) | Paciente inactivo: aparece con el filtro "Inactivos". |

`src/main/resources/data.sql` es opcional con los datos demo: el DDL ya inserta los roles. Es
necesario si usaste `-SinDatosDemo`, porque crea el administrador `admin@mentenova.local` /
`Admin1234!`. Ejecútalo después del DDL:
`psql -U mentenova_app -d mentenova -f src/main/resources/data.sql`.

## Si tu módulo necesita cambiar la base

1. Crea un archivo en `database/cambios/` con el siguiente número correlativo y un nombre
   descriptivo, por ejemplo `002_agregar_campo_x_a_cita.sql`. Si solo son datos de prueba,
   va en `database/demo/` (por ejemplo `002_citas_para_probar_x.sql`).
2. Escribe solo el cambio (`ALTER TABLE`, `CREATE TABLE`, `INSERT` de catálogos, etc.), **sin**
   `BEGIN`/`COMMIT`. El script lo ejecuta en una transacción y lo registra en la tabla
   `control_cambios` (por ejemplo `cambios/002_agregar_campo_x_a_cita.sql`); si falla, no queda
   nada a medias.
3. No edites un script que ya subiste: para corregirlo, crea otro. El script avisa si un archivo
   ya aplicado fue modificado.
4. Súbelo junto con tu código. Tus compañeros solo vuelven a ejecutar `actualizar_base_local.ps1`.

No modifiques `mentenova_schema.sql`: es la base del APF2 y solo se aplica en una base vacía.

| Script aplicado | Qué agrega |
|---|---|
| `cambios/001_auditoria_paciente.sql` | Registra en `auditoria` quién crea o modifica cada paciente (la ficha la muestra al Administrador). |
| `demo/001_pacientes_casos_prueba.sql` | Los casos de prueba de pacientes descritos arriba. |

## Archivos

| Archivo | Contenido |
|---|---|
| `actualizar_base_local.ps1` | Script único de instalación y actualización (Windows). |
| `mentenova_schema.sql` | DDL completo del APF2 (sección 6.2). |
| `permisos_usuario_app.sql` | Asigna a `mentenova_app` las tablas y funciones; lo usa el script. |
| `reportes_datos_demo.sql` | Datos de demostración: usuarios por rol, citas y pagos. |
| `cambios/` | Cambios incrementales de la estructura de la base. |
| `demo/` | Datos de prueba incrementales (no se cargan con `-SinDatosDemo`). |

## Sin Windows o a mano

```bash
psql -U postgres -c "CREATE ROLE mentenova_app LOGIN PASSWORD 'mentenova_app';"
psql -U postgres -c "CREATE DATABASE mentenova OWNER mentenova_app;"
psql -U mentenova_app -d mentenova -f database/mentenova_schema.sql
psql -U mentenova_app -d mentenova -f database/reportes_datos_demo.sql
```

Después aplica, en orden, los archivos de `database/cambios/` y luego los de `database/demo/` que
aún no tengas.

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
