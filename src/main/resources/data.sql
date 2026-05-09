-- ============================================================
-- BillingSystem Web - Seed Data
-- Compatible with both MySQL and H2 (dev mode)
-- ============================================================

-- Default users (BCrypt hashed passwords: admin123, staff123)
MERGE INTO users (id, username, password, role, full_name, is_active)
KEY (username)
VALUES
  (1, 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN', 'System Administrator', true),
  (2, 'staff', '$2a$10$GqrMxCpDf9gHAjh/iFxZAeXYHZl5zy5PvNZ4mHE.Ov1Yrg.iM5sG', 'STAFF', 'Staff User', true);

-- Walk-in customer
MERGE INTO customers (id, name, phone, email, address)
KEY (id)
VALUES (1, 'Walk-in Customer', '0000000000', '', '');

-- Sample products
MERGE INTO products (id, name, description, price, quantity, gst_percent, unit, is_active)
KEY (id)
VALUES
  (1,  'Laptop Dell Inspiron',  'Intel i5, 8GB RAM, 512GB SSD', 55000.00, 25, 18.00, 'PCS', true),
  (2,  'Wireless Mouse',        'Logitech M185, USB Nano',        650.00, 100, 18.00, 'PCS', true),
  (3,  'USB Keyboard',          'Mechanical, Backlit',           1200.00,  75, 18.00, 'PCS', true),
  (4,  'HDMI Cable 2m',         'Gold Plated, 4K Support',        350.00, 200, 18.00, 'PCS', true),
  (5,  'Printer Paper A4',      '75 GSM, 500 Sheets/Ream',        280.00, 500, 12.00, 'REAM', true),
  (6,  'Pen Blue (Box)',         'Ball Point, 10 pens',            45.00,  300,  5.00, 'BOX', true),
  (7,  'Stapler',               'Full Strip, 26/6',               120.00, 150, 12.00, 'PCS', true),
  (8,  'File Folder',           'Polypropylene, A4',               25.00,  400,  5.00, 'PCS', true);
