package com.yao.crm.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ReportExportJobService {

    private final ReportExportService reportExportService;
    private final ThreadPoolTaskExecutor executor;
    private final Map<String, JobRecord> jobs = new ConcurrentHashMap<String, JobRecord>();

    public ReportExportJobService(ReportExportService reportExportService,
                                @Qualifier("reportExportExecutor") ThreadPoolTaskExecutor executor) {
        this.reportExportService = reportExportService;
        this.executor = executor;
    }

    public Map<String, Object> submit(String requestedBy, String roleFilter, LocalDate fromDate, LocalDate toDate) {
        throw new IllegalStateException("tenant_id_required");
    }

    public Map<String, Object> submitByTenant(String requestedBy, String tenantId, String roleFilter, LocalDate fromDate, LocalDate toDate) {
        return submitByTenant(requestedBy, tenantId, roleFilter, fromDate, toDate, "", "", "Asia/Shanghai", "CNY", "en");
    }

    public Map<String, Object> submitByTenant(String requestedBy,
                                              String tenantId,
                                              String roleFilter,
                                              LocalDate fromDate,
                                              LocalDate toDate,
                                              String owner,
                                              String department,
                                              String timezone,
                                              String currency,
                                              String language) {
        String requiredTenantId = requireTenantId(tenantId);
        cleanupOldFinishedJobs();
        JobRecord record = createRecord(requestedBy, requiredTenantId, roleFilter, fromDate, toDate, owner, department, timezone, currency, language, null);
        jobs.put(record.jobId, record);
        start(record);
        return toStatus(record);
    }

    public Map<String, Object> retry(String jobId, String requester, boolean canViewAll) {
        throw new IllegalStateException("tenant_id_required");
    }

    public Map<String, Object> retryByTenant(String jobId, String requester, String tenantId, boolean canViewAll) {
        String requiredTenantId = requireTenantId(tenantId);
        cleanupOldFinishedJobs();
        JobRecord source = jobs.get(jobId);
        if (source == null) {
            throw new IllegalArgumentException("export_job_not_found");
        }
        if (!requiredTenantId.equals(source.tenantId)) {
            throw new IllegalArgumentException("forbidden");
        }
        if (!canViewAll && !Objects.equals(source.requestedBy, requester)) {
            throw new IllegalArgumentException("forbidden");
        }

        JobRecord retried = createRecord(source.requestedBy, source.tenantId, source.role, source.from, source.to, source.owner, source.department, source.timezone, source.currency, source.language, source.jobId);
        jobs.put(retried.jobId, retried);
        start(retried);
        return toStatus(retried);
    }

    public List<Map<String, Object>> list(String requester, boolean canViewAll, int limit, String status) {
        throw new IllegalStateException("tenant_id_required");
    }

    public List<Map<String, Object>> listByTenant(String requester, String tenantId, boolean canViewAll, int limit, String status) {
        Map<String, Object> paged = listByTenantPaged(requester, tenantId, canViewAll, 1, Math.max(1, limit), status);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) paged.get("items");
        return items;
    }

    public Map<String, Object> listByTenantPaged(String requester, String tenantId, boolean canViewAll, int page, int size, String status) {
        String requiredTenantId = requireTenantId(tenantId);
        cleanupOldFinishedJobs();
        List<JobRecord> rows = new ArrayList<JobRecord>(jobs.values());
        Collections.sort(rows, new Comparator<JobRecord>() {
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
            if (!requiredTenantId.equals(row.tenantId)) {
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

    public Map<String, Object> status(String jobId, String requester, boolean canViewAll) {
        throw new IllegalStateException("tenant_id_required");
    }

    public Map<String, Object> statusByTenant(String jobId, String requester, String tenantId, boolean canViewAll) {
        String requiredTenantId = requireTenantId(tenantId);
        cleanupOldFinishedJobs();
        JobRecord record = jobs.get(jobId);
        if (record == null) {
            throw new IllegalArgumentException("export_job_not_found");
        }
        if (!requiredTenantId.equals(record.tenantId)) {
            throw new IllegalArgumentException("forbidden");
        }
        if (!canViewAll && !Objects.equals(record.requestedBy, requester)) {
            throw new IllegalArgumentException("forbidden");
        }
        return toStatus(record);
    }

    public String download(String jobId, String requester, boolean canViewAll) {
        throw new IllegalStateException("tenant_id_required");
    }

    public String downloadByTenant(String jobId, String requester, String tenantId, boolean canViewAll) {
        String requiredTenantId = requireTenantId(tenantId);
        cleanupOldFinishedJobs();
        JobRecord record = jobs.get(jobId);
        if (record == null) {
            throw new IllegalArgumentException("export_job_not_found");
        }
        if (!requiredTenantId.equals(record.tenantId)) {
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
                                   String roleFilter,
                                   LocalDate fromDate,
                                   LocalDate toDate,
                                   String owner,
                                   String department,
                                   String timezone,
                                   String currency,
                                   String language,
                                   String sourceJobId) {
        JobRecord record = new JobRecord();
        record.jobId = "rpt_" + Long.toString(System.currentTimeMillis(), 36) + String.format("%03d", (int) (Math.random() * 1000));
        record.sourceJobId = sourceJobId;
        record.requestedBy = requestedBy;
        record.tenantId = requireTenantId(tenantId);
        record.status = "PENDING";
        record.progress = 5;
        record.role = roleFilter == null ? "" : roleFilter.trim().toUpperCase(Locale.ROOT);
        record.from = fromDate;
        record.to = toDate;
        record.owner = owner == null ? "" : owner.trim();
        record.department = department == null ? "" : department.trim();
        record.timezone = timezone == null || timezone.trim().isEmpty() ? "Asia/Shanghai" : timezone.trim();
        record.currency = currency == null || currency.trim().isEmpty() ? "CNY" : currency.trim().toUpperCase(Locale.ROOT);
        record.language = language == null || language.trim().isEmpty() ? "en" : language.trim().toLowerCase(Locale.ROOT);
        record.createdAt = LocalDateTime.now();
        return record;
    }

    private void buildCsv(JobRecord record) {
        try {
            record.status = "RUNNING";
            record.progress = 20;
            String csv = reportExportService.exportOverviewCsvByTenant(record.tenantId, record.from, record.to, record.role, record.owner, record.department, record.language);
            record.progress = 95;
            record.csv = csv;
            record.rowCount = countRows(csv);
            record.status = "DONE";
            record.progress = 100;
            record.finishedAt = LocalDateTime.now();
        } catch (Exception ex) {
            record.status = "FAILED";
            record.progress = 100;
            record.error = ex.getMessage();
            record.finishedAt = LocalDateTime.now();
        }
    }

    private int countRows(String csv) {
        if (csv == null || csv.isEmpty()) return 0;
        String[] lines = csv.split("\\r?\\n");
        return Math.max(0, lines.length - 1);
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
        Map<String, String> filters = new HashMap<String, String>();
        filters.put("from", record.from == null ? "" : record.from.toString());
        filters.put("to", record.to == null ? "" : record.to.toString());
        filters.put("role", record.role == null ? "" : record.role);
        filters.put("owner", record.owner == null ? "" : record.owner);
        filters.put("department", record.department == null ? "" : record.department);
        filters.put("timezone", record.timezone == null ? "" : record.timezone);
        filters.put("currency", record.currency == null ? "" : record.currency);
        filters.put("language", record.language == null ? "en" : record.language);
        body.put("filters", filters);
        return body;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.trim().isEmpty()) return null;
        String s = status.trim().toUpperCase(Locale.ROOT);
        if ("ALL".equals(s)) return null;
        if ("PENDING".equals(s) || "RUNNING".equals(s) || "DONE".equals(s) || "FAILED".equals(s)) return s;
        return null;
    }

    private String requireTenantId(String tenantId) {
        String normalized = tenantId == null ? "" : tenantId.trim();
        if (normalized.isEmpty()) {
            throw new IllegalStateException("tenant_id_required");
        }
        return normalized;
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

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }

    private static class JobRecord {
        private String jobId;
        private String sourceJobId;
        private String requestedBy;
        private String tenantId;
        private String role;
        private String owner;
        private String department;
        private String timezone;
        private String currency;
        private String language;
        private LocalDate from;
        private LocalDate to;
        private String status;
        private int progress;
        private int rowCount;
        private String csv;
        private String error;
        private LocalDateTime createdAt;
        private LocalDateTime finishedAt;
    }
}
