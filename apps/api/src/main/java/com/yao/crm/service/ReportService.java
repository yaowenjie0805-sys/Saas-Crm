package com.yao.crm.service;

import com.yao.crm.entity.Customer;
import com.yao.crm.entity.FollowUp;
import com.yao.crm.entity.Opportunity;
import com.yao.crm.entity.OrderRecord;
import com.yao.crm.entity.PaymentRecord;
import com.yao.crm.entity.Quote;
import com.yao.crm.entity.TaskItem;
import com.yao.crm.entity.Lead;
import com.yao.crm.repository.CustomerRepository;
import com.yao.crm.repository.FollowUpRepository;
import com.yao.crm.repository.OpportunityRepository;
import com.yao.crm.repository.OrderRecordRepository;
import com.yao.crm.repository.PaymentRecordRepository;
import com.yao.crm.repository.QuoteRepository;
import com.yao.crm.repository.TaskRepository;
import com.yao.crm.repository.UserAccountRepository;
import com.yao.crm.repository.LeadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import static com.yao.crm.service.ReportUtils.isBlank;
import static com.yao.crm.service.ReportUtils.endOfDay;
import static com.yao.crm.service.ReportUtils.normalizeFromDate;
import static com.yao.crm.service.ReportUtils.normalizeToDate;
import static com.yao.crm.service.ReportUtils.normalized;
import static com.yao.crm.service.ReportUtils.startOfDay;

@Service
public class ReportService {
    private static final List<String> QUOTE_APPROVED_OR_ACCEPTED =
            java.util.Collections.unmodifiableList(java.util.Arrays.asList("APPROVED", "ACCEPTED"));
    private static final List<String> QUOTE_ACCEPTED_ONLY = java.util.Collections.singletonList("ACCEPTED");
    private static final List<String> PAYMENT_RECEIVED_ONLY = java.util.Collections.singletonList("RECEIVED");

