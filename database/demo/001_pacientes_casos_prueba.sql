SET client_encoding = 'UTF8';

INSERT INTO usuario (id_rol, nombres, apellidos, correo, password_hash, telefono)
SELECT r.id_rol, 'Pedro', 'Salinas Rojas', 'pedro.salinas@demo.mentenova.local',
       '$2a$10$8SXR70ZV4fO6fNfmpuSQ6O3lk6sMu1/M7NnV8/wTmVCy7sefx3a/K', '+51 955 410 220'
FROM rol r
WHERE r.nombre = 'PACIENTE';

INSERT INTO paciente (tipo_documento, numero_documento, nombres, apellidos, fecha_nacimiento,
                      telefono, correo, direccion, activo) VALUES
    ('DNI', '70000025', 'Pedro', 'Salinas Rojas', DATE '1985-09-18',
     '+51 955 410 220', 'pedro.salinas@example.com', 'Jr. Huallaga 345, Cercado de Lima', TRUE),
    (NULL, NULL, 'Lucas', 'Paredes Soto', DATE '2016-04-12',
     '+51 944 210 331', NULL, 'Av. Arequipa 1234, Lince', TRUE),
    ('CE', '001234567', 'Valeria', 'Mendoza Pérez', DATE '1994-02-20',
     '+51 933 118 245', 'valeria.mendoza@example.com', NULL, TRUE),
    ('PASAPORTE', 'X1234567', 'James', 'Carter', DATE '1989-07-03',
     '+51 922 640 118', 'james.carter@example.com', 'Calle Los Pinos 210, Miraflores', TRUE),
    ('DNI', '70000026', 'Rosa Inés', 'Vargas Quiroz', DATE '1972-11-05',
     '+51 911 300 452', NULL, NULL, FALSE);
