SET client_encoding = 'UTF8';

BEGIN;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM usuario WHERE LOWER(correo) LIKE '%@demo.mentenova.local') THEN
        RAISE EXCEPTION 'Los datos demo ya fueron cargados (existen usuarios @demo.mentenova.local).';
    END IF;
END $$;

INSERT INTO usuario (id_rol, nombres, apellidos, correo, password_hash, telefono)
SELECT r.id_rol, u.nombres, u.apellidos, u.correo,
       '$2a$10$8SXR70ZV4fO6fNfmpuSQ6O3lk6sMu1/M7NnV8/wTmVCy7sefx3a/K', u.telefono
FROM (VALUES
    ('ADMINISTRADOR', 'Ana',    'Martínez Soto',  'admin@demo.mentenova.local',      '+51 900 000 001'),
    ('RECEPCIONISTA', 'Rosa',   'Huamán Quispe',  'recepcion@demo.mentenova.local',  '+51 900 000 002'),
    ('PSICOLOGO',     'Laura',  'Méndez Ríos',    'psicologo1@demo.mentenova.local', '+51 900 000 011'),
    ('PSICOLOGO',     'Andrés', 'Torres Vega',    'psicologo2@demo.mentenova.local', '+51 900 000 012'),
    ('PSICOLOGO',     'Carmen', 'Salas Paredes',  'psicologo3@demo.mentenova.local', '+51 900 000 013'),
    ('PSICOLOGO',     'Diego',  'Ramos Chávez',   'psicologo4@demo.mentenova.local', '+51 900 000 014'),
    ('PACIENTE',      'María',  'González Pérez', 'paciente@demo.mentenova.local',   '+51 987 654 321')
) AS u(rol, nombres, apellidos, correo, telefono)
JOIN rol r ON r.nombre = u.rol;

INSERT INTO psicologo (id_usuario, numero_colegiatura, telefono)
SELECT u.id_usuario, 'CPsP-DEMO-' || substring(u.correo FROM 'psicologo(\d)'), u.telefono
FROM usuario u
WHERE u.correo LIKE 'psicologo_@demo.mentenova.local';

INSERT INTO especialidad (nombre, descripcion) VALUES
    ('Psicología infantil y adolescente', 'Atención de niños y adolescentes con apoderado'),
    ('Psicología de adultos',             'Terapia individual para adultos'),
    ('Terapia de pareja',                 'Orientación y terapia de pareja'),
    ('Orientación familiar',              'Orientación y terapia familiar'),
    ('Evaluación psicológica',            'Evaluaciones psicológicas')
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO psicologo_especialidad (id_psicologo, id_especialidad)
SELECT ps.id_psicologo, e.id_especialidad
FROM (VALUES
    ('psicologo1@demo.mentenova.local', 'Psicología infantil y adolescente'),
    ('psicologo1@demo.mentenova.local', 'Evaluación psicológica'),
    ('psicologo2@demo.mentenova.local', 'Psicología de adultos'),
    ('psicologo2@demo.mentenova.local', 'Terapia de pareja'),
    ('psicologo3@demo.mentenova.local', 'Orientación familiar'),
    ('psicologo3@demo.mentenova.local', 'Psicología infantil y adolescente'),
    ('psicologo4@demo.mentenova.local', 'Psicología de adultos'),
    ('psicologo4@demo.mentenova.local', 'Evaluación psicológica')
) AS m(correo, especialidad)
JOIN usuario u ON u.correo = m.correo
JOIN psicologo ps ON ps.id_usuario = u.id_usuario
JOIN especialidad e ON e.nombre = m.especialidad;

INSERT INTO disponibilidad (id_psicologo, dia_semana, hora_inicio, hora_fin, modalidad)
SELECT ps.id_psicologo, d.dia, f.inicio, f.fin, f.modalidad
FROM psicologo ps
JOIN usuario u ON u.id_usuario = ps.id_usuario
CROSS JOIN generate_series(1, 6) AS d(dia)
CROSS JOIN (VALUES
    (TIME '09:00', TIME '14:00', 'PRESENCIAL'),
    (TIME '15:00', TIME '20:00', 'VIRTUAL')
) AS f(inicio, fin, modalidad)
WHERE u.correo LIKE '%@demo.mentenova.local';

INSERT INTO paciente (id_usuario, tipo_documento, numero_documento, nombres, apellidos, fecha_nacimiento, telefono, correo)
SELECT u.id_usuario, 'DNI', '70000001', 'María', 'González Pérez', DATE '1997-03-14', '+51 987 654 321', u.correo
FROM usuario u
WHERE u.correo = 'paciente@demo.mentenova.local';

