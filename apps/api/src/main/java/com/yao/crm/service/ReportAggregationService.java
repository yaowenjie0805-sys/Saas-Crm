package com.yao.crm.service;

import com.yao.crm.repository.CustomerRepository;
import com.yao.crm.repository.FollowUpRepository;
import com.yao.crm.repository.OpportunityRepository;
import com.yao.crm.repository.OrderRecordRepository;
import com.yao.crm.repository.PaymentRecordRepository;
import com.yao.crm.repository.QuoteRepository;
import com.yao.crm.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.yao.crm.service.ReportUtils.asInt;
import static com.yao.crm.service.ReportUtils.normalizeBucket;
import static com.yao.crm.service.ReportUtils.safeLong;
import static com.yao.crm.service.ReportUtils.toIntMap;
import static com.yao.crm.service.ReportUtils.toLongMap;

/**
 * 报表聚合服务 - 处理概览数据的聚合逻辑
 * 提取 overviewByTenantFastPath 和 overviewByTenantScopedFastPath 的公共逻辑
 */
@Service
public class ReportAggregationService {

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
    private final ValueNormalizerService valueNormalizerService;

    public ReportAggregationService(CustomerRepository customerRepository,
                                    OpportunityRepository opportunityRepository,
                                    TaskRepository taskRepository,
                                    FollowUpRepository followUpRepository,
                                    QuoteRepository quoteRepository,
                                    OrderRecordRepository orderRecordRepository,
                                    PaymentRecordRepository paymentRecordRepository,
                                    ValueNormalizerService valueNormalizerService) {
        this.customerRepository = customerRepository;
        this.opportunityRepository = opportunityRepository;
        this.taskRepository = taskRepository;
        this.followUpRepository = followUpRepository;
        this.quoteRepository = quoteRepository;
        this.orderRecordRepository = orderRecordRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.valueNormalizerService = valueNormalizerService;
    }

    /**
     * 函数式接口：用于获取计数
     */
    @FunctionalInterface
    public interface CountFetcher {
        long fetch();
    }

    /**
     * 函数式接口：用于获取求和值
     */
    @FunctionalInterface
    public interface SumFetcher {
        Long fetch();
    }

    /**
     * 函数式接口：用于获取分组计数列表
     */
    @FunctionalInterface
    public interface GroupedCountFetcher {
        List<Object[]> fetch();
    }

    /**
     * 函数式接口：用于获取分组求和列表
     */
    @FunctionalInterface
    public interface GroupedSumFetcher {
        List<Object[]> fetch();
    }

    /**
     * 函数式接口：用于获取跟进渠道分组列表
     */
    @FunctionalInterface
    public interface FollowUpChannelFetcher {
        List<Object[]> fetch();
    }

    /**
     * 数据获取器封装 - 包含所有 Repository 调用
     */
    public static class DataFetchers {
        // 基础计数
        public final CountFetcher customerCount;
        public final CountFetcher opportunitiesCount;
        public final CountFetcher highProgressCount;
        public final CountFetcher doneTasks;
        public final CountFetcher pendingTasks;
        public final CountFetcher followUps;
        public final CountFetcher quotes;
        public final CountFetcher quoteApproved;
        public final CountFetcher quoteAccepted;
        public final CountFetcher orders;
        public final CountFetcher orderCompleted;

        // 求和值
        public final SumFetcher totalRevenue;
        public final SumFetcher weightedAmountRaw;
        public final SumFetcher orderAmountTotal;
        public final SumFetcher orderPaymentReceived;

        // 分组数据
        public final GroupedCountFetcher customerByOwnerFetcher;
        public final GroupedSumFetcher revenueByStatusFetcher;
        public final GroupedCountFetcher opportunityByStageFetcher;
        public final GroupedCountFetcher quoteByStatusFetcher;
        public final GroupedCountFetcher orderByStatusFetcher;
        public final FollowUpChannelFetcher followUpChannelFetcher;

