package com.yao.crm.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportExportJobServiceTest {

    @Mock
    private ReportExportService reportExportService;

    @Mock
    private ThreadPoolTaskExecutor executor;

    @Mock
    private CacheService cacheService;

    @Test
    void listByTenantPagedShouldPruneStaleTenantIndexEntries() {
        ReportExportJobService service = new ReportExportJobService(reportExportService, executor, cacheService);

        String tenantId = "tenant-a";
        String staleJobId = "rpt_stale";
        String validJobId = "rpt_valid";
        String tenantIndexKey = "report:export:tenant-index:" + tenantId;
        String staleJobKey = "report:export:job:" + staleJobId;
        String validJobKey = "report:export:job:" + validJobId;

        when(cacheService.get(eq(tenantIndexKey), eq(String[].class)))
                .thenReturn(Optional.of(new String[]{staleJobId, validJobId}));
        when(cacheService.get(eq(staleJobKey), eq(Map.class)))
                .thenReturn(Optional.empty());
        when(cacheService.get(eq(validJobKey), eq(Map.class)))
                .thenReturn(Optional.of(validCachedRecord(validJobId, tenantId)));

        Map<String, Object> page = service.listByTenantPaged("analyst", tenantId, true, 1, 10, null);

        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> items = (java.util.List<Map<String, Object>>) page.get("items");
        assertEquals(1, items.size());
        assertEquals(validJobId, items.get(0).get("jobId"));

        verify(cacheService).set(eq(tenantIndexKey), any(String[].class));
    }

    @Test
    void listByTenantPagedShouldKeepCreatedAtDescOrderAcrossPages() {
        ReportExportJobService service = new ReportExportJobService(reportExportService, executor, cacheService);

        String tenantId = "tenant-a";
        String tenantIndexKey = "report:export:tenant-index:" + tenantId;
        String newest = "rpt_3";
        String middle = "rpt_2";
        String oldest = "rpt_1";

        when(cacheService.get(eq(tenantIndexKey), eq(String[].class)))
                .thenReturn(Optional.of(new String[]{oldest, middle, newest}));
        when(cacheService.get(eq("report:export:job:" + newest), eq(Map.class)))
                .thenReturn(Optional.of(cachedRecordWithCreatedAt(newest, "", tenantId, "analyst", "DONE", "", "2026-04-06T22:00:03")));
        when(cacheService.get(eq("report:export:job:" + middle), eq(Map.class)))
                .thenReturn(Optional.of(cachedRecordWithCreatedAt(middle, "", tenantId, "analyst", "DONE", "", "2026-04-06T22:00:02")));
        when(cacheService.get(eq("report:export:job:" + oldest), eq(Map.class)))
                .thenReturn(Optional.of(cachedRecordWithCreatedAt(oldest, "", tenantId, "analyst", "DONE", "", "2026-04-06T22:00:01")));

        Map<String, Object> page1 = service.listByTenantPaged("analyst", tenantId, true, 1, 2, null);
        Map<String, Object> page2 = service.listByTenantPaged("analyst", tenantId, true, 2, 2, null);

        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> page1Items = (java.util.List<Map<String, Object>>) page1.get("items");
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> page2Items = (java.util.List<Map<String, Object>>) page2.get("items");

        assertNotNull(page1Items);
        assertNotNull(page2Items);
        assertEquals(2, page1Items.size());
        assertEquals(1, page2Items.size());
        assertEquals(newest, page1Items.get(0).get("jobId"));
        assertEquals(middle, page1Items.get(1).get("jobId"));
        assertEquals(oldest, page2Items.get(0).get("jobId"));
    }

    @Test
    void downloadByTenantShouldAllowOnlySingleSuccessfulDownload() {
        ReportExportJobService service = new ReportExportJobService(reportExportService, executor, cacheService);

        String tenantId = "tenant-a";
        String requester = "analyst";
        String jobId = "rpt_once";
        String jobKey = "report:export:job:" + jobId;
        String tenantIndexKey = "report:export:tenant-index:" + tenantId;

        when(cacheService.get(eq(jobKey), eq(Map.class)))
                .thenReturn(Optional.of(validCachedRecord(jobId, tenantId)));
        when(cacheService.get(eq(tenantIndexKey), eq(String[].class)))
                .thenReturn(Optional.of(new String[]{jobId}));

        String first = service.downloadByTenant(jobId, requester, tenantId, true);
        assertEquals("section,key,value\nsummary,customers,1", first);

        IllegalStateException second = assertThrows(
                IllegalStateException.class,
                () -> service.downloadByTenant(jobId, requester, tenantId, true)
        );
        assertEquals("export_job_not_ready", second.getMessage());
    }

    @Test
    void retryByTenantShouldReturnExistingInFlightRetry() {
        ReportExportJobService service = new ReportExportJobService(reportExportService, executor, cacheService);

        String tenantId = "tenant-a";
        String requester = "analyst";
        String sourceJobId = "rpt_source";
        String runningRetryJobId = "rpt_retry_running";

        when(cacheService.get(eq("report:export:job:" + sourceJobId), eq(Map.class)))
                .thenReturn(Optional.of(cachedRecord(sourceJobId, "", tenantId, requester, "DONE", "")));
        when(cacheService.get(eq("report:export:retry-source-index:" + tenantId + ":" + sourceJobId), eq(String[].class)))
                .thenReturn(Optional.empty());
        when(cacheService.get(eq("report:export:tenant-index:" + tenantId), eq(String[].class)))
                .thenReturn(Optional.of(new String[]{sourceJobId, runningRetryJobId}));
        when(cacheService.get(eq("report:export:job:" + runningRetryJobId), eq(Map.class)))
                .thenReturn(Optional.of(cachedRecord(runningRetryJobId, sourceJobId, tenantId, requester, "RUNNING", "")));

        Map<String, Object> result = service.retryByTenant(sourceJobId, requester, tenantId, true);

        assertEquals(runningRetryJobId, result.get("jobId"));
        assertEquals(sourceJobId, result.get("sourceJobId"));
        assertEquals("RUNNING", result.get("status"));
        verify(executor, never()).submit(any(Runnable.class));
    }

    @Test
    void retryByTenantShouldUseSourceIndexWithoutTenantScan() {
        ReportExportJobService service = new ReportExportJobService(reportExportService, executor, cacheService);

        String tenantId = "tenant-a";
        String requester = "analyst";
        String sourceJobId = "rpt_source";
        String runningRetryJobId = "rpt_retry_running";

        when(cacheService.get(eq("report:export:job:" + sourceJobId), eq(Map.class)))
                .thenReturn(Optional.of(cachedRecord(sourceJobId, "", tenantId, requester, "DONE", "")));
        when(cacheService.get(eq("report:export:retry-source-index:" + tenantId + ":" + sourceJobId), eq(String[].class)))
                .thenReturn(Optional.of(new String[]{runningRetryJobId}));
        when(cacheService.get(eq("report:export:job:" + runningRetryJobId), eq(Map.class)))
                .thenReturn(Optional.of(cachedRecord(runningRetryJobId, sourceJobId, tenantId, requester, "RUNNING", "")));

        Map<String, Object> result = service.retryByTenant(sourceJobId, requester, tenantId, true);

        assertEquals(runningRetryJobId, result.get("jobId"));
        verify(cacheService, never()).get(eq("report:export:tenant-index:" + tenantId), eq(String[].class));
        verify(executor, never()).submit(any(Runnable.class));
    }

    @Test
    void retryByTenantShouldRejectNonTerminalSourceJob() {
        ReportExportJobService service = new ReportExportJobService(reportExportService, executor, cacheService);

        String tenantId = "tenant-a";
        String requester = "analyst";
        String sourceJobId = "rpt_source_running";

        when(cacheService.get(eq("report:export:job:" + sourceJobId), eq(Map.class)))
                .thenReturn(Optional.of(cachedRecord(sourceJobId, "", tenantId, requester, "RUNNING", "")));

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> service.retryByTenant(sourceJobId, requester, tenantId, true)
        );
        assertEquals("export_job_not_retryable", error.getMessage());
        verify(executor, never()).submit(any(Runnable.class));
    }

    @Test
    void submitByTenantShouldWriteTenantIndexOnlyOnceForSameJob() {
        ReportExportJobService service = new ReportExportJobService(reportExportService, executor, cacheService);

        String tenantId = "tenant-a";
        String tenantIndexKey = "report:export:tenant-index:" + tenantId;
        AtomicReference<String[]> tenantIndex = new AtomicReference<String[]>(new String[0]);
        when(cacheService.get(eq(tenantIndexKey), eq(String[].class)))
                .thenAnswer(invocation -> Optional.of(tenantIndex.get()));
        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Object value = invocation.getArgument(1);
            if (tenantIndexKey.equals(key) && value instanceof String[]) {
                tenantIndex.set((String[]) value);
            }
            return null;
        }).when(cacheService).set(any(String.class), any());
        when(reportExportService.exportOverviewCsvByTenant(eq(tenantId), any(), any(), any(), any(), any(), any()))
                .thenReturn("section,key,value\nsummary,customers,1");

        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(executor).submit(any(Runnable.class));

        service.submitByTenant("analyst", tenantId, "MANAGER", null, null);

        verify(cacheService, times(1)).set(eq(tenantIndexKey), any(String[].class));
        verify(cacheService, times(1)).get(eq(tenantIndexKey), eq(String[].class));
    }

    @Test
    void retryByTenantShouldReadRetrySourceIndexOnceAcrossPersistLifecycle() {
        ReportExportJobService service = new ReportExportJobService(reportExportService, executor, cacheService);

        String tenantId = "tenant-a";
        String requester = "analyst";
        String sourceJobId = "rpt_source";
        String retryIndexKey = "report:export:retry-source-index:" + tenantId + ":" + sourceJobId;
        String tenantIndexKey = "report:export:tenant-index:" + tenantId;

        when(cacheService.get(eq("report:export:job:" + sourceJobId), eq(Map.class)))
                .thenReturn(Optional.of(cachedRecord(sourceJobId, "", tenantId, requester, "DONE", "section,key,value\nsummary,customers,1")));
        when(cacheService.get(eq(retryIndexKey), eq(String[].class)))
                .thenReturn(Optional.empty());
        when(cacheService.get(eq(tenantIndexKey), eq(String[].class)))
                .thenReturn(Optional.of(new String[]{sourceJobId}));
        when(reportExportService.exportOverviewCsvByTenant(eq(tenantId), any(), any(), any(), any(), any(), any()))
                .thenReturn("section,key,value\nsummary,customers,1");
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(executor).submit(any(Runnable.class));

        Map<String, Object> retried = service.retryByTenant(sourceJobId, requester, tenantId, true);

        assertEquals(sourceJobId, retried.get("sourceJobId"));
        verify(cacheService, times(1)).get(eq(retryIndexKey), eq(String[].class));
    }

    @Test
    void statusByTenantShouldLoadFromCacheOnlyOnceUnderConcurrency() throws Exception {
        ReportExportJobService service = new ReportExportJobService(reportExportService, executor, cacheService);

        String tenantId = "tenant-a";
        String requester = "analyst";
        String jobId = "rpt_hot";
        String jobKey = "report:export:job:" + jobId;

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        when(cacheService.get(eq(jobKey), eq(Map.class))).thenAnswer(invocation -> {
            // Simulate a slow cache read to maximize concurrent overlap.
            Thread.sleep(80L);
            return Optional.of(cachedRecord(jobId, "", tenantId, requester, "RUNNING", ""));
        });

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Map<String, Object>> a = pool.submit(() -> {
                ready.countDown();
                start.await(2, TimeUnit.SECONDS);
                return service.statusByTenant(jobId, requester, tenantId, true);
            });
            Future<Map<String, Object>> b = pool.submit(() -> {
                ready.countDown();
                start.await(2, TimeUnit.SECONDS);
                return service.statusByTenant(jobId, requester, tenantId, true);
            });

            assertTrue(ready.await(2, TimeUnit.SECONDS));
            start.countDown();

            assertEquals("RUNNING", a.get(3, TimeUnit.SECONDS).get("status"));
            assertEquals("RUNNING", b.get(3, TimeUnit.SECONDS).get("status"));
        } finally {
            pool.shutdownNow();
        }

        verify(cacheService, times(1)).get(eq(jobKey), eq(Map.class));
    }

    @Test
    void statusByTenantShouldAvoidRepeatedCacheReadsForMissingJobWithinTtl() {
        ReportExportJobService service = new ReportExportJobService(reportExportService, executor, cacheService);

        String tenantId = "tenant-a";
        String requester = "analyst";
        String jobId = "rpt_missing";
        String jobKey = "report:export:job:" + jobId;

        when(cacheService.get(eq(jobKey), eq(Map.class))).thenReturn(Optional.empty());

        IllegalArgumentException first = assertThrows(
                IllegalArgumentException.class,
                () -> service.statusByTenant(jobId, requester, tenantId, true)
        );
        IllegalArgumentException second = assertThrows(
                IllegalArgumentException.class,
                () -> service.statusByTenant(jobId, requester, tenantId, true)
        );

        assertEquals("export_job_not_found", first.getMessage());
        assertEquals("export_job_not_found", second.getMessage());
        verify(cacheService, times(1)).get(eq(jobKey), eq(Map.class));
    }

    private Map<String, Object> validCachedRecord(String jobId, String tenantId) {
        return cachedRecord(jobId, "", tenantId, "analyst", "DONE", "section,key,value\nsummary,customers,1");
    }

    private Map<String, Object> cachedRecordWithCreatedAt(String jobId,
                                                          String sourceJobId,
                                                          String tenantId,
                                                          String requestedBy,
                                                          String status,
                                                          String csv,
                                                          String createdAt) {
        Map<String, Object> map = cachedRecord(jobId, sourceJobId, tenantId, requestedBy, status, csv);
        map.put("createdAt", createdAt);
        map.put("finishedAt", "DONE".equals(status) ? "2026-04-06T22:00:10" : "");
        return map;
    }

    private Map<String, Object> cachedRecord(String jobId,
                                             String sourceJobId,
                                             String tenantId,
                                             String requestedBy,
                                             String status,
                                             String csv) {
        java.util.HashMap<String, Object> map = new java.util.HashMap<String, Object>();
        map.put("jobId", jobId);
        map.put("sourceJobId", sourceJobId);
        map.put("requestedBy", requestedBy);
        map.put("tenantId", tenantId);
        map.put("role", "");
        map.put("owner", "");
        map.put("department", "");
        map.put("timezone", "Asia/Shanghai");
        map.put("currency", "CNY");
        map.put("language", "en");
        map.put("from", "");
        map.put("to", "");
        map.put("status", status);
        map.put("progress", "DONE".equals(status) ? 100 : 20);
        map.put("rowCount", 1);
        map.put("csv", csv);
        map.put("error", "");
        map.put("createdAt", "2026-04-06T20:00:00");
        map.put("finishedAt", "DONE".equals(status) ? "2026-04-06T20:00:01" : "");
        return map;
    }
}
