-- ============================================================================
-- GlobalTrade Supply Chain Management System — Database Schema
-- MySQL 8+ compatible
-- ============================================================================

CREATE DATABASE IF NOT EXISTS globaltrade_scm
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE globaltrade_scm;

-- ============================================================================
-- Core Supply Chain Tables
-- ============================================================================

CREATE TABLE warehouses (
    warehouse_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    warehouse_code      VARCHAR(20)     NOT NULL UNIQUE,
    name                VARCHAR(200)    NOT NULL,
    location            VARCHAR(200)    NOT NULL,
    country             VARCHAR(100)    NOT NULL,
    region              VARCHAR(50)     NOT NULL,
    capacity            INT             NOT NULL,
    utilization_percent DECIMAL(5,2)    DEFAULT 0.00,
    manager_name        VARCHAR(150),
    contact_phone       VARCHAR(30),
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_warehouse_region (region),
    INDEX idx_warehouse_utilization (utilization_percent)
) ENGINE=InnoDB;

CREATE TABLE vendors (
    vendor_id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    vendor_code         VARCHAR(20)     NOT NULL UNIQUE,
    name                VARCHAR(200)    NOT NULL,
    country             VARCHAR(100)    NOT NULL,
    city                VARCHAR(100),
    contact_email       VARCHAR(150),
    contact_phone       VARCHAR(30),
    category            VARCHAR(100)    NOT NULL,
    performance_rating  DECIMAL(3,2)    DEFAULT 0.00,
    compliance_status   VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    contract_expiry     DATE,
    annual_volume       DECIMAL(15,2),
    payment_terms       VARCHAR(50),
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_vendor_country (country),
    INDEX idx_vendor_compliance (compliance_status),
    INDEX idx_vendor_rating (performance_rating)
) ENGINE=InnoDB;

CREATE TABLE shipments (
    shipment_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    tracking_number     VARCHAR(50)     NOT NULL UNIQUE,
    origin_country      VARCHAR(100)    NOT NULL,
    origin_city         VARCHAR(100)    NOT NULL,
    destination_country VARCHAR(100)    NOT NULL,
    destination_city    VARCHAR(100)    NOT NULL,
    carrier             VARCHAR(100)    NOT NULL,
    status              VARCHAR(30)     NOT NULL DEFAULT 'CREATED',
    weight_kg           DECIMAL(10,2),
    declared_value      DECIMAL(15,2),
    currency            VARCHAR(3),
    customs_cleared     BOOLEAN         DEFAULT FALSE,
    estimated_delivery  DATETIME,
    actual_delivery     DATETIME,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    notes               VARCHAR(500),
    INDEX idx_shipment_status (status),
    INDEX idx_shipment_carrier (carrier),
    INDEX idx_shipment_destination (destination_country)
) ENGINE=InnoDB;

CREATE TABLE inventory_items (
    item_id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku                 VARCHAR(50)     NOT NULL,
    product_name        VARCHAR(200)    NOT NULL,
    warehouse_id        BIGINT          NOT NULL,
    quantity            INT             NOT NULL DEFAULT 0,
    reorder_level       INT             NOT NULL DEFAULT 10,
    max_stock_level     INT             DEFAULT 1000,
    unit_of_measure     VARCHAR(20)     DEFAULT 'UNITS',
    last_replenished    DATETIME,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_inventory_sku (sku),
    INDEX idx_inventory_warehouse (warehouse_id),
    INDEX idx_inventory_low_stock (quantity, reorder_level),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(warehouse_id)
) ENGINE=InnoDB;

CREATE TABLE purchase_orders (
    order_id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number            VARCHAR(30)     NOT NULL UNIQUE,
    vendor_id               BIGINT          NOT NULL,
    item_description        VARCHAR(500)    NOT NULL,
    quantity                INT             NOT NULL,
    unit_price              DECIMAL(15,2)   NOT NULL,
    total_amount            DECIMAL(15,2)   NOT NULL,
    currency                VARCHAR(3)      NOT NULL DEFAULT 'USD',
    status                  VARCHAR(30)     NOT NULL DEFAULT 'DRAFT',
    order_date              DATE            NOT NULL,
    expected_delivery       DATE,
    actual_delivery         DATE,
    destination_warehouse_id BIGINT,
    notes                   VARCHAR(500),
    created_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_po_vendor (vendor_id),
    INDEX idx_po_status (status),
    INDEX idx_po_order_date (order_date),
    FOREIGN KEY (vendor_id) REFERENCES vendors(vendor_id),
    FOREIGN KEY (destination_warehouse_id) REFERENCES warehouses(warehouse_id)
) ENGINE=InnoDB;

