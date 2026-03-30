package com.yao.crm.service;

import com.yao.crm.entity.AuditLog;
import com.yao.crm.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import javax.persistence.criteria.Predicate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AuditExportJobService {

    private final AuditLogRepository auditLogRepository;
    private final ThreadPoolTaskExecutor executor;
    private final Map<String, JobRecord> jobs = new ConcurrentHashMap<String, JobRecord>();
    private final AtomicLong totalSubmitted = new AtomicLong(0L);
    private final AtomicLong totalRetried = new AtomicLong(0L);
    private final AtomicLong totalDone = new AtomicLong(0L);
    private final AtomicLong totalFailed = new AtomicLong(0L);
    private final ConcurrentLinkedQueue<Long> durationHistoryMs = new ConcurrentLinkedQueue<Long>();

    public AuditExportJobService(AuditLogRepository auditLogRepository,
                               @Qualifier("auditExportExecutor") ThreadPoolTaskExecutor executor) {
        this.auditLogRepository = auditLogRepository;
        this.executor = executor;
    }

    public Map<String, Object> submit(String requestedBy,
                                      String tenantId,
                                      String role,
                                      String username,
                                      String action,
                                      LocalDateTime fromTime,
                                      LocalDateTime toTime,
                                      String language) {
        cleanupOldFinishedJobs();
        JobRecord record = createRecord(requestedBy, tenantId, role, username, action, fromTime, toTime, null, language);
        jobs.put(record.jobId, record);
        totalSubmitted.incrementAndGet();
        start(record);
        return toStatus(record);
    }

    public List<Map<String, Object>> list(String requester, String tenantId, boolean canViewAll, int limit, String status) {
        Map<String, Object> paged = listPaged(requester, tenantId, canViewAll, 1, Math.max(1, limit), status);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) paged.get("items");
        return items;
    }

    public Map<String, Object> listPaged(String requester, String tenantId, boolean canViewAll, int page, int size, String status) {
        cleanupOldFinishedJobs();
        List<JobRecord> rows = new ArrayList<JobRecord>(jobs.values());
        rows.sort(new Comparator<JobRecord>() {
            @Override
            public int compare(JobRecord a, JobRecord b) {
                LocalDateTime ta = a.createdAt == null ? LocalDateTime.MIN : a.createdAt;
                LocalDateTime tb = b.createdAt == null ? LocalDateTime.MIN : b.createdAt;
                return tb.compareTo(ta);
            }
        });

        String statusFilter = normalizeStatus(status);
        List<Map<String, Object>> filtered = new ArrayList<Map<String, Object>>();
        for (JobRecord row : rows) {
            if (!Objects.equals(row.tenantId, tenantId)) {
                continue;
            }
            if (!canViewAll && !Objects.equals(row.requestedBy, requester)) {
                continue;
            }
            if (statusFilter != null && !statusFilter.equals(row.status)) {
                continue;
            }
            filtered.add(toStatus(row));
        }

        int safeSize = Math.max(1, size);
        int safePage = Math.max(1, page);
        int total = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / safeSize));
        if (safePage > totalPages) {
            safePage = totalPages;
        }
        int fromIndex = Math.min((safePage - 1) * safeSize, total);
        int toIndex = Math.min(fromIndex + safeSize, total);
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>(filtered.subList(fromIndex, toIndex));

        Map<String, Object> body = new HashMap<String, Object>();
        body.put("items", out);
        body.put("page", safePage);
        body.put("size", safeSize);
        body.put("totalPages", totalPages);
        body.put("total", total);
        return body;
    }

    public Map<String, Object> retry(String jobId, String requester, String tenantId, boolean canViewAll) {
        cleanupOldFinishedJobs();
        JobRecord source = jobs.get(jobId);
        if (source == null) {
            throw new IllegalArgumentException("export_job_not_found");
        }
        if (!Objects.equals(source.tenantId, tenantId)) {
            throw new IllegalArgumentException("forbidden");
        }
        if (!canViewAll && !Objects.equals(source.requestedBy, requester)) {
            throw new IllegalArgumentException("forbidden");
        }

        JobRecord retried = createRecord(
                source.requestedBy,
                source.tenantId,
                source.filterRole,
                source.filterUsername,
                source.filterAction,
                source.from,
                source.to,
                source.jobId,
                source.language
        );
        jobs.put(retried.jobId, retried);
        totalSubmitted.incrementAndGet();
        totalRetried.incrementAndGet();
        start(retried);
        return toStatus(retried);
    }

    public Map<String, Object> status(String jobId, String requester, String tenantId, boolean canViewAll) {
        cleanupOldFinishedJobs();
        JobRecord record = jobs.get(jobId);
        if (record == null) {
            throw new IllegalArgumentException("export_job_not_found");
        }
        if (!Objects.equals(record.tenantId, tenantId)) {
            throw new IllegalArgumentException("forbidden");
        }
        if (!canViewAll && !Objects.equals(record.requestedBy, requester)) {
            throw new IllegalArgumentException("forbidden");
        }
        return toStatus(record);
    }

    public String download(String jobId, String requester, String tenantId, boolean canViewAll) {
        cleanupOldFinishedJobs();
        JobRecord record = jobs.get(jobId);
        if (record == null) {
            throw new IllegalArgumentException("export_job_not_found");
        }
        if (!Objects.equals(record.tenantId, tenantId)) {
            throw new IllegalArgumentException("forbidden");
        }
        if (!canViewAll && !Objects.equals(record.requestedBy, requester)) {
            throw new IllegalArgumentException("forbidden");
        }
        if (!"DONE".equals(record.status) || record.csv == null) {
            throw new IllegalStateException("export_job_not_ready");
        }
        return record.csv;
    }

    public Map<String, Object> metricsSnapshot(String tenantId) {
        cleanupOldFinishedJobs();
        int pending = 0;
        int running = 0;
        int done = 0;
        int failed = 0;
        for (JobRecord row : jobs.values()) {
            if (!Objects.equals(row.tenantId, tenantId)) continue;
            if ("PENDING".equals(row.status)) pending++;
            else if ("RUNNING".equals(row.status)) running++;
            else if ("DONE".equals(row.status)) done++;
            else if ("FAILED".equals(row.status)) failed++;
        }
        List<Long> durations = new ArrayList<Long>(durationHistoryMs);
        Collections.sort(durations);

        Map<String, Object> out = new HashMap<String, Object>();
        out.put("tenantId", tenantId);
        out.put("queuePending", pending);
        out.put("queueRunning", running);
        out.put("finishedDone", done);
        out.put("finishedFailed", failed);
        out.put("totalSubmitted", totalSubmitted.get());
        out.put("totalRetried", totalRetried.get());
        out.put("totalDone", totalDone.get());
        out.put("totalFailed", totalFailed.get());
        out.put("latencyP50Ms", percentile(durations, 50));
        out.put("latencyP95Ms", percentile(durations, 95));
        out.put("latencySamples", durations.size());
        return out;
    }

    private void start(final JobRecord record) {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                buildCsv(record);
            }
        });
    }

    private JobRecord createRecord(String requestedBy,
                                   String tenantId,
                                   String role,
                                   String username,
                                   String action,
                                   LocalDateTime fromTime,
                                   LocalDateTime toTime,
                                   String sourceJobId,
                                   String language) {
        JobRecord record = new JobRecord();
        record.jobId = "exp_" + Long.toString(System.currentTimeMillis(), 36) + String.format("%03d", (int) (Math.random() * 1000));
        record.requestedBy = requestedBy;
        record.status = "PENDING";
        record.progress = 5;
        record.createdAt = LocalDateTime.now();
        record.tenantId = tenantId == null || tenantId.trim().isEmpty() ? "tenant_default" : tenantId.trim();
        record.filterRole = role == null ? "" : role;
        record.filterUsername = username == null ? "" : username;
        record.filterAction = action == null ? "" : action;
        record.from = fromTime;
        record.to = toTime;
        record.sourceJobId = sourceJobId;
        record.language = language == null ? "en" : language;
        return record;
    }

    private void buildCsv(JobRecord record) {
        try {
            record.status = "RUNNING";
            record.progress = 15;

            Specification<AuditLog> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<Predicate>();
                if (!isBlank(record.filterUsername)) {
                    predicates.add(cb.equal(root.get("username"), record.filterUsername));
                }
                if (!isBlank(record.filterRole)) {
                    predicates.add(cb.equal(root.get("role"), record.filterRole));
                }
                if (!isBlank(record.filterAction)) {
                    predicates.add(cb.equal(root.get("action"), record.filterAction));
                }
                predicates.add(cb.equal(root.get("tenantId"), record.tenantId));
                if (record.from != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), record.from));
                }
                if (record.to != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), record.to));
                }
                return cb.and(predicates.toArray(new Predicate[predicates.size()]));

            };

            List<AuditLog> logs = auditLogRepository.findAll(spec);
            record.progress = 75;

            boolean zh = String.valueOf(record.language).toLowerCase(Locale.ROOT).startsWith("zh");
            StringBuilder csv = new StringBuilder();
            csv.append('\uFEFF');
            if (zh) {
                csv.append("id,用户,角色,动作,资源,资源ID,详情,创建时间\n");
            } else {
                csv.append("id,username,role,action,resource,resourceId,details,createdAt\n");
            }
            for (AuditLog log : logs) {
                csv.append(escapeCsv(log.getId())).append(',')
                        .append(escapeCsv(log.getUsername())).append(',')
                        .append(escapeCsv(log.getRole())).append(',')
                        .append(escapeCsv(log.getAction())).append(',')
                        .append(escapeCsv(log.getResource())).append(',')
                        .append(escapeCsv(log.getResourceId())).append(',')
                        .append(escapeCsv(log.getDetails())).append(',')
                        .append(escapeCsv(log.getCreatedAt() == null ? "" : log.getCreatedAt().toString()))
                        .append('\n');
            }

            record.csv = csv.toString();
            record.rowCount = logs.size();
            record.progress = 100;
            record.status = "DONE";
            record.finishedAt = LocalDateTime.now();
            totalDone.incrementAndGet();
            addDuration(record);
        } catch (Exception ex) {
            record.status = "FAILED";
            record.progress = 100;
            record.error = ex.getMessage();
            record.finishedAt = LocalDateTime.now();
            totalFailed.incrementAndGet();
            addDuration(record);
        }
    }

    private Map<String, Object> toStatus(JobRecord record) {
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("jobId", record.jobId);
        body.put("sourceJobId", record.sourceJobId);
        body.put("status", record.status);
        body.put("progress", record.progress);
        body.put("rowCount", record.rowCount);
        body.put("error", record.error);
        body.put("createdAt", record.createdAt == null ? null : record.createdAt.toString());
        body.put("finishedAt", record.finishedAt == null ? null : record.finishedAt.toString());
        body.put("downloadReady", "DONE".equals(record.status));
        body.put("tenantId", record.tenantId);
        body.put("filters", toFilters(record));
        return body;
    }

    private Map<String, String> toFilters(JobRecord record) {
        Map<String, String> filters = new HashMap<String, String>();
        filters.put("username", record.filterUsername);
        filters.put("role", record.filterRole);
        filters.put("action", record.filterAction);
        filters.put("from", record.from == null ? "" : record.from.toLocalDate().toString());
        filters.put("to", record.to == null ? "" : record.to.toLocalDate().toString());
        filters.put("tenantId", record.tenantId == null ? "" : record.tenantId);
        return filters;
    }

    private String normalizeStatus(String status) {
        if (isBlank(status)) return null;
        String s = status.trim().toUpperCase(Locale.ROOT);
        if ("ALL".equals(s)) return null;
        if ("PENDING".equals(s) || "RUNNING".equals(s) || "DONE".equals(s) || "FAILED".equals(s)) {
            return s;
        }
        return null;
    }
    private void cleanupOldFinishedJobs() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        for (Map.Entry<String, JobRecord> entry : jobs.entrySet()) {
            JobRecord row = entry.getValue();
            if (("DONE".equals(row.status) || "FAILED".equals(row.status))
                    && row.finishedAt != null
                    && row.finishedAt.isBefore(threshold)) {
                jobs.remove(entry.getKey());
            }
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private long percentile(List<Long> values, int p) {
        if (values == null || values.isEmpty()) return 0L;
        int index = (int) Math.ceil((p / 100.0d) * values.size()) - 1;
        int safe = Math.max(0, Math.min(values.size() - 1, index));
        return values.get(safe);
    }

    private void addDuration(JobRecord record) {
        if (record == null || record.createdAt == null || record.finishedAt == null) return;
        long ms = Duration.between(record.createdAt, record.finishedAt).toMillis();
        durationHistoryMs.add(Math.max(0L, ms));
        while (durationHistoryMs.size() > 200) {
            durationHistoryMs.poll();
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }

    private static class JobRecord {
        private String jobId;
        private String sourceJobId;
        private String status;
        private int progress;
        private String requestedBy;
        private String tenantId;
        private String filterUsername;
        private String filterRole;
        private String filterAction;
        private LocalDateTime from;
        private LocalDateTime to;
        private String language;
        private int rowCount;
        private String csv;
        private String error;
        private LocalDateTime createdAt;
        private LocalDateTime finishedAt;
    }
}
