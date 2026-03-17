CREATE TABLE IF NOT EXISTS products (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  code VARCHAR(80) NOT NULL,
  name VARCHAR(180) NOT NULL,
  category VARCHAR(80),
  status VARCHAR(20) NOT NULL,
  standard_price BIGINT NOT NULL,
  tax_rate DECIMAL(10,4) NOT NULL,
  currency VARCHAR(16) NOT NULL,
  unit VARCHAR(32),
  sale_region VARCHAR(80),
  valid_from DATE,
  valid_to DATE,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);
CREATE UNIQUE INDEX uk_products_tenant_code ON products(tenant_id, code);
CREATE INDEX idx_products_tenant_status ON products(tenant_id, status, updated_at);

CREATE TABLE IF NOT EXISTS price_books (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  name VARCHAR(120) NOT NULL,
  status VARCHAR(20) NOT NULL,
  is_default BOOLEAN NOT NULL,
  department VARCHAR(80),
  currency VARCHAR(16) NOT NULL,
  valid_from DATE,
  valid_to DATE,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_price_books_tenant_status ON price_books(tenant_id, status, updated_at);

CREATE TABLE IF NOT EXISTS price_book_items (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  price_book_id VARCHAR(64) NOT NULL,
  product_id VARCHAR(64) NOT NULL,
  price BIGINT NOT NULL,
  tax_rate DECIMAL(10,4) NOT NULL,
  currency VARCHAR(16) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);
CREATE UNIQUE INDEX uk_price_book_items_unique ON price_book_items(tenant_id, price_book_id, product_id);

CREATE TABLE IF NOT EXISTS quotes (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  quote_no VARCHAR(80) NOT NULL,
  customer_id VARCHAR(64) NOT NULL,
  opportunity_id VARCHAR(64),
  price_book_id VARCHAR(64),
  owner VARCHAR(120) NOT NULL,
  status VARCHAR(32) NOT NULL,
  subtotal_amount BIGINT NOT NULL,
  tax_amount BIGINT NOT NULL,
  total_amount BIGINT NOT NULL,
  version INT NOT NULL,
  valid_until DATE,
  notes VARCHAR(500),
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);
CREATE UNIQUE INDEX uk_quotes_tenant_no ON quotes(tenant_id, quote_no);
CREATE INDEX idx_quotes_tenant_status ON quotes(tenant_id, status, updated_at);

CREATE TABLE IF NOT EXISTS quote_items (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  quote_id VARCHAR(64) NOT NULL,
  product_id VARCHAR(64) NOT NULL,
  product_name VARCHAR(180) NOT NULL,
  quantity INT NOT NULL,
  unit_price BIGINT NOT NULL,
  discount_rate DECIMAL(10,4) NOT NULL,
  tax_rate DECIMAL(10,4) NOT NULL,
  subtotal_amount BIGINT NOT NULL,
  tax_amount BIGINT NOT NULL,
  total_amount BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_quote_items_quote ON quote_items(tenant_id, quote_id, created_at);

CREATE TABLE IF NOT EXISTS order_records (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  order_no VARCHAR(80) NOT NULL,
  customer_id VARCHAR(64) NOT NULL,
  opportunity_id VARCHAR(64),
  quote_id VARCHAR(64),
  owner VARCHAR(120) NOT NULL,
  status VARCHAR(32) NOT NULL,
  amount BIGINT NOT NULL,
  sign_date DATE,
  notes VARCHAR(500),
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);
CREATE UNIQUE INDEX uk_orders_tenant_no ON order_records(tenant_id, order_no);
CREATE INDEX idx_orders_tenant_status ON order_records(tenant_id, status, updated_at);

