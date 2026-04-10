package com.yao.crm.service;

import static com.yao.crm.support.TestTenant.TENANT_TEST;

import com.yao.crm.repository.CustomerRepository;
import com.yao.crm.repository.FollowUpRepository;
import com.yao.crm.repository.LeadRepository;
import com.yao.crm.repository.OpportunityRepository;
import com.yao.crm.repository.OrderRecordRepository;
import com.yao.crm.repository.PaymentRecordRepository;
import com.yao.crm.repository.QuoteRepository;
import com.yao.crm.repository.TaskRepository;
import com.yao.crm.repository.UserAccountRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportServiceTest {
    private static final String CACHE_INVALIDATION_MODE = TENANT_TEST.replace("_test", "");

    @Test
    void shouldUseScopedFastPathWhenNoDateAndScopedOwnerPresent() {
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        OpportunityRepository opportunityRepository = mock(OpportunityRepository.class);
        TaskRepository taskRepository = mock(TaskRepository.class);
        FollowUpRepository followUpRepository = mock(FollowUpRepository.class);
        QuoteRepository quoteRepository = mock(QuoteRepository.class);
        OrderRecordRepository orderRecordRepository = mock(OrderRecordRepository.class);
        PaymentRecordRepository paymentRecordRepository = mock(PaymentRecordRepository.class);
        UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
        LeadRepository leadRepository = mock(LeadRepository.class);
        ValueNormalizerService valueNormalizerService = mock(ValueNormalizerService.class);
        DashboardMetricsCacheService cacheService = mock(DashboardMetricsCacheService.class);
        ReportAggregationService reportAggregationService = mock(ReportAggregationService.class);

        when(userAccountRepository.findIdentityPairsByTenantIdAndRoleIgnoreCase(TENANT_TEST, "SALES"))
                .thenReturn(Collections.singletonList(new Object[]{"alice", "Alice"}));
        when(userAccountRepository.findIdentityPairsByTenantIdAndDepartmentIgnoreCase(TENANT_TEST, "bd"))
                .thenReturn(Collections.singletonList(new Object[]{"alice", "Alice"}));

        when(customerRepository.countByTenantIdAndOwnerIn(anyString(), anyCollection())).thenReturn(2L);
        when(customerRepository.sumValueByTenantIdAndOwnerIn(anyString(), anyCollection())).thenReturn(300L);
        when(customerRepository.countByOwnerGroupedAndOwnerIn(anyString(), anyCollection()))
                .thenReturn(Collections.singletonList(new Object[]{"Alice", 2L}));
        when(customerRepository.sumValueByStatusGroupedAndOwnerIn(anyString(), anyCollection()))
                .thenReturn(Collections.singletonList(new Object[]{"Active", 300L}));

        when(opportunityRepository.countByTenantIdAndOwnerIn(anyString(), anyCollection())).thenReturn(1L);
        when(opportunityRepository.sumWeightedAmountRawByTenantIdAndOwnerIn(anyString(), anyCollection())).thenReturn(5000L);
        when(opportunityRepository.countByTenantIdAndOwnerInAndProgressGte(anyString(), anyCollection(), anyInt())).thenReturn(1L);
        when(opportunityRepository.countByStageGroupedAndOwnerIn(anyString(), anyCollection()))
                .thenReturn(Collections.singletonList(new Object[]{"Proposal", 1L}));

        when(taskRepository.countByTenantIdAndDoneTrueAndOwnerIn(anyString(), anyCollection())).thenReturn(1L);
        when(taskRepository.countByTenantIdAndDoneFalseAndOwnerIn(anyString(), anyCollection())).thenReturn(1L);

        when(followUpRepository.countByTenantIdAndAuthorIn(anyString(), anyCollection())).thenReturn(1L);
        when(followUpRepository.countByChannelGroupedAndAuthorIn(anyString(), anyCollection()))
                .thenReturn(Collections.singletonList(new Object[]{"phone", 1L}));
        when(valueNormalizerService.normalizeFollowUpChannel("phone")).thenReturn("phone");

        when(quoteRepository.countByTenantIdAndOwnerIn(anyString(), anyCollection())).thenReturn(2L);
        when(quoteRepository.countByTenantIdAndOwnerInAndStatusInUppercase(anyString(), anyCollection(), anyList()))
                .thenReturn(1L);
        when(quoteRepository.countByStatusGroupedAndOwnerIn(anyString(), anyCollection()))
                .thenReturn(Collections.singletonList(new Object[]{"APPROVED", 1L}));

        when(orderRecordRepository.countByTenantIdAndOwnerIn(anyString(), anyCollection())).thenReturn(1L);
        when(orderRecordRepository.countByTenantIdAndOwnerInAndStatus(anyString(), anyCollection(), anyString())).thenReturn(1L);
        when(orderRecordRepository.sumAmountByTenantIdAndOwnerIn(anyString(), anyCollection())).thenReturn(200L);
        when(orderRecordRepository.countByStatusGroupedAndOwnerIn(anyString(), anyCollection()))
                .thenReturn(Collections.singletonList(new Object[]{"COMPLETED", 1L}));

        when(paymentRecordRepository.sumAmountByTenantIdAndOwnerInAndStatusInUppercase(anyString(), anyCollection(), anyList()))
                .thenReturn(150L);
        Map<String, Object> scopedSummary = new java.util.HashMap<String, Object>();
        scopedSummary.put("customers", 2L);
        scopedSummary.put("revenue", 300L);
        Map<String, Object> scopedBody = new java.util.HashMap<String, Object>();
        scopedBody.put("summary", scopedSummary);
        when(reportAggregationService.aggregateWithScope(eq(TENANT_TEST), anySet())).thenReturn(scopedBody);

        ReportService reportService = new ReportService(
                customerRepository,
                opportunityRepository,
                taskRepository,
                followUpRepository,
                quoteRepository,
                orderRecordRepository,
                paymentRecordRepository,
                userAccountRepository,
                leadRepository,
                valueNormalizerService,
                cacheService,
                reportAggregationService
        );

        Map<String, Object> report = reportService.overviewByTenant(
                TENANT_TEST,
                null,
                null,
                "SALES",
                "alice",
                "BD"
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) report.get("summary");
        Assertions.assertNotNull(summary);
        Assertions.assertEquals(2L, summary.get("customers"));
        Assertions.assertEquals(300L, summary.get("revenue"));

        verify(reportAggregationService).aggregateWithScope(eq(TENANT_TEST), anySet());
        verify(customerRepository, never()).findByTenantId(anyString());
        verify(customerRepository, never()).countByTenantIdAndOwnerIn(anyString(), anyCollection());
        verify(customerRepository, never()).findByTenantIdAndOwnerIn(anyString(), anyCollection());
        verify(userAccountRepository).findIdentityPairsByTenantIdAndRoleIgnoreCase(TENANT_TEST, "SALES");
        verify(userAccountRepository).findIdentityPairsByTenantIdAndDepartmentIgnoreCase(TENANT_TEST, "bd");
    }

    @Test
    void shouldReturnEmptyOverviewWhenScopedOwnersResolvedToEmpty() {
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        OpportunityRepository opportunityRepository = mock(OpportunityRepository.class);
        TaskRepository taskRepository = mock(TaskRepository.class);
        FollowUpRepository followUpRepository = mock(FollowUpRepository.class);
        QuoteRepository quoteRepository = mock(QuoteRepository.class);
        OrderRecordRepository orderRecordRepository = mock(OrderRecordRepository.class);
        PaymentRecordRepository paymentRecordRepository = mock(PaymentRecordRepository.class);
        UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
        LeadRepository leadRepository = mock(LeadRepository.class);
        ValueNormalizerService valueNormalizerService = mock(ValueNormalizerService.class);
        DashboardMetricsCacheService cacheService = mock(DashboardMetricsCacheService.class);
        ReportAggregationService reportAggregationService = mock(ReportAggregationService.class);

        when(userAccountRepository.findIdentityPairsByTenantIdAndRoleIgnoreCase(TENANT_TEST, "SALES"))
                .thenReturn(Collections.singletonList(new Object[]{"alice", "Alice"}));
        when(userAccountRepository.findIdentityPairsByTenantIdAndDepartmentIgnoreCase(TENANT_TEST, "bd"))
                .thenReturn(Collections.singletonList(new Object[]{"alice", "Alice"}));
        Map<String, Object> emptySummary = new java.util.HashMap<String, Object>();
        emptySummary.put("customers", 0L);
        emptySummary.put("opportunities", 0L);
        emptySummary.put("quotes", 0L);
        Map<String, Object> emptyBody = new java.util.HashMap<String, Object>();
        emptyBody.put("summary", emptySummary);
        when(reportAggregationService.emptyOverviewBody()).thenReturn(emptyBody);

        ReportService reportService = new ReportService(
                customerRepository,
                opportunityRepository,
                taskRepository,
                followUpRepository,
                quoteRepository,
                orderRecordRepository,
                paymentRecordRepository,
                userAccountRepository,
                leadRepository,
                valueNormalizerService,
                cacheService,
                reportAggregationService
        );

        Map<String, Object> report = reportService.overviewByTenant(
                TENANT_TEST,
                null,
                null,
                "SALES",
                "bob",
                "BD"
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) report.get("summary");
        Assertions.assertEquals(0, ((Number) summary.get("customers")).intValue());
        Assertions.assertEquals(0, ((Number) summary.get("opportunities")).intValue());
        Assertions.assertEquals(0, ((Number) summary.get("quotes")).intValue());

        verify(customerRepository, never()).countByTenantIdAndOwnerIn(anyString(), anyCollection());
        verify(opportunityRepository, never()).countByTenantIdAndOwnerIn(anyString(), anyCollection());
        verify(taskRepository, never()).countByTenantIdAndDoneTrueAndOwnerIn(anyString(), anyCollection());
        verify(reportAggregationService, never()).aggregateWithScope(anyString(), anySet());
    }

    @Test
    void shouldUseDateFastPathWhenDateRangeProvided() {
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        OpportunityRepository opportunityRepository = mock(OpportunityRepository.class);
        TaskRepository taskRepository = mock(TaskRepository.class);
        FollowUpRepository followUpRepository = mock(FollowUpRepository.class);
        QuoteRepository quoteRepository = mock(QuoteRepository.class);
        OrderRecordRepository orderRecordRepository = mock(OrderRecordRepository.class);
        PaymentRecordRepository paymentRecordRepository = mock(PaymentRecordRepository.class);
        UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
        LeadRepository leadRepository = mock(LeadRepository.class);
        ValueNormalizerService valueNormalizerService = mock(ValueNormalizerService.class);
        DashboardMetricsCacheService cacheService = mock(DashboardMetricsCacheService.class);
        ReportAggregationService reportAggregationService = mock(ReportAggregationService.class);

        when(customerRepository.countByTenantIdAndCreatedAtBetween(anyString(), any(), any())).thenReturn(3L);
        when(customerRepository.sumValueByTenantIdAndCreatedAtBetween(anyString(), any(), any())).thenReturn(500L);
        when(customerRepository.countByOwnerGroupedAndCreatedAtBetween(anyString(), any(), any())).thenReturn(Collections.emptyList());
        when(customerRepository.sumValueByStatusGroupedAndCreatedAtBetween(anyString(), any(), any())).thenReturn(Collections.emptyList());

        when(opportunityRepository.countByTenantIdAndCreatedAtBetween(anyString(), any(), any())).thenReturn(2L);
        when(opportunityRepository.sumWeightedAmountRawByTenantIdAndCreatedAtBetween(anyString(), any(), any())).thenReturn(12000L);
        when(opportunityRepository.countByTenantIdAndProgressGteAndCreatedAtBetween(anyString(), anyInt(), any(), any())).thenReturn(1L);
        when(opportunityRepository.countByStageGroupedAndCreatedAtBetween(anyString(), any(), any())).thenReturn(Collections.emptyList());

        when(followUpRepository.countByChannelGroupedAndCreatedAtBetween(anyString(), any(), any())).thenReturn(Collections.emptyList());
        when(quoteRepository.countByStatusGroupedAndCreatedAtBetween(anyString(), any(), any())).thenReturn(Collections.emptyList());
        when(orderRecordRepository.countByStatusGroupedAndCreatedAtBetween(anyString(), any(), any())).thenReturn(Collections.emptyList());

        ReportService reportService = new ReportService(
                customerRepository,
                opportunityRepository,
                taskRepository,
                followUpRepository,
                quoteRepository,
                orderRecordRepository,
                paymentRecordRepository,
                userAccountRepository,
                leadRepository,
                valueNormalizerService,
                cacheService,
                reportAggregationService
        );

        Map<String, Object> report = reportService.overviewByTenant(
                TENANT_TEST,
                LocalDate.now().minusDays(7),
                LocalDate.now(),
                "",
                "",
                ""
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) report.get("summary");
        Assertions.assertNotNull(summary);
        Assertions.assertEquals(3L, summary.get("customers"));

        verify(customerRepository).countByTenantIdAndCreatedAtBetween(anyString(), any(), any());
        verify(opportunityRepository).countByTenantIdAndCreatedAtBetween(anyString(), any(), any());
        verify(customerRepository, never()).findByTenantIdAndCreatedAtBetween(anyString(), any(), any());
        verify(opportunityRepository, never()).findByTenantIdAndCreatedAtBetween(anyString(), any(), any());
    }

    @Test
    void shouldUseDateFastPathWhenOnlyFromDateProvided() {
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        OpportunityRepository opportunityRepository = mock(OpportunityRepository.class);
        TaskRepository taskRepository = mock(TaskRepository.class);
        FollowUpRepository followUpRepository = mock(FollowUpRepository.class);
        QuoteRepository quoteRepository = mock(QuoteRepository.class);
        OrderRecordRepository orderRecordRepository = mock(OrderRecordRepository.class);
        PaymentRecordRepository paymentRecordRepository = mock(PaymentRecordRepository.class);
        UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
        LeadRepository leadRepository = mock(LeadRepository.class);
        ValueNormalizerService valueNormalizerService = mock(ValueNormalizerService.class);
        DashboardMetricsCacheService cacheService = mock(DashboardMetricsCacheService.class);
        ReportAggregationService reportAggregationService = mock(ReportAggregationService.class);

        when(customerRepository.countByTenantIdAndCreatedAtBetween(anyString(), any(), any())).thenReturn(1L);
        when(customerRepository.sumValueByTenantIdAndCreatedAtBetween(anyString(), any(), any())).thenReturn(100L);
        when(customerRepository.countByOwnerGroupedAndCreatedAtBetween(anyString(), any(), any())).thenReturn(Collections.emptyList());
        when(customerRepository.sumValueByStatusGroupedAndCreatedAtBetween(anyString(), any(), any())).thenReturn(Collections.emptyList());
        when(opportunityRepository.countByTenantIdAndCreatedAtBetween(anyString(), any(), any())).thenReturn(1L);
        when(opportunityRepository.sumWeightedAmountRawByTenantIdAndCreatedAtBetween(anyString(), any(), any())).thenReturn(1000L);
        when(opportunityRepository.countByTenantIdAndProgressGteAndCreatedAtBetween(anyString(), anyInt(), any(), any())).thenReturn(1L);
        when(opportunityRepository.countByStageGroupedAndCreatedAtBetween(anyString(), any(), any())).thenReturn(Collections.emptyList());
        when(taskRepository.countByTenantIdAndDoneTrueAndCreatedAtBetween(anyString(), any(), any())).thenReturn(1L);
        when(taskRepository.countByTenantIdAndDoneFalseAndCreatedAtBetween(anyString(), any(), any())).thenReturn(0L);
        when(followUpRepository.countByTenantIdAndCreatedAtBetween(anyString(), any(), any())).thenReturn(0L);
        when(quoteRepository.countByTenantIdAndCreatedAtBetween(anyString(), any(), any())).thenReturn(0L);
        when(quoteRepository.countByTenantIdAndStatusInUppercaseAndCreatedAtBetween(anyString(), anyList(), any(), any())).thenReturn(0L);
        when(orderRecordRepository.countByTenantIdAndCreatedAtBetween(anyString(), any(), any())).thenReturn(0L);
        when(orderRecordRepository.countByTenantIdAndStatusAndCreatedAtBetween(anyString(), anyString(), any(), any())).thenReturn(0L);
        when(orderRecordRepository.sumAmountByTenantIdAndCreatedAtBetween(anyString(), any(), any())).thenReturn(0L);
        when(paymentRecordRepository.sumAmountByTenantIdAndStatusInUppercaseAndCreatedAtBetween(anyString(), anyList(), any(), any())).thenReturn(0L);

        ReportService reportService = new ReportService(
                customerRepository,
                opportunityRepository,
                taskRepository,
                followUpRepository,
                quoteRepository,
                orderRecordRepository,
                paymentRecordRepository,
                userAccountRepository,
                leadRepository,
                valueNormalizerService,
                cacheService,
                reportAggregationService
        );

        Map<String, Object> report = reportService.overviewByTenant(
                TENANT_TEST,
                LocalDate.now().minusDays(7),
                null,
                "",
                "",
                ""
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) report.get("summary");
        Assertions.assertNotNull(summary);
        Assertions.assertEquals(1L, summary.get("customers"));

        verify(customerRepository).countByTenantIdAndCreatedAtBetween(anyString(), any(), any());
        verify(opportunityRepository).countByTenantIdAndCreatedAtBetween(anyString(), any(), any());
        verify(customerRepository, never()).findByTenantId(anyString());
        verify(opportunityRepository, never()).findByTenantId(anyString());
    }

    @Test
    void shouldReuseRoleAndDepartmentIdentityCacheWithinTtl() {
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        OpportunityRepository opportunityRepository = mock(OpportunityRepository.class);
        TaskRepository taskRepository = mock(TaskRepository.class);
        FollowUpRepository followUpRepository = mock(FollowUpRepository.class);
        QuoteRepository quoteRepository = mock(QuoteRepository.class);
        OrderRecordRepository orderRecordRepository = mock(OrderRecordRepository.class);
        PaymentRecordRepository paymentRecordRepository = mock(PaymentRecordRepository.class);
        UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
        LeadRepository leadRepository = mock(LeadRepository.class);
        ValueNormalizerService valueNormalizerService = mock(ValueNormalizerService.class);
        DashboardMetricsCacheService cacheService = mock(DashboardMetricsCacheService.class);
        ReportAggregationService reportAggregationService = mock(ReportAggregationService.class);

        when(userAccountRepository.findIdentityPairsByTenantIdAndRoleIgnoreCase(TENANT_TEST, "SALES"))
                .thenReturn(Collections.singletonList(new Object[]{"alice", "Alice"}));
        when(userAccountRepository.findIdentityPairsByTenantIdAndDepartmentIgnoreCase(TENANT_TEST, "BD"))
                .thenReturn(Collections.singletonList(new Object[]{"alice", "Alice"}));
        when(customerRepository.countByTenantIdAndOwnerIn(anyString(), anyCollection())).thenReturn(1L);
        when(customerRepository.sumValueByTenantIdAndOwnerIn(anyString(), anyCollection())).thenReturn(100L);
        when(customerRepository.countByOwnerGroupedAndOwnerIn(anyString(), anyCollection())).thenReturn(Collections.emptyList());
        when(customerRepository.sumValueByStatusGroupedAndOwnerIn(anyString(), anyCollection())).thenReturn(Collections.emptyList());
        when(opportunityRepository.countByTenantIdAndOwnerIn(anyString(), anyCollection())).thenReturn(1L);
        when(opportunityRepository.sumWeightedAmountRawByTenantIdAndOwnerIn(anyString(), anyCollection())).thenReturn(1000L);
        when(opportunityRepository.countByTenantIdAndOwnerInAndProgressGte(anyString(), anyCollection(), anyInt())).thenReturn(1L);
        when(opportunityRepository.countByStageGroupedAndOwnerIn(anyString(), anyCollection())).thenReturn(Collections.emptyList());
        when(taskRepository.countByTenantIdAndDoneTrueAndOwnerIn(anyString(), anyCollection())).thenReturn(1L);
        when(taskRepository.countByTenantIdAndDoneFalseAndOwnerIn(anyString(), anyCollection())).thenReturn(0L);
        when(followUpRepository.countByTenantIdAndAuthorIn(anyString(), anyCollection())).thenReturn(0L);
        when(followUpRepository.countByChannelGroupedAndAuthorIn(anyString(), anyCollection())).thenReturn(Collections.emptyList());
        when(quoteRepository.countByTenantIdAndOwnerIn(anyString(), anyCollection())).thenReturn(0L);
        when(quoteRepository.countByTenantIdAndOwnerInAndStatusInUppercase(anyString(), anyCollection(), anyList())).thenReturn(0L);
        when(quoteRepository.countByStatusGroupedAndOwnerIn(anyString(), anyCollection())).thenReturn(Collections.emptyList());
        when(orderRecordRepository.countByTenantIdAndOwnerIn(anyString(), anyCollection())).thenReturn(0L);
        when(orderRecordRepository.countByTenantIdAndOwnerInAndStatus(anyString(), anyCollection(), anyString())).thenReturn(0L);
        when(orderRecordRepository.sumAmountByTenantIdAndOwnerIn(anyString(), anyCollection())).thenReturn(0L);
        when(orderRecordRepository.countByStatusGroupedAndOwnerIn(anyString(), anyCollection())).thenReturn(Collections.emptyList());
        when(paymentRecordRepository.sumAmountByTenantIdAndOwnerInAndStatusInUppercase(anyString(), anyCollection(), anyList())).thenReturn(0L);

        ReportService reportService = new ReportService(
                customerRepository,
                opportunityRepository,
                taskRepository,
                followUpRepository,
                quoteRepository,
                orderRecordRepository,
                paymentRecordRepository,
                userAccountRepository,
                leadRepository,
                valueNormalizerService,
                cacheService,
                reportAggregationService
        );

        reportService.overviewByTenant(TENANT_TEST, null, null, "sales", "alice", "bd");
        reportService.overviewByTenant(TENANT_TEST, null, null, "sales", "alice", "bd");

        verify(userAccountRepository, times(1)).findIdentityPairsByTenantIdAndRoleIgnoreCase(TENANT_TEST, "SALES");
        verify(userAccountRepository, times(1)).findIdentityPairsByTenantIdAndDepartmentIgnoreCase(TENANT_TEST, "bd");
    }

    @Test
    void funnelShouldUseCountQueriesForOwnerAndDateRange() {
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        OpportunityRepository opportunityRepository = mock(OpportunityRepository.class);
        TaskRepository taskRepository = mock(TaskRepository.class);
        FollowUpRepository followUpRepository = mock(FollowUpRepository.class);
        QuoteRepository quoteRepository = mock(QuoteRepository.class);
        OrderRecordRepository orderRecordRepository = mock(OrderRecordRepository.class);
        PaymentRecordRepository paymentRecordRepository = mock(PaymentRecordRepository.class);
        UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
        LeadRepository leadRepository = mock(LeadRepository.class);
        ValueNormalizerService valueNormalizerService = mock(ValueNormalizerService.class);
        DashboardMetricsCacheService cacheService = mock(DashboardMetricsCacheService.class);
        ReportAggregationService reportAggregationService = mock(ReportAggregationService.class);

        when(leadRepository.countByTenantIdAndOwnerInAndCreatedAtBetween(anyString(), anyCollection(), any(), any())).thenReturn(10L);
        when(opportunityRepository.countByTenantIdAndOwnerInAndCreatedAtBetween(anyString(), anyCollection(), any(), any())).thenReturn(4L);
        when(quoteRepository.countByTenantIdAndOwnerInAndCreatedAtBetween(anyString(), anyCollection(), any(), any())).thenReturn(2L);
        when(orderRecordRepository.countByTenantIdAndOwnerInAndCreatedAtBetween(anyString(), anyCollection(), any(), any())).thenReturn(1L);

        ReportService reportService = new ReportService(
                customerRepository,
                opportunityRepository,
                taskRepository,
                followUpRepository,
                quoteRepository,
                orderRecordRepository,
                paymentRecordRepository,
                userAccountRepository,
                leadRepository,
                valueNormalizerService,
                cacheService,
                reportAggregationService
        );

        Map<String, Object> funnel = reportService.funnelByTenant(
                TENANT_TEST,
                LocalDate.now().minusDays(7),
                LocalDate.now(),
                "alice"
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> counts = (Map<String, Object>) funnel.get("counts");
        @SuppressWarnings("unchecked")
        Map<String, Object> rates = (Map<String, Object>) funnel.get("rates");
        Assertions.assertEquals(10L, counts.get("leads"));
        Assertions.assertEquals(4L, counts.get("opportunities"));
        Assertions.assertEquals(2L, counts.get("quotes"));
        Assertions.assertEquals(1L, counts.get("orders"));
        Assertions.assertEquals(40.0, ((Number) rates.get("leadToOpportunity")).doubleValue());
        Assertions.assertEquals(50.0, ((Number) rates.get("opportunityToQuote")).doubleValue());
        Assertions.assertEquals(50.0, ((Number) rates.get("quoteToOrder")).doubleValue());

        verify(leadRepository).countByTenantIdAndOwnerInAndCreatedAtBetween(anyString(), anyCollection(), any(), any());
        verify(opportunityRepository).countByTenantIdAndOwnerInAndCreatedAtBetween(anyString(), anyCollection(), any(), any());
        verify(quoteRepository).countByTenantIdAndOwnerInAndCreatedAtBetween(anyString(), anyCollection(), any(), any());
        verify(orderRecordRepository).countByTenantIdAndOwnerInAndCreatedAtBetween(anyString(), anyCollection(), any(), any());
        verify(leadRepository, never()).findByTenantIdAndOwnerInAndCreatedAtBetween(anyString(), anyCollection(), any(), any());
        verify(opportunityRepository, never()).findByTenantIdAndOwnerInAndCreatedAtBetween(anyString(), anyCollection(), any(), any());
        verify(quoteRepository, never()).findByTenantIdAndOwnerInAndCreatedAtBetween(anyString(), anyCollection(), any(), any());
        verify(orderRecordRepository, never()).findByTenantIdAndOwnerInAndCreatedAtBetween(anyString(), anyCollection(), any(), any());
    }

    @Test
    void funnelShouldUseTenantCountsWhenNoOwnerAndNoDate() {
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        OpportunityRepository opportunityRepository = mock(OpportunityRepository.class);
        TaskRepository taskRepository = mock(TaskRepository.class);
        FollowUpRepository followUpRepository = mock(FollowUpRepository.class);
        QuoteRepository quoteRepository = mock(QuoteRepository.class);
        OrderRecordRepository orderRecordRepository = mock(OrderRecordRepository.class);
        PaymentRecordRepository paymentRecordRepository = mock(PaymentRecordRepository.class);
        UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
        LeadRepository leadRepository = mock(LeadRepository.class);
        ValueNormalizerService valueNormalizerService = mock(ValueNormalizerService.class);
        DashboardMetricsCacheService cacheService = mock(DashboardMetricsCacheService.class);
        ReportAggregationService reportAggregationService = mock(ReportAggregationService.class);

        when(leadRepository.countByTenantId(anyString())).thenReturn(8L);
        when(opportunityRepository.countByTenantId(anyString())).thenReturn(4L);
        when(quoteRepository.countByTenantId(anyString())).thenReturn(2L);
        when(orderRecordRepository.countByTenantId(anyString())).thenReturn(1L);

        ReportService reportService = new ReportService(
                customerRepository,
                opportunityRepository,
                taskRepository,
                followUpRepository,
                quoteRepository,
                orderRecordRepository,
                paymentRecordRepository,
                userAccountRepository,
                leadRepository,
                valueNormalizerService,
                cacheService,
                reportAggregationService
        );

        Map<String, Object> funnel = reportService.funnelByTenant(TENANT_TEST, null, null, "");

        @SuppressWarnings("unchecked")
        Map<String, Object> counts = (Map<String, Object>) funnel.get("counts");
        Assertions.assertEquals(8L, counts.get("leads"));
        Assertions.assertEquals(4L, counts.get("opportunities"));
        Assertions.assertEquals(2L, counts.get("quotes"));
        Assertions.assertEquals(1L, counts.get("orders"));

        verify(leadRepository).countByTenantId(TENANT_TEST);
        verify(opportunityRepository).countByTenantId(TENANT_TEST);
        verify(quoteRepository).countByTenantId(TENANT_TEST);
        verify(orderRecordRepository).countByTenantId(TENANT_TEST);
        verify(leadRepository, never()).findByTenantId(anyString());
        verify(opportunityRepository, never()).findByTenantId(anyString());
        verify(quoteRepository, never()).findByTenantId(anyString());
        verify(orderRecordRepository, never()).findByTenantId(anyString());
    }

    @Test
    void funnelCacheKeyShouldBeNormalized() {
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        OpportunityRepository opportunityRepository = mock(OpportunityRepository.class);
        TaskRepository taskRepository = mock(TaskRepository.class);
        FollowUpRepository followUpRepository = mock(FollowUpRepository.class);
        QuoteRepository quoteRepository = mock(QuoteRepository.class);
        OrderRecordRepository orderRecordRepository = mock(OrderRecordRepository.class);
        PaymentRecordRepository paymentRecordRepository = mock(PaymentRecordRepository.class);
        UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
        LeadRepository leadRepository = mock(LeadRepository.class);
        ValueNormalizerService valueNormalizerService = mock(ValueNormalizerService.class);
        DashboardMetricsCacheService cacheService = mock(DashboardMetricsCacheService.class);
        ReportAggregationService reportAggregationService = mock(ReportAggregationService.class);

        doReturn(new DashboardMetricsCacheService.CachedValue<Map<String, Object>>(
                Collections.<String, Object>emptyMap(),
                true,
                "LOCAL",
                false
        )).when(cacheService).getOrLoad(anyString(), anyString(), anyString(), any());

        ReportService reportService = new ReportService(
                customerRepository,
                opportunityRepository,
                taskRepository,
                followUpRepository,
                quoteRepository,
                orderRecordRepository,
                paymentRecordRepository,
                userAccountRepository,
                leadRepository,
                valueNormalizerService,
                cacheService,
                reportAggregationService
        );

        reportService.funnelByTenantCached(
                TENANT_TEST,
                "  Alice  ",
                " manager ",
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                "  BOB  "
        );

        verify(cacheService).getOrLoad(
                eq(TENANT_TEST),
                eq("reports-funnel"),
                eq("2026-03-01|2026-03-31|bob"),
                any()
        );
    }

    @Test
    void summarizeQuotesAndOrdersShouldUseSinglePassLoops() {
        CountingList<com.yao.crm.entity.Quote> quotes = new CountingList<com.yao.crm.entity.Quote>();
        com.yao.crm.entity.Quote approved = mock(com.yao.crm.entity.Quote.class);
        when(approved.getStatus()).thenReturn("APPROVED");
        com.yao.crm.entity.Quote accepted = mock(com.yao.crm.entity.Quote.class);
        when(accepted.getStatus()).thenReturn("accepted");
        com.yao.crm.entity.Quote blank = mock(com.yao.crm.entity.Quote.class);
        when(blank.getStatus()).thenReturn("  ");
        quotes.add(approved);
        quotes.add(accepted);
        quotes.add(blank);

        CountingList<com.yao.crm.entity.OrderRecord> orders = new CountingList<com.yao.crm.entity.OrderRecord>();
        com.yao.crm.entity.OrderRecord completed = mock(com.yao.crm.entity.OrderRecord.class);
        when(completed.getStatus()).thenReturn("COMPLETED");
        when(completed.getId()).thenReturn("order-1");
        when(completed.getAmount()).thenReturn(120L);
        com.yao.crm.entity.OrderRecord pending = mock(com.yao.crm.entity.OrderRecord.class);
        when(pending.getStatus()).thenReturn("NEW");
        when(pending.getId()).thenReturn("order-2");
        when(pending.getAmount()).thenReturn(null);
        orders.add(completed);
        orders.add(pending);

        ReportService.QuoteSummary quoteSummary = ReportService.summarizeQuotes(quotes);
        ReportService.OrderSummary orderSummary = ReportService.summarizeOrders(orders);

        Assertions.assertEquals(1, quotes.iteratorCalls);
        Assertions.assertEquals(0, quotes.streamCalls);
        Assertions.assertEquals(1, orders.iteratorCalls);
        Assertions.assertEquals(0, orders.streamCalls);

        Assertions.assertEquals(2L, quoteSummary.approvedCount);
        Assertions.assertEquals(1L, quoteSummary.acceptedCount);
        Assertions.assertEquals(1, quoteSummary.byStatus.get("APPROVED"));
        Assertions.assertEquals(1, quoteSummary.byStatus.get("accepted"));
        Assertions.assertEquals(1, quoteSummary.byStatus.get("Unknown"));

        Assertions.assertEquals(1L, orderSummary.completedCount);
        Assertions.assertEquals(120L, orderSummary.amountTotal);
        Assertions.assertTrue(orderSummary.orderIds.contains("order-1"));
        Assertions.assertTrue(orderSummary.orderIds.contains("order-2"));
        Assertions.assertEquals(1, orderSummary.byStatus.get("COMPLETED"));
        Assertions.assertEquals(1, orderSummary.byStatus.get("NEW"));
    }

    @Test
    void overviewByTenantCachedShouldFailFastWhenTenantBlank() {
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        OpportunityRepository opportunityRepository = mock(OpportunityRepository.class);
        TaskRepository taskRepository = mock(TaskRepository.class);
        FollowUpRepository followUpRepository = mock(FollowUpRepository.class);
        QuoteRepository quoteRepository = mock(QuoteRepository.class);
        OrderRecordRepository orderRecordRepository = mock(OrderRecordRepository.class);
        PaymentRecordRepository paymentRecordRepository = mock(PaymentRecordRepository.class);
        UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
        LeadRepository leadRepository = mock(LeadRepository.class);
        ValueNormalizerService valueNormalizerService = mock(ValueNormalizerService.class);
        ReportAggregationService reportAggregationService = mock(ReportAggregationService.class);
        DashboardMetricsCacheService cacheService = new DashboardMetricsCacheService(
                emptyRedisProvider(),
                emptyMeterProvider(),
                false,
                CACHE_INVALIDATION_MODE,
                60000L,
                60000L,
                60000L,
                60000L,
                60000L,
                60000L,
                60000L,
                30000L,
                60000L
        );

        ReportService reportService = new ReportService(
                customerRepository,
                opportunityRepository,
                taskRepository,
                followUpRepository,
                quoteRepository,
                orderRecordRepository,
                paymentRecordRepository,
                userAccountRepository,
                leadRepository,
                valueNormalizerService,
                cacheService,
                reportAggregationService
        );

        IllegalStateException error = Assertions.assertThrows(
                IllegalStateException.class,
                () -> reportService.overviewByTenantCached("   ", "alice", "MANAGER", null, null, "", "", "")
        );

        Assertions.assertEquals("tenant_id_required", error.getMessage());
        verify(reportAggregationService, never()).aggregateWithoutScope(anyString());
    }

    @SuppressWarnings("unchecked")
    private org.springframework.beans.factory.ObjectProvider<org.springframework.data.redis.core.RedisTemplate<String, Object>> emptyRedisProvider() {
        org.springframework.beans.factory.support.DefaultListableBeanFactory factory = new org.springframework.beans.factory.support.DefaultListableBeanFactory();
        return (org.springframework.beans.factory.ObjectProvider<org.springframework.data.redis.core.RedisTemplate<String, Object>>) (org.springframework.beans.factory.ObjectProvider<?>) factory.getBeanProvider(org.springframework.data.redis.core.RedisTemplate.class);
    }

    @SuppressWarnings("unchecked")
    private org.springframework.beans.factory.ObjectProvider<io.micrometer.core.instrument.MeterRegistry> emptyMeterProvider() {
        org.springframework.beans.factory.support.DefaultListableBeanFactory factory = new org.springframework.beans.factory.support.DefaultListableBeanFactory();
        return (org.springframework.beans.factory.ObjectProvider<io.micrometer.core.instrument.MeterRegistry>) (org.springframework.beans.factory.ObjectProvider<?>) factory.getBeanProvider(io.micrometer.core.instrument.MeterRegistry.class);
    }

    private static final class CountingList<E> extends ArrayList<E> {
        private int iteratorCalls;
        private int streamCalls;

        @Override
        public Iterator<E> iterator() {
            this.iteratorCalls++;
            return super.iterator();
        }

        @Override
        public java.util.stream.Stream<E> stream() {
            this.streamCalls++;
            return super.stream();
        }
    }
}