        public DataFetchers(CountFetcher customerCount,
                            CountFetcher opportunitiesCount,
                            CountFetcher highProgressCount,
                            CountFetcher doneTasks,
                            CountFetcher pendingTasks,
                            CountFetcher followUps,
                            CountFetcher quotes,
                            CountFetcher quoteApproved,
                            CountFetcher quoteAccepted,
                            CountFetcher orders,
                            CountFetcher orderCompleted,
                            SumFetcher totalRevenue,
                            SumFetcher weightedAmountRaw,
                            SumFetcher orderAmountTotal,
                            SumFetcher orderPaymentReceived,
                            GroupedCountFetcher customerByOwnerFetcher,
                            GroupedSumFetcher revenueByStatusFetcher,
                            GroupedCountFetcher opportunityByStageFetcher,
                            GroupedCountFetcher quoteByStatusFetcher,
                            GroupedCountFetcher orderByStatusFetcher,
                            FollowUpChannelFetcher followUpChannelFetcher) {
            this.customerCount = customerCount;
            this.opportunitiesCount = opportunitiesCount;
            this.highProgressCount = highProgressCount;
            this.doneTasks = doneTasks;
            this.pendingTasks = pendingTasks;
            this.followUps = followUps;
            this.quotes = quotes;
            this.quoteApproved = quoteApproved;
            this.quoteAccepted = quoteAccepted;
            this.orders = orders;
            this.orderCompleted = orderCompleted;
            this.totalRevenue = totalRevenue;
            this.weightedAmountRaw = weightedAmountRaw;
            this.orderAmountTotal = orderAmountTotal;
            this.orderPaymentReceived = orderPaymentReceived;
            this.customerByOwnerFetcher = customerByOwnerFetcher;
            this.revenueByStatusFetcher = revenueByStatusFetcher;
            this.opportunityByStageFetcher = opportunityByStageFetcher;
            this.quoteByStatusFetcher = quoteByStatusFetcher;
            this.orderByStatusFetcher = orderByStatusFetcher;
            this.followUpChannelFetcher = followUpChannelFetcher;
        }
    }

    /**
     * 无 Scope 限制的聚合入口
     */
    public Map<String, Object> aggregateWithoutScope(String tenantId) {
        DataFetchers fetchers = new DataFetchers(
                () -> customerRepository.countByTenantId(tenantId),
                () -> opportunityRepository.countByTenantId(tenantId),
                () -> opportunityRepository.countByTenantIdAndProgressGte(tenantId, 80),
                () -> taskRepository.countByTenantIdAndDoneTrue(tenantId),
                () -> taskRepository.countByTenantIdAndDoneFalse(tenantId),
                () -> followUpRepository.countByTenantId(tenantId),
                () -> quoteRepository.countByTenantId(tenantId),
                () -> quoteRepository.countByTenantIdAndStatusInUppercase(tenantId, QUOTE_APPROVED_OR_ACCEPTED),
                () -> quoteRepository.countByTenantIdAndStatusInUppercase(tenantId, QUOTE_ACCEPTED_ONLY),
                () -> orderRecordRepository.countByTenantId(tenantId),
                () -> orderRecordRepository.countByTenantIdAndStatus(tenantId, "COMPLETED"),
                () -> customerRepository.sumValueByTenantId(tenantId),
                () -> opportunityRepository.sumWeightedAmountRawByTenantId(tenantId),
                () -> orderRecordRepository.sumAmountByTenantId(tenantId),
                () -> paymentRecordRepository.sumAmountByTenantIdAndStatusInUppercase(tenantId, PAYMENT_RECEIVED_ONLY),
                () -> customerRepository.countByOwnerGrouped(tenantId),
                () -> customerRepository.sumValueByStatusGrouped(tenantId),
                () -> opportunityRepository.countByStageGrouped(tenantId),
                () -> quoteRepository.countByStatusGrouped(tenantId),
                () -> orderRecordRepository.countByStatusGrouped(tenantId),
                () -> followUpRepository.countByChannelGrouped(tenantId)
        );
        return doAggregate(fetchers);
    }

