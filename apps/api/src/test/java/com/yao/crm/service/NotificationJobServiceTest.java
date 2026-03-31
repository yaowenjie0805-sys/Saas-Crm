package com.yao.crm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yao.crm.entity.NotificationJob;
import com.yao.crm.repository.NotificationJobRepository;
import com.yao.crm.util.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationJobServiceTest {

    @Mock
    private NotificationJobRepository jobRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private IntegrationWebhookService integrationWebhookService;

    @Mock
    private IdGenerator idGenerator;

    private NotificationJobService service;

    @BeforeEach
    void setUp() {
        service = createService(100);
    }

    @Test
    @DisplayName("shouldProcessAllJobsInBatches_whenProcessQueue")
    void shouldProcessAllJobsInBatches_whenProcessQueue() {
        NotificationJob job1 = createJob("job-1");
        NotificationJob job2 = createJob("job-2");
        NotificationJob job3 = createJob("job-3");

        Page<NotificationJob> firstBatch = new PageImpl<NotificationJob>(Arrays.asList(job1, job2));
        Page<NotificationJob> secondBatch = new PageImpl<NotificationJob>(Collections.singletonList(job3));
        Page<NotificationJob> emptyBatch = new PageImpl<NotificationJob>(Collections.<NotificationJob>emptyList());

        when(jobRepository.findByStatusInAndNextRetryAtBefore(
                eq(Arrays.asList("PENDING", "RETRY")),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(firstBatch, secondBatch, emptyBatch);
        when(jobRepository.save(any(NotificationJob.class))).thenAnswer(inv -> inv.getArgument(0));
        when(integrationWebhookService.sendEvent(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(true);

        int processed = service.processQueue();

        assertEquals(3, processed);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(jobRepository, times(3)).findByStatusInAndNextRetryAtBefore(
                eq(Arrays.asList("PENDING", "RETRY")),
                any(LocalDateTime.class),
                pageableCaptor.capture()
        );
        List<Pageable> pageables = pageableCaptor.getAllValues();
        assertEquals(3, pageables.size());
        for (Pageable pageable : pageables) {
            assertEquals(0, pageable.getPageNumber());
            assertEquals(100, pageable.getPageSize());
            assertTrue(pageable.getSort().getOrderFor("createdAt") != null);
            assertEquals(Sort.Direction.ASC, pageable.getSort().getOrderFor("createdAt").getDirection());
            assertTrue(pageable.getSort().getOrderFor("id") != null);
            assertEquals(Sort.Direction.ASC, pageable.getSort().getOrderFor("id").getDirection());
        }

        verify(integrationWebhookService, times(3)).sendEvent(anyString(), anyString(), anyString(), anyString(), anyString());
        verify(jobRepository, times(6)).save(any(NotificationJob.class));
        verify(auditLogService, times(3)).record(eq("system"), eq("SYSTEM"), eq("NOTIFY_SUCCESS"), anyString(), anyString(), eq("notification sent"), anyString());
        verify(auditLogService, never()).record(eq("system"), eq("SYSTEM"), eq("NOTIFY_FAILED"), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("shouldUseConfiguredBatchSize_whenProcessQueue")
    void shouldUseConfiguredBatchSize_whenProcessQueue() {
        service = createService(25);

        NotificationJob job1 = createJob("job-1");
        Page<NotificationJob> firstBatch = new PageImpl<NotificationJob>(Collections.singletonList(job1));
        Page<NotificationJob> emptyBatch = new PageImpl<NotificationJob>(Collections.<NotificationJob>emptyList());

        when(jobRepository.findByStatusInAndNextRetryAtBefore(
                eq(Arrays.asList("PENDING", "RETRY")),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(firstBatch, emptyBatch);
        when(jobRepository.save(any(NotificationJob.class))).thenAnswer(inv -> inv.getArgument(0));
        when(integrationWebhookService.sendEvent(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(true);

        int processed = service.processQueue();

        assertEquals(1, processed);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(jobRepository, times(2)).findByStatusInAndNextRetryAtBefore(
                eq(Arrays.asList("PENDING", "RETRY")),
                any(LocalDateTime.class),
                pageableCaptor.capture()
        );
        List<Pageable> pageables = pageableCaptor.getAllValues();
        assertEquals(2, pageables.size());
        assertEquals(25, pageables.get(0).getPageSize());
    }

    @Test
    @DisplayName("shouldFallbackToDefaultBatchSize_whenConfiguredBatchSizeIsNonPositive")
    void shouldFallbackToDefaultBatchSize_whenConfiguredBatchSizeIsNonPositive() {
        service = createService(0);

        NotificationJob job1 = createJob("job-1");
        Page<NotificationJob> firstBatch = new PageImpl<NotificationJob>(Collections.singletonList(job1));
        Page<NotificationJob> emptyBatch = new PageImpl<NotificationJob>(Collections.<NotificationJob>emptyList());

        when(jobRepository.findByStatusInAndNextRetryAtBefore(
                eq(Arrays.asList("PENDING", "RETRY")),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(firstBatch, emptyBatch);
        when(jobRepository.save(any(NotificationJob.class))).thenAnswer(inv -> inv.getArgument(0));
        when(integrationWebhookService.sendEvent(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(true);

        int processed = service.processQueue();

        assertEquals(1, processed);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(jobRepository, times(2)).findByStatusInAndNextRetryAtBefore(
                eq(Arrays.asList("PENDING", "RETRY")),
                any(LocalDateTime.class),
                pageableCaptor.capture()
        );
        List<Pageable> pageables = pageableCaptor.getAllValues();
        assertEquals(2, pageables.size());
        assertEquals(100, pageables.get(0).getPageSize());
    }

    @Test
    @DisplayName("shouldDeduplicateAndNormalizeProviders_whenEnqueueSlaEscalated")
    void shouldDeduplicateAndNormalizeProviders_whenEnqueueSlaEscalated() throws Exception {
        service = createService(100, " wecom , , DINGTALK, wecom,  dingTalk  ");

        when(jobRepository.findByTenantIdAndDedupeKeyIn(eq("tenant-1"), any(List.class)))
                .thenReturn(Collections.<NotificationJob>emptyList());
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"event\":\"approval_sla_escalated\"}");
        when(jobRepository.save(any(NotificationJob.class))).thenAnswer(inv -> inv.getArgument(0));
        when(idGenerator.generate(anyString())).thenReturn("noj-123");

        service.enqueueSlaEscalated("tenant-1", "instance-1", "task-1", "approver-1");

        ArgumentCaptor<List> dedupeKeysCaptor = ArgumentCaptor.forClass(List.class);
        verify(jobRepository, times(1)).findByTenantIdAndDedupeKeyIn(eq("tenant-1"), dedupeKeysCaptor.capture());
        List dedupeKeys = dedupeKeysCaptor.getValue();
        assertEquals(2, dedupeKeys.size());
        assertTrue(dedupeKeys.contains("tenant-1|instance-1|task-1|approval_sla_escalated|WECOM"));
        assertTrue(dedupeKeys.contains("tenant-1|instance-1|task-1|approval_sla_escalated|DINGTALK"));

        verify(jobRepository, times(2)).save(any(NotificationJob.class));
        verify(auditLogService, times(2)).record(eq("system"), eq("SYSTEM"), eq("NOTIFY_ENQUEUED"), eq("NOTIFICATION_JOB"), anyString(), eq("Queued notification job"), eq("tenant-1"));
    }

    @Test
    @DisplayName("shouldReturnImmediately_whenProvidersAreBlank")
    void shouldReturnImmediately_whenProvidersAreBlank() {
        service = createService(100, " ,  , ");

        service.enqueueSlaEscalated("tenant-1", "instance-1", "task-1", "approver-1");

        verifyNoInteractions(jobRepository, auditLogService, objectMapper, integrationWebhookService);
    }

    @Test
    @DisplayName("shouldUseFailedAsEffectiveStatus_whenRetryByFilterUsesAll")
    void shouldUseFailedAsEffectiveStatus_whenRetryByFilterUsesAll() {
        NotificationJob failed = createJob("job-failed-1");
        failed.setStatus("FAILED");
        when(jobRepository.findByTenantIdAndStatus(eq("tenant-1"), eq("FAILED"), any(Pageable.class)))
                .thenReturn(new PageImpl<NotificationJob>(Collections.singletonList(failed)));
        when(jobRepository.save(any(NotificationJob.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> summary = service.retryByFilter("tenant-1", " ALL ", 1, 20);

        assertEquals("FAILED", summary.get("status"));
        assertEquals(1, summary.get("requested"));
        assertEquals(1, summary.get("succeeded"));
        verify(jobRepository).findByTenantIdAndStatus(eq("tenant-1"), eq("FAILED"), any(Pageable.class));
    }

    @Test
    @DisplayName("shouldSkipRetry_whenRetryByFilterUsesNonRetryableStatus")
    void shouldSkipRetry_whenRetryByFilterUsesNonRetryableStatus() {
        Map<String, Object> summary = service.retryByFilter("tenant-1", "SUCCESS", 1, 20);

        assertEquals("SUCCESS", summary.get("status"));
        assertEquals(0, summary.get("requested"));
        assertEquals(0, summary.get("succeeded"));
        assertEquals(0, summary.get("skipped"));
        verify(jobRepository, never()).findByTenantIdAndStatus(anyString(), anyString(), any(Pageable.class));
        verify(jobRepository, never()).save(any(NotificationJob.class));
    }

    private NotificationJob createJob(String id) {
        NotificationJob job = new NotificationJob();
        job.setId(id);
        job.setTenantId("tenant-1");
        job.setEventType("approval_sla_escalated");
        job.setTarget("WECOM");
        job.setPayload("{\"event\":\"approval_sla_escalated\"}");
        job.setStatus("PENDING");
        job.setRetryCount(0);
        job.setMaxRetries(5);
        job.setNextRetryAt(LocalDateTime.now().minusMinutes(1));
        job.setCreatedAt(LocalDateTime.now().minusMinutes(2));
        job.setUpdatedAt(LocalDateTime.now().minusMinutes(2));
        return job;
    }

    private NotificationJobService createService(int processQueueBatchSize) {
        return createService(processQueueBatchSize, "WECOM,DINGTALK");
    }

    private NotificationJobService createService(int processQueueBatchSize, String providers) {
        return new NotificationJobService(
                jobRepository,
                auditLogService,
                objectMapper,
                integrationWebhookService,
                idGenerator,
                processQueueBatchSize,
                5,
                providers
        );
    }
}
