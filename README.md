# MenteNova - Sistema de Gestión de Citas

Sistema web de gestión de citas para el Centro Psicológico MenteNova (APF2 - Curso Integrador I).

## Stack tecnológico (Alternativa 1 seleccionada en el APF2)

| Capa | Tecnología |
|------|------------|
| Backend | Java 17, Spring Boot 3.2.5, Spring MVC |
| Seguridad | Spring Security (form-based login, BCrypt) |
| Vistas | Thymeleaf + Bootstrap 5.3 |
| Persistencia | Spring Data JPA / Hibernate |
| Base de datos | PostgreSQL |

## Estructura del proyecto

```
mentenova/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/mentenova/
│   │   │   ├── MenteNovaApplication.java
│   │   │   ├── entity/          (Usuario, Rol)
│   │   │   ├── repository/      (UsuarioRepository, RolRepository)
│   │   │   ├── security/        (CustomUserDetailsService, SecurityConfig)
│   │   │   └── controller/      (LoginController)
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── data.sql         (datos iniciales: roles + admin)
│   │       ├── templates/
│   │       │   └── login.html
│   │       └── static/
│   │           └── css/
│   │               └── login.css
│   └── test/
├── .gitignore
└── README.md
```

## Requisitos previos

- JDK 17 o superior
- Maven 3.9+
- PostgreSQL 15+ (el entorno ya tiene PostgreSQL 18.6 en instalación)
- IDE: IntelliJ IDEA, Eclipse o VS Code con soporte Java

## Configuración

1. Crear la base de datos en PostgreSQL:
   ```sql
   CREATE DATABASE mentenova;
   CREATE USER mentenova_app WITH PASSWORD 'CAMBIAR_EN_ENTORNO';
   GRANT ALL PRIVILEGES ON DATABASE mentenova TO mentenova_app;
   ```

2. Ejecutar el script DDL del documento APF2 (`mentenova_schema.sql`) para crear tablas `rol`, `usuario`, etc.

3. Editar `src/main/resources/application.properties` y configurar las credenciales de PostgreSQL.

4. Colocar la imagen `logo-MenteNova.jpg` en:
   `src/main/resources/static/images/logo-MenteNova.jpg`
   (la imagen original está en `Imagen MenteNova/logo-MenteNova.jpg` — no se movió ni modificó).

5. Ejecutar la aplicación:
   ```bash
   mvn spring-boot:run
   ```

6. Acceder a: `http://localhost:8080/mentenova/login`

## Usuario por defecto (desarrollo)

| Campo | Valor |
|-------|-------|
| Correo | `admin@mentenova.local` |
| Contraseña | `Admin1234!` |
| Rol | ADMINISTRADOR |

> **Nota:** Reemplazar el `password_hash` en `data.sql` con un hash BCrypt real generado por la aplicación antes de usar en cualquier entorno que no sea desarrollo local.

## Créditos

Proyecto académico - Universidad Tecnológica del Perú, Curso Integrador I.
Equipo: Piero Nicolas Quispe Pozo, Jeanpaul Cordova Gallardo, Luis Enrique Ramirez Pardo, Estefani Michell Salazar Mejia, Christian Raphael Neyra Barrios.