package com.yao.crm.service;

import com.yao.crm.entity.AuditLog;
import com.yao.crm.repository.AuditLogRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import javax.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

@Service
public class AuditExportJobService {

    private final AuditLogRepository auditLogRepository;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Map<String, JobRecord> jobs = new ConcurrentHashMap<String, JobRecord>();

    public AuditExportJobService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public Map<String, Object> submit(String requestedBy,
                                      String role,
                                      String username,
                                      String action,
                                      LocalDateTime fromTime,
                                      LocalDateTime toTime) {
        cleanupOldFinishedJobs();
        JobRecord record = createRecord(requestedBy, role, username, action, fromTime, toTime, null);
        jobs.put(record.jobId, record);
        start(record);
        return toStatus(record);
    }

    public List<Map<String, Object>> list(String requester, boolean canViewAll, int limit, String status) {
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
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (JobRecord row : rows) {
            if (!canViewAll && !Objects.equals(row.requestedBy, requester)) {
                continue;
            }
            if (statusFilter != null && !statusFilter.equals(row.status)) {
                continue;
            }
            out.add(toStatus(row));
            if (out.size() >= Math.max(1, limit)) {
                break;
            }
        }
        return out;
    }

    public Map<String, Object> retry(String jobId, String requester, boolean canViewAll) {
        cleanupOldFinishedJobs();
        JobRecord source = jobs.get(jobId);
        if (source == null) {
            throw new IllegalArgumentException("export_job_not_found");
        }
        if (!canViewAll && !Objects.equals(source.requestedBy, requester)) {
            throw new IllegalArgumentException("forbidden");
        }

        JobRecord retried = createRecord(
                source.requestedBy,
                source.filterRole,
                source.filterUsername,
                source.filterAction,
                source.from,
                source.to,
                source.jobId
        );
        jobs.put(retried.jobId, retried);
        start(retried);
        return toStatus(retried);
    }

    public Map<String, Object> status(String jobId, String requester, boolean canViewAll) {
        cleanupOldFinishedJobs();
        JobRecord record = jobs.get(jobId);
        if (record == null) {
            throw new IllegalArgumentException("export_job_not_found");
        }
        if (!canViewAll && !Objects.equals(record.requestedBy, requester)) {
            throw new IllegalArgumentException("forbidden");
        }
        return toStatus(record);
    }

    public String download(String jobId, String requester, boolean canViewAll) {
        cleanupOldFinishedJobs();
        JobRecord record = jobs.get(jobId);
        if (record == null) {
            throw new IllegalArgumentException("export_job_not_found");
        }
        if (!canViewAll && !Objects.equals(record.requestedBy, requester)) {
            throw new IllegalArgumentException("forbidden");
        }
        if (!"DONE".equals(record.status) || record.csv == null) {
            throw new IllegalStateException("export_job_not_ready");
        }
        return record.csv;
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
                                   String role,
                                   String username,
                                   String action,
                                   LocalDateTime fromTime,
                                   LocalDateTime toTime,
                                   String sourceJobId) {
        JobRecord record = new JobRecord();
        record.jobId = "exp_" + Long.toString(System.currentTimeMillis(), 36) + String.format("%03d", (int) (Math.random() * 1000));
        record.requestedBy = requestedBy;
        record.status = "PENDING";
        record.progress = 5;
        record.createdAt = LocalDateTime.now();
        record.filterRole = role == null ? "" : role;
        record.filterUsername = username == null ? "" : username;
        record.filterAction = action == null ? "" : action;
        record.from = fromTime;
        record.to = toTime;
        record.sourceJobId = sourceJobId;
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
                if (record.from != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), record.from));
                }
                if (record.to != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), record.to));
                }
                return cb.and(predicates.toArray(new Predicate[0]));
            };

            List<AuditLog> logs = auditLogRepository.findAll(spec);
            record.progress = 75;

            StringBuilder csv = new StringBuilder();
            csv.append("id,username,role,action,resource,resourceId,details,createdAt\n");
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
        } catch (Exception ex) {
            record.status = "FAILED";
            record.progress = 100;
            record.error = ex.getMessage();
            record.finishedAt = LocalDateTime.now();
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
        private String filterUsername;
        private String filterRole;
        private String filterAction;
        private LocalDateTime from;
        private LocalDateTime to;
        private int rowCount;
        private String csv;
        private String error;
        private LocalDateTime createdAt;
        private LocalDateTime finishedAt;
    }
}
