package com.yao.crm.controller;

import com.yao.crm.entity.Customer;
import com.yao.crm.entity.Opportunity;
import com.yao.crm.repository.ApprovalInstanceRepository;
import com.yao.crm.repository.ContractRecordRepository;
import com.yao.crm.repository.CustomerRepository;
import com.yao.crm.repository.FollowUpRepository;
import com.yao.crm.repository.OpportunityRepository;
import com.yao.crm.repository.OrderRecordRepository;
import com.yao.crm.repository.PaymentRecordRepository;
import com.yao.crm.repository.QuoteRepository;
import com.yao.crm.repository.TaskRepository;
import com.yao.crm.service.DashboardMetricsCacheService;
import com.yao.crm.service.I18nService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static com.yao.crm.support.TestTenant.TENANT_TEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class V1SalesInsightControllerTest {

    private TaskRepository taskRepository;
    private FollowUpRepository followUpRepository;
    private ContractRecordRepository contractRecordRepository;
    private ApprovalInstanceRepository approvalInstanceRepository;
    private PaymentRecordRepository paymentRecordRepository;
    private CustomerRepository customerRepository;
    private OpportunityRepository opportunityRepository;
    private QuoteRepository quoteRepository;
    private OrderRecordRepository orderRecordRepository;
    private DashboardMetricsCacheService dashboardMetricsCacheService;
    private V1SalesInsightController controller;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        taskRepository = mock(TaskRepository.class);
        followUpRepository = mock(FollowUpRepository.class);
        contractRecordRepository = mock(ContractRecordRepository.class);
        approvalInstanceRepository = mock(ApprovalInstanceRepository.class);
        paymentRecordRepository = mock(PaymentRecordRepository.class);
        customerRepository = mock(CustomerRepository.class);
        opportunityRepository = mock(OpportunityRepository.class);
        quoteRepository = mock(QuoteRepository.class);
        orderRecordRepository = mock(OrderRecordRepository.class);
        dashboardMetricsCacheService = mock(DashboardMetricsCacheService.class);
        I18nService i18nService = mock(I18nService.class);
        when(i18nService.msg(any(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));

        controller = new V1SalesInsightController(
                taskRepository,
                followUpRepository,
                contractRecordRepository,
                approvalInstanceRepository,
                paymentRecordRepository,
                customerRepository,
                opportunityRepository,
                quoteRepository,
                orderRecordRepository,
                dashboardMetricsCacheService,
                i18nService
        );

        request = new MockHttpServletRequest();
        request.setAttribute("authRole", "ADMIN");
        request.setAttribute("authUsername", "alice");
        request.setAttribute("authTenantId", TENANT_TEST);
    }

    @Test
    @SuppressWarnings("unchecked")
    void workbenchTodayShouldRejectInvalidDateFormat() {
        ResponseEntity<?> response = controller.workbenchToday(
                request,
                "2026-13-01",
                "",
                "",
                "",
                ""
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("invalid_date_format", body.get("code"));
        verifyNoInteractions(dashboardMetricsCacheService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void workbenchTodayShouldRejectDescendingDateRange() {
        ResponseEntity<?> response = controller.workbenchToday(
                request,
                "2026-03-31",
                "2026-03-01",
                "",
                "",
                ""
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("date_range_invalid", body.get("code"));
        verifyNoInteractions(dashboardMetricsCacheService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void workbenchTodayShouldTrimFiltersAndUseInclusiveDayBoundaries() {
        when(dashboardMetricsCacheService.getOrLoad(eq(TENANT_TEST), eq("workbench-today"), anyString(), any()))
                .thenAnswer(invocation -> {
                    Supplier<Map<String, Object>> loader = invocation.getArgument(3);
                    return new DashboardMetricsCacheService.CachedValue<Map<String, Object>>(loader.get(), false, "LOCAL", false);
                });
        when(taskRepository.countByTenantIdAndDoneFalseAndUpdatedAtBetween(eq(TENANT_TEST), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(taskRepository.findTop200ByTenantIdAndDoneAndUpdatedAtBetweenOrderByUpdatedAtDesc(eq(TENANT_TEST), eq(false), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(followUpRepository.findByTenantIdAndCreatedAtBetween(eq(TENANT_TEST), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(contractRecordRepository.findByTenantIdAndCreatedAtBetween(eq(TENANT_TEST), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(approvalInstanceRepository.findByTenantIdAndStatusInAndUpdatedAtBetweenOrderByCreatedAtDesc(
                eq(TENANT_TEST),
                anyCollection(),
                any(LocalDateTime.class),
                any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(paymentRecordRepository.findByTenantIdAndCreatedAtBetween(eq(TENANT_TEST), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        ResponseEntity<?> response = controller.workbenchToday(
                request,
                " 2026-03-01 ",
                " 2026-03-31 ",
                "   ",
                "  north  ",
                "  Asia/Shanghai  "
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(taskRepository).countByTenantIdAndDoneFalseAndUpdatedAtBetween(
                TENANT_TEST,
                LocalDateTime.of(2026, 3, 1, 0, 0, 0),
                LocalDateTime.of(2026, 3, 31, 23, 59, 59)
        );
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("2026-03-01", body.get("from"));
        assertEquals("2026-03-31", body.get("to"));
        assertEquals("Asia/Shanghai", body.get("timezone"));
        assertEquals("north", body.get("department"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void customerTimelineShouldRejectBlankPathId() {
        ResponseEntity<?> response = controller.customerTimeline(request, "   ");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("bad_request", body.get("code"));
        verifyNoInteractions(customerRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void customerTimelineShouldTrimPathIdBeforeLookup() {
        Customer customer = new Customer();
        customer.setId("c_1");
        customer.setOwner("alice");
        when(customerRepository.findByIdAndTenantId("c_1", TENANT_TEST)).thenReturn(Optional.of(customer));
        when(dashboardMetricsCacheService.getOrLoad(eq(TENANT_TEST), eq("customer-timeline"), anyString(), any()))
                .thenAnswer(invocation -> new DashboardMetricsCacheService.CachedValue<Object>(Collections.emptyList(), false, "LOCAL", false));

        ResponseEntity<?> response = controller.customerTimeline(request, "  c_1  ");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(customerRepository).findByIdAndTenantId("c_1", TENANT_TEST);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("c_1", body.get("customerId"));
    }

    @Test
    void customerTimelineShouldTrimTenantBeforeLookup() {
        request.setAttribute("authTenantId", "  " + TENANT_TEST + "  ");
        Customer customer = new Customer();
        customer.setId("c_1");
        customer.setOwner("alice");
        when(customerRepository.findByIdAndTenantId("c_1", TENANT_TEST)).thenReturn(Optional.of(customer));
        when(dashboardMetricsCacheService.getOrLoad(eq(TENANT_TEST), eq("customer-timeline"), anyString(), any()))
                .thenAnswer(invocation -> new DashboardMetricsCacheService.CachedValue<Object>(Collections.emptyList(), false, "LOCAL", false));

        ResponseEntity<?> response = controller.customerTimeline(request, "c_1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(customerRepository).findByIdAndTenantId("c_1", TENANT_TEST);
    }

    @Test
    @SuppressWarnings("unchecked")
    void opportunityTimelineShouldTrimPathIdBeforeLookup() {
        Opportunity opportunity = new Opportunity();
        opportunity.setId("o_1");
        opportunity.setTenantId(TENANT_TEST);
        opportunity.setOwner("alice");
        when(opportunityRepository.findById("o_1")).thenReturn(Optional.of(opportunity));
        when(quoteRepository.findByTenantIdAndOpportunityId(TENANT_TEST, "o_1")).thenReturn(Collections.emptyList());
        when(orderRecordRepository.findByTenantIdAndOpportunityId(TENANT_TEST, "o_1")).thenReturn(Collections.emptyList());
        when(dashboardMetricsCacheService.getOrLoad(eq(TENANT_TEST), eq("opportunity-timeline"), anyString(), any()))
                .thenAnswer(invocation -> new DashboardMetricsCacheService.CachedValue<Object>(Collections.emptyList(), false, "LOCAL", false));

        ResponseEntity<?> response = controller.opportunityTimeline(request, "  o_1  ");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(opportunityRepository).findById("o_1");
        verify(approvalInstanceRepository, never())
                .findByTenantIdAndBizTypeIgnoreCaseAndBizIdInOrderByCreatedAtDesc(anyString(), anyString(), any());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("o_1", body.get("opportunityId"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void opportunityTimelineShouldRejectBlankPathId() {
        ResponseEntity<?> response = controller.opportunityTimeline(request, "  ");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("bad_request", body.get("code"));
        verify(opportunityRepository, never()).findById(anyString());
    }
}
