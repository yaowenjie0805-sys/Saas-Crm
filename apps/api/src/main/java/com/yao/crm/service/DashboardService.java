package com.yao.crm.service;

import com.yao.crm.dto.DashboardResponse;
import com.yao.crm.dto.StatItem;
import com.yao.crm.entity.Customer;
import com.yao.crm.entity.Opportunity;
import com.yao.crm.entity.TaskItem;
import com.yao.crm.repository.CustomerRepository;
import com.yao.crm.repository.OpportunityRepository;
import com.yao.crm.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
public class DashboardService {

    private final CustomerRepository customerRepository;
    private final OpportunityRepository opportunityRepository;
    private final TaskRepository taskRepository;
    private final DashboardMetricsCacheService dashboardMetricsCacheService;

    public DashboardService(CustomerRepository customerRepository,
                            OpportunityRepository opportunityRepository,
                            TaskRepository taskRepository,
                            DashboardMetricsCacheService dashboardMetricsCacheService) {
        this.customerRepository = customerRepository;
        this.opportunityRepository = opportunityRepository;
        this.taskRepository = taskRepository;
        this.dashboardMetricsCacheService = dashboardMetricsCacheService;
    }

    @Transactional(readOnly = true)
    public DashboardMetricsCacheService.CachedValue<DashboardResponse> loadByTenantCached(String tenantId, String actor, String actorRole) {
        String key = (actorRole == null ? "" : actorRole.trim().toUpperCase(Locale.ROOT));
        return dashboardMetricsCacheService.getOrLoad(tenantId, "dashboard-overview", key, new java.util.function.Supplier<DashboardResponse>() {
            @Override
            public DashboardResponse get() {
                return loadByTenant(tenantId);
            }
        });
    }

    @Transactional(readOnly = true)
    public DashboardResponse loadByTenant(String tenantId) {
        long customerCount = customerRepository.countByTenantId(tenantId);
        long totalSales = safeLong(customerRepository.sumValueByTenantId(tenantId));
        Double avgCycleRaw = opportunityRepository.avgCycleDaysByTenantId(tenantId);
        int avgCycle = avgCycleRaw == null ? 0 : (int) Math.round(avgCycleRaw.doubleValue());
        long doneTasks = taskRepository.countByTenantIdAndDoneTrue(tenantId);
        double retention = customerCount == 0 ? 0.0 : Math.round(((customerCount - doneTasks * 0.2) / customerCount) * 1000.0) / 10.0;

        List<Opportunity> opportunities = opportunityRepository.findTop8ByTenantIdOrderByUpdatedAtDesc(tenantId);
        List<TaskItem> tasks = taskRepository.findTop8ByTenantIdOrderByUpdatedAtDesc(tenantId);
        List<Customer> customers = customerRepository.findTop8ByTenantIdOrderByUpdatedAtDesc(tenantId);

        DashboardResponse response = new DashboardResponse();
        response.setStats(Arrays.asList(
                new StatItem("Total Customers", String.valueOf(customerCount), "+6.3%"),
                new StatItem("Projected Revenue", formatMoney(totalSales), "+8.4%"),
                new StatItem("Avg Sales Cycle", avgCycle + " days", "-2.7%"),
                new StatItem("Retention Rate", retention + "%", "+1.1%")
        ));
        response.setOpportunities(opportunities);
        response.setTasks(tasks);
        response.setCustomers(customers);
        return response;
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value.longValue();
    }

    private String formatMoney(long value) {
        if (value >= 1000000) return "CNY " + String.format(Locale.US, "%.2f", (value / 1000000.0)) + "M";
        if (value >= 1000) return "CNY " + Math.round(value / 1000.0) + "K";
        return "CNY " + value;
    }
}
