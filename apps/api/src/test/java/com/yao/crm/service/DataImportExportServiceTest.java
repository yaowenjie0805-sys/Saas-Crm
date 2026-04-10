package com.yao.crm.service;

import static com.yao.crm.support.TestTenant.TENANT_TEST;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yao.crm.entity.Customer;
import com.yao.crm.repository.ContactRepository;
import com.yao.crm.repository.CustomerRepository;
import com.yao.crm.repository.LeadRepository;
import com.yao.crm.repository.ProductRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataImportExportServiceTest {

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

    private DataImportExportService service;

    @BeforeEach
    void setUp() {
        final Map<String, Object> exportJobStore = new ConcurrentHashMap<>();
        lenient().doAnswer(invocation -> {
            String jobId = invocation.getArgument(0);
            Object context = invocation.getArgument(1);
            exportJobStore.put(jobId, context);
            return null;
        }).when(cacheService).setExportJobContext(any(), any());
        lenient().when(cacheService.getExportJobContext(any(), any(Class.class)))
                .thenAnswer(invocation -> Optional.ofNullable(exportJobStore.get(invocation.getArgument(0))));

        ObjectMapper objectMapper = new ObjectMapper();
        FileParsingService fileParsingService = new FileParsingService(objectMapper);
        DataMappingService dataMappingService = new DataMappingService();
        DataImportService dataImportService = new DataImportService(
                cacheService,
                fileParsingService,
                dataMappingService,
                customerRepository,
                contactRepository,
                leadRepository,
                productRepository
        );
        DataExportService dataExportService = new DataExportService(
                cacheService,
                objectMapper,
                dataMappingService,
                customerRepository,
                contactRepository,
                leadRepository,
                productRepository
        );
        service = new DataImportExportService(
                dataImportService,
                dataExportService,
                dataMappingService
        );
    }

    @Test
    @Disabled("TODO: rewrite to use mock CacheService for async behavior")
    void testCreateImportJob_CSV_Success() {
        String csvContent = "name,industry,phone,status\nAcme,Internet,010-12345678,ACTIVE\nBee,Finance,010-87654321,ACTIVE\n";
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DataImportExportService.ImportJobResult result =
                service.createImportJob(TENANT_TEST, "user123", "Customer", inputStream, "test.csv", "csv");

        assertNotNull(result);
        assertNotNull(result.getJobId());
        assertEquals(2, result.getTotalRows());
    }

    @Test
    @Disabled("TODO: rewrite to use mock CacheService for async behavior")
    void testCreateImportJob_InvalidEntityType() {
        String csvContent = "name,email\na,b@example.com";
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));

        DataImportExportService.ImportJobResult result =
                service.createImportJob(TENANT_TEST, "user123", "InvalidType", inputStream, "test.csv", "csv");

        assertNotNull(result);
        assertEquals("FAILED", result.getStatus());
    }

    @Test
    @Disabled("TODO: rewrite to use mock CacheService for async behavior")
    void testGetImportJobStatus() {
        String csvContent = "name,industry\nAcme,Internet\n";
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DataImportExportService.ImportJobResult createResult =
                service.createImportJob(TENANT_TEST, "user123", "Customer", inputStream, "test.csv", "csv");

        DataImportExportService.ImportJobResult status = service.getImportJobStatus(createResult.getJobId());

        assertNotNull(status);
        assertEquals(createResult.getJobId(), status.getJobId());
    }

    @Test
    void testGetImportJobStatus_NotFound() {
        DataImportExportService.ImportJobResult status = service.getImportJobStatus("nonexistent_job");
        assertNull(status);
    }

    @Test
    void testImportJobResultShouldExposeFailCountForSerialization() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        DataImportExportService.ImportJobResult result =
                new DataImportExportService.ImportJobResult("job-1", "COMPLETED", 10, 10, 8, 2);

        Method getter = DataImportExportService.ImportJobResult.class.getDeclaredMethod("getFailCount");
        assertNotNull(getter);
        assertEquals(2, result.getFailCount());

        String json = objectMapper.writeValueAsString(result);

        assertTrue(json.contains("\"failCount\":2"));
    }

    @Test
    @Disabled("TODO: rewrite to use mock CacheService for async behavior")
    void testCancelImportJob() {
        String csvContent = "name,industry\nAcme,Internet\n";
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DataImportExportService.ImportJobResult createResult =
                service.createImportJob(TENANT_TEST, "user123", "Customer", inputStream, "test.csv", "csv");

        service.cancelImportJob(createResult.getJobId());
        DataImportExportService.ImportJobResult status = service.getImportJobStatus(createResult.getJobId());

        assertTrue("CANCELLED".equals(status.getStatus()) || "COMPLETED".equals(status.getStatus()));
    }

    @Test
    void testEntityTypeFromCode() {
        assertEquals(DataImportExportService.EntityType.CUSTOMER,
                DataImportExportService.EntityType.fromCode("Customer"));
        assertEquals(DataImportExportService.EntityType.CONTACT,
                DataImportExportService.EntityType.fromCode("Contact"));
        assertEquals(DataImportExportService.EntityType.LEAD,
                DataImportExportService.EntityType.fromCode("Lead"));
        assertEquals(DataImportExportService.EntityType.PRODUCT,
                DataImportExportService.EntityType.fromCode("Product"));
    }

    @Test
    void testEntityTypeFromCode_Invalid() {
        assertThrows(IllegalArgumentException.class, () ->
                DataImportExportService.EntityType.fromCode("InvalidType"));
    }

    @Test
    void testCreateExportJob_Customer() {
        List<Customer> customers = Arrays.asList(
                createCustomer("cust_1", "CompanyA"),
                createCustomer("cust_2", "CompanyB")
        );
        stubCustomerPagedQuery(customers);

        DataImportExportService.ExportJobResult result =
                service.createExportJob(TENANT_TEST, "user123", "Customer", null, null, "csv");

        assertNotNull(result);
        assertNotNull(result.getJobId());
        assertEquals(2, result.getTotalRows());
    }

    @Test
    void testCreateExportJob_ShouldDefaultToCsvWhenFormatMissing() {
        List<Customer> customers = Arrays.asList(createCustomer("cust_1", "CompanyA"));
        stubCustomerPagedQuery(customers);

        DataImportExportService.ExportJobResult result =
                service.createExportJob(TENANT_TEST, "user123", "Customer", null, null, null);

        assertNotNull(result);
        assertNotNull(result.getJobId());
        assertEquals("COMPLETED", result.getStatus());
        assertEquals(1, result.getTotalRows());
    }

    @Test
    void testCreateExportJob_ShouldFailWhenFormatUnsupported() {
        DataImportExportService.ExportJobResult result =
                service.createExportJob(TENANT_TEST, "user123", "Customer", null, null, "xml");

        assertNotNull(result);
        assertEquals("FAILED", result.getStatus());
        assertNull(result.getJobId());
        assertTrue(result.getErrorMessage() != null && result.getErrorMessage().contains("unsupported_export_format"));
    }

    @Test
    void testCreateExportJob_InvalidEntityType() {
        DataImportExportService.ExportJobResult result =
                service.createExportJob(TENANT_TEST, "user123", "InvalidType", null, null, "csv");

        assertNotNull(result);
        assertEquals("FAILED", result.getStatus());
    }

    @Test
    @Disabled("TODO: rewrite to use mock CacheService for async behavior")
    void testGetExportJobStatus() {
        List<Customer> customers = Arrays.asList(createCustomer("cust_1", "CompanyA"));
        stubCustomerPagedQuery(customers);

        DataImportExportService.ExportJobResult createResult =
                service.createExportJob(TENANT_TEST, "user123", "Customer", null, null, "csv");

        DataImportExportService.ExportJobResult status = service.getExportJobStatus(
                TENANT_TEST,
                "user123",
                createResult.getJobId(),
                false
        );

        assertNotNull(status);
        assertEquals(createResult.getJobId(), status.getJobId());
    }

    @Test
    void testGetExportJobStatus_NotFound() {
        DataImportExportService.ExportJobResult status = service.getExportJobStatus(
                TENANT_TEST,
                "user123",
                "nonexistent_job",
                false
        );
        assertNull(status);
    }

    @Test
    void testGetExportJobStatus_ShouldRejectCrossTenantAccess() {
        List<Customer> customers = Arrays.asList(createCustomer("cust_1", "CompanyA"));
        stubCustomerPagedQuery(customers);

        DataImportExportService.ExportJobResult createResult =
                service.createExportJob(TENANT_TEST, "user123", "Customer", null, null, "csv");

        assertThrows(IllegalArgumentException.class, () ->
                service.getExportJobStatus("tenant_other", "user123", createResult.getJobId(), false));
    }

    @Test
    void testGetExportJobStatus_ShouldRejectNonOwnerWithoutPrivilege() {
        List<Customer> customers = Arrays.asList(createCustomer("cust_1", "CompanyA"));
        stubCustomerPagedQuery(customers);

        DataImportExportService.ExportJobResult createResult =
                service.createExportJob(TENANT_TEST, "ownerA", "Customer", null, null, "csv");

        assertThrows(IllegalArgumentException.class, () ->
                service.getExportJobStatus(TENANT_TEST, "ownerB", createResult.getJobId(), false));
    }

    @Test
    void testGetImportTemplate() throws Exception {
        byte[] template = service.getImportTemplate("Customer", "csv");

        assertNotNull(template);
        String content = new String(template, StandardCharsets.UTF_8);
        assertFalse(content.trim().isEmpty());
        assertTrue(content.contains(","));
    }

    @Test
    void testGetImportTemplate_InvalidEntityType() {
        assertThrows(IllegalArgumentException.class, () ->
                service.getImportTemplate("InvalidType", "csv"));
    }

    @Test
    void testGetImportTemplate_ShouldRejectUnsupportedFormat() {
        assertThrows(IllegalArgumentException.class, () ->
                service.getImportTemplate("Customer", "xml"));
    }

    @Test
    void testCreateExportJob_ShouldReleaseCachedDataAfterCompletion() throws Exception {
        List<Customer> customers = Arrays.asList(
                createCustomer("cust_1", "CompanyA"),
                createCustomer("cust_2", "CompanyB")
        );
        stubCustomerPagedQuery(customers);

        DataImportExportService.ExportJobResult result =
                service.createExportJob(TENANT_TEST, "user123", "Customer", null, null, "csv");

        Object context = cacheService.getExportJobContext(result.getJobId(), Object.class).orElse(null);
        assertNotNull(context);
        Method getData = context.getClass().getDeclaredMethod("getData");
        Object data = getData.invoke(context);
        assertNull(data);
    }

    @Test
    void testCreateExportJob_ShouldReleaseCachedFieldsAfterCompletion() throws Exception {
        List<Customer> customers = Arrays.asList(createCustomer("cust_1", "CompanyA"));
        stubCustomerPagedQuery(customers);

        DataImportExportService.ExportJobResult result =
                service.createExportJob(TENANT_TEST, "user123", "Customer", null, null, "csv");

        Object context = cacheService.getExportJobContext(result.getJobId(), Object.class).orElse(null);
        assertNotNull(context);
        Method getFields = context.getClass().getDeclaredMethod("getFields");
        Object fields = getFields.invoke(context);
        assertNull(fields);
    }

    @Test
    void testGetExportFile_ShouldClearCachedFilePathAfterDownload() throws Exception {
        List<Customer> customers = Arrays.asList(createCustomer("cust_1", "CompanyA"));
        stubCustomerPagedQuery(customers);

        DataImportExportService.ExportJobResult result =
                service.createExportJob(TENANT_TEST, "user123", "Customer", null, null, "csv");

        byte[] bytes = service.getExportFile(TENANT_TEST, "user123", result.getJobId(), false);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        Object context = cacheService.getExportJobContext(result.getJobId(), Object.class).orElse(null);
        assertNotNull(context);
        Method getFilePath = context.getClass().getDeclaredMethod("getFilePath");
        Object filePath = getFilePath.invoke(context);
        assertNull(filePath);
    }

    @Test
    void testGetExportFile_ShouldClearStaleCachedFilePathWhenFileMissing() throws Exception {
        List<Customer> customers = Arrays.asList(createCustomer("cust_1", "CompanyA"));
        stubCustomerPagedQuery(customers);

        DataImportExportService.ExportJobResult result =
                service.createExportJob(TENANT_TEST, "user123", "Customer", null, null, "csv");

        Object context = cacheService.getExportJobContext(result.getJobId(), Object.class).orElse(null);
        assertNotNull(context);
        Method setFilePath = context.getClass().getDeclaredMethod("setFilePath", String.class);
        setFilePath.invoke(context, "C:\\\\temp\\\\not-exists-export.csv");
        cacheService.setExportJobContext(result.getJobId(), context);

        byte[] bytes = service.getExportFile(TENANT_TEST, "user123", result.getJobId(), false);
        assertNull(bytes);

        Object refreshed = cacheService.getExportJobContext(result.getJobId(), Object.class).orElse(null);
        assertNotNull(refreshed);
        Method getFilePath = refreshed.getClass().getDeclaredMethod("getFilePath");
        Object filePath = getFilePath.invoke(refreshed);
        assertNull(filePath);
    }

    @Test
    void testGetExportJobStatus_ShouldClearStaleFilePathWhenFileMissing() throws Exception {
        List<Customer> customers = Arrays.asList(createCustomer("cust_1", "CompanyA"));
        stubCustomerPagedQuery(customers);

        DataImportExportService.ExportJobResult result =
                service.createExportJob(TENANT_TEST, "user123", "Customer", null, null, "csv");

        Object context = cacheService.getExportJobContext(result.getJobId(), Object.class).orElse(null);
        assertNotNull(context);
        Method setFilePath = context.getClass().getDeclaredMethod("setFilePath", String.class);
        setFilePath.invoke(context, "C:\\\\temp\\\\not-exists-export-status.csv");
        cacheService.setExportJobContext(result.getJobId(), context);

        DataImportExportService.ExportJobResult status = service.getExportJobStatus(
                TENANT_TEST, "user123", result.getJobId(), false
        );
        assertNotNull(status);
        assertNull(status.getFilePath());
    }

    @Test
    void testGetExportJobStatus_ShouldClearInvalidFilePath() throws Exception {
        List<Customer> customers = Arrays.asList(createCustomer("cust_1", "CompanyA"));
        stubCustomerPagedQuery(customers);

        DataImportExportService.ExportJobResult result =
                service.createExportJob(TENANT_TEST, "user123", "Customer", null, null, "csv");

        Object context = cacheService.getExportJobContext(result.getJobId(), Object.class).orElse(null);
        assertNotNull(context);
        Method setFilePath = context.getClass().getDeclaredMethod("setFilePath", String.class);
        setFilePath.invoke(context, "bad\u0000path.csv");
        cacheService.setExportJobContext(result.getJobId(), context);

        DataImportExportService.ExportJobResult status = service.getExportJobStatus(
                TENANT_TEST, "user123", result.getJobId(), false
        );
        assertNotNull(status);
        assertNull(status.getFilePath());
    }

    private Customer createCustomer(String id, String name) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setName(name);
        customer.setTenantId(TENANT_TEST);
        return customer;
    }

    private void stubCustomerPagedQuery(List<Customer> customers) {
        when(customerRepository.findByTenantId(TENANT_TEST, PageRequest.of(0, 500)))
                .thenReturn(new PageImpl<>(customers, PageRequest.of(0, 500), customers.size()));
    }
}