    /**
     * 有 Scope 限制的聚合入口
     */
    public Map<String, Object> aggregateWithScope(String tenantId, Set<String> owners) {
        if (owners == null || owners.isEmpty()) {
            return emptyOverviewBody();
        }
        DataFetchers fetchers = new DataFetchers(
                () -> customerRepository.countByTenantIdAndOwnerIn(tenantId, owners),
                () -> opportunityRepository.countByTenantIdAndOwnerIn(tenantId, owners),
                () -> opportunityRepository.countByTenantIdAndOwnerInAndProgressGte(tenantId, owners, 80),
                () -> taskRepository.countByTenantIdAndDoneTrueAndOwnerIn(tenantId, owners),
                () -> taskRepository.countByTenantIdAndDoneFalseAndOwnerIn(tenantId, owners),
                () -> followUpRepository.countByTenantIdAndAuthorIn(tenantId, owners),
                () -> quoteRepository.countByTenantIdAndOwnerIn(tenantId, owners),
                () -> quoteRepository.countByTenantIdAndOwnerInAndStatusInUppercase(tenantId, owners, QUOTE_APPROVED_OR_ACCEPTED),
                () -> quoteRepository.countByTenantIdAndOwnerInAndStatusInUppercase(tenantId, owners, QUOTE_ACCEPTED_ONLY),
                () -> orderRecordRepository.countByTenantIdAndOwnerIn(tenantId, owners),
                () -> orderRecordRepository.countByTenantIdAndOwnerInAndStatus(tenantId, owners, "COMPLETED"),
                () -> customerRepository.sumValueByTenantIdAndOwnerIn(tenantId, owners),
                () -> opportunityRepository.sumWeightedAmountRawByTenantIdAndOwnerIn(tenantId, owners),
                () -> orderRecordRepository.sumAmountByTenantIdAndOwnerIn(tenantId, owners),
                () -> paymentRecordRepository.sumAmountByTenantIdAndOwnerInAndStatusInUppercase(tenantId, owners, PAYMENT_RECEIVED_ONLY),
                () -> customerRepository.countByOwnerGroupedAndOwnerIn(tenantId, owners),
                () -> customerRepository.sumValueByStatusGroupedAndOwnerIn(tenantId, owners),
                () -> opportunityRepository.countByStageGroupedAndOwnerIn(tenantId, owners),
                () -> quoteRepository.countByStatusGroupedAndOwnerIn(tenantId, owners),
                () -> orderRecordRepository.countByStatusGroupedAndOwnerIn(tenantId, owners),
                () -> followUpRepository.countByChannelGroupedAndAuthorIn(tenantId, owners)
        );
        return doAggregate(fetchers);
    }

