-- ============================================================================
-- MenteNova - Datos iniciales (seed) para el módulo de login
-- APF2 - Curso Integrador I: Sistemas Software
-- Ejecutar solo en entorno de desarrollo / piloto académico
-- ============================================================================

-- Roles base (coherentes con el DDL del documento APF2)
INSERT INTO rol (nombre, descripcion, activo) VALUES
    ('ADMINISTRADOR', 'Gestión integral, seguridad, parámetros y reportes', TRUE)
    ON CONFLICT (nombre) DO NOTHING;

INSERT INTO rol (nombre, descripcion, activo) VALUES
    ('RECEPCIONISTA', 'Gestión operativa de pacientes, citas y pagos', TRUE)
    ON CONFLICT (nombre) DO NOTHING;

INSERT INTO rol (nombre, descripcion, activo) VALUES
    ('PSICOLOGO', 'Gestión de agenda propia y atención administrativa', TRUE)
    ON CONFLICT (nombre) DO NOTHING;

INSERT INTO rol (nombre, descripcion, activo) VALUES
    ('PACIENTE', 'Consulta y operaciones limitadas sobre sus propias citas', TRUE)
    ON CONFLICT (nombre) DO NOTHING;

-- Usuario administrador por defecto (contraseña: Admin1234!)
INSERT INTO usuario (id_rol, nombres, apellidos, correo, password_hash, telefono, activo, fecha_creacion, fecha_actualizacion)
VALUES (
    (SELECT id_rol FROM rol WHERE nombre = 'ADMINISTRADOR'),
    'Administrador',
    'Sistema',
    'admin@mentenova.local',
    '$2a$12$B2/Bp/8BQA97j6x6INbUHusNFbtozJn5a3y0akjVMa9xWoFNVfEkS',
    '+51 1 234 5678',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (LOWER(correo)) DO NOTHING;