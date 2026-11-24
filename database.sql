-- =====================================================================
-- ІС технічного обслуговування лабораторного обладнання
-- Курсова робота «Кросплатформне програмування», варіант 3
-- Схема БД (MySQL 8+), етап 2
-- =====================================================================

DROP DATABASE IF EXISTS lab_maintenance;
CREATE DATABASE lab_maintenance CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE lab_maintenance;

-- ---------------------------------------------------------------------
-- Обладнання
-- ---------------------------------------------------------------------
CREATE TABLE equipment (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    inventory_number  VARCHAR(32)  NOT NULL,
    name              VARCHAR(120) NOT NULL,
    model             VARCHAR(120) NULL,
    location          VARCHAR(64)  NULL,
    criticality       TINYINT      NOT NULL,           -- 1..5, фактор F1
    commissioned_date DATE         NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_equipment_inv UNIQUE (inventory_number),
    CONSTRAINT ck_equipment_crit CHECK (criticality BETWEEN 1 AND 5)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Фахівці
-- ---------------------------------------------------------------------
CREATE TABLE specialist (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    full_name      VARCHAR(120) NOT NULL,
    specialization VARCHAR(120) NULL,
    phone          VARCHAR(20)  NULL,
    email          VARCHAR(120) NULL,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Заявки на обслуговування
-- ---------------------------------------------------------------------
CREATE TABLE maintenance_request (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    equipment_id   BIGINT        NOT NULL,
    specialist_id  BIGINT        NULL,                  -- призначається при діагностиці
    description    VARCHAR(1000) NOT NULL,
    severity       TINYINT       NOT NULL,              -- 1..5, фактор F2
    downtime_hours INT           NOT NULL DEFAULT 0,    -- фактор F3
    diagnosis      VARCHAR(1000) NULL,
    status         ENUM ('NEW', 'DIAGNOSED', 'IN_SERVICE', 'DONE') NOT NULL DEFAULT 'NEW',
    priority       DECIMAL(5, 2) NOT NULL DEFAULT 0,    -- інтегральний показник I, 0..100
    created_at     DATETIME      NOT NULL,
    updated_at     DATETIME      NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_request_equipment FOREIGN KEY (equipment_id)
        REFERENCES equipment (id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_request_specialist FOREIGN KEY (specialist_id)
        REFERENCES specialist (id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_request_severity CHECK (severity BETWEEN 1 AND 5),
    CONSTRAINT ck_request_downtime CHECK (downtime_hours >= 0),
    CONSTRAINT ck_request_priority CHECK (priority BETWEEN 0 AND 100),
    INDEX ix_request_status_priority (status, priority)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Сервісні записи (виконані роботи)
-- ---------------------------------------------------------------------
CREATE TABLE service_record (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    request_id       BIGINT        NOT NULL,
    specialist_id    BIGINT        NOT NULL,
    work_description VARCHAR(1000) NOT NULL,
    hours_spent      DECIMAL(6, 2) NOT NULL,
    cost             DECIMAL(10, 2) NOT NULL DEFAULT 0,
    performed_at     DATETIME      NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_record_request FOREIGN KEY (request_id)
        REFERENCES maintenance_request (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_record_specialist FOREIGN KEY (specialist_id)
        REFERENCES specialist (id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_record_hours CHECK (hours_spent > 0),
    CONSTRAINT ck_record_cost CHECK (cost >= 0)
) ENGINE = InnoDB;

-- =====================================================================
-- Тестові дані
-- =====================================================================
INSERT INTO equipment (inventory_number, name, model, location, criticality, commissioned_date) VALUES
    ('INV-0001', 'Осцилограф цифровий',        'Rigol DS1054Z',      'ауд. 312', 4, '2021-09-01'),
    ('INV-0002', 'Генератор сигналів',         'Siglent SDG1032X',   'ауд. 312', 3, '2021-09-01'),
    ('INV-0003', 'Паяльна станція',            'Hakko FX-888D',      'ауд. 315', 2, '2019-02-15'),
    ('INV-0004', 'Мультиметр настільний',      'Keysight 34461A',    'ауд. 315', 3, '2022-03-10'),
    ('INV-0005', '3D-принтер',                 'Prusa MK4',          'ауд. 210', 2, '2024-01-20'),
    ('INV-0006', 'Сервер лабораторії мереж',   'Dell PowerEdge R650','серверна', 5, '2023-08-30');

INSERT INTO specialist (full_name, specialization, phone, email, active) VALUES
    ('Коваленко Ігор Петрович',  'Електронна апаратура', '+380501112233', 'kovalenko@example.com', TRUE),
    ('Мельник Олена Василівна',  'Комп''ютерна техніка', '+380672223344', 'melnyk@example.com',    TRUE),
    ('Шевчук Андрій Миколайович','Механіка, 3D-друк',    '+380933334455', 'shevchuk@example.com',  FALSE);

-- priority розраховано за формулою I = 0,40·F1 + 0,35·F2 + 0,25·F3
INSERT INTO maintenance_request
    (equipment_id, specialist_id, description, severity, downtime_hours, diagnosis, status, priority, created_at, updated_at)
VALUES
    -- crit 5, sev 5, 72 год → 40 + 35 + 25 = 100.00
    (6, 2,    'Сервер не завантажується після вимкнення живлення', 5, 72,
     'Вийшов з ладу блок живлення', 'IN_SERVICE', 100.00, '2026-11-18 09:15:00', '2026-11-19 11:00:00'),
    -- crit 4, sev 3, 24 год → 30 + 17.5 + 8.33 = 55.83
    (1, 1,    'Немає зображення на каналі 2', 3, 24,
     'Пошкоджено роз''єм BNC каналу 2', 'DIAGNOSED', 55.83, '2026-11-20 10:00:00', '2026-11-21 14:30:00'),
    -- crit 2, sev 2, 8 год → 10 + 8.75 + 2.78 = 21.53
    (3, NULL, 'Жало не нагрівається до заданої температури', 2, 8,
     NULL, 'NEW', 21.53, '2026-11-22 12:40:00', '2026-11-22 12:40:00'),
    -- crit 2, sev 4, 48 год → 10 + 26.25 + 16.67 = 52.92
    (5, 2,    'Зсув шарів під час друку', 4, 48,
     'Ослаблений ремінь осі X', 'DONE', 52.92, '2026-11-10 08:00:00', '2026-11-12 16:00:00');

INSERT INTO service_record (request_id, specialist_id, work_description, hours_spent, cost, performed_at) VALUES
    (1, 2, 'Заміна блока живлення 800 Вт, перевірка RAID', 2.50, 4200.00, '2026-11-19 11:00:00'),
    (4, 2, 'Натяг і заміна ременя осі X, калібрування',    1.00,  350.00, '2026-11-12 15:00:00');