    /**
     * 核心聚合逻辑
     */
    private Map<String, Object> doAggregate(DataFetchers f) {
        // 基础计数
        long customerCount = f.customerCount.fetch();
        long opportunitiesCount = f.opportunitiesCount.fetch();
        long highProgressCount = f.highProgressCount.fetch();
        long doneTasks = f.doneTasks.fetch();
        long pendingTasks = f.pendingTasks.fetch();
        long taskTotal = doneTasks + pendingTasks;
        long followUps = f.followUps.fetch();
        long quotes = f.quotes.fetch();
        long quoteApproved = f.quoteApproved.fetch();
        long quoteAccepted = f.quoteAccepted.fetch();
        long orders = f.orders.fetch();
        long orderCompleted = f.orderCompleted.fetch();

        // 求和值
        long totalRevenue = safeLong(f.totalRevenue.fetch());
        long weightedAmountRaw = safeLong(f.weightedAmountRaw.fetch());
        long weightedAmount = Math.round(weightedAmountRaw / 100.0d);
        long orderAmountTotal = safeLong(f.orderAmountTotal.fetch());
        long orderPaymentReceived = safeLong(f.orderPaymentReceived.fetch());
        long orderPaymentOutstanding = Math.max(0L, orderAmountTotal - orderPaymentReceived);

        // 计算比率
        double winRate = opportunitiesCount == 0 ? 0.0 : Math.round((highProgressCount * 1000.0 / opportunitiesCount)) / 10.0;
        double taskDoneRate = taskTotal == 0 ? 0.0 : Math.round((doneTasks * 1000.0 / taskTotal)) / 10.0;
        double quoteApproveRate = quotes == 0 ? 0.0 : Math.round((quoteApproved * 1000.0 / quotes)) / 10.0;
        double quoteToOrderRate = quoteAccepted == 0 ? 0.0 : Math.round((orders * 1000.0 / quoteAccepted)) / 10.0;
        double orderCompleteRate = orders == 0 ? 0.0 : Math.round((orderCompleted * 1000.0 / orders)) / 10.0;
        double orderCollectionRate = orderAmountTotal <= 0 ? 0.0 : Math.round((orderPaymentReceived * 1000.0 / orderAmountTotal)) / 10.0;

        // 分组数据
        Map<String, Integer> customerByOwner = customerCount <= 0
                ? new HashMap<String, Integer>()
                : toIntMap(f.customerByOwnerFetcher.fetch());
        Map<String, Long> revenueByStatus = customerCount <= 0
                ? new HashMap<String, Long>()
                : toLongMap(f.revenueByStatusFetcher.fetch());
        Map<String, Integer> opportunityByStage = opportunitiesCount <= 0
                ? new HashMap<String, Integer>()
                : toIntMap(f.opportunityByStageFetcher.fetch());
        Map<String, Integer> quoteByStatus = quotes <= 0
                ? new HashMap<String, Integer>()
                : toIntMap(f.quoteByStatusFetcher.fetch());
        Map<String, Integer> orderByStatus = orders <= 0
                ? new HashMap<String, Integer>()
                : toIntMap(f.orderByStatusFetcher.fetch());

        // 跟进渠道分布
        Map<String, Integer> followUpByChannel = new HashMap<String, Integer>();
        if (followUps > 0) {
            for (Object[] row : f.followUpChannelFetcher.fetch()) {
                String channel = normalizeBucket(row.length > 0 ? row[0] : null);
                channel = valueNormalizerService.normalizeFollowUpChannel(channel);
                int count = asInt(row.length > 1 ? row[1] : null);
                followUpByChannel.put(channel, followUpByChannel.containsKey(channel)
                        ? followUpByChannel.get(channel) + count
                        : count);
            }
        }

        // 构建摘要
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

        // 任务状态
        Map<String, Integer> taskStatus = new HashMap<String, Integer>();
        taskStatus.put("done", (int) doneTasks);
        taskStatus.put("pending", (int) pendingTasks);

        // 构建返回体
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

    /**
     * 空概览数据
     */
    public Map<String, Object> emptyOverviewBody() {
        Map<String, Object> summary = new HashMap<String, Object>();
        summary.put("customers", 0L);
        summary.put("revenue", 0L);
        summary.put("opportunities", 0L);
        summary.put("weightedAmount", 0L);
        summary.put("winRate", 0.0);
        summary.put("taskDoneRate", 0.0);
        summary.put("followUps", 0L);
        summary.put("quotes", 0L);
        summary.put("orders", 0L);
        summary.put("quoteApproveRate", 0.0);
        summary.put("quoteToOrderRate", 0.0);
        summary.put("orderCompleteRate", 0.0);
        summary.put("orderPaymentReceived", 0L);
        summary.put("orderPaymentOutstanding", 0L);
        summary.put("orderCollectionRate", 0.0);

        Map<String, Integer> taskStatus = new HashMap<String, Integer>();
        taskStatus.put("done", 0);
        taskStatus.put("pending", 0);

        Map<String, Object> body = new HashMap<String, Object>();
        body.put("summary", summary);
        body.put("customerByOwner", new HashMap<String, Integer>());
        body.put("revenueByStatus", new HashMap<String, Long>());
        body.put("opportunityByStage", new HashMap<String, Integer>());
        body.put("taskStatus", taskStatus);
        body.put("followUpByChannel", new HashMap<String, Integer>());
        body.put("quoteByStatus", new HashMap<String, Integer>());
        body.put("orderByStatus", new HashMap<String, Integer>());
        return body;
    }
}
