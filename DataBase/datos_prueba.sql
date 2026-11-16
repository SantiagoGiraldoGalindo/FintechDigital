-- =====================================================================
-- FintechDigital - Datos de prueba (ficticios)
-- Ejecutar DESPUÉS de script.sql
-- Los password_hash son marcadores, no hashes reales ni contraseñas.
-- =====================================================================

USE fintechdigital;

INSERT INTO usuarios (nombre, apellido, email, documento, telefono, password_hash, puntos) VALUES
('Ana',    'Gómez',   'ana.gomez@example.com',    '1000000001', '3000000001', 'HASH_DE_PRUEBA_NO_VALIDO', 120),
('Carlos', 'Ruiz',    'carlos.ruiz@example.com',  '1000000002', '3000000002', 'HASH_DE_PRUEBA_NO_VALIDO', 45),
('Lucía',  'Torres',  'lucia.torres@example.com', '1000000003', '3000000003', 'HASH_DE_PRUEBA_NO_VALIDO', 300);

INSERT INTO billeteras (usuario_id, numero, saldo, moneda) VALUES
(1, 'FD-0000000001', 500000.00, 'COP'),
(2, 'FD-0000000002', 250000.00, 'COP'),
(3, 'FD-0000000003', 1000000.00, 'COP');

INSERT INTO categorias (nombre, descripcion) VALUES
('Alimentación', 'Restaurantes y supermercado'),
('Transporte',   'Taxis, buses y combustible'),
('Servicios',    'Agua, luz, internet'),
('Ocio',         'Entretenimiento y suscripciones'),
('Otros',        'Sin categoría específica');

INSERT INTO transacciones (billetera_origen_id, billetera_destino_id, categoria_id, tipo, monto, estado, descripcion) VALUES
(NULL, 1, NULL, 'DEPOSITO',      500000.00, 'COMPLETADA', 'Depósito inicial'),
(NULL, 2, NULL, 'DEPOSITO',      250000.00, 'COMPLETADA', 'Depósito inicial'),
(NULL, 3, NULL, 'DEPOSITO',     1000000.00, 'COMPLETADA', 'Depósito inicial'),
(1,    2, 5,    'TRANSFERENCIA',  50000.00, 'COMPLETADA', 'Pago entre amigos'),
(2,    NULL, 1, 'PAGO',          32000.00, 'COMPLETADA', 'Almuerzo'),
(3,    NULL, 3, 'PAGO',         120000.00, 'COMPLETADA', 'Internet del mes'),
(NULL, 1, NULL, 'RECOMPENSA',     5000.00, 'COMPLETADA', 'Bono por uso frecuente');

INSERT INTO recompensas (nombre, descripcion, puntos_requeridos) VALUES
('Bono de $10.000', 'Saldo adicional en tu billetera', 100),
('Bono de $50.000', 'Saldo adicional en tu billetera', 450),
('Cine para dos',   'Entradas de cine',                250);

INSERT INTO canjes (usuario_id, recompensa_id) VALUES
(3, 1);
