package com.yao.crm.service;

import static com.yao.crm.support.TestTenant.TENANT_TEST;

import com.yao.crm.dto.DashboardResponse;
import com.yao.crm.repository.CustomerRepository;
import com.yao.crm.repository.OpportunityRepository;
import com.yao.crm.repository.TaskRepository;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DashboardServiceTest {

    @Test
    void shouldUseRoleOnlyDashboardCacheKey() {
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        OpportunityRepository opportunityRepository = mock(OpportunityRepository.class);
        TaskRepository taskRepository = mock(TaskRepository.class);
        DashboardMetricsCacheService cacheService = mock(DashboardMetricsCacheService.class);

        DashboardResponse empty = new DashboardResponse();
        empty.setStats(Collections.emptyList());
        empty.setCustomers(Collections.emptyList());
        empty.setTasks(Collections.emptyList());
        empty.setOpportunities(Collections.emptyList());
        doReturn(new DashboardMetricsCacheService.CachedValue<DashboardResponse>(
                empty, true, "LOCAL", false
        )).when(cacheService).getOrLoad(any(), any(), any(), any());

        DashboardService service = new DashboardService(
                customerRepository,
                opportunityRepository,
                taskRepository,
                cacheService
        );

        service.loadByTenantCached(TENANT_TEST, "  Alice  ", " manager ");

        verify(cacheService).getOrLoad(
                eq(TENANT_TEST),
                eq("dashboard-overview"),
                eq("MANAGER"),
                any()
        );
    }
}