INSERT INTO paciente (tipo_documento, numero_documento, nombres, apellidos, fecha_nacimiento, telefono) VALUES
    ('DNI', '70000002', 'Juan Pablo',     'Ruiz Campos',      DATE '1990-07-02', '+51 976 543 210'),
    ('DNI', '70000003', 'Ana Valentina',  'López Herrera',    DATE '1984-11-23', '+51 965 432 109'),
    ('DNI', '70000004', 'Daniel',         'Santiago Rojas',   DATE '2009-05-10', '+51 954 321 098'),
    ('DNI', '70000005', 'Laura',          'Gómez Castillo',   DATE '1999-01-30', '+51 943 210 987'),
    ('DNI', '70000006', 'Camila',         'Rodríguez Flores', DATE '1993-08-17', '+51 932 109 876'),
    ('DNI', '70000007', 'Carlos',         'Méndez Vargas',    DATE '1979-02-05', '+51 921 098 765'),
    ('DNI', '70000008', 'Sofía',          'Ramírez Torres',   DATE '2012-09-28', '+51 910 987 654'),
    ('DNI', '70000009', 'Miguel',         'Torres Salazar',   DATE '1988-04-12', '+51 909 876 543'),
    ('DNI', '70000010', 'Valentina',      'Castro Navarro',   DATE '2001-12-03', '+51 998 765 432'),
    ('DNI', '70000011', 'Jorge',          'Pérez Aguilar',    DATE '1975-06-21', '+51 987 001 122'),
    ('DNI', '70000012', 'Lucía',          'Fernández Rivas',  DATE '1995-10-09', '+51 987 002 233'),
    ('DNI', '70000013', 'Mateo',          'Silva Cárdenas',   DATE '2010-03-15', '+51 987 003 344'),
    ('DNI', '70000014', 'Isabella',       'Vega Mendoza',     DATE '2006-07-27', '+51 987 004 455'),
    ('DNI', '70000015', 'Ricardo',        'Paredes Luna',     DATE '1982-01-08', '+51 987 005 566'),
    ('DNI', '70000016', 'Fernanda',       'Quispe Mamani',    DATE '1998-05-19', '+51 987 006 677'),
    ('DNI', '70000017', 'Sebastián',      'Huerta Ponce',     DATE '1991-09-02', '+51 987 007 788'),
    ('DNI', '70000018', 'Gabriela',       'Ortiz Campos',     DATE '1987-12-14', '+51 987 008 899'),
    ('DNI', '70000019', 'Nicolás',        'Benítez Lara',     DATE '2013-02-25', '+51 987 009 900'),
    ('DNI', '70000020', 'Daniela',        'Cruz Espinoza',    DATE '2003-08-06', '+51 987 010 011'),
    ('DNI', '70000021', 'Alejandro',      'Rojas Medina',     DATE '1970-04-30', '+51 987 011 122'),
    ('DNI', '70000022', 'Paula',          'Chávez Romero',    DATE '1996-11-11', '+51 987 012 233'),
    ('DNI', '70000023', 'Tomás',          'Guerrero Ibáñez',  DATE '2008-06-03', '+51 987 013 344'),
    ('DNI', '70000024', 'Renata',         'Molina Acosta',    DATE '1992-10-22', '+51 987 014 455');

INSERT INTO cita (id_paciente, id_psicologo, id_estado_cita, fecha, hora_inicio, hora_fin,
                  modalidad, motivo_administrativo, creado_por, fecha_creacion)
WITH params AS (
    SELECT (SELECT id_usuario FROM usuario WHERE correo = 'recepcion@demo.mentenova.local') AS id_recepcion,
           (NOW() AT TIME ZONE 'America/Lima')::date AS hoy,
           (NOW() AT TIME ZONE 'America/Lima')::time AS ahora
),
psicologos AS (
    SELECT ps.id_psicologo
    FROM psicologo ps
    JOIN usuario u ON u.id_usuario = ps.id_usuario
    WHERE u.correo LIKE '%@demo.mentenova.local'
),
pacientes AS (
    SELECT pa.id_paciente,
           ROW_NUMBER() OVER (ORDER BY pa.numero_documento) - 1 AS n,
           COUNT(*) OVER () AS total
    FROM paciente pa
    WHERE pa.numero_documento LIKE '7000%'
),
slots AS (
    SELECT p.hoy + g.dia AS fecha,
           ps.id_psicologo,
           h.hora,
           p.hoy, p.ahora, p.id_recepcion,
           ABS(hashtext((p.hoy + g.dia)::text || '-' || ps.id_psicologo || '-' || h.hora)) % 100 AS azar,
           ABS(hashtext((p.hoy + g.dia)::text || '-' || ps.id_psicologo || '-' || h.hora || '-e')) % 100 AS azar_estado,
           ABS(hashtext((p.hoy + g.dia)::text || '-' || ps.id_psicologo || '-' || h.hora || '-p')) AS azar_paciente
    FROM params p
    CROSS JOIN generate_series(-90, 21) AS g(dia)
    CROSS JOIN psicologos ps
    CROSS JOIN (VALUES (9), (10), (11), (12), (13), (15), (16), (17), (18), (19)) AS h(hora)
    WHERE EXTRACT(ISODOW FROM p.hoy + g.dia) <> 7
)
SELECT pa.id_paciente,
       s.id_psicologo,
       ec.id_estado_cita,
       s.fecha,
       make_time(s.hora, 0, 0),
       make_time(s.hora + 1, 0, 0),
       CASE WHEN s.hora < 14 THEN 'PRESENCIAL' ELSE 'VIRTUAL' END,
       CASE WHEN s.azar % 3 = 0 THEN 'Primera consulta'
            WHEN s.azar % 3 = 1 THEN 'Sesión de seguimiento'
            ELSE 'Evaluación programada' END,
       s.id_recepcion,
       LEAST(NOW() - (1 + s.azar % 5) * INTERVAL '1 day',
             ((s.fecha - (1 + s.azar % 10))::timestamp + INTERVAL '10 hours') AT TIME ZONE 'America/Lima')
