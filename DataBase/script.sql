-- =====================================================================
-- FintechDigital - Esquema inicial (MySQL 8.x)
-- Ejecutar primero este script y luego datos_prueba.sql
-- =====================================================================

CREATE DATABASE IF NOT EXISTS fintechdigital
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE fintechdigital;

-- ---------------------------------------------------------------------
-- Usuarios
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS usuarios (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre          VARCHAR(80)  NOT NULL,
    apellido        VARCHAR(80)  NOT NULL,
    email           VARCHAR(120) NOT NULL UNIQUE,
    documento       VARCHAR(20)  NOT NULL UNIQUE,
    telefono        VARCHAR(20),
    password_hash   VARCHAR(255) NOT NULL,
    puntos          INT          NOT NULL DEFAULT 0,
    estado          ENUM('ACTIVO','INACTIVO','BLOQUEADO') NOT NULL DEFAULT 'ACTIVO',
    fecha_registro  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_usuarios_puntos CHECK (puntos >= 0)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Billeteras digitales
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS billeteras (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id      BIGINT        NOT NULL,
    numero          VARCHAR(20)   NOT NULL UNIQUE,
    saldo           DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    moneda          CHAR(3)       NOT NULL DEFAULT 'COP',
    estado          ENUM('ACTIVO','INACTIVO','BLOQUEADO') NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_billeteras_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    CONSTRAINT chk_billeteras_saldo CHECK (saldo >= 0)
) ENGINE=InnoDB;

CREATE INDEX idx_billeteras_usuario ON billeteras(usuario_id);

-- ---------------------------------------------------------------------
-- Categorías (para analítica de gastos)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS categorias (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(60) NOT NULL UNIQUE,
    descripcion VARCHAR(255)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Transacciones
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS transacciones (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    billetera_origen_id   BIGINT NULL,
    billetera_destino_id  BIGINT NULL,
    categoria_id          BIGINT NULL,
    tipo                  ENUM('DEPOSITO','RETIRO','TRANSFERENCIA','PAGO','RECOMPENSA') NOT NULL,
    monto                 DECIMAL(18,2) NOT NULL,
    estado                ENUM('PENDIENTE','COMPLETADA','FALLIDA','REVERTIDA') NOT NULL DEFAULT 'PENDIENTE',
    descripcion           VARCHAR(255),
    fecha                 TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_trx_origen    FOREIGN KEY (billetera_origen_id)  REFERENCES billeteras(id),
    CONSTRAINT fk_trx_destino   FOREIGN KEY (billetera_destino_id) REFERENCES billeteras(id),
    CONSTRAINT fk_trx_categoria FOREIGN KEY (categoria_id)         REFERENCES categorias(id),
    CONSTRAINT chk_trx_monto CHECK (monto > 0)
) ENGINE=InnoDB;

CREATE INDEX idx_trx_origen  ON transacciones(billetera_origen_id);
CREATE INDEX idx_trx_destino ON transacciones(billetera_destino_id);
CREATE INDEX idx_trx_fecha   ON transacciones(fecha);

-- ---------------------------------------------------------------------
-- Recompensas y canjes
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS recompensas (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre            VARCHAR(100) NOT NULL,
    descripcion       VARCHAR(255),
    puntos_requeridos INT          NOT NULL,
    activa            BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_recompensas_puntos CHECK (puntos_requeridos > 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS canjes (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id     BIGINT    NOT NULL,
    recompensa_id  BIGINT    NOT NULL,
    fecha_canje    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_canjes_usuario    FOREIGN KEY (usuario_id)    REFERENCES usuarios(id),
    CONSTRAINT fk_canjes_recompensa FOREIGN KEY (recompensa_id) REFERENCES recompensas(id)
) ENGINE=InnoDB;