CREATE TABLE customs_declarations (
    declaration_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    declaration_number  VARCHAR(30)     NOT NULL UNIQUE,
    shipment_id         BIGINT          NOT NULL,
    hs_code             VARCHAR(20)     NOT NULL,
    goods_description   VARCHAR(500)    NOT NULL,
    declared_value      DECIMAL(15,2)   NOT NULL,
    tariff_rate         DECIMAL(5,2),
    duty_amount         DECIMAL(15,2),
    origin_country      VARCHAR(100)    NOT NULL,
    destination_country VARCHAR(100)    NOT NULL,
    status              VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    submission_deadline DATE            NOT NULL,
    submitted_date      DATE,
    approved_date       DATE,
    trade_agreement     VARCHAR(100),
    remarks             VARCHAR(500),
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_customs_shipment (shipment_id),
    INDEX idx_customs_status (status),
    INDEX idx_customs_deadline (submission_deadline),
    FOREIGN KEY (shipment_id) REFERENCES shipments(shipment_id)
) ENGINE=InnoDB;

CREATE TABLE carrier_routes (
    route_id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    route_code          VARCHAR(20)     NOT NULL UNIQUE,
    carrier             VARCHAR(100)    NOT NULL,
    origin              VARCHAR(100)    NOT NULL,
    destination         VARCHAR(100)    NOT NULL,
    transit_days        INT             NOT NULL,
    cost_per_kg         DECIMAL(10,2)   NOT NULL,
    max_weight_kg       DECIMAL(10,2),
    reliability_score   DECIMAL(3,2)    DEFAULT 0.90,
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_route_carrier (carrier),
    INDEX idx_route_origin_dest (origin, destination)
) ENGINE=InnoDB;

-- ============================================================================
-- Audit & Compliance Table
-- ============================================================================

CREATE TABLE audit_logs (
    log_id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             VARCHAR(100)    NOT NULL,
    user_role           VARCHAR(50),
    action              VARCHAR(50)     NOT NULL,
    entity_type         VARCHAR(50)     NOT NULL,
    entity_id           VARCHAR(50),
    method_name         VARCHAR(100)    NOT NULL,
    details             VARCHAR(1000),
    ip_address          VARCHAR(45),
    result_status       VARCHAR(20),
    execution_time_ms   BIGINT,
    timestamp           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_user (user_id),
    INDEX idx_audit_entity (entity_type, entity_id),
    INDEX idx_audit_timestamp (timestamp),
    INDEX idx_audit_action (action)
) ENGINE=InnoDB;

-- ============================================================================
-- Security Tables (JAAS JDBC Realm)
-- ============================================================================

