package com.yao.crm.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class ReportExportJobService {

    private static final String JOB_CACHE_KEY_PREFIX = "report:export:job:";
    private static final String TENANT_JOB_INDEX_PREFIX = "report:export:tenant-index:";
    private static final String RETRY_SOURCE_INDEX_PREFIX = "report:export:retry-source-index:";
    private static final long MISSING_JOB_TTL_MILLIS = 3_000L;
    private static final long INDEX_CACHE_TTL_MILLIS = 2_000L;
    private static final Comparator<JobRecord> CREATED_AT_DESC = new Comparator<JobRecord>() {
        @Override
        public int compare(JobRecord a, JobRecord b) {
            LocalDateTime ta = a == null || a.createdAt == null ? LocalDateTime.MIN : a.createdAt;
            LocalDateTime tb = b == null || b.createdAt == null ? LocalDateTime.MIN : b.createdAt;
            int cmp = tb.compareTo(ta);
            if (cmp != 0) {
                return cmp;
            }
            String ja = a == null || a.jobId == null ? "" : a.jobId;
            String jb = b == null || b.jobId == null ? "" : b.jobId;
            return jb.compareTo(ja);
        }
    };

    private final ReportExportService reportExportService;
    private final ThreadPoolTaskExecutor executor;
    private final CacheService cacheService;
    private final Map<String, JobRecord> jobs = new ConcurrentHashMap<String, JobRecord>();
    private final Map<String, Set<String>> tenantMemoryIndex = new ConcurrentHashMap<String, Set<String>>();
    private final Map<String, Object> jobLoadLocks = new ConcurrentHashMap<String, Object>();
    private final Map<String, Long> missingJobLookups = new ConcurrentHashMap<String, Long>();
    private final Map<String, CachedIds> tenantIndexSnapshots = new ConcurrentHashMap<String, CachedIds>();
    private final Map<String, CachedIds> retrySourceIndexSnapshots = new ConcurrentHashMap<String, CachedIds>();
    private final AtomicLong lastCleanupAtMillis = new AtomicLong(0L);

    private static final long CLEANUP_INTERVAL_MILLIS = 30_000L;

    public ReportExportJobService(ReportExportService reportExportService,
                                  @Qualifier("reportExportExecutor") ThreadPoolTaskExecutor executor,
                                  CacheService cacheService) {
        this.reportExportService = reportExportService;
        this.executor = executor;
        this.cacheService = cacheService;
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
        putJobRecord(record);
        persistRecord(record);
        start(record);
        return toStatus(record);
    }

    public Map<String, Object> retryByTenant(String jobId, String requester, String tenantId, boolean canViewAll) {
        String requiredTenantId = requireTenantId(tenantId);
        cleanupOldFinishedJobs();
        JobRecord source = resolveJobRecord(jobId);
        if (source == null) {
            throw new IllegalArgumentException("export_job_not_found");
        }
        if (!requiredTenantId.equals(source.tenantId)) {
            throw new IllegalArgumentException("forbidden");
        }
        if (!canViewAll && !Objects.equals(source.requestedBy, requester)) {
            throw new IllegalArgumentException("forbidden");
        }
        if (!isTerminalStatus(source.status)) {
            throw new IllegalStateException("export_job_not_retryable");
        }
        JobRecord inFlightRetry = findInFlightRetryBySourceFast(requiredTenantId, source.jobId);
        if (inFlightRetry != null) {
            return toStatus(inFlightRetry);
        }

        JobRecord retried = createRecord(source.requestedBy, source.tenantId, source.role, source.from, source.to, source.owner, source.department, source.timezone, source.currency, source.language, source.jobId);
        putJobRecord(retried);
        persistRecord(retried);
        start(retried);
        return toStatus(retried);
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
        List<JobRecord> rows = listTenantRecords(requiredTenantId);
        String statusFilter = normalizeStatus(status);
        int safeSize = Math.max(1, size);
        int safePage = Math.max(1, page);
        int total = countPagedMatches(rows, requiredTenantId, requester, canViewAll, statusFilter);
        int totalPages = Math.max(1, (int) Math.ceil((double) total / safeSize));
        if (safePage > totalPages) {
            safePage = totalPages;
        }
        int fromIndex = Math.min((safePage - 1) * safeSize, total);
        int needed = fromIndex + safeSize;
        List<Map<String, Object>> out = slicePagedRows(
                rows,
                requiredTenantId,
                requester,
                canViewAll,
                statusFilter,
                fromIndex,
                safeSize,
                needed
        );

        Map<String, Object> body = new HashMap<String, Object>();
        body.put("items", out);
        body.put("page", safePage);
        body.put("size", safeSize);
        body.put("totalPages", totalPages);
        body.put("total", total);
        return body;
    }

    private int countPagedMatches(List<JobRecord> rows,
                                  String requiredTenantId,
                                  String requester,
                                  boolean canViewAll,
                                  String statusFilter) {
        int total = 0;
        for (JobRecord row : rows) {
            if (matchesRow(row, requiredTenantId, requester, canViewAll, statusFilter)) {
                total++;
            }
        }
        return total;
    }

    private List<Map<String, Object>> slicePagedRows(List<JobRecord> rows,
                                                     String requiredTenantId,
                                                     String requester,
                                                     boolean canViewAll,
                                                     String statusFilter,
                                                     int fromIndex,
                                                     int pageSize,
                                                     int neededCount) {
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>(pageSize);
        if (pageSize <= 0 || neededCount <= 0) {
            return out;
        }

        PriorityQueue<JobRecord> top = new PriorityQueue<JobRecord>(Math.max(1, neededCount), new Comparator<JobRecord>() {
            @Override
            public int compare(JobRecord a, JobRecord b) {
                return CREATED_AT_DESC.compare(b, a);
            }
        });

        for (JobRecord row : rows) {
            if (!matchesRow(row, requiredTenantId, requester, canViewAll, statusFilter)) {
                continue;
            }
            if (top.size() < neededCount) {
                top.offer(row);
                continue;
            }
            JobRecord oldestInTop = top.peek();
            if (oldestInTop != null && CREATED_AT_DESC.compare(row, oldestInTop) >= 0) {
                continue;
            }
            top.poll();
            top.offer(row);
        }

        List<JobRecord> best = new ArrayList<JobRecord>(top);
        Collections.sort(best, CREATED_AT_DESC);

        int collected = 0;
        for (int i = fromIndex; i < best.size() && collected < pageSize; i++) {
            out.add(toStatus(best.get(i)));
            collected++;
        }
        return out;
    }

    private boolean matchesRow(JobRecord row,
                               String requiredTenantId,
                               String requester,
                               boolean canViewAll,
                               String statusFilter) {
        if (row == null) {
            return false;
        }
        if (!requiredTenantId.equals(row.tenantId)) {
            return false;
        }
        if (!canViewAll && !Objects.equals(row.requestedBy, requester)) {
            return false;
        }
        if (statusFilter != null && !statusFilter.equals(row.status)) {
            return false;
        }
        return true;
    }

    public Map<String, Object> statusByTenant(String jobId, String requester, String tenantId, boolean canViewAll) {
        String requiredTenantId = requireTenantId(tenantId);
        cleanupOldFinishedJobs();
        JobRecord record = resolveJobRecord(jobId);
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

    public String downloadByTenant(String jobId, String requester, String tenantId, boolean canViewAll) {
        String requiredTenantId = requireTenantId(tenantId);
        cleanupOldFinishedJobs();
        JobRecord record = resolveJobRecord(jobId);
        if (record == null) {
            throw new IllegalArgumentException("export_job_not_found");
        }
        if (!requiredTenantId.equals(record.tenantId)) {
            throw new IllegalArgumentException("forbidden");
        }
        if (!canViewAll && !Objects.equals(record.requestedBy, requester)) {
            throw new IllegalArgumentException("forbidden");
        }
        synchronized (record) {
            if (!isDownloadReady(record)) {
                throw new IllegalStateException("export_job_not_ready");
            }
            String csv = record.csv;
            // One-time payload semantics: after successful download, clear payload to avoid stale ready state.
            record.csv = null;
            persistRecord(record);
            return csv;
        }
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
            persistRecord(record);
            String csv = reportExportService.exportOverviewCsvByTenant(record.tenantId, record.from, record.to, record.role, record.owner, record.department, record.language);
            record.progress = 95;
            record.csv = csv;
            record.rowCount = countRows(csv);
            record.status = "DONE";
            record.progress = 100;
            record.finishedAt = LocalDateTime.now();
            persistRecord(record);
        } catch (Exception ex) {
            record.status = "FAILED";
            record.progress = 100;
            record.error = ex.getMessage();
            record.finishedAt = LocalDateTime.now();
            persistRecord(record);
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
        body.put("downloadReady", isDownloadReady(record));
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

    private boolean isDownloadReady(JobRecord record) {
        if (record == null || !"DONE".equals(record.status)) {
            return false;
        }
        String csv = record.csv;
        return csv != null && !csv.trim().isEmpty();
    }

    private boolean isTerminalStatus(String status) {
        return "DONE".equals(status) || "FAILED".equals(status);
    }

    private JobRecord findInFlightRetryBySourceFast(String tenantId, String sourceJobId) {
        if (sourceJobId == null || sourceJobId.trim().isEmpty()) {
            return null;
        }
        List<String> indexedJobIds = getRetrySourceIndexedJobIds(tenantId, sourceJobId);
        if (!indexedJobIds.isEmpty()) {
            JobRecord indexed = findInFlightRetryInCandidates(tenantId, sourceJobId, indexedJobIds, true);
            if (indexed != null) {
                return indexed;
            }
        }
        JobRecord fallback = findInFlightRetryBySource(tenantId, sourceJobId);
        if (fallback != null) {
            updateRetrySourceIndex(tenantId, sourceJobId, fallback.jobId);
        }
        return fallback;
    }

    private JobRecord findInFlightRetryBySource(String tenantId, String sourceJobId) {
        if (sourceJobId == null || sourceJobId.trim().isEmpty()) {
            return null;
        }
        List<JobRecord> records = listTenantRecords(tenantId);
        for (JobRecord record : records) {
            if (!Objects.equals(sourceJobId, record.sourceJobId)) {
                continue;
            }
            if ("PENDING".equals(record.status) || "RUNNING".equals(record.status)) {
                return record;
            }
        }
        return null;
    }

    private JobRecord findInFlightRetryInCandidates(String tenantId,
                                                    String sourceJobId,
                                                    List<String> candidateJobIds,
                                                    boolean pruneIndex) {
        Set<String> resolvedIds = new LinkedHashSet<String>();
        JobRecord hit = null;
        for (String jobId : candidateJobIds) {
            JobRecord record = resolveJobRecord(jobId);
            if (record == null) {
                continue;
            }
            if (!tenantId.equals(record.tenantId) || !Objects.equals(sourceJobId, record.sourceJobId)) {
                continue;
            }
            resolvedIds.add(record.jobId);
            if ("PENDING".equals(record.status) || "RUNNING".equals(record.status)) {
                if (hit == null) {
                    hit = record;
                } else {
                    LocalDateTime a = hit.createdAt == null ? LocalDateTime.MIN : hit.createdAt;
                    LocalDateTime b = record.createdAt == null ? LocalDateTime.MIN : record.createdAt;
                    if (b.isAfter(a)) {
                        hit = record;
                    }
                }
            }
        }
        if (pruneIndex && !new LinkedHashSet<String>(candidateJobIds).equals(resolvedIds)) {
            saveRetrySourceIndex(tenantId, sourceJobId, resolvedIds);
        }
        return hit;
    }

    private String requireTenantId(String tenantId) {
        String normalized = tenantId == null ? "" : tenantId.trim();
        if (normalized.isEmpty()) {
            throw new IllegalStateException("tenant_id_required");
        }
        return normalized;
    }

    private void cleanupOldFinishedJobs() {
        long now = System.currentTimeMillis();
        long last = lastCleanupAtMillis.get();
        if (now - last < CLEANUP_INTERVAL_MILLIS) {
            return;
        }
        if (!lastCleanupAtMillis.compareAndSet(last, now)) {
            return;
        }

        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        for (Map.Entry<String, JobRecord> entry : jobs.entrySet()) {
            JobRecord row = entry.getValue();
            if (("DONE".equals(row.status) || "FAILED".equals(row.status))
                    && row.finishedAt != null
                    && row.finishedAt.isBefore(threshold)) {
                jobs.remove(entry.getKey());
                removeFromTenantMemoryIndex(row.tenantId, entry.getKey());
                cacheService.delete(JOB_CACHE_KEY_PREFIX + entry.getKey());
                removeFromTenantIndex(row.tenantId, entry.getKey());
                removeFromRetrySourceIndex(row.tenantId, row.sourceJobId, entry.getKey());
            }
        }
    }

    private List<JobRecord> listTenantRecords(String tenantId) {
        List<String> indexedJobIds = getTenantIndexedJobIds(tenantId);
        Set<String> candidateJobIds = new LinkedHashSet<String>();
        candidateJobIds.addAll(getTenantMemoryJobIds(tenantId));
        candidateJobIds.addAll(indexedJobIds);

        List<JobRecord> records = new ArrayList<JobRecord>();
        Set<String> resolvedJobIds = new LinkedHashSet<String>();
        for (String jobId : candidateJobIds) {
            JobRecord record = resolveJobRecord(jobId);
            if (record == null || !tenantId.equals(record.tenantId)) {
                continue;
            }
            records.add(record);
            resolvedJobIds.add(record.jobId);
        }
        if (!new LinkedHashSet<String>(indexedJobIds).equals(resolvedJobIds)) {
            saveTenantIndex(tenantId, resolvedJobIds);
        }
        return records;
    }

    private void persistRecord(JobRecord record) {
        if (record == null || record.jobId == null || record.jobId.trim().isEmpty()) {
            return;
        }
        missingJobLookups.remove(record.jobId);
        cacheService.set(JOB_CACHE_KEY_PREFIX + record.jobId, toCacheRecord(record));
        updateTenantIndex(record.tenantId, record.jobId);
        updateRetrySourceIndex(record.tenantId, record.sourceJobId, record.jobId);
    }

    private JobRecord loadRecord(String jobId) {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) cacheService.get(JOB_CACHE_KEY_PREFIX + jobId, Map.class).orElse(null);
        if (map == null || map.isEmpty()) {
            return null;
        }
        JobRecord record = fromCacheRecord(map);
        if (record != null && record.jobId != null) {
            putJobRecord(record);
        }
        return record;
    }

    private void putJobRecord(JobRecord record) {
        if (record == null || record.jobId == null || record.jobId.trim().isEmpty()) {
            return;
        }
        jobs.put(record.jobId, record);
        missingJobLookups.remove(record.jobId);
        if (record.tenantId == null || record.tenantId.trim().isEmpty()) {
            return;
        }
        tenantMemoryIndex
                .computeIfAbsent(record.tenantId, key -> ConcurrentHashMap.newKeySet())
                .add(record.jobId);
    }

    private List<String> getTenantMemoryJobIds(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> ids = tenantMemoryIndex.get(tenantId);
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<String>();
        for (String id : ids) {
            if (id == null) {
                continue;
            }
            String normalized = id.trim();
            if (!normalized.isEmpty()) {
                out.add(normalized);
            }
        }
        return out;
    }

    private void removeFromTenantMemoryIndex(String tenantId, String jobId) {
        if (tenantId == null || tenantId.trim().isEmpty() || jobId == null || jobId.trim().isEmpty()) {
            return;
        }
        Set<String> ids = tenantMemoryIndex.get(tenantId);
        if (ids == null) {
            return;
        }
        ids.remove(jobId);
        if (ids.isEmpty()) {
            tenantMemoryIndex.remove(tenantId, ids);
        }
    }

    private JobRecord resolveJobRecord(String jobId) {
        if (jobId == null || jobId.trim().isEmpty()) {
            return null;
        }
        if (isMissingJobFresh(jobId)) {
            return null;
        }
        JobRecord record = jobs.get(jobId);
        if (record != null) {
            return record;
        }
        Object lock = jobLoadLocks.computeIfAbsent(jobId, ignored -> new Object());
        synchronized (lock) {
            record = jobs.get(jobId);
            if (record != null) {
                return record;
            }
            try {
                record = loadRecord(jobId);
                if (record == null) {
                    missingJobLookups.put(jobId, System.currentTimeMillis());
                    return null;
                }
                missingJobLookups.remove(jobId);
                return record;
            } finally {
                jobLoadLocks.remove(jobId, lock);
            }
        }
    }

    private boolean isMissingJobFresh(String jobId) {
        Long at = missingJobLookups.get(jobId);
        if (at == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now - at < MISSING_JOB_TTL_MILLIS) {
            return true;
        }
        missingJobLookups.remove(jobId, at);
        return false;
    }

    private void updateTenantIndex(String tenantId, String jobId) {
        if (tenantId == null || tenantId.trim().isEmpty() || jobId == null || jobId.trim().isEmpty()) {
            return;
        }
        Set<String> ids = new LinkedHashSet<String>(getTenantIndexedJobIds(tenantId));
        if (ids.contains(jobId)) {
            return;
        }
        ids.add(jobId);
        saveTenantIndex(tenantId, ids);
    }

    private List<String> getTenantIndexedJobIds(String tenantId) {
        return new ArrayList<String>(getTenantIndexedJobIdSet(tenantId));
    }

    private void removeFromTenantIndex(String tenantId, String jobId) {
        if (tenantId == null || tenantId.trim().isEmpty() || jobId == null || jobId.trim().isEmpty()) {
            return;
        }
        Set<String> ids = new LinkedHashSet<String>(getTenantIndexedJobIds(tenantId));
        if (ids.remove(jobId)) {
            saveTenantIndex(tenantId, ids);
        }
    }

    private void saveTenantIndex(String tenantId, java.util.Collection<String> rawIds) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            return;
        }
        List<String> ids = normalizeIds(rawIds);
        if (ids.size() > 500) {
            ids = ids.subList(ids.size() - 500, ids.size());
        }
        Set<String> snapshotIds = new LinkedHashSet<String>(ids);
        cacheService.set(TENANT_JOB_INDEX_PREFIX + tenantId, ids.toArray(new String[0]));
        tenantIndexSnapshots.put(tenantId, new CachedIds(snapshotIds, System.currentTimeMillis()));
    }

    private Set<String> getTenantIndexedJobIdSet(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            return Collections.emptySet();
        }
        long now = System.currentTimeMillis();
        CachedIds snapshot = tenantIndexSnapshots.get(tenantId);
        if (snapshot != null && now - snapshot.loadedAtMillis < INDEX_CACHE_TTL_MILLIS) {
            return new LinkedHashSet<String>(snapshot.ids);
        }

        String[] ids = cacheService.get(TENANT_JOB_INDEX_PREFIX + tenantId, String[].class).orElse(new String[0]);
        Set<String> normalized = new LinkedHashSet<String>(normalizeIds(Arrays.asList(ids)));
        tenantIndexSnapshots.put(tenantId, new CachedIds(normalized, now));
        return new LinkedHashSet<String>(normalized);
    }

    private List<String> normalizeIds(java.util.Collection<String> rawIds) {
        List<String> ids = new ArrayList<String>();
        if (rawIds != null) {
            for (String id : rawIds) {
                if (id == null) {
                    continue;
                }
                String normalized = id.trim();
                if (!normalized.isEmpty()) {
                    ids.add(normalized);
                }
            }
        }
        return ids;
    }

    private void updateRetrySourceIndex(String tenantId, String sourceJobId, String jobId) {
        if (tenantId == null || tenantId.trim().isEmpty()
                || sourceJobId == null || sourceJobId.trim().isEmpty()
                || jobId == null || jobId.trim().isEmpty()) {
            return;
        }
        Set<String> ids = new LinkedHashSet<String>(getRetrySourceIndexedJobIds(tenantId, sourceJobId));
        if (ids.contains(jobId)) {
            return;
        }
        ids.add(jobId);
        saveRetrySourceIndex(tenantId, sourceJobId, ids);
    }

    private void removeFromRetrySourceIndex(String tenantId, String sourceJobId, String jobId) {
        if (tenantId == null || tenantId.trim().isEmpty()
                || sourceJobId == null || sourceJobId.trim().isEmpty()
                || jobId == null || jobId.trim().isEmpty()) {
            return;
        }
        Set<String> ids = new LinkedHashSet<String>(getRetrySourceIndexedJobIds(tenantId, sourceJobId));
        if (ids.remove(jobId)) {
            saveRetrySourceIndex(tenantId, sourceJobId, ids);
        }
    }

    private List<String> getRetrySourceIndexedJobIds(String tenantId, String sourceJobId) {
        if (tenantId == null || tenantId.trim().isEmpty() || sourceJobId == null || sourceJobId.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<String>(getRetrySourceIndexedJobIdSet(tenantId, sourceJobId));
    }

    private void saveRetrySourceIndex(String tenantId, String sourceJobId, java.util.Collection<String> rawIds) {
        if (tenantId == null || tenantId.trim().isEmpty() || sourceJobId == null || sourceJobId.trim().isEmpty()) {
            return;
        }
        List<String> ids = normalizeIds(rawIds);
        if (ids.size() > 100) {
            ids = ids.subList(ids.size() - 100, ids.size());
        }
        Set<String> snapshotIds = new LinkedHashSet<String>(ids);
        String key = retrySourceIndexKey(tenantId, sourceJobId);
        cacheService.set(key, ids.toArray(new String[0]));
        retrySourceIndexSnapshots.put(key, new CachedIds(snapshotIds, System.currentTimeMillis()));
    }

    private Set<String> getRetrySourceIndexedJobIdSet(String tenantId, String sourceJobId) {
        if (tenantId == null || tenantId.trim().isEmpty() || sourceJobId == null || sourceJobId.trim().isEmpty()) {
            return Collections.emptySet();
        }
        String key = retrySourceIndexKey(tenantId, sourceJobId);
        long now = System.currentTimeMillis();
        CachedIds snapshot = retrySourceIndexSnapshots.get(key);
        if (snapshot != null && now - snapshot.loadedAtMillis < INDEX_CACHE_TTL_MILLIS) {
            return new LinkedHashSet<String>(snapshot.ids);
        }

        String[] ids = cacheService.get(key, String[].class).orElse(new String[0]);
        Set<String> normalized = new LinkedHashSet<String>(normalizeIds(Arrays.asList(ids)));
        retrySourceIndexSnapshots.put(key, new CachedIds(normalized, now));
        return new LinkedHashSet<String>(normalized);
    }

    private String retrySourceIndexKey(String tenantId, String sourceJobId) {
        return RETRY_SOURCE_INDEX_PREFIX + tenantId + ":" + sourceJobId;
    }

    private Map<String, Object> toCacheRecord(JobRecord record) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("jobId", record.jobId);
        map.put("sourceJobId", record.sourceJobId);
        map.put("requestedBy", record.requestedBy);
        map.put("tenantId", record.tenantId);
        map.put("role", record.role);
        map.put("owner", record.owner);
        map.put("department", record.department);
        map.put("timezone", record.timezone);
        map.put("currency", record.currency);
        map.put("language", record.language);
        map.put("from", record.from == null ? null : record.from.toString());
        map.put("to", record.to == null ? null : record.to.toString());
        map.put("status", record.status);
        map.put("progress", record.progress);
        map.put("rowCount", record.rowCount);
        map.put("csv", record.csv);
        map.put("error", record.error);
        map.put("createdAt", record.createdAt == null ? null : record.createdAt.toString());
        map.put("finishedAt", record.finishedAt == null ? null : record.finishedAt.toString());
        return map;
    }

    private JobRecord fromCacheRecord(Map<String, Object> map) {
        try {
            JobRecord record = new JobRecord();
            record.jobId = text(map.get("jobId"));
            record.sourceJobId = text(map.get("sourceJobId"));
            record.requestedBy = text(map.get("requestedBy"));
            record.tenantId = text(map.get("tenantId"));
            record.role = text(map.get("role"));
            record.owner = text(map.get("owner"));
            record.department = text(map.get("department"));
            record.timezone = text(map.get("timezone"));
            record.currency = text(map.get("currency"));
            record.language = text(map.get("language"));
            String from = text(map.get("from"));
            String to = text(map.get("to"));
            record.from = from.isEmpty() ? null : LocalDate.parse(from);
            record.to = to.isEmpty() ? null : LocalDate.parse(to);
            record.status = text(map.get("status"));
            record.progress = number(map.get("progress"));
            record.rowCount = number(map.get("rowCount"));
            record.csv = text(map.get("csv"));
            record.error = text(map.get("error"));
            String createdAt = text(map.get("createdAt"));
            String finishedAt = text(map.get("finishedAt"));
            record.createdAt = createdAt.isEmpty() ? null : LocalDateTime.parse(createdAt);
            record.finishedAt = finishedAt.isEmpty() ? null : LocalDateTime.parse(finishedAt);
            return record;
        } catch (Exception ex) {
            return null;
        }
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private int number(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        String text = value == null ? "" : String.valueOf(value).trim();
        if (text.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            return 0;
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
        private volatile String status;
        private volatile int progress;
        private volatile int rowCount;
        private volatile String csv;
        private volatile String error;
        private LocalDateTime createdAt;
        private volatile LocalDateTime finishedAt;
    }

    private static class CachedIds {
        private final Set<String> ids;
        private final long loadedAtMillis;

        private CachedIds(Set<String> ids, long loadedAtMillis) {
            this.ids = ids == null ? Collections.emptySet() : new LinkedHashSet<String>(ids);
            this.loadedAtMillis = loadedAtMillis;
        }
    }
}
