package com.yao.crm.service;

import com.yao.crm.entity.LeadImportJob;
import com.yao.crm.entity.LeadImportJobItem;
import com.yao.crm.repository.LeadImportJobItemRepository;
import com.yao.crm.repository.LeadImportJobRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LeadImportFailedRowsExportJobService {

    private final LeadImportJobRepository leadImportJobRepository;
    private final LeadImportJobItemRepository leadImportJobItemRepository;
    private final AuditLogService auditLogService;
    private final ThreadPoolTaskExecutor executor;
    private final Map<String, JobRecord> jobs = new ConcurrentHashMap<String, JobRecord>();
    private final int maxExportRows;

    public LeadImportFailedRowsExportJobService(LeadImportJobRepository leadImportJobRepository,
                                                LeadImportJobItemRepository leadImportJobItemRepository,
                                                AuditLogService auditLogService,
                                                @Value("${lead.import.failed-rows-export.max-rows:50000}") int maxExportRows,
                                                @Qualifier("taskExecutor") ThreadPoolTaskExecutor executor) {
        this.leadImportJobRepository = leadImportJobRepository;
        this.leadImportJobItemRepository = leadImportJobItemRepository;
        this.auditLogService = auditLogService;
        this.maxExportRows = Math.max(1000, maxExportRows);
        this.executor = executor;
    }

    public Map<String, Object> submitByTenant(String tenantId,
                                              String importJobId,
                                              String requester,
                                              String role,
                                              boolean canViewAll,
                                              String requestId,
                                              String language,
                                              String errorCode,
                                              LocalDate fromDate,
                                              LocalDate toDate) {
        cleanupOldFinishedJobs();
        LeadImportJob importJob = leadImportJobRepository.findByIdAndTenantId(importJobId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("lead_import_job_not_found"));
        if (!canViewAll && !Objects.equals(importJob.getCreatedBy(), requester)) {
            throw new IllegalArgumentException("lead_import_export_forbidden");
        }
        if ((errorCode == null || errorCode.trim().isEmpty()) && fromDate == null && toDate == null) {
            long totalFailedRows = leadImportJobItemRepository.countByTenantIdAndJobIdAndStatus(tenantId, importJobId, "FAILED");
            if (totalFailedRows > maxExportRows) {
                throw new IllegalArgumentException("lead_import_export_limit_exceeded");
            }
        }

        JobRecord record = createRecord(tenantId, importJobId, requester, role, language, errorCode, fromDate, toDate, null);
        jobs.put(record.jobId, record);
        auditLogService.record(requester, role, "IMPORT_FAILED_ROWS_EXPORT_CREATED", "LEAD_IMPORT_EXPORT_JOB", record.jobId,
                "requestId=" + requestId + ";importJobId=" + importJobId, tenantId);
        start(record, requestId);
        return toStatus(record);
    }

    public Map<String, Object> listByTenant(String tenantId,
                                            String importJobId,
                                            String requester,
                                            boolean canViewAll,
                                            int page,
                                            int size,
                                            String status) {
        cleanupOldFinishedJobs();
        int finalPage = Math.max(1, page);
        int finalSize = Math.max(1, Math.min(size, 50));
        String statusFilter = normalizeStatus(status);

        List<JobRecord> rows = new ArrayList<JobRecord>(jobs.values());
        Collections.sort(rows, new Comparator<JobRecord>() {
            @Override
            public int compare(JobRecord a, JobRecord b) {
                LocalDateTime ta = a.createdAt == null ? LocalDateTime.MIN : a.createdAt;
                LocalDateTime tb = b.createdAt == null ? LocalDateTime.MIN : b.createdAt;
                return tb.compareTo(ta);
            }
        });

        List<Map<String, Object>> filtered = new ArrayList<Map<String, Object>>();
        for (JobRecord row : rows) {
            if (!tenantId.equals(row.tenantId)) continue;
            if (!importJobId.equals(row.importJobId)) continue;
            if (!canViewAll && !Objects.equals(row.requestedBy, requester)) continue;
            if (statusFilter != null && !statusFilter.equalsIgnoreCase(row.status)) continue;
            filtered.add(toStatus(row));
        }

        int total = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) finalSize));
        int fromIdx = Math.min((finalPage - 1) * finalSize, total);
        int toIdx = Math.min(fromIdx + finalSize, total);

        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("items", filtered.subList(fromIdx, toIdx));
        out.put("page", finalPage);
        out.put("size", finalSize);
        out.put("totalPages", totalPages);
        out.put("total", total);
        return out;
    }

    public String downloadByTenant(String tenantId,
                                   String importJobId,
                                   String exportJobId,
                                   String requester,
                                   String role,
                                   boolean canViewAll,
                                   String requestId) {
        cleanupOldFinishedJobs();
        JobRecord record = jobs.get(exportJobId);
        if (record == null || !tenantId.equals(record.tenantId) || !importJobId.equals(record.importJobId)) {
            throw new IllegalArgumentException("lead_import_export_job_not_found");
        }
        if (!canViewAll && !Objects.equals(record.requestedBy, requester)) {
            throw new IllegalArgumentException("lead_import_export_forbidden");
        }
        if (!"DONE".equals(record.status) || record.csv == null) {
            throw new IllegalStateException("lead_import_export_job_not_ready");
        }
        auditLogService.record(requester, role, "IMPORT_FAILED_ROWS_EXPORT_DOWNLOADED", "LEAD_IMPORT_EXPORT_JOB", record.jobId,
                "requestId=" + requestId + ";importJobId=" + importJobId, tenantId);
        return "\uFEFF" + record.csv;
    }

    private void start(final JobRecord record, final String requestId) {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                buildCsv(record, requestId);
            }
        });
    }

    private void buildCsv(JobRecord record, String requestId) {
        try {
            record.status = "RUNNING";
            record.progress = 15;

            List<LeadImportJobItem> items = readFailedItems(record.tenantId, record.importJobId, record.errorCode, record.fromDate, record.toDate, maxExportRows + 1);
            if (items.size() > maxExportRows) {
                record.status = "FAILED";
                record.progress = 100;
                record.error = "lead_import_export_limit_exceeded";
                record.finishedAt = LocalDateTime.now();
                auditLogService.record(record.requestedBy, record.role, "IMPORT_FAILED_ROWS_EXPORT_FAILED", "LEAD_IMPORT_EXPORT_JOB", record.jobId,
                        "requestId=" + requestId + ";reason=lead_import_export_limit_exceeded", record.tenantId);
                return;
            }

            boolean zh = record.language != null && record.language.startsWith("zh");
            StringBuilder csv = new StringBuilder();
            csv.append(zh ? "任务ID,行号,错误码,错误信息,原始行,创建时间\n" : "jobId,lineNo,errorCode,errorMessage,rawLine,createdAt\n");
            for (LeadImportJobItem item : items) {
                csv.append(escapeCsv(record.importJobId)).append(',')
                        .append(escapeCsv(item.getLineNo() == null ? "" : String.valueOf(item.getLineNo()))).append(',')
                        .append(escapeCsv(item.getErrorCode())).append(',')
                        .append(escapeCsv(item.getErrorMessage())).append(',')
                        .append(escapeCsv(item.getRawLine())).append(',')
                        .append(escapeCsv(item.getCreatedAt() == null ? "" : item.getCreatedAt().toString()))
                        .append('\n');
            }
            record.csv = csv.toString();
            record.rowCount = items.size();
            record.status = "DONE";
            record.progress = 100;
            record.finishedAt = LocalDateTime.now();
            auditLogService.record(record.requestedBy, record.role, "IMPORT_FAILED_ROWS_EXPORT_FINISHED", "LEAD_IMPORT_EXPORT_JOB", record.jobId,
                    "requestId=" + requestId + ";rows=" + items.size(), record.tenantId);
        } catch (Exception ex) {
            record.status = "FAILED";
            record.progress = 100;
            record.error = ex.getMessage() == null ? "lead_import_export_unknown_error" : ex.getMessage();
            record.finishedAt = LocalDateTime.now();
            auditLogService.record(record.requestedBy, record.role, "IMPORT_FAILED_ROWS_EXPORT_FAILED", "LEAD_IMPORT_EXPORT_JOB", record.jobId,
                    "requestId=" + requestId + ";reason=" + record.error, record.tenantId);
        }
    }

    private List<LeadImportJobItem> readFailedItems(String tenantId,
                                                    String jobId,
                                                    String errorCode,
                                                    LocalDate fromDate,
                                                    LocalDate toDate,
                                                    int maxRows) {
        List<LeadImportJobItem> out = new ArrayList<LeadImportJobItem>();
        int page = 0;
        int size = 1000;
        while (true) {
            Page<LeadImportJobItem> rows = leadImportJobItemRepository
                    .findByTenantIdAndJobIdAndStatusOrderByLineNoAsc(tenantId, jobId, "FAILED", PageRequest.of(page, size));
            for (LeadImportJobItem item : rows.getContent()) {
                if (!isMatchErrorCode(errorCode, item.getErrorCode())) continue;
                if (!isMatchDateRange(fromDate, toDate, item.getCreatedAt())) continue;
                out.add(item);
                if (out.size() >= maxRows) return out;
            }
            if (!rows.hasNext()) break;
            page++;
        }
        return out;
    }

    private boolean isMatchErrorCode(String expected, String actual) {
        if (expected == null || expected.trim().isEmpty()) return true;
        return expected.trim().equalsIgnoreCase(String.valueOf(actual));
    }

    private boolean isMatchDateRange(LocalDate fromDate, LocalDate toDate, LocalDateTime createdAt) {
        if (createdAt == null) return fromDate == null && toDate == null;
        if (fromDate != null && createdAt.toLocalDate().isBefore(fromDate)) return false;
        return toDate == null || !createdAt.toLocalDate().isAfter(toDate);
    }

    private JobRecord createRecord(String tenantId,
                                   String importJobId,
                                   String requestedBy,
                                   String role,
                                   String language,
                                   String errorCode,
                                   LocalDate fromDate,
                                   LocalDate toDate,
                                   String sourceJobId) {
        JobRecord record = new JobRecord();
        record.jobId = "imexp_" + Long.toString(System.currentTimeMillis(), 36) + String.format("%03d", (int) (Math.random() * 1000));
        record.sourceJobId = sourceJobId;
        record.tenantId = tenantId;
        record.importJobId = importJobId;
        record.requestedBy = requestedBy;
        record.role = role == null ? "UNKNOWN" : role;
        record.language = language == null ? "en" : language.trim().toLowerCase(Locale.ROOT);
        record.errorCode = errorCode == null ? "" : errorCode.trim();
        record.fromDate = fromDate;
        record.toDate = toDate;
        record.status = "PENDING";
        record.progress = 5;
        record.createdAt = LocalDateTime.now();
        return record;
    }

    private Map<String, Object> toStatus(JobRecord record) {
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("jobId", record.jobId);
        body.put("sourceJobId", record.sourceJobId);
        body.put("importJobId", record.importJobId);
        body.put("status", record.status);
        body.put("progress", record.progress);
        body.put("rowCount", record.rowCount);
        body.put("error", record.error);
        body.put("createdAt", record.createdAt == null ? null : record.createdAt.toString());
        body.put("finishedAt", record.finishedAt == null ? null : record.finishedAt.toString());
        body.put("downloadReady", "DONE".equals(record.status));
        Map<String, String> filters = new HashMap<String, String>();
        filters.put("errorCode", record.errorCode == null ? "" : record.errorCode);
        filters.put("from", record.fromDate == null ? "" : record.fromDate.toString());
        filters.put("to", record.toDate == null ? "" : record.toDate.toString());
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

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }

    private static class JobRecord {
        private String jobId;
        private String sourceJobId;
        private String tenantId;
        private String importJobId;
        private String requestedBy;
        private String role;
        private String language;
        private String status;
        private int progress;
        private int rowCount;
        private String csv;
        private String error;
        private String errorCode;
        private LocalDate fromDate;
        private LocalDate toDate;
        private LocalDateTime createdAt;
        private LocalDateTime finishedAt;
    }
}