CREATE TABLE users (
    user_id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    username            VARCHAR(100)    NOT NULL UNIQUE,
    password_hash       VARCHAR(256)    NOT NULL,
    password_salt       VARCHAR(100)    NOT NULL,
    email               VARCHAR(150),
    full_name           VARCHAR(200),
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    last_login          DATETIME,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE user_roles (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    username            VARCHAR(100)    NOT NULL,
    role_name           VARCHAR(50)     NOT NULL,
    UNIQUE KEY uk_user_role (username, role_name),
    FOREIGN KEY (username) REFERENCES users(username)
) ENGINE=InnoDB;

-- ============================================================================
-- Sample Data for Demonstration
-- ============================================================================

-- Warehouses
INSERT INTO warehouses (warehouse_code, name, location, country, region, capacity, utilization_percent, manager_name) VALUES
('WH-US-NYC', 'New York Distribution Center', 'Brooklyn, NY', 'United States', 'NORTH_AMERICA', 50000, 72.50, 'John Mitchell'),
('WH-UK-LON', 'London Logistics Hub', 'Dagenham, London', 'United Kingdom', 'EUROPE', 35000, 68.20, 'Sarah Williams'),
('WH-CN-SHA', 'Shanghai Mega Warehouse', 'Pudong, Shanghai', 'China', 'ASIA_PACIFIC', 80000, 85.00, 'Wei Chen'),
('WH-DE-FRA', 'Frankfurt Central Depot', 'Frankfurt am Main', 'Germany', 'EUROPE', 40000, 55.80, 'Klaus Weber'),
('WH-SG-SIN', 'Singapore Trade Hub', 'Jurong, Singapore', 'Singapore', 'ASIA_PACIFIC', 30000, 61.00, 'Priya Nair');

-- Vendors
INSERT INTO vendors (vendor_code, name, country, city, contact_email, category, performance_rating, compliance_status, contract_expiry, payment_terms) VALUES
('VND-001', 'Shenzhen Electronics Co.', 'China', 'Shenzhen', 'sales@shenzhenelec.cn', 'Electronics', 4.50, 'COMPLIANT', '2027-06-30', 'NET-60'),
('VND-002', 'Bavaria Auto Parts GmbH', 'Germany', 'Munich', 'orders@bavariaparts.de', 'Automotive Parts', 4.80, 'COMPLIANT', '2027-12-31', 'NET-30'),
('VND-003', 'Mumbai Textiles Ltd', 'India', 'Mumbai', 'export@mumbaitextiles.in', 'Textiles', 3.90, 'UNDER_REVIEW', '2026-09-15', 'NET-45'),
('VND-004', 'São Paulo Steel Corp', 'Brazil', 'São Paulo', 'vendas@spsteel.br', 'Raw Materials', 4.20, 'COMPLIANT', '2027-03-31', 'NET-30'),
('VND-005', 'Tokyo Precision Instruments', 'Japan', 'Tokyo', 'info@tokyoprecision.jp', 'Precision Equipment', 4.90, 'COMPLIANT', '2028-01-15', 'NET-30');

-- Shipments
INSERT INTO shipments (tracking_number, origin_country, origin_city, destination_country, destination_city, carrier, status, weight_kg, declared_value, currency, estimated_delivery) VALUES
('GT-2026-0001', 'China', 'Shanghai', 'United States', 'New York', 'Maersk Line', 'IN_TRANSIT', 15000.00, 450000.00, 'USD', '2026-09-01 14:00:00'),
('GT-2026-0002', 'Germany', 'Hamburg', 'United Kingdom', 'London', 'Hapag-Lloyd', 'AT_CUSTOMS', 8500.00, 220000.00, 'EUR', '2026-08-20 10:00:00'),
('GT-2026-0003', 'Japan', 'Tokyo', 'Singapore', 'Singapore', 'NYK Line', 'PICKED_UP', 3200.00, 180000.00, 'USD', '2026-08-25 16:00:00'),
('GT-2026-0004', 'India', 'Mumbai', 'Germany', 'Frankfurt', 'MSC', 'DELAYED', 12000.00, 95000.00, 'USD', '2026-08-18 08:00:00'),
('GT-2026-0005', 'Brazil', 'Santos', 'China', 'Shanghai', 'COSCO', 'CREATED', 25000.00, 310000.00, 'USD', '2026-09-15 12:00:00');

-- Inventory Items
INSERT INTO inventory_items (sku, product_name, warehouse_id, quantity, reorder_level, max_stock_level, unit_of_measure) VALUES
('ELEC-CPU-001', 'Processor Unit A7', 1, 1500, 500, 5000, 'UNITS'),
('ELEC-MEM-002', 'DDR5 Memory Module 16GB', 1, 320, 400, 3000, 'UNITS'),
('AUTO-BRK-001', 'Ceramic Brake Pad Set', 4, 2800, 1000, 8000, 'SETS'),
('TEXT-COT-001', 'Premium Cotton Fabric Roll', 2, 150, 200, 2000, 'ROLLS'),
('RAWM-STL-001', 'Grade A Steel Plate 10mm', 3, 4500, 2000, 10000, 'SHEETS'),
('PREC-SNS-001', 'Precision Optical Sensor', 5, 80, 100, 500, 'UNITS');

-- Purchase Orders
INSERT INTO purchase_orders (order_number, vendor_id, item_description, quantity, unit_price, total_amount, currency, status, order_date, expected_delivery, destination_warehouse_id) VALUES
('PO-2026-0001', 1, 'Processor Unit A7 — Batch Order', 2000, 45.00, 90000.00, 'USD', 'CONFIRMED', '2026-08-01', '2026-09-01', 1),
('PO-2026-0002', 3, 'Premium Cotton Fabric Roll — Q3 Stock', 500, 120.00, 60000.00, 'USD', 'SHIPPED', '2026-07-15', '2026-08-20', 2),
('PO-2026-0003', 2, 'Ceramic Brake Pad Set — Annual Contract', 5000, 35.50, 177500.00, 'EUR', 'PROCESSING', '2026-08-10', '2026-09-10', 4),
('PO-2026-0004', 5, 'Precision Optical Sensor — Urgent Order', 200, 250.00, 50000.00, 'USD', 'SUBMITTED', '2026-08-12', '2026-08-28', 5);

-- Customs Declarations
INSERT INTO customs_declarations (declaration_number, shipment_id, hs_code, goods_description, declared_value, tariff_rate, duty_amount, origin_country, destination_country, status, submission_deadline, trade_agreement) VALUES
('CD-2026-0001', 1, '8542.31', 'Electronic integrated circuits — processors', 450000.00, 2.50, 11250.00, 'China', 'United States', 'UNDER_REVIEW', '2026-08-25', 'USMCA'),
('CD-2026-0002', 2, '8708.30', 'Brakes and servo-brakes — automotive parts', 220000.00, 0.00, 0.00, 'Germany', 'United Kingdom', 'PENDING', '2026-08-18', 'UK-EU TCA'),
('CD-2026-0003', 4, '5208.12', 'Unbleached cotton fabrics — plain weave', 95000.00, 8.00, 7600.00, 'India', 'Germany', 'PENDING', '2026-08-16', 'EU-India FTA');

-- Carrier Routes
INSERT INTO carrier_routes (route_code, carrier, origin, destination, transit_days, cost_per_kg, max_weight_kg, reliability_score) VALUES
('RT-001', 'Maersk Line', 'Shanghai', 'New York', 28, 2.50, 50000.00, 0.94),
('RT-002', 'Hapag-Lloyd', 'Hamburg', 'London', 5, 1.80, 30000.00, 0.96),
('RT-003', 'NYK Line', 'Tokyo', 'Singapore', 8, 2.10, 25000.00, 0.92),
('RT-004', 'MSC', 'Mumbai', 'Frankfurt', 18, 2.30, 40000.00, 0.88),
('RT-005', 'COSCO', 'Santos', 'Shanghai', 32, 2.80, 60000.00, 0.90),
('RT-006', 'DHL Express', 'Shanghai', 'New York', 3, 12.50, 5000.00, 0.98),
('RT-007', 'Maersk Line', 'Hamburg', 'New York', 12, 2.20, 45000.00, 0.95);

-- Users (passwords hashed with PBKDF2 for demonstration)
-- Password for all users: "GlobalTrade2026!"
INSERT INTO users (username, password_hash, password_salt, email, full_name, is_active) VALUES
('admin', 'vAkXtCkfpkR/yh52CkYmao3ORqQZjP398CbiQKSfloM=', 'VdQSDBgrO0ji6n9TCTIpig==', 'admin@globaltrade.com', 'System Administrator', TRUE),
('logistics_coord', 'vAkXtCkfpkR/yh52CkYmao3ORqQZjP398CbiQKSfloM=', 'VdQSDBgrO0ji6n9TCTIpig==', 'logistics@globaltrade.com', 'Maria Rodriguez', TRUE),
('warehouse_mgr', 'vAkXtCkfpkR/yh52CkYmao3ORqQZjP398CbiQKSfloM=', 'VdQSDBgrO0ji6n9TCTIpig==', 'warehouse@globaltrade.com', 'James Chen', TRUE),
('customs_agent', 'vAkXtCkfpkR/yh52CkYmao3ORqQZjP398CbiQKSfloM=', 'VdQSDBgrO0ji6n9TCTIpig==', 'customs@globaltrade.com', 'Emma Thompson', TRUE),
('vendor_rep', 'vAkXtCkfpkR/yh52CkYmao3ORqQZjP398CbiQKSfloM=', 'VdQSDBgrO0ji6n9TCTIpig==', 'vendor@shenzhenelec.cn', 'Li Wei', TRUE);

-- User Roles
INSERT INTO user_roles (username, role_name) VALUES
('admin', 'SYSTEM_ADMIN'),
('logistics_coord', 'LOGISTICS_COORDINATOR'),
('warehouse_mgr', 'WAREHOUSE_MANAGER'),
('customs_agent', 'CUSTOMS_AGENT'),
('vendor_rep', 'VENDOR_REPRESENTATIVE');