FROM slots s
JOIN pacientes pa ON pa.n = s.azar_paciente % pa.total
JOIN estado_cita ec ON ec.codigo =
    CASE
        WHEN s.fecha < s.hoy OR (s.fecha = s.hoy AND make_time(s.hora + 1, 0, 0) <= s.ahora) THEN
            CASE WHEN s.azar_estado < 70 THEN 'ATENDIDA'
                 WHEN s.azar_estado < 80 THEN 'NO_ASISTIO'
                 WHEN s.azar_estado < 95 THEN 'CANCELADA'
                 ELSE 'REPROGRAMADA' END
        ELSE
            CASE WHEN s.azar_estado < 55 THEN 'CONFIRMADA'
                 WHEN s.azar_estado < 90 THEN 'PENDIENTE'
                 ELSE 'CANCELADA' END
    END
WHERE s.azar < 16;

INSERT INTO pago (id_cita, monto, medio_pago, estado, fecha_pago, numero_operacion, registrado_por, fecha_registro)
WITH base AS (
    SELECT c.id_cita, c.modalidad, c.fecha_creacion, ec.codigo AS estado_cita,
           ((c.fecha + c.hora_fin)::timestamp AT TIME ZONE 'America/Lima') AS fin_cita,
           ABS(hashtext(c.id_cita::text || '-pago')) % 100 AS azar,
           ABS(hashtext(c.id_cita::text || '-medio')) % 100 AS azar_medio,
           (SELECT id_usuario FROM usuario WHERE correo = 'recepcion@demo.mentenova.local') AS id_recepcion
    FROM cita c
    JOIN estado_cita ec ON ec.id_estado_cita = c.id_estado_cita
    JOIN psicologo ps ON ps.id_psicologo = c.id_psicologo
    JOIN usuario u ON u.id_usuario = ps.id_usuario
    WHERE u.correo LIKE '%@demo.mentenova.local'
),
pagos AS (
    SELECT b.*,
           CASE
               WHEN b.estado_cita = 'ATENDIDA' AND b.azar < 90 THEN 'PAGADO'
               WHEN b.estado_cita = 'ATENDIDA' THEN 'PENDIENTE'
               WHEN b.estado_cita = 'CONFIRMADA' AND b.azar < 35 THEN 'PAGADO'
               WHEN b.estado_cita = 'CANCELADA' AND b.azar < 25 THEN 'ANULADO'
           END AS estado_pago,
           CASE WHEN b.azar_medio < 30 THEN 'YAPE'
                WHEN b.azar_medio < 50 THEN 'EFECTIVO'
                WHEN b.azar_medio < 70 THEN 'TARJETA'
                WHEN b.azar_medio < 85 THEN 'TRANSFERENCIA'
                WHEN b.azar_medio < 97 THEN 'PLIN'
                ELSE 'OTRO' END AS medio
    FROM base b
)
SELECT p.id_cita,
       CASE p.modalidad WHEN 'VIRTUAL' THEN 120.00 ELSE 150.00 END,
       p.medio,
       p.estado_pago,
       CASE WHEN p.estado_pago = 'PENDIENTE' THEN NULL
            WHEN p.estado_cita = 'ATENDIDA' THEN p.fin_cita
            ELSE LEAST(NOW(), p.fecha_creacion + INTERVAL '2 hours') END,
       CASE WHEN p.medio = 'EFECTIVO' THEN NULL ELSE 'OP-' || LPAD(p.id_cita::text, 6, '0') END,
       p.id_recepcion,
       CASE WHEN p.estado_cita = 'ATENDIDA' THEN p.fin_cita
            ELSE LEAST(NOW(), p.fecha_creacion + INTERVAL '2 hours') END
FROM pagos p
WHERE p.estado_pago IS NOT NULL;

COMMIT;
