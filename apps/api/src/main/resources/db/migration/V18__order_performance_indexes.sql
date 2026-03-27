-- V18: 补充订单相关复合索引，优化高频查询性能
-- 优化场景：
-- 1. 订单列表按状态+负责人筛选
-- 2. 客户订单历史查询
-- 3. 订单导出任务的查询优化

-- 订单状态+负责人索引（支持销售范围查询）
CREATE INDEX idx_orders_tenant_status_owner
  ON order_records(tenant_id, status, owner);

-- 客户订单历史索引（支持客户订单时间线查询）
CREATE INDEX idx_orders_tenant_customer_created
  ON order_records(tenant_id, customer_id, created_at);

-- 订单状态+创建时间索引（支持按时间范围的订单导出）
CREATE INDEX idx_orders_tenant_status_created
  ON order_records(tenant_id, status, created_at);
