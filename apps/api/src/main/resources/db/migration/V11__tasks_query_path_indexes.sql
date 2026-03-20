SET @drop_tasks_done_idx = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.statistics
      WHERE table_schema = DATABASE()
        AND table_name = 'tasks'
        AND index_name = 'idx_tasks_tenant_done_updated'
    ),
    'DROP INDEX idx_tasks_tenant_done_updated ON tasks',
    'SELECT 1'
  )
);
PREPARE stmt_drop_tasks_done_idx FROM @drop_tasks_done_idx;
EXECUTE stmt_drop_tasks_done_idx;
DEALLOCATE PREPARE stmt_drop_tasks_done_idx;

SET @drop_tasks_owner_created_idx = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.statistics
      WHERE table_schema = DATABASE()
        AND table_name = 'tasks'
        AND index_name = 'idx_tasks_tenant_owner_created'
    ),
    'DROP INDEX idx_tasks_tenant_owner_created ON tasks',
    'SELECT 1'
  )
);
PREPARE stmt_drop_tasks_owner_created_idx FROM @drop_tasks_owner_created_idx;
EXECUTE stmt_drop_tasks_owner_created_idx;
DEALLOCATE PREPARE stmt_drop_tasks_owner_created_idx;

SET @create_tasks_done_updated_id_idx = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.statistics
      WHERE table_schema = DATABASE()
        AND table_name = 'tasks'
        AND index_name = 'idx_tasks_tenant_done_updated_id'
    ),
    'SELECT 1',
    'CREATE INDEX idx_tasks_tenant_done_updated_id ON tasks(tenant_id, done, updated_at, id)'
  )
);
PREPARE stmt_create_tasks_done_updated_id_idx FROM @create_tasks_done_updated_id_idx;
EXECUTE stmt_create_tasks_done_updated_id_idx;
DEALLOCATE PREPARE stmt_create_tasks_done_updated_id_idx;

SET @create_tasks_owner_done_updated_id_idx = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.statistics
      WHERE table_schema = DATABASE()
        AND table_name = 'tasks'
        AND index_name = 'idx_tasks_tenant_owner_done_updated_id'
    ),
    'SELECT 1',
    'CREATE INDEX idx_tasks_tenant_owner_done_updated_id ON tasks(tenant_id, owner, done, updated_at, id)'
  )
);
PREPARE stmt_create_tasks_owner_done_updated_id_idx FROM @create_tasks_owner_done_updated_id_idx;
EXECUTE stmt_create_tasks_owner_done_updated_id_idx;
DEALLOCATE PREPARE stmt_create_tasks_owner_done_updated_id_idx;
