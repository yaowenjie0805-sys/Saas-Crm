package com.yao.crm.controller;

import com.yao.crm.entity.Tenant;
import com.yao.crm.repository.TenantRepository;
import com.yao.crm.service.I18nService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v2/integrations")
public class V2IntegrationConnectorController extends BaseApiController {

    private final TenantRepository tenantRepository;

    public V2IntegrationConnectorController(TenantRepository tenantRepository, I18nService i18nService) {
        super(i18nService);
        this.tenantRepository = tenantRepository;
    }

    @GetMapping("/connectors")
    public ResponseEntity<?> connectors(HttpServletRequest request) {
        String tenantId = currentTenant(request);
        Optional<Tenant> optional = tenantRepository.findById(tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "tenant_not_found", msg(request, "tenant_not_found"), null));
        }
        Tenant tenant = optional.get();
        boolean cn = "CN".equalsIgnoreCase(tenant.getMarketProfile());

        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        items.add(item("WECOM", cn, cn ? "ACTIVE" : "PLANNED", "CN"));
        items.add(item("DINGTALK", cn, cn ? "ACTIVE" : "PLANNED", "CN"));
        items.add(item("SALESFORCE", !cn, "PLANNED", "GLOBAL"));
        items.add(item("HUBSPOT", !cn, "PLANNED", "GLOBAL"));

        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("tenantId", tenantId);
        body.put("marketProfile", tenant.getMarketProfile());
        body.put("items", items);
        return ResponseEntity.ok(successWithFields(request, "integration_connectors_loaded", body));
    }

    private Map<String, Object> item(String code, boolean enabled, String status, String market) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("code", code);
        row.put("enabled", enabled);
        row.put("status", status);
        row.put("market", market);
        return row;
    }
}
