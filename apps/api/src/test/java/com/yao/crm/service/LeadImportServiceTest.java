package com.yao.crm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yao.crm.dto.LeadImportChunkMessage;
import com.yao.crm.entity.Lead;
import com.yao.crm.entity.LeadImportJob;
import com.yao.crm.entity.LeadImportJobChunk;
import com.yao.crm.entity.LeadImportJobItem;
import com.yao.crm.repository.LeadImportJobChunkRepository;
import com.yao.crm.repository.LeadImportJobItemRepository;
import com.yao.crm.repository.LeadImportJobRepository;
import com.yao.crm.repository.LeadRepository;
import com.yao.crm.util.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeadImportServiceTest {

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private LeadImportJobRepository jobRepository;

    @Mock
    private LeadImportJobItemRepository itemRepository;

    @Mock
    private LeadImportJobChunkRepository chunkRepository;

    @Mock
    private LeadAssignmentService leadAssignmentService;

    @Mock
    private LeadAutomationService leadAutomationService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private IdGenerator idGenerator;

    @Mock
    private Environment environment;

    private LeadImportService service;

    @BeforeEach
    void setUp() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"test"});
        service = new LeadImportService(
                leadRepository,
                jobRepository,
                itemRepository,
                chunkRepository,
                leadAssignmentService,
                leadAutomationService,
                auditLogService,
                new ObjectMapper(),
                rabbitTemplate,
                idGenerator,
                environment,
                1000,
                3,
                200000,
                20,
                2,
                true
        );
    }

    @Test
    void shouldCacheDedupeKeysAcrossChunksAndRejectDuplicatesWithoutRepeatingScan() throws Exception {
        LeadImportJob job = newJob("job-1");
        LeadImportJobChunk chunk1 = newChunk("chunk-1", "tenant-1", "job-1", 1);
        LeadImportJobChunk chunk2 = newChunk("chunk-2", "tenant-1", "job-1", 2);

        when(jobRepository.findByIdAndTenantId("job-1", "tenant-1")).thenReturn(Optional.of(job));
        when(jobRepository.findById("job-1")).thenReturn(Optional.of(job));
        when(jobRepository.save(any(LeadImportJob.class))).thenAnswer(inv -> inv.getArgument(0));
        when(chunkRepository.findByTenantIdAndJobIdAndChunkNo("tenant-1", "job-1", 1)).thenReturn(Optional.of(chunk1));
        when(chunkRepository.findByTenantIdAndJobIdAndChunkNo("tenant-1", "job-1", 2)).thenReturn(Optional.of(chunk2));
        when(chunkRepository.save(any(LeadImportJobChunk.class))).thenAnswer(inv -> inv.getArgument(0));
        when(chunkRepository.countByTenantIdAndJobId("tenant-1", "job-1")).thenReturn(2L);
        when(chunkRepository.countByTenantIdAndJobIdAndStatus("tenant-1", "job-1", "PROCESSED")).thenReturn(1L, 2L);
        when(chunkRepository.countByTenantIdAndJobIdAndStatus("tenant-1", "job-1", "FAILED")).thenReturn(0L);
        when(chunkRepository.countByTenantIdAndJobIdAndStatus("tenant-1", "job-1", "CANCELED")).thenReturn(0L);
        when(leadRepository.findDedupeKeysByTenantId(eq("tenant-1"), any(Pageable.class)))
                .thenReturn(new PageImpl<Object[]>(Collections.<Object[]>emptyList(), PageRequest.of(0, 500), 0));
        when(leadRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(leadAssignmentService.assignOwnerForTenant("tenant-1")).thenReturn("");

        service.consumeLeadImportChunk(chunkMessage("tenant-1", "job-1", 1,
                Arrays.asList("Alice,Acme,0101234567,alice@example.com,web,,NEW"), "req-1"));

        assertTrue(getDedupeCacheMap().containsKey("tenant-1|job-1"));

        service.consumeLeadImportChunk(chunkMessage("tenant-1", "job-1", 2,
                Arrays.asList("Alice,Acme,0101234567,alice@example.com,web,,NEW"), "req-2"));

        verify(leadRepository, times(1)).findDedupeKeysByTenantId(eq("tenant-1"), any(Pageable.class));
        verify(itemRepository, times(1)).save(any(LeadImportJobItem.class));
        verify(leadRepository, times(1)).saveAll(anyList());
        verify(leadRepository, never()).save(any(Lead.class));
        assertEquals("PARTIAL_SUCCESS", job.getStatus());
        assertFalse(getDedupeCacheMap().containsKey("tenant-1|job-1"));
    }

    @Test
    void shouldReuseCachedDedupeKeysAcrossChunksAndCleanUpAfterSuccess() throws Exception {
        LeadImportJob job = newJob("job-2");
        LeadImportJobChunk chunk1 = newChunk("chunk-1", "tenant-1", "job-2", 1);
        LeadImportJobChunk chunk2 = newChunk("chunk-2", "tenant-1", "job-2", 2);

        when(jobRepository.findByIdAndTenantId("job-2", "tenant-1")).thenReturn(Optional.of(job));
        when(jobRepository.findById("job-2")).thenReturn(Optional.of(job));
        when(jobRepository.save(any(LeadImportJob.class))).thenAnswer(inv -> inv.getArgument(0));
        when(chunkRepository.findByTenantIdAndJobIdAndChunkNo("tenant-1", "job-2", 1)).thenReturn(Optional.of(chunk1));
        when(chunkRepository.findByTenantIdAndJobIdAndChunkNo("tenant-1", "job-2", 2)).thenReturn(Optional.of(chunk2));
        when(chunkRepository.save(any(LeadImportJobChunk.class))).thenAnswer(inv -> inv.getArgument(0));
        when(chunkRepository.countByTenantIdAndJobId("tenant-1", "job-2")).thenReturn(2L);
        when(chunkRepository.countByTenantIdAndJobIdAndStatus("tenant-1", "job-2", "PROCESSED")).thenReturn(1L, 2L);
        when(chunkRepository.countByTenantIdAndJobIdAndStatus("tenant-1", "job-2", "FAILED")).thenReturn(0L);
        when(chunkRepository.countByTenantIdAndJobIdAndStatus("tenant-1", "job-2", "CANCELED")).thenReturn(0L);
        when(leadRepository.findDedupeKeysByTenantId(eq("tenant-1"), any(Pageable.class)))
                .thenReturn(new PageImpl<Object[]>(Collections.<Object[]>emptyList(), PageRequest.of(0, 500), 0));
        when(leadRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(leadAssignmentService.assignOwnerForTenant("tenant-1")).thenReturn("");

        service.consumeLeadImportChunk(chunkMessage("tenant-1", "job-2", 1,
                Arrays.asList("Alice,Acme,0101234567,alice@example.com,web,,NEW"), "req-1"));
        assertTrue(getDedupeCacheMap().containsKey("tenant-1|job-2"));

        service.consumeLeadImportChunk(chunkMessage("tenant-1", "job-2", 2,
                Arrays.asList("Bob,Acme,0101234568,bob@example.com,web,,NEW"), "req-2"));

        verify(leadRepository, times(1)).findDedupeKeysByTenantId(eq("tenant-1"), any(Pageable.class));
        verify(itemRepository, never()).save(any(LeadImportJobItem.class));
        verify(leadRepository, times(2)).saveAll(anyList());
        assertEquals("SUCCESS", job.getStatus());
        assertFalse(getDedupeCacheMap().containsKey("tenant-1|job-2"));
    }

    @Test
    void shouldDropPendingDedupeKeysAfterChunkFailureSoRetryCanProceed() throws Exception {
        LeadImportJob job = newJob("job-3");
        LeadImportJobChunk chunk = newChunk("chunk-1", "tenant-1", "job-3", 1);

        when(jobRepository.findByIdAndTenantId("job-3", "tenant-1")).thenReturn(Optional.of(job));
        when(jobRepository.findById("job-3")).thenReturn(Optional.of(job));
        when(jobRepository.save(any(LeadImportJob.class))).thenAnswer(inv -> inv.getArgument(0));
        when(chunkRepository.findByTenantIdAndJobIdAndChunkNo("tenant-1", "job-3", 1)).thenReturn(Optional.of(chunk));
        when(chunkRepository.save(any(LeadImportJobChunk.class))).thenAnswer(inv -> inv.getArgument(0));
        when(chunkRepository.countByTenantIdAndJobId("tenant-1", "job-3")).thenReturn(2L);
        when(chunkRepository.countByTenantIdAndJobIdAndStatus("tenant-1", "job-3", "PROCESSED")).thenReturn(0L, 1L);
        when(chunkRepository.countByTenantIdAndJobIdAndStatus("tenant-1", "job-3", "FAILED")).thenReturn(1L, 0L);
        when(chunkRepository.countByTenantIdAndJobIdAndStatus("tenant-1", "job-3", "CANCELED")).thenReturn(0L);
        when(leadRepository.findDedupeKeysByTenantId(eq("tenant-1"), any(Pageable.class)))
                .thenReturn(new PageImpl<Object[]>(Collections.<Object[]>emptyList(), PageRequest.of(0, 500), 0));
        when(leadRepository.saveAll(anyList()))
                .thenThrow(new RuntimeException("chunk save failure"))
                .thenAnswer(inv -> inv.getArgument(0));
        when(leadAssignmentService.assignOwnerForTenant("tenant-1")).thenReturn("");

        String dedupeKey = dedupeKey("Alice", "Acme", "0101234567", "alice@example.com");

        service.consumeLeadImportChunk(chunkMessage("tenant-1", "job-3", 1,
                Arrays.asList("Alice,Acme,0101234567,alice@example.com,web,,NEW"), "req-1"));

        assertFalse(cacheContainsKey("tenant-1", "job-3", dedupeKey));
        verify(itemRepository, never()).save(any(LeadImportJobItem.class));

        service.consumeLeadImportChunk(chunkMessage("tenant-1", "job-3", 1,
                Arrays.asList("Alice,Acme,0101234567,alice@example.com,web,,NEW"), "req-2"));

        assertTrue(cacheContainsKey("tenant-1", "job-3", dedupeKey));
        verify(leadRepository, times(2)).saveAll(anyList());
    }

    @Test
    void shouldNotChangeJobStatusWhenRetryHasNoPendingChunks() {
        LeadImportJob job = newJob("job-4");
        job.setStatus("FAILED");
        LeadImportJobChunk chunk = newChunk("chunk-1", "tenant-1", "job-4", 1);
        chunk.setStatus("PROCESSED");

        when(jobRepository.findByIdAndTenantId("job-4", "tenant-1")).thenReturn(Optional.of(job));
        when(jobRepository.countByTenantIdAndStatusIn(eq("tenant-1"), anyList())).thenReturn(0L);
        when(chunkRepository.findByTenantIdAndJobIdOrderByChunkNoAsc("tenant-1", "job-4"))
                .thenReturn(Collections.singletonList(chunk));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.retryJob("tenant-1", "job-4", "operator-1", "req-1")
        );

        assertEquals("lead_import_retry_no_pending_chunks", ex.getMessage());
        assertEquals("FAILED", job.getStatus());
        verify(jobRepository, never()).save(any(LeadImportJob.class));
    }

    @SuppressWarnings("unchecked")
    private Map<String, ?> getDedupeCacheMap() throws Exception {
        Field field = LeadImportService.class.getDeclaredField("dedupeKeyCaches");
        field.setAccessible(true);
        return (Map<String, ?>) field.get(service);
    }

    private boolean cacheContainsKey(String tenantId, String jobId, String key) throws Exception {
        Object cache = getDedupeCacheMap().get(tenantId + "|" + jobId);
        if (cache == null) {
            return false;
        }
        Method method = cache.getClass().getDeclaredMethod("contains", String.class);
        method.setAccessible(true);
        return (Boolean) method.invoke(cache, key);
    }

    private String dedupeKey(String name, String company, String phone, String email) {
        return (name.trim() + "|" + company.trim() + "|" + phone.trim() + "|" + email.trim()).toLowerCase(Locale.ROOT);
    }

    private LeadImportJob newJob(String jobId) {
        LeadImportJob job = new LeadImportJob();
        job.setId(jobId);
        job.setTenantId("tenant-1");
        job.setStatus("RUNNING");
        job.setTotalRows(2);
        job.setProcessedRows(0);
        job.setSuccessCount(0);
        job.setFailCount(0);
        job.setPercent(0);
        job.setCancelRequested(false);
        job.setCreatedBy("operator-1");
        job.setLastHeartbeatAt(LocalDateTime.now());
        job.setCreatedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());
        return job;
    }

    private LeadImportJobChunk newChunk(String chunkId, String tenantId, String jobId, int chunkNo) {
        LeadImportJobChunk chunk = new LeadImportJobChunk();
        chunk.setId(chunkId);
        chunk.setTenantId(tenantId);
        chunk.setJobId(jobId);
        chunk.setChunkNo(chunkNo);
        chunk.setStatus("PENDING");
        chunk.setRetryCount(0);
        return chunk;
    }

    private LeadImportChunkMessage chunkMessage(String tenantId, String jobId, int chunkNo, List<String> rows, String requestId) {
        LeadImportChunkMessage message = new LeadImportChunkMessage();
        message.setTenantId(tenantId);
        message.setJobId(jobId);
        message.setChunkNo(chunkNo);
        message.setRows(rows);
        message.setRetryCount(0);
        message.setRequestId(requestId);
        return message;
    }
}
