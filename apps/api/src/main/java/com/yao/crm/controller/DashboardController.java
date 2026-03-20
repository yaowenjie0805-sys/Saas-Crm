package com.yao.crm.controller;

import com.yao.crm.service.DashboardService;
import com.yao.crm.service.I18nService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;

@RestController
@RequestMapping("/api")
public class DashboardController extends BaseApiController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService, I18nService i18nService) {
        super(i18nService);
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard(HttpServletRequest request) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES", "ANALYST")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }
        com.yao.crm.service.DashboardMetricsCacheService.CachedValue<com.yao.crm.dto.DashboardResponse> cached =
                dashboardService.loadByTenantCached(currentTenant(request), currentUser(request), currentRole(request));
        com.yao.crm.dto.DashboardResponse source = cached.getValue();
        com.yao.crm.dto.DashboardResponse body = new com.yao.crm.dto.DashboardResponse();
        body.setStats(source.getStats());
        body.setOpportunities(source.getOpportunities());
        body.setTasks(source.getTasks());
        body.setCustomers(source.getCustomers());
        body.setRequestId(traceId(request));
        return ResponseEntity.ok()
                .header("X-CRM-Cache", cached.isHit() ? "HIT" : "MISS")
                .header("X-CRM-Cache-Tier", cached.getTier())
                .header("X-CRM-Cache-Fallback", cached.isFallback() ? "1" : "0")
                .body(body);
    }
}

