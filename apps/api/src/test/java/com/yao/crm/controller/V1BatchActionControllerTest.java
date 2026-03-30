package com.yao.crm.controller;

import com.yao.crm.dto.request.BatchActionRequest;
import com.yao.crm.entity.Customer;
import com.yao.crm.entity.Product;
import com.yao.crm.repository.ContractRecordRepository;
import com.yao.crm.repository.CustomerRepository;
import com.yao.crm.repository.OpportunityRepository;
import com.yao.crm.repository.OrderRecordRepository;
import com.yao.crm.repository.PaymentRecordRepository;
import com.yao.crm.repository.ProductRepository;
import com.yao.crm.repository.QuoteRepository;
import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.I18nService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class V1BatchActionControllerTest {

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private OpportunityRepository opportunityRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private QuoteRepository quoteRepository;
    @Mock
    private OrderRecordRepository orderRecordRepository;
    @Mock
    private ContractRecordRepository contractRecordRepository;
    @Mock
    private PaymentRecordRepository paymentRecordRepository;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private I18nService i18nService;

    private V1BatchActionController controller;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        controller = new V1BatchActionController(
                customerRepository,
                opportunityRepository,
                productRepository,
                quoteRepository,
                orderRecordRepository,
                contractRecordRepository,
                paymentRecordRepository,
                auditLogService,
                i18nService
        );
        when(i18nService.msg(any(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
        request = new MockHttpServletRequest();
        request.setAttribute("authRole", "MANAGER");
        request.setAttribute("authUsername", "manager-1");
        request.setAttribute("authTenantId", "tenant-1");
    }

    @Test
    void batchCustomersShouldNormalizeIdsAndOwner() {
        BatchActionRequest payload = new BatchActionRequest();
        payload.setAction(" assign_owner ");
        payload.setIds(Arrays.asList("  c-1  ", "   "));
        payload.setOwner("  alice  ");

        Customer row = new Customer();
        row.setId("c-1");
        row.setOwner("old-owner");
        when(customerRepository.findByTenantIdAndIdIn("tenant-1", Collections.singletonList("c-1")))
                .thenReturn(Collections.singletonList(row));

        ResponseEntity<?> response = controller.batchCustomers(request, payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(customerRepository).findByTenantIdAndIdIn("tenant-1", Collections.singletonList("c-1"));
        verify(customerRepository).save(argThat(saved -> "alice".equals(saved.getOwner())));
        Map<?, ?> body = bodyAsMap(response);
        assertEquals(1, body.get("requested"));
        assertEquals(1, body.get("succeeded"));
    }

    @Test
    void batchProductsShouldNormalizeIdsAndStatus() {
        BatchActionRequest payload = new BatchActionRequest();
        payload.setAction(" update_status ");
        payload.setIds(Arrays.asList("  prd-1  "));
        payload.setStatus(" active ");

        Product row = new Product();
        row.setId("prd-1");
        row.setStatus("INACTIVE");
        when(productRepository.findByTenantIdAndIdIn("tenant-1", Collections.singletonList("prd-1")))
                .thenReturn(Collections.singletonList(row));

        ResponseEntity<?> response = controller.batchProducts(request, payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(productRepository).findByTenantIdAndIdIn("tenant-1", Collections.singletonList("prd-1"));
        verify(productRepository).save(argThat(saved -> "ACTIVE".equals(saved.getStatus())));
    }

    @Test
    void batchProductsShouldUseConsistentUnsupportedActionKey() {
        BatchActionRequest payload = new BatchActionRequest();
        payload.setAction("ASSIGN_OWNER");
        payload.setIds(Collections.singletonList("  prd-1  "));
        payload.setOwner("alice");

        Product row = new Product();
        row.setId("prd-1");
        when(productRepository.findByTenantIdAndIdIn("tenant-1", Collections.singletonList("prd-1")))
                .thenReturn(Collections.singletonList(row));

        ResponseEntity<?> response = controller.batchProducts(request, payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(productRepository, never()).save(any(Product.class));
        Map<?, ?> body = bodyAsMap(response);
        assertEquals(1, body.get("skipped"));
        assertEquals(1, body.get("failed"));
        List<?> failures = (List<?>) body.get("failures");
        assertNotNull(failures);
        Map<?, ?> first = (Map<?, ?>) failures.get(0);
        assertEquals("batch_action_not_supported", first.get("message"));
    }

    @SuppressWarnings("unchecked")
    private Map<?, ?> bodyAsMap(ResponseEntity<?> response) {
        return (Map<?, ?>) response.getBody();
    }
}
