package com.yao.crm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yao.crm.entity.Customer;
import com.yao.crm.repository.ContactRepository;
import com.yao.crm.repository.CustomerRepository;
import com.yao.crm.repository.LeadRepository;
import com.yao.crm.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Disabled("TODO: rewrite to use mock CacheService instead of internal fields")

@ExtendWith(MockitoExtension.class)
class DataImportServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CacheService cacheService;

    private DataImportService service;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        FileParsingService fileParsingService = new FileParsingService(objectMapper);
        DataMappingService dataMappingService = new DataMappingService();
        service = new DataImportService(
                cacheService,
                fileParsingService,
                dataMappingService,
                customerRepository,
                contactRepository,
                leadRepository,
                productRepository
        );
    }

    @Test
    void shouldReleaseParsedPayloadAfterJobCompletes() throws Exception {
        String csvContent = "name,industry,phone,status\nAcme,Internet,010-12345678,ACTIVE\n";
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DataImportService.ImportJobResult result =
                service.createImportJob("tenant_1", "user123", "Customer", inputStream, "test.csv", "csv");

        assertNotNull(result.getJobId());
        assertEquals("COMPLETED", result.getStatus());

        Object jobContext = getImportJobs().get(result.getJobId());
        assertNotNull(jobContext);
        assertNull(readField(jobContext, "data"));
        assertNull(readField(jobContext, "headers"));
        assertNotNull(readField(jobContext, "finishedAt"));

        DataImportService.ImportJobResult status = service.getImportJobStatus(result.getJobId());
        assertNotNull(status);
        assertEquals(result.getJobId(), status.getJobId());
        assertEquals("COMPLETED", status.getStatus());
    }

    @Test
    void shouldCleanupExpiredTerminalJobsButKeepActiveJobs() throws Exception {
        String csvContent = "name,industry,phone,status\nAcme,Internet,010-12345678,ACTIVE\n";
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DataImportService.ImportJobResult completed =
                service.createImportJob("tenant_1", "user123", "Customer", inputStream, "test.csv", "csv");

        Object completedContext = getImportJobs().get(completed.getJobId());
        assertNotNull(completedContext);
        setField(completedContext, "finishedAt", LocalDateTime.now().minusHours(25));
        setField(completedContext, "status", DataImportService.ImportJobStatus.COMPLETED);

        DataImportService.ImportJobResult active =
                service.createImportJob("tenant_1", "user123", "Customer",
                        new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)), "test.csv", "csv");
        Object activeContext = getImportJobs().get(active.getJobId());
        assertNotNull(activeContext);
        setField(activeContext, "status", DataImportService.ImportJobStatus.RUNNING);
        setField(activeContext, "finishedAt", null);

        ((java.util.concurrent.atomic.AtomicLong) readServiceField("lastCleanupAt")).set(0L);

        assertNull(service.getImportJobStatus(completed.getJobId()));
        assertNotNull(service.getImportJobStatus(active.getJobId()));
        assertFalse(getImportJobs().containsKey(completed.getJobId()));
        assertTrue(getImportJobs().containsKey(active.getJobId()));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getImportJobs() throws Exception {
        Field field = DataImportService.class.getDeclaredField("importJobs");
        field.setAccessible(true);
        return (Map<String, Object>) field.get(service);
    }

    private Object readField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Object readServiceField(String fieldName) throws Exception {
        Field field = DataImportService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(service);
    }
}
