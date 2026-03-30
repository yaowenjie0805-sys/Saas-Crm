CREATE INDEX idx_notification_jobs_status_next_retry_created
  ON notification_jobs(status, next_retry_at, created_at, id);
