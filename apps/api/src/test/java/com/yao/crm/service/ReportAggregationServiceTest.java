package com.yao.crm.service;

import com.yao.crm.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReportAggregationService
 */
@ExtendWith(MockitoExtension.class)
class ReportAggregationServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private OpportunityRepository opportunityRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private FollowUpRepository followUpRepository;

    @Mock
    private QuoteRepository quoteRepository;

    @Mock
    private OrderRecordRepository orderRecordRepository;

    @Mock
    private PaymentRecordRepository paymentRecordRepository;

    @Mock
    private ValueNormalizerService valueNormalizerService;

    private ReportAggregationService service;

    @BeforeEach
    void setUp() {
        service = new ReportAggregationService(
                customerRepository,
                opportunityRepository,
                taskRepository,
                followUpRepository,
                quoteRepository,
                orderRecordRepository,
                paymentRecordRepository,
                valueNormalizerService
        );
    }

    @Test
    @DisplayName("shouldReturnCorrectAggregateStructure_whenAggregateWithoutScope")
    void shouldReturnCorrectAggregateStructure_whenAggregateWithoutScope() {
        // Arrange
        String tenantId = "tenant-1";
        when(customerRepository.countByTenantId(tenantId)).thenReturn(10L);
        when(opportunityRepository.countByTenantId(tenantId)).thenReturn(5L);
        when(opportunityRepository.countByTenantIdAndProgressGte(tenantId, 80)).thenReturn(2L);
        when(taskRepository.countByTenantIdAndDoneTrue(tenantId)).thenReturn(8L);
        when(taskRepository.countByTenantIdAndDoneFalse(tenantId)).thenReturn(4L);
        when(followUpRepository.countByTenantId(tenantId)).thenReturn(20L);
        when(quoteRepository.countByTenantId(tenantId)).thenReturn(15L);
        when(quoteRepository.countByTenantIdAndStatusInUppercase(eq(tenantId), anyList())).thenReturn(10L);
        when(orderRecordRepository.countByTenantId(tenantId)).thenReturn(12L);
        when(orderRecordRepository.countByTenantIdAndStatus(tenantId, "COMPLETED")).thenReturn(8L);
        when(customerRepository.sumValueByTenantId(tenantId)).thenReturn(100000L);
        when(opportunityRepository.sumWeightedAmountRawByTenantId(tenantId)).thenReturn(50000L);
        when(orderRecordRepository.sumAmountByTenantId(tenantId)).thenReturn(80000L);
        when(paymentRecordRepository.sumAmountByTenantIdAndStatusInUppercase(eq(tenantId), anyList())).thenReturn(60000L);
        when(customerRepository.countByOwnerGrouped(tenantId)).thenReturn(Collections.singletonList(new Object[]{"owner1", 5}));
        when(customerRepository.sumValueByStatusGrouped(tenantId)).thenReturn(Collections.singletonList(new Object[]{"ACTIVE", 80000L}));
        when(opportunityRepository.countByStageGrouped(tenantId)).thenReturn(Collections.singletonList(new Object[]{"PROPOSAL", 3}));
        when(quoteRepository.countByStatusGrouped(tenantId)).thenReturn(Collections.singletonList(new Object[]{"APPROVED", 8}));
        when(orderRecordRepository.countByStatusGrouped(tenantId)).thenReturn(Collections.singletonList(new Object[]{"COMPLETED", 8}));
        when(followUpRepository.countByChannelGrouped(tenantId)).thenReturn(Collections.singletonList(new Object[]{"PHONE", 10}));
        when(valueNormalizerService.normalizeFollowUpChannel(anyString())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Map<String, Object> result = service.aggregateWithoutScope(tenantId);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("summary"));
        assertTrue(result.containsKey("customerByOwner"));
        assertTrue(result.containsKey("revenueByStatus"));
        assertTrue(result.containsKey("opportunityByStage"));
        assertTrue(result.containsKey("taskStatus"));
        assertTrue(result.containsKey("followUpByChannel"));
        assertTrue(result.containsKey("quoteByStatus"));
        assertTrue(result.containsKey("orderByStatus"));

        Map<String, Object> summary = (Map<String, Object>) result.get("summary");
        assertEquals(10L, summary.get("customers"));
        assertEquals(100000L, summary.get("revenue"));
        assertEquals(5L, summary.get("opportunities"));
        assertEquals(20L, summary.get("followUps"));
        assertEquals(15L, summary.get("quotes"));
        assertEquals(12L, summary.get("orders"));
    }

    @Test
    @DisplayName("shouldReturnEmptyOverview_whenAggregateWithEmptyOwners")
    void shouldReturnEmptyOverview_whenAggregateWithEmptyOwners() {
        // Arrange
        String tenantId = "tenant-1";
        Set<String> emptyOwners = new HashSet<String>();

        // Act
        Map<String, Object> result = service.aggregateWithScope(tenantId, emptyOwners);

        // Assert
        assertNotNull(result);
        Map<String, Object> summary = (Map<String, Object>) result.get("summary");
        assertEquals(0, summary.get("customers"));
        assertEquals(0, summary.get("revenue"));
        assertEquals(0, summary.get("opportunities"));
    }

    @Test
    @DisplayName("shouldFilterByOwners_whenAggregateWithScope")
    void shouldFilterByOwners_whenAggregateWithScope() {
        // Arrange
        String tenantId = "tenant-1";
        Set<String> owners = new HashSet<String>(Arrays.asList("owner1", "owner2"));

        when(customerRepository.countByTenantIdAndOwnerIn(tenantId, owners)).thenReturn(5L);
        when(opportunityRepository.countByTenantIdAndOwnerIn(tenantId, owners)).thenReturn(3L);
        when(opportunityRepository.countByTenantIdAndOwnerInAndProgressGte(eq(tenantId), eq(owners), eq(80))).thenReturn(1L);
        when(taskRepository.countByTenantIdAndDoneTrueAndOwnerIn(tenantId, owners)).thenReturn(4L);
        when(taskRepository.countByTenantIdAndDoneFalseAndOwnerIn(tenantId, owners)).thenReturn(2L);
        when(followUpRepository.countByTenantIdAndAuthorIn(tenantId, owners)).thenReturn(10L);
        when(quoteRepository.countByTenantIdAndOwnerIn(tenantId, owners)).thenReturn(8L);
        when(quoteRepository.countByTenantIdAndOwnerInAndStatusInUppercase(eq(tenantId), eq(owners), anyList())).thenReturn(5L);
        when(orderRecordRepository.countByTenantIdAndOwnerIn(tenantId, owners)).thenReturn(6L);
        when(orderRecordRepository.countByTenantIdAndOwnerInAndStatus(tenantId, owners, "COMPLETED")).thenReturn(4L);
        when(customerRepository.sumValueByTenantIdAndOwnerIn(tenantId, owners)).thenReturn(50000L);
        when(opportunityRepository.sumWeightedAmountRawByTenantIdAndOwnerIn(tenantId, owners)).thenReturn(25000L);
        when(orderRecordRepository.sumAmountByTenantIdAndOwnerIn(tenantId, owners)).thenReturn(40000L);
        when(paymentRecordRepository.sumAmountByTenantIdAndOwnerInAndStatusInUppercase(eq(tenantId), eq(owners), anyList())).thenReturn(30000L);
        when(customerRepository.countByOwnerGroupedAndOwnerIn(tenantId, owners)).thenReturn(Collections.singletonList(new Object[]{"owner1", 3}));
        when(customerRepository.sumValueByStatusGroupedAndOwnerIn(tenantId, owners)).thenReturn(Collections.singletonList(new Object[]{"ACTIVE", 40000L}));
        when(opportunityRepository.countByStageGroupedAndOwnerIn(tenantId, owners)).thenReturn(Collections.singletonList(new Object[]{"PROPOSAL", 2}));
        when(quoteRepository.countByStatusGroupedAndOwnerIn(tenantId, owners)).thenReturn(Collections.singletonList(new Object[]{"APPROVED", 4}));
        when(orderRecordRepository.countByStatusGroupedAndOwnerIn(tenantId, owners)).thenReturn(Collections.singletonList(new Object[]{"COMPLETED", 4}));
        when(followUpRepository.countByChannelGroupedAndAuthorIn(tenantId, owners)).thenReturn(Collections.singletonList(new Object[]{"PHONE", 5}));
        when(valueNormalizerService.normalizeFollowUpChannel(anyString())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Map<String, Object> result = service.aggregateWithScope(tenantId, owners);

        // Assert
        assertNotNull(result);
        Map<String, Object> summary = (Map<String, Object>) result.get("summary");
        assertEquals(5L, summary.get("customers"));
        assertEquals(50000L, summary.get("revenue"));
        assertEquals(3L, summary.get("opportunities"));

        // Verify owner-filtered methods were called
        verify(customerRepository).countByTenantIdAndOwnerIn(tenantId, owners);
        verify(opportunityRepository).countByTenantIdAndOwnerIn(tenantId, owners);
    }

    @Test
    @DisplayName("shouldReturnEmptyOverview_whenAllRepositoriesReturnZero")
    void shouldReturnEmptyOverview_whenAllRepositoriesReturnZero() {
        // Arrange
        String tenantId = "tenant-1";
        when(customerRepository.countByTenantId(tenantId)).thenReturn(0L);
        when(opportunityRepository.countByTenantId(tenantId)).thenReturn(0L);
        when(opportunityRepository.countByTenantIdAndProgressGte(tenantId, 80)).thenReturn(0L);
        when(taskRepository.countByTenantIdAndDoneTrue(tenantId)).thenReturn(0L);
        when(taskRepository.countByTenantIdAndDoneFalse(tenantId)).thenReturn(0L);
        when(followUpRepository.countByTenantId(tenantId)).thenReturn(0L);
        when(quoteRepository.countByTenantId(tenantId)).thenReturn(0L);
        when(orderRecordRepository.countByTenantId(tenantId)).thenReturn(0L);
        when(customerRepository.sumValueByTenantId(tenantId)).thenReturn(0L);
        when(opportunityRepository.sumWeightedAmountRawByTenantId(tenantId)).thenReturn(0L);
        when(orderRecordRepository.sumAmountByTenantId(tenantId)).thenReturn(0L);
        when(paymentRecordRepository.sumAmountByTenantIdAndStatusInUppercase(eq(tenantId), anyList())).thenReturn(0L);

        // Act
        Map<String, Object> result = service.aggregateWithoutScope(tenantId);

        // Assert
        assertNotNull(result);
        Map<String, Object> summary = (Map<String, Object>) result.get("summary");
        assertEquals(0, summary.get("customers"));
        assertEquals(0L, summary.get("revenue"));
        assertEquals(0, summary.get("opportunities"));
        assertEquals(0.0, summary.get("winRate"));
        assertEquals(0.0, summary.get("taskDoneRate"));
        assertEquals(0.0, summary.get("quoteApproveRate"));
        assertEquals(0.0, summary.get("orderCompleteRate"));

        // Verify empty maps for grouped data
        assertTrue(((Map<?, ?>) result.get("customerByOwner")).isEmpty());
        assertTrue(((Map<?, ?>) result.get("opportunityByStage")).isEmpty());
        assertTrue(((Map<?, ?>) result.get("followUpByChannel")).isEmpty());
    }

    @Test
    @DisplayName("shouldCalculateCorrectRates_whenValidData")
    void shouldCalculateCorrectRates_whenValidData() {
        // Arrange
        String tenantId = "tenant-1";
        // 10 opportunities, 8 with progress >= 80 -> winRate = 80.0
        when(customerRepository.countByTenantId(tenantId)).thenReturn(100L);
        when(opportunityRepository.countByTenantId(tenantId)).thenReturn(10L);
        when(opportunityRepository.countByTenantIdAndProgressGte(tenantId, 80)).thenReturn(8L);
        // 6 done tasks, 4 pending -> taskDoneRate = 60.0
        when(taskRepository.countByTenantIdAndDoneTrue(tenantId)).thenReturn(6L);
        when(taskRepository.countByTenantIdAndDoneFalse(tenantId)).thenReturn(4L);
        when(followUpRepository.countByTenantId(tenantId)).thenReturn(50L);
        // 20 quotes, 15 approved/accepted -> quoteApproveRate = 75.0
        when(quoteRepository.countByTenantId(tenantId)).thenReturn(20L);
        when(quoteRepository.countByTenantIdAndStatusInUppercase(eq(tenantId), anyList())).thenReturn(15L);
        // 10 orders, 7 completed -> orderCompleteRate = 70.0
        when(orderRecordRepository.countByTenantId(tenantId)).thenReturn(10L);
        when(orderRecordRepository.countByTenantIdAndStatus(tenantId, "COMPLETED")).thenReturn(7L);
        // Revenue and payment for collection rate
        when(customerRepository.sumValueByTenantId(tenantId)).thenReturn(100000L);
        when(opportunityRepository.sumWeightedAmountRawByTenantId(tenantId)).thenReturn(50000L);
        when(orderRecordRepository.sumAmountByTenantId(tenantId)).thenReturn(80000L);
        // 60000 received out of 80000 -> collectionRate = 75.0
        when(paymentRecordRepository.sumAmountByTenantIdAndStatusInUppercase(eq(tenantId), anyList())).thenReturn(60000L);
        // Grouped data
        when(customerRepository.countByOwnerGrouped(tenantId)).thenReturn(Collections.singletonList(new Object[]{"owner1", 50}));
        when(customerRepository.sumValueByStatusGrouped(tenantId)).thenReturn(Collections.singletonList(new Object[]{"ACTIVE", 80000L}));
        when(opportunityRepository.countByStageGrouped(tenantId)).thenReturn(Collections.singletonList(new Object[]{"PROPOSAL", 5}));
        when(quoteRepository.countByStatusGrouped(tenantId)).thenReturn(Collections.singletonList(new Object[]{"APPROVED", 10}));
        when(orderRecordRepository.countByStatusGrouped(tenantId)).thenReturn(Collections.singletonList(new Object[]{"COMPLETED", 7}));
        when(followUpRepository.countByChannelGrouped(tenantId)).thenReturn(Collections.singletonList(new Object[]{"PHONE", 25}));
        when(valueNormalizerService.normalizeFollowUpChannel(anyString())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Map<String, Object> result = service.aggregateWithoutScope(tenantId);

        // Assert
        Map<String, Object> summary = (Map<String, Object>) result.get("summary");
        // winRate = 8/10 * 100 = 80.0
        assertEquals(80.0, summary.get("winRate"));
        // taskDoneRate = 6/10 * 100 = 60.0
        assertEquals(60.0, summary.get("taskDoneRate"));
        // quoteApproveRate = 15/20 * 100 = 75.0
        assertEquals(75.0, summary.get("quoteApproveRate"));
        // orderCompleteRate = 7/10 * 100 = 70.0
        assertEquals(70.0, summary.get("orderCompleteRate"));
        // collectionRate = 60000/80000 * 100 = 75.0
        assertEquals(75.0, summary.get("orderCollectionRate"));
    }

    @Test
    @DisplayName("shouldReturnCorrectEmptyBody_whenEmptyOverviewBody")
    void shouldReturnCorrectEmptyBody_whenEmptyOverviewBody() {
        // Act
        Map<String, Object> result = service.emptyOverviewBody();

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("summary"));
        assertTrue(result.containsKey("customerByOwner"));
        assertTrue(result.containsKey("taskStatus"));

        Map<String, Object> summary = (Map<String, Object>) result.get("summary");
        assertEquals(0, summary.get("customers"));
        assertEquals(0, summary.get("revenue"));
        assertEquals(0, summary.get("opportunities"));
        assertEquals(0.0, summary.get("winRate"));
        assertEquals(0.0, summary.get("taskDoneRate"));
        assertEquals(0L, summary.get("orderPaymentReceived"));
        assertEquals(0L, summary.get("orderPaymentOutstanding"));

        Map<String, Integer> taskStatus = (Map<String, Integer>) result.get("taskStatus");
        assertEquals(0, taskStatus.get("done"));
        assertEquals(0, taskStatus.get("pending"));

        assertTrue(((Map<?, ?>) result.get("customerByOwner")).isEmpty());
        assertTrue(((Map<?, ?>) result.get("revenueByStatus")).isEmpty());
    }

    @Test
    @DisplayName("shouldHandleNullRepositoryResults_whenAggregateWithoutScope")
    void shouldHandleNullRepositoryResults_whenAggregateWithoutScope() {
        // Arrange
        String tenantId = "tenant-1";
        when(customerRepository.countByTenantId(tenantId)).thenReturn(0L);
        when(opportunityRepository.countByTenantId(tenantId)).thenReturn(0L);
        when(opportunityRepository.countByTenantIdAndProgressGte(tenantId, 80)).thenReturn(0L);
        when(taskRepository.countByTenantIdAndDoneTrue(tenantId)).thenReturn(0L);
        when(taskRepository.countByTenantIdAndDoneFalse(tenantId)).thenReturn(0L);
        when(followUpRepository.countByTenantId(tenantId)).thenReturn(0L);
        when(quoteRepository.countByTenantId(tenantId)).thenReturn(0L);
        when(orderRecordRepository.countByTenantId(tenantId)).thenReturn(0L);
        // Return null for sum queries
        when(customerRepository.sumValueByTenantId(tenantId)).thenReturn(null);
        when(opportunityRepository.sumWeightedAmountRawByTenantId(tenantId)).thenReturn(null);
        when(orderRecordRepository.sumAmountByTenantId(tenantId)).thenReturn(null);
        when(paymentRecordRepository.sumAmountByTenantIdAndStatusInUppercase(eq(tenantId), anyList())).thenReturn(null);

        // Act
        Map<String, Object> result = service.aggregateWithoutScope(tenantId);

        // Assert - should not throw and should have 0 values
        assertNotNull(result);
        Map<String, Object> summary = (Map<String, Object>) result.get("summary");
        assertEquals(0L, summary.get("revenue"));
        assertEquals(0L, summary.get("weightedAmount"));
        assertEquals(0L, summary.get("orderPaymentReceived"));
    }
}
