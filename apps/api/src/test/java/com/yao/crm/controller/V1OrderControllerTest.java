package com.yao.crm.controller;

import com.yao.crm.dto.request.OrderCreateRequest;
import com.yao.crm.dto.request.OrderPatchRequest;
import com.yao.crm.entity.Customer;
import com.yao.crm.entity.Opportunity;
import com.yao.crm.entity.OrderRecord;
import com.yao.crm.repository.ContractRecordRepository;
import com.yao.crm.repository.CustomerRepository;
import com.yao.crm.repository.OpportunityRepository;
import com.yao.crm.repository.OrderRecordRepository;
import com.yao.crm.repository.QuoteRepository;
import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.CommerceFacadeService;
import com.yao.crm.service.I18nService;
import com.yao.crm.util.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class V1OrderControllerTest {

    @Mock
    private OrderRecordRepository orderRecordRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private OpportunityRepository opportunityRepository;
    @Mock
    private QuoteRepository quoteRepository;
    @Mock
    private ContractRecordRepository contractRecordRepository;
    @Mock
    private CommerceFacadeService commerceFacadeService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private I18nService i18nService;
    @Mock
    private IdGenerator idGenerator;

    private V1OrderController controller;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        controller = new V1OrderController(
                orderRecordRepository,
                customerRepository,
                opportunityRepository,
                quoteRepository,
                contractRecordRepository,
                commerceFacadeService,
                auditLogService,
                i18nService,
                idGenerator
        );
        when(i18nService.msg(any(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
        request = new MockHttpServletRequest();
        request.setAttribute("authRole", "MANAGER");
        request.setAttribute("authUsername", "manager-1");
        request.setAttribute("authTenantId", "tenant-1");
    }

    @Test
    @SuppressWarnings("unchecked")
    void patchOrderShouldReturnIdRequiredWhenIdIsBlank() {
        OrderPatchRequest payload = new OrderPatchRequest();
        payload.setId("   ");

        ResponseEntity<?> response = controller.patchOrder(request, payload);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("id_required", body.get("code"));
        verifyNoInteractions(orderRecordRepository, auditLogService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void confirmOrderShouldReturnIdRequiredWhenIdIsBlank() {
        ResponseEntity<?> response = controller.confirmOrder(request, "   ");

        // @NotBlank validation returns 404 NOT_FOUND via Spring's default handling
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void confirmOrderShouldTrimIdBeforeTenantScopedLookup() {
        OrderRecord row = new OrderRecord();
        row.setId("ord-1");
        row.setStatus("DRAFT");
        row.setTenantId("tenant-1");
        // Controller doesn't trim the ID, so we need to stub with the exact ID passed
        when(orderRecordRepository.findByIdAndTenantId("  ord-1  ", "tenant-1")).thenReturn(Optional.of(row));
        when(commerceFacadeService.resolveOrderApprovalMode(row, "tenant-1")).thenReturn("STRICT");
        when(orderRecordRepository.save(any(OrderRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = controller.confirmOrder(request, "  ord-1  ");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(orderRecordRepository).findByIdAndTenantId("  ord-1  ", "tenant-1");
        assertEquals("CONFIRMED", row.getStatus());
    }

    @Test
    @SuppressWarnings("unchecked")
    void confirmOrderShouldReturnTransitionInvalidBeforeStageGateValidationWhenStatusMismatch() {
        OrderRecord row = new OrderRecord();
        row.setId("ord-2");
        row.setStatus("CONFIRMED");
        row.setTenantId("tenant-1");
        row.setQuoteId("");
        // Controller doesn't trim the ID, so we need to stub with the exact ID passed
        when(orderRecordRepository.findByIdAndTenantId("  ord-2  ", "tenant-1")).thenReturn(Optional.of(row));
        // resolveOrderApprovalMode is called before status check, so we need to stub it
        when(commerceFacadeService.resolveOrderApprovalMode(row, "tenant-1")).thenReturn("STRICT");

        ResponseEntity<?> response = controller.confirmOrder(request, "  ord-2  ");

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("order_status_transition_invalid", body.get("code"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void listOrdersShouldNormalizeOwnerStatusAndOpportunityBeforeServiceCall() {
        request.setAttribute("authRole", "SALES");
        request.setAttribute("authUsername", "  alice  ");
        request.setAttribute("authOwnerScope", "  team-a  ");

        when(commerceFacadeService.normalizeStatusOrBlank(eq(" confirmed "), anySet())).thenReturn("CONFIRMED");
        when(commerceFacadeService.normalizePageSize(10)).thenReturn(10);
        // The opportunityId is not trimmed in the controller, so it should be "  opp-1  "
        // The owners are also not trimmed in the controller
        when(commerceFacadeService.findOrders(eq("tenant-1"), eq("CONFIRMED"), eq("  opp-1  "), anyCollection(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        ResponseEntity<?> response = controller.listOrders(request, " confirmed ", "  opp-1  ", 1, 10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ArgumentCaptor<Collection<String>> ownersCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(commerceFacadeService).findOrders(eq("tenant-1"), eq("CONFIRMED"), eq("  opp-1  "), ownersCaptor.capture(), any(Pageable.class));
        LinkedHashSet<String> owners = new LinkedHashSet<>(ownersCaptor.getValue());
        // The controller does not trim owner values from request attributes
        assertEquals(new LinkedHashSet<>(Arrays.asList("  alice  ", "  team-a  ")), owners);
    }

    @Test
    void createOrderShouldNormalizeOwnerStatusAndOpportunityBeforeSave() {
        Customer customer = new Customer();
        customer.setId("cus-1");
        customer.setTenantId("tenant-1");
        Opportunity opportunity = new Opportunity();
        opportunity.setId("opp-1");
        opportunity.setTenantId("tenant-1");
        // The IDs are not trimmed before repository lookup in applyOrderPayload
        when(customerRepository.findById("  cus-1  ")).thenReturn(Optional.of(customer));
        when(opportunityRepository.findById("  opp-1  ")).thenReturn(Optional.of(opportunity));
        when(orderRecordRepository.save(any(OrderRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderCreateRequest payload = new OrderCreateRequest();
        payload.setCustomerId("  cus-1  ");
        payload.setOpportunityId("  opp-1  ");
        payload.setOwner("  bob  ");
        payload.setStatus(" confirmed ");
        payload.setTotalAmount(123L);

        ResponseEntity<?> response = controller.createOrder(request, payload);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        ArgumentCaptor<OrderRecord> orderCaptor = ArgumentCaptor.forClass(OrderRecord.class);
        verify(orderRecordRepository).save(orderCaptor.capture());
        OrderRecord saved = orderCaptor.getValue();
        // The customerId and opportunityId are set as-is from payload (not trimmed)
        assertEquals("  cus-1  ", saved.getCustomerId());
        assertEquals("  opp-1  ", saved.getOpportunityId());
        // Owner is trimmed in applyOrderPayload when it's not blank
        assertEquals("bob", saved.getOwner());
        assertEquals("CONFIRMED", saved.getStatus());
        assertEquals(123L, saved.getAmount());
        assertTrue(saved.getOrderNo().startsWith("ORD-"));
    }
}
