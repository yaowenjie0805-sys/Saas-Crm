package com.yao.crm.service;

import static com.yao.crm.support.TestTenant.TENANT_TEST;

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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
        when(customerRepository.findByTenantId(TENANT_TEST)).thenReturn(customers);

        DataImportExportService.ExportJobResult result =
                service.createExportJob(TENANT_TEST, "user123", "Customer", null, null, "csv");

        assertNotNull(result);
        assertNotNull(result.getJobId());
        assertEquals(2, result.getTotalRows());
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
        when(customerRepository.findByTenantId(TENANT_TEST)).thenReturn(customers);

        DataImportExportService.ExportJobResult createResult =
                service.createExportJob(TENANT_TEST, "user123", "Customer", null, null, "csv");

        DataImportExportService.ExportJobResult status = service.getExportJobStatus(createResult.getJobId());

        assertNotNull(status);
        assertEquals(createResult.getJobId(), status.getJobId());
    }

    @Test
    void testGetExportJobStatus_NotFound() {
        DataImportExportService.ExportJobResult status = service.getExportJobStatus("nonexistent_job");
        assertNull(status);
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

    private Customer createCustomer(String id, String name) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setName(name);
        customer.setTenantId(TENANT_TEST);
        return customer;
    }
}