    private final CustomerRepository customerRepository;
    private final OpportunityRepository opportunityRepository;
    private final TaskRepository taskRepository;
    private final FollowUpRepository followUpRepository;
    private final QuoteRepository quoteRepository;
    private final OrderRecordRepository orderRecordRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final UserAccountRepository userAccountRepository;
    private final LeadRepository leadRepository;
    private final ValueNormalizerService valueNormalizerService;
    private final DashboardMetricsCacheService dashboardMetricsCacheService;
    private final ReportAggregationService reportAggregationService;
    private final Cache<String, IdentityScopeCacheEntry> roleIdentityCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();
    private final Cache<String, IdentityScopeCacheEntry> departmentIdentityCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    public ReportService(CustomerRepository customerRepository,
                         OpportunityRepository opportunityRepository,
                         TaskRepository taskRepository,
                         FollowUpRepository followUpRepository,
                         QuoteRepository quoteRepository,
                         OrderRecordRepository orderRecordRepository,
                         PaymentRecordRepository paymentRecordRepository,
                         UserAccountRepository userAccountRepository,
                         LeadRepository leadRepository,
                         ValueNormalizerService valueNormalizerService,
                         DashboardMetricsCacheService dashboardMetricsCacheService,
                         ReportAggregationService reportAggregationService) {
        this.customerRepository = customerRepository;
        this.opportunityRepository = opportunityRepository;
        this.taskRepository = taskRepository;
        this.followUpRepository = followUpRepository;
        this.quoteRepository = quoteRepository;
        this.orderRecordRepository = orderRecordRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.userAccountRepository = userAccountRepository;
        this.leadRepository = leadRepository;
        this.valueNormalizerService = valueNormalizerService;
        this.dashboardMetricsCacheService = dashboardMetricsCacheService;
        this.reportAggregationService = reportAggregationService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> overview(LocalDate fromDate, LocalDate toDate, String role) {
        return overviewByTenant("tenant_default", fromDate, toDate, role, "", "");
    }

    @Transactional(readOnly = true)
    public Map<String, Object> overviewByTenant(String tenantId, LocalDate fromDate, LocalDate toDate, String role, String owner, String department) {
        if (fromDate == null && toDate == null && isBlank(role) && isBlank(owner) && isBlank(department)) {
            return overviewByTenantFastPath(tenantId);
        }
        LocalDate safeFromDate = normalizeFromDate(fromDate, toDate);
        LocalDate safeToDate = normalizeToDate(fromDate, toDate);
        Set<String> roleIdentities = loadRoleIdentities(tenantId, role);
        Set<String> departmentIdentities = loadDepartmentIdentities(tenantId, department == null ? "" : department.trim());
        LocalDateTime fromTime = startOfDay(safeFromDate);
        LocalDateTime toTime = endOfDay(safeToDate);
        final String ownerFilter = owner == null ? "" : owner.trim().toLowerCase();
        Set<String> scopedOwners = mergeOwnerScope(roleIdentities, departmentIdentities, ownerFilter);
        if (fromTime != null && toTime != null) {
            return overviewByTenantDateFastPath(tenantId, fromTime, toTime, scopedOwners);
        }
        if (fromDate == null && toDate == null && scopedOwners != null) {
            return overviewByTenantScopedFastPath(tenantId, scopedOwners);
        }

        List<Customer> customers = loadCustomers(tenantId, fromTime, toTime, scopedOwners);
        List<Opportunity> opportunities = loadOpportunities(tenantId, fromTime, toTime, scopedOwners);
        List<TaskItem> tasks = loadTasks(tenantId, fromTime, toTime, scopedOwners);
        List<FollowUp> followUps = loadFollowUps(tenantId, fromTime, toTime, scopedOwners);
        List<Quote> quotes = loadQuotes(tenantId, fromTime, toTime, scopedOwners);
        List<OrderRecord> orders = loadOrders(tenantId, fromTime, toTime, scopedOwners);
        List<PaymentRecord> payments = loadPayments(tenantId, fromTime, toTime, scopedOwners);

        long totalRevenue = 0L;
        Map<String, Integer> customerByOwner = new HashMap<String, Integer>();
        for (Customer customer : customers) {
            totalRevenue += customer.getValue() == null ? 0 : customer.getValue();
            String ownerName = isBlank(customer.getOwner()) ? "Unknown" : customer.getOwner();
            customerByOwner.put(ownerName, customerByOwner.containsKey(ownerName) ? customerByOwner.get(ownerName) + 1 : 1);
        }

        Map<String, Long> revenueByStatus = new HashMap<String, Long>();
        for (Customer customer : customers) {
            String status = isBlank(customer.getStatus()) ? "Unknown" : customer.getStatus();
            Long value = customer.getValue() == null ? 0L : customer.getValue();
            revenueByStatus.put(status, revenueByStatus.containsKey(status) ? revenueByStatus.get(status) + value : value);
        }

        Map<String, Integer> opportunityByStage = new HashMap<String, Integer>();
        long weightedAmount = 0L;
        int highProgressCount = 0;
        for (Opportunity opportunity : opportunities) {
            String stage = isBlank(opportunity.getStage()) ? "Unknown" : opportunity.getStage();
            opportunityByStage.put(stage, opportunityByStage.containsKey(stage) ? opportunityByStage.get(stage) + 1 : 1);
            int progress = opportunity.getProgress() == null ? 0 : opportunity.getProgress();
            long amount = opportunity.getAmount() == null ? 0L : opportunity.getAmount();
            weightedAmount += Math.round(amount * (progress / 100.0));
            if (progress >= 80) highProgressCount++;
        }

        int doneTasks = 0;
        int pendingTasks = 0;
        for (TaskItem task : tasks) {
            if (Boolean.TRUE.equals(task.getDone())) doneTasks++; else pendingTasks++;
        }

        Map<String, Integer> followUpByChannel = new HashMap<String, Integer>();
        for (FollowUp followUp : followUps) {
            String channel = isBlank(followUp.getChannel()) ? "Unknown" : valueNormalizerService.normalizeFollowUpChannel(followUp.getChannel());
            followUpByChannel.put(channel, followUpByChannel.containsKey(channel) ? followUpByChannel.get(channel) + 1 : 1);
        }

        double winRate = opportunities.isEmpty() ? 0.0 : Math.round((highProgressCount * 1000.0 / opportunities.size())) / 10.0;
        double taskDoneRate = tasks.isEmpty() ? 0.0 : Math.round((doneTasks * 1000.0 / tasks.size())) / 10.0;

        Map<String, Object> summary = new HashMap<String, Object>();
        summary.put("customers", customers.size());
        summary.put("revenue", totalRevenue);
        summary.put("opportunities", opportunities.size());
        summary.put("weightedAmount", weightedAmount);
        summary.put("winRate", winRate);
        summary.put("taskDoneRate", taskDoneRate);
        summary.put("followUps", followUps.size());
        long quoteApproved = quotes.stream().filter(q -> "APPROVED".equalsIgnoreCase(q.getStatus()) || "ACCEPTED".equalsIgnoreCase(q.getStatus())).count();
        long quoteAccepted = quotes.stream().filter(q -> "ACCEPTED".equalsIgnoreCase(q.getStatus())).count();
        long orderCompleted = orders.stream().filter(o -> "COMPLETED".equalsIgnoreCase(o.getStatus())).count();
        double quoteApproveRate = quotes.isEmpty() ? 0.0 : Math.round((quoteApproved * 1000.0 / quotes.size())) / 10.0;
        double quoteToOrderRate = quoteAccepted == 0 ? 0.0 : Math.round((orders.size() * 1000.0 / quoteAccepted)) / 10.0;
        double orderCompleteRate = orders.isEmpty() ? 0.0 : Math.round((orderCompleted * 1000.0 / orders.size())) / 10.0;
        summary.put("quotes", quotes.size());
        summary.put("orders", orders.size());
        summary.put("quoteApproveRate", quoteApproveRate);
        summary.put("quoteToOrderRate", quoteToOrderRate);
        summary.put("orderCompleteRate", orderCompleteRate);
        Set<String> orderIds = orders.stream().map(OrderRecord::getId).collect(Collectors.toSet());
        long orderAmountTotal = orders.stream().mapToLong(o -> o.getAmount() == null ? 0L : o.getAmount()).sum();
        long orderPaymentReceived = payments.stream()
                .filter(p -> !isBlank(p.getOrderId()) && orderIds.contains(p.getOrderId()) && "RECEIVED".equalsIgnoreCase(p.getStatus()))
                .mapToLong(p -> p.getAmount() == null ? 0L : p.getAmount())
                .sum();
        long orderPaymentOutstanding = Math.max(0L, orderAmountTotal - orderPaymentReceived);
        double orderCollectionRate = orderAmountTotal <= 0 ? 0.0 : Math.round((orderPaymentReceived * 1000.0 / orderAmountTotal)) / 10.0;
        summary.put("orderPaymentReceived", orderPaymentReceived);
        summary.put("orderPaymentOutstanding", orderPaymentOutstanding);
        summary.put("orderCollectionRate", orderCollectionRate);

        Map<String, Object> body = new HashMap<String, Object>();
        body.put("summary", summary);
        body.put("customerByOwner", customerByOwner);
        body.put("revenueByStatus", revenueByStatus);
        body.put("opportunityByStage", opportunityByStage);
        Map<String, Integer> taskStatus = new HashMap<String, Integer>();
        taskStatus.put("done", doneTasks);
        taskStatus.put("pending", pendingTasks);
        body.put("taskStatus", taskStatus);
        body.put("followUpByChannel", followUpByChannel);
        Map<String, Integer> quoteByStatus = new HashMap<String, Integer>();
        for (Quote quote : quotes) {
            String key = isBlank(quote.getStatus()) ? "Unknown" : quote.getStatus();
            quoteByStatus.put(key, quoteByStatus.containsKey(key) ? quoteByStatus.get(key) + 1 : 1);
        }
        Map<String, Integer> orderByStatus = new HashMap<String, Integer>();
        for (OrderRecord order : orders) {
            String key = isBlank(order.getStatus()) ? "Unknown" : order.getStatus();
            orderByStatus.put(key, orderByStatus.containsKey(key) ? orderByStatus.get(key) + 1 : 1);
        }
        body.put("quoteByStatus", quoteByStatus);
        body.put("orderByStatus", orderByStatus);
        return body;
    }

    private Map<String, Object> overviewByTenantDateFastPath(String tenantId,
                                                              LocalDateTime from,
                                                              LocalDateTime to,
                                                              Set<String> owners) {
        if (owners != null && owners.isEmpty()) {
            return reportAggregationService.emptyOverviewBody();
        }

        final boolean scoped = owners != null;
        long customerCount = scoped
                ? customerRepository.countByTenantIdAndOwnerInAndCreatedAtBetween(tenantId, owners, from, to)
                : customerRepository.countByTenantIdAndCreatedAtBetween(tenantId, from, to);
        long totalRevenue = ReportUtils.safeLong(scoped
                ? customerRepository.sumValueByTenantIdAndOwnerInAndCreatedAtBetween(tenantId, owners, from, to)
                : customerRepository.sumValueByTenantIdAndCreatedAtBetween(tenantId, from, to));

        long opportunitiesCount = scoped
                ? opportunityRepository.countByTenantIdAndOwnerInAndCreatedAtBetween(tenantId, owners, from, to)
                : opportunityRepository.countByTenantIdAndCreatedAtBetween(tenantId, from, to);
        long weightedAmountRaw = ReportUtils.safeLong(scoped
                ? opportunityRepository.sumWeightedAmountRawByTenantIdAndOwnerInAndCreatedAtBetween(tenantId, owners, from, to)
                : opportunityRepository.sumWeightedAmountRawByTenantIdAndCreatedAtBetween(tenantId, from, to));
        long weightedAmount = Math.round(weightedAmountRaw / 100.0d);
        long highProgressCount = scoped
                ? opportunityRepository.countByTenantIdAndOwnerInAndProgressGteAndCreatedAtBetween(tenantId, owners, 80, from, to)
                : opportunityRepository.countByTenantIdAndProgressGteAndCreatedAtBetween(tenantId, 80, from, to);

        long doneTasks = scoped
                ? taskRepository.countByTenantIdAndDoneTrueAndOwnerInAndCreatedAtBetween(tenantId, owners, from, to)
                : taskRepository.countByTenantIdAndDoneTrueAndCreatedAtBetween(tenantId, from, to);
        long pendingTasks = scoped
                ? taskRepository.countByTenantIdAndDoneFalseAndOwnerInAndCreatedAtBetween(tenantId, owners, from, to)
                : taskRepository.countByTenantIdAndDoneFalseAndCreatedAtBetween(tenantId, from, to);
        long taskTotal = doneTasks + pendingTasks;

        long followUps = scoped
                ? followUpRepository.countByTenantIdAndAuthorInAndCreatedAtBetween(tenantId, owners, from, to)
                : followUpRepository.countByTenantIdAndCreatedAtBetween(tenantId, from, to);
        long quotes = scoped
                ? quoteRepository.countByTenantIdAndOwnerInAndCreatedAtBetween(tenantId, owners, from, to)
                : quoteRepository.countByTenantIdAndCreatedAtBetween(tenantId, from, to);
        long quoteApproved = scoped
                ? quoteRepository.countByTenantIdAndOwnerInAndStatusInUppercaseAndCreatedAtBetween(
                tenantId, owners, QUOTE_APPROVED_OR_ACCEPTED, from, to)
                : quoteRepository.countByTenantIdAndStatusInUppercaseAndCreatedAtBetween(
                tenantId, QUOTE_APPROVED_OR_ACCEPTED, from, to);
        long quoteAccepted = scoped
                ? quoteRepository.countByTenantIdAndOwnerInAndStatusInUppercaseAndCreatedAtBetween(
                tenantId, owners, QUOTE_ACCEPTED_ONLY, from, to)
                : quoteRepository.countByTenantIdAndStatusInUppercaseAndCreatedAtBetween(
                tenantId, QUOTE_ACCEPTED_ONLY, from, to);

        long orders = scoped
                ? orderRecordRepository.countByTenantIdAndOwnerInAndCreatedAtBetween(tenantId, owners, from, to)
                : orderRecordRepository.countByTenantIdAndCreatedAtBetween(tenantId, from, to);
        long orderCompleted = scoped
                ? orderRecordRepository.countByTenantIdAndOwnerInAndStatusAndCreatedAtBetween(tenantId, owners, "COMPLETED", from, to)
                : orderRecordRepository.countByTenantIdAndStatusAndCreatedAtBetween(tenantId, "COMPLETED", from, to);
        long orderAmountTotal = ReportUtils.safeLong(scoped
                ? orderRecordRepository.sumAmountByTenantIdAndOwnerInAndCreatedAtBetween(tenantId, owners, from, to)
                : orderRecordRepository.sumAmountByTenantIdAndCreatedAtBetween(tenantId, from, to));
        long orderPaymentReceived = ReportUtils.safeLong(scoped
                ? paymentRecordRepository.sumAmountByTenantIdAndOwnerInAndStatusInUppercaseAndCreatedAtBetween(
                tenantId, owners, PAYMENT_RECEIVED_ONLY, from, to)
                : paymentRecordRepository.sumAmountByTenantIdAndStatusInUppercaseAndCreatedAtBetween(
                tenantId, PAYMENT_RECEIVED_ONLY, from, to));
        long orderPaymentOutstanding = Math.max(0L, orderAmountTotal - orderPaymentReceived);

        double winRate = opportunitiesCount == 0 ? 0.0 : Math.round((highProgressCount * 1000.0 / opportunitiesCount)) / 10.0;
        double taskDoneRate = taskTotal == 0 ? 0.0 : Math.round((doneTasks * 1000.0 / taskTotal)) / 10.0;
        double quoteApproveRate = quotes == 0 ? 0.0 : Math.round((quoteApproved * 1000.0 / quotes)) / 10.0;
        double quoteToOrderRate = quoteAccepted == 0 ? 0.0 : Math.round((orders * 1000.0 / quoteAccepted)) / 10.0;
        double orderCompleteRate = orders == 0 ? 0.0 : Math.round((orderCompleted * 1000.0 / orders)) / 10.0;
        double orderCollectionRate = orderAmountTotal <= 0 ? 0.0 : Math.round((orderPaymentReceived * 1000.0 / orderAmountTotal)) / 10.0;

        Map<String, Integer> customerByOwner = customerCount <= 0
                ? new HashMap<String, Integer>()
                : ReportUtils.toIntMap(scoped
                ? customerRepository.countByOwnerGroupedAndOwnerInAndCreatedAtBetween(tenantId, owners, from, to)
                : customerRepository.countByOwnerGroupedAndCreatedAtBetween(tenantId, from, to));
        Map<String, Long> revenueByStatus = customerCount <= 0
                ? new HashMap<String, Long>()
                : ReportUtils.toLongMap(scoped
                ? customerRepository.sumValueByStatusGroupedAndOwnerInAndCreatedAtBetween(tenantId, owners, from, to)
                : customerRepository.sumValueByStatusGroupedAndCreatedAtBetween(tenantId, from, to));
        Map<String, Integer> opportunityByStage = opportunitiesCount <= 0
                ? new HashMap<String, Integer>()
                : ReportUtils.toIntMap(scoped
                ? opportunityRepository.countByStageGroupedAndOwnerInAndCreatedAtBetween(tenantId, owners, from, to)
                : opportunityRepository.countByStageGroupedAndCreatedAtBetween(tenantId, from, to));
        Map<String, Integer> quoteByStatus = quotes <= 0
                ? new HashMap<String, Integer>()
                : ReportUtils.toIntMap(scoped
                ? quoteRepository.countByStatusGroupedAndOwnerInAndCreatedAtBetween(tenantId, owners, from, to)
                : quoteRepository.countByStatusGroupedAndCreatedAtBetween(tenantId, from, to));
        Map<String, Integer> orderByStatus = orders <= 0
                ? new HashMap<String, Integer>()
                : ReportUtils.toIntMap(scoped
                ? orderRecordRepository.countByStatusGroupedAndOwnerInAndCreatedAtBetween(tenantId, owners, from, to)
                : orderRecordRepository.countByStatusGroupedAndCreatedAtBetween(tenantId, from, to));

        Map<String, Integer> followUpByChannel = new HashMap<String, Integer>();
        if (followUps > 0) {
            List<Object[]> channelRows = scoped
                    ? followUpRepository.countByChannelGroupedAndAuthorInAndCreatedAtBetween(tenantId, owners, from, to)
                    : followUpRepository.countByChannelGroupedAndCreatedAtBetween(tenantId, from, to);
            for (Object[] row : channelRows) {
                String channel = ReportUtils.normalizeBucket(row.length > 0 ? row[0] : null);
                channel = valueNormalizerService.normalizeFollowUpChannel(channel);
                int count = ReportUtils.asInt(row.length > 1 ? row[1] : null);
                followUpByChannel.put(channel, followUpByChannel.containsKey(channel)
                        ? followUpByChannel.get(channel) + count
                        : count);
            }
        }

        Map<String, Object> summary = new HashMap<String, Object>();
        summary.put("customers", customerCount);
        summary.put("revenue", totalRevenue);
        summary.put("opportunities", opportunitiesCount);
        summary.put("weightedAmount", weightedAmount);
        summary.put("winRate", winRate);
        summary.put("taskDoneRate", taskDoneRate);
        summary.put("followUps", followUps);
        summary.put("quotes", quotes);
        summary.put("orders", orders);
        summary.put("quoteApproveRate", quoteApproveRate);
        summary.put("quoteToOrderRate", quoteToOrderRate);
        summary.put("orderCompleteRate", orderCompleteRate);
        summary.put("orderPaymentReceived", orderPaymentReceived);
        summary.put("orderPaymentOutstanding", orderPaymentOutstanding);
        summary.put("orderCollectionRate", orderCollectionRate);

        Map<String, Integer> taskStatus = new HashMap<String, Integer>();
        taskStatus.put("done", (int) doneTasks);
        taskStatus.put("pending", (int) pendingTasks);

        Map<String, Object> body = new HashMap<String, Object>();
        body.put("summary", summary);
        body.put("customerByOwner", customerByOwner);
        body.put("revenueByStatus", revenueByStatus);
        body.put("opportunityByStage", opportunityByStage);
        body.put("taskStatus", taskStatus);
        body.put("followUpByChannel", followUpByChannel);
        body.put("quoteByStatus", quoteByStatus);
        body.put("orderByStatus", orderByStatus);
        return body;
    }

    private Map<String, Object> overviewByTenantFastPath(String tenantId) {
        return reportAggregationService.aggregateWithoutScope(tenantId);
    }

    private Map<String, Object> overviewByTenantScopedFastPath(String tenantId, Set<String> owners) {
        return reportAggregationService.aggregateWithScope(tenantId, owners);
    }

    private Map<String, Object> emptyOverviewBody() {
        return reportAggregationService.emptyOverviewBody();
    }

    public DashboardMetricsCacheService.CachedValue<Map<String, Object>> overviewByTenantCached(String tenantId,
                                                                                                 String actor,
                                                                                                 String actorRole,
                                                                                                 LocalDate fromDate,
                                                                                                 LocalDate toDate,
                                                                                                 String role,
                                                                                                 String owner,
                                                                                                 String department) {
        LocalDate safeFromDate = normalizeFromDate(fromDate, toDate);
        LocalDate safeToDate = normalizeToDate(fromDate, toDate);
        final String cacheKey = ReportUtils.normalizeDate(safeFromDate)
                + "|" + ReportUtils.normalizeDate(safeToDate)
                + "|" + ReportUtils.normalizeRole(role)
                + "|" + normalized(owner)
                + "|" + normalized(department);
        return dashboardMetricsCacheService.getOrLoad(tenantId, "reports-overview", cacheKey,
                new java.util.function.Supplier<Map<String, Object>>() {
                    @Override
                    public Map<String, Object> get() {
                        return overviewByTenant(tenantId, safeFromDate, safeToDate, role, owner, department);
                    }
                });
    }

    public DashboardMetricsCacheService.CachedValue<Map<String, Object>> funnelByTenantCached(String tenantId,
                                                                                               String actor,
                                                                                               String actorRole,
                                                                                               LocalDate fromDate,
                                                                                               LocalDate toDate,
                                                                                               String owner) {
        LocalDate safeFromDate = normalizeFromDate(fromDate, toDate);
        LocalDate safeToDate = normalizeToDate(fromDate, toDate);
        final String cacheKey = ReportUtils.normalizeDate(safeFromDate)
                + "|" + ReportUtils.normalizeDate(safeToDate)
                + "|" + normalized(owner);
        return dashboardMetricsCacheService.getOrLoad(tenantId, "reports-funnel", cacheKey,
                new java.util.function.Supplier<Map<String, Object>>() {
                    @Override
                    public Map<String, Object> get() {
                        return funnelByTenant(tenantId, safeFromDate, safeToDate, owner);
                    }
                });
    }

    public Map<String, Object> funnelByTenant(String tenantId, LocalDate fromDate, LocalDate toDate, String owner) {
        final String ownerFilter = owner == null ? "" : owner.trim().toLowerCase();
        LocalDate safeFromDate = normalizeFromDate(fromDate, toDate);
        LocalDate safeToDate = normalizeToDate(fromDate, toDate);
        LocalDateTime fromTime = startOfDay(safeFromDate);
        LocalDateTime toTime = endOfDay(safeToDate);
        Set<String> ownerScope = ownerFilter.isEmpty()
                ? null
                : new HashSet<String>(java.util.Collections.singleton(ownerFilter));
        final boolean scoped = ownerScope != null;
        final boolean dated = fromTime != null && toTime != null;

        long leadsCount = dated
                ? (scoped
                ? leadRepository.countByTenantIdAndOwnerInAndCreatedAtBetween(tenantId, ownerScope, fromTime, toTime)
                : leadRepository.countByTenantIdAndCreatedAtBetween(tenantId, fromTime, toTime))
                : (scoped
                ? leadRepository.countByTenantIdAndOwnerIn(tenantId, ownerScope)
                : leadRepository.countByTenantId(tenantId));
        long oppCount = dated
                ? (scoped
                ? opportunityRepository.countByTenantIdAndOwnerInAndCreatedAtBetween(tenantId, ownerScope, fromTime, toTime)
                : opportunityRepository.countByTenantIdAndCreatedAtBetween(tenantId, fromTime, toTime))
                : (scoped
                ? opportunityRepository.countByTenantIdAndOwnerIn(tenantId, ownerScope)
                : opportunityRepository.countByTenantId(tenantId));
        long quoteCount = dated
                ? (scoped
                ? quoteRepository.countByTenantIdAndOwnerInAndCreatedAtBetween(tenantId, ownerScope, fromTime, toTime)
                : quoteRepository.countByTenantIdAndCreatedAtBetween(tenantId, fromTime, toTime))
                : (scoped
                ? quoteRepository.countByTenantIdAndOwnerIn(tenantId, ownerScope)
                : quoteRepository.countByTenantId(tenantId));
        long orderCount = dated
                ? (scoped
                ? orderRecordRepository.countByTenantIdAndOwnerInAndCreatedAtBetween(tenantId, ownerScope, fromTime, toTime)
                : orderRecordRepository.countByTenantIdAndCreatedAtBetween(tenantId, fromTime, toTime))
                : (scoped
                ? orderRecordRepository.countByTenantIdAndOwnerIn(tenantId, ownerScope)
                : orderRecordRepository.countByTenantId(tenantId));

        double leadToOpp = leadsCount == 0 ? 0.0 : Math.round((oppCount * 1000.0 / leadsCount)) / 10.0;
        double oppToQuote = oppCount == 0 ? 0.0 : Math.round((quoteCount * 1000.0 / oppCount)) / 10.0;
        double quoteToOrder = quoteCount == 0 ? 0.0 : Math.round((orderCount * 1000.0 / quoteCount)) / 10.0;

        Map<String, Object> out = new HashMap<String, Object>();
        Map<String, Object> counts = new HashMap<String, Object>();
        counts.put("leads", leadsCount);
        counts.put("opportunities", oppCount);
        counts.put("quotes", quoteCount);
        counts.put("orders", orderCount);
        out.put("counts", counts);

        Map<String, Object> rates = new HashMap<String, Object>();
        rates.put("leadToOpportunity", leadToOpp);
        rates.put("opportunityToQuote", oppToQuote);
        rates.put("quoteToOrder", quoteToOrder);
        out.put("rates", rates);
        return out;
    }

    private Set<String> mergeOwnerScope(Set<String> roleIdentities, Set<String> departmentIdentities, String ownerFilter) {
        Set<String> out = intersectScopes(roleIdentities, departmentIdentities);
        if (isBlank(ownerFilter)) return out;
        Set<String> ownerOnly = new HashSet<String>();
        ownerOnly.add(ownerFilter);
        return intersectScopes(out, ownerOnly);
    }

    private Set<String> intersectScopes(Set<String> left, Set<String> right) {
        if (left == null) return right == null ? null : new HashSet<String>(right);
        if (right == null) return new HashSet<String>(left);
        Set<String> out = new HashSet<String>(left);
        out.retainAll(right);
        return out;
    }

    private List<Customer> loadCustomers(String tenantId, LocalDateTime from, LocalDateTime to, Set<String> owners) {
        if (owners != null && owners.isEmpty()) return java.util.Collections.emptyList();
        if (from != null && to != null) {
            return owners == null
                    ? customerRepository.findByTenantIdAndCreatedAtBetween(tenantId, from, to)
                    : customerRepository.findByTenantIdAndOwnerInAndCreatedAtBetween(tenantId, owners, from, to);
        }
        return owners == null
                ? customerRepository.findByTenantId(tenantId)
                : customerRepository.findByTenantIdAndOwnerIn(tenantId, owners);
    }

    private List<Opportunity> loadOpportunities(String tenantId, LocalDateTime from, LocalDateTime to, Set<String> owners) {
        if (owners != null && owners.isEmpty()) return java.util.Collections.emptyList();
        if (from != null && to != null) {
            return owners == null
                    ? opportunityRepository.findByTenantIdAndCreatedAtBetween(tenantId, from, to)
                    : opportunityRepository.findByTenantIdAndOwnerInAndCreatedAtBetween(tenantId, owners, from, to);
        }
        return owners == null
                ? opportunityRepository.findByTenantId(tenantId)
                : opportunityRepository.findByTenantIdAndOwnerIn(tenantId, owners);
    }

    private List<TaskItem> loadTasks(String tenantId, LocalDateTime from, LocalDateTime to, Set<String> owners) {
        if (owners != null && owners.isEmpty()) return java.util.Collections.emptyList();
        if (from != null && to != null) {
            return owners == null
                    ? taskRepository.findByTenantIdAndCreatedAtBetween(tenantId, from, to)
                    : taskRepository.findByTenantIdAndOwnerInAndCreatedAtBetween(tenantId, owners, from, to);
        }
        return owners == null
                ? taskRepository.findByTenantId(tenantId)
                : taskRepository.findByTenantIdAndOwnerIn(tenantId, owners);
    }

    private List<FollowUp> loadFollowUps(String tenantId, LocalDateTime from, LocalDateTime to, Set<String> owners) {
        if (owners != null && owners.isEmpty()) return java.util.Collections.emptyList();
        if (from != null && to != null) {
            return owners == null
                    ? followUpRepository.findByTenantIdAndCreatedAtBetween(tenantId, from, to)
                    : followUpRepository.findByTenantIdAndAuthorInAndCreatedAtBetween(tenantId, owners, from, to);
        }
        return owners == null
                ? followUpRepository.findByTenantId(tenantId)
                : followUpRepository.findByTenantIdAndAuthorIn(tenantId, owners);
    }

    private List<Quote> loadQuotes(String tenantId, LocalDateTime from, LocalDateTime to, Set<String> owners) {
        if (owners != null && owners.isEmpty()) return java.util.Collections.emptyList();
        if (from != null && to != null) {
            return owners == null
                    ? quoteRepository.findByTenantIdAndCreatedAtBetween(tenantId, from, to)
                    : quoteRepository.findByTenantIdAndOwnerInAndCreatedAtBetween(tenantId, owners, from, to);
        }
        return owners == null
                ? quoteRepository.findByTenantId(tenantId)
                : quoteRepository.findByTenantIdAndOwnerIn(tenantId, owners);
    }

    private List<OrderRecord> loadOrders(String tenantId, LocalDateTime from, LocalDateTime to, Set<String> owners) {
        if (owners != null && owners.isEmpty()) return java.util.Collections.emptyList();
        if (from != null && to != null) {
            return owners == null
                    ? orderRecordRepository.findByTenantIdAndCreatedAtBetween(tenantId, from, to)
                    : orderRecordRepository.findByTenantIdAndOwnerInAndCreatedAtBetween(tenantId, owners, from, to);
        }
        return owners == null
                ? orderRecordRepository.findByTenantId(tenantId)
                : orderRecordRepository.findByTenantIdAndOwnerIn(tenantId, owners);
    }

    private List<PaymentRecord> loadPayments(String tenantId, LocalDateTime from, LocalDateTime to, Set<String> owners) {
        if (owners != null && owners.isEmpty()) return java.util.Collections.emptyList();
        if (from != null && to != null) {
            return owners == null
                    ? paymentRecordRepository.findByTenantIdAndCreatedAtBetween(tenantId, from, to)
                    : paymentRecordRepository.findByTenantIdAndOwnerInAndCreatedAtBetween(tenantId, owners, from, to);
        }
        return owners == null
                ? paymentRecordRepository.findByTenantId(tenantId)
                : paymentRecordRepository.findByTenantIdAndOwnerIn(tenantId, owners);
    }

    private List<Lead> loadLeads(String tenantId, LocalDateTime from, LocalDateTime to, Set<String> owners) {
        if (owners != null && owners.isEmpty()) return java.util.Collections.emptyList();
        if (from != null && to != null) {
            return owners == null
                    ? leadRepository.findByTenantIdAndCreatedAtBetween(tenantId, from, to)
                    : leadRepository.findByTenantIdAndOwnerInAndCreatedAtBetween(tenantId, owners, from, to);
        }
        return owners == null
                ? leadRepository.findByTenantId(tenantId)
                : leadRepository.findByTenantIdAndOwnerIn(tenantId, owners);
    }

    private Set<String> loadRoleIdentities(String tenantId, String role) {
        if (isBlank(role)) return null;
        String normalizedRole = ReportUtils.normalizeRole(role);
        String cacheKey = tenantId + "|" + normalizedRole;
        IdentityScopeCacheEntry entry = roleIdentityCache.get(cacheKey, new java.util.function.Function<String, IdentityScopeCacheEntry>() {
            @Override
            public IdentityScopeCacheEntry apply(String k) {
                Set<String> identities = loadRoleIdentitiesFromRepository(tenantId, normalizedRole);
                return new IdentityScopeCacheEntry(Collections.unmodifiableSet(new HashSet<String>(identities)));
            }
        });
        return entry.identities;
    }

    private Set<String> loadDepartmentIdentities(String tenantId, String department) {
        if (isBlank(department)) return null;
        String normalizedDepartment = normalized(department);
        String cacheKey = tenantId + "|" + normalizedDepartment;
        IdentityScopeCacheEntry entry = departmentIdentityCache.get(cacheKey, new java.util.function.Function<String, IdentityScopeCacheEntry>() {
            @Override
            public IdentityScopeCacheEntry apply(String k) {
                Set<String> identities = loadDepartmentIdentitiesFromRepository(tenantId, normalizedDepartment);
                return new IdentityScopeCacheEntry(Collections.unmodifiableSet(new HashSet<String>(identities)));
            }
        });
        return entry.identities;
    }

    private Set<String> loadRoleIdentitiesFromRepository(String tenantId, String role) {
        Set<String> identities = new HashSet<String>();
        for (Object[] row : userAccountRepository.findIdentityPairsByTenantIdAndRole(tenantId, role)) {
            if (row == null) continue;
            if (row.length > 0 && !isBlank((String) row[0])) identities.add(normalized((String) row[0]));
            if (row.length > 1 && !isBlank((String) row[1])) identities.add(normalized((String) row[1]));
        }
        return identities;
    }

    private Set<String> loadDepartmentIdentitiesFromRepository(String tenantId, String department) {
        Set<String> identities = new HashSet<String>();
        for (Object[] row : userAccountRepository.findIdentityPairsByTenantIdAndDepartment(tenantId, department)) {
            if (row == null) continue;
            if (row.length > 0 && !isBlank((String) row[0])) identities.add(normalized((String) row[0]));
            if (row.length > 1 && !isBlank((String) row[1])) identities.add(normalized((String) row[1]));
        }
        return identities;
    }

    private static final class IdentityScopeCacheEntry {
        private final Set<String> identities;

        private IdentityScopeCacheEntry(Set<String> identities) {
            this.identities = identities;
        }
    }
}
