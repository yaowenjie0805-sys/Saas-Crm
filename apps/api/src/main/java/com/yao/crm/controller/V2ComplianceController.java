package com.yao.crm.controller;

import com.yao.crm.entity.Tenant;
import com.yao.crm.repository.TenantRepository;
import com.yao.crm.service.I18nService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v2/compliance")
public class V2ComplianceController extends BaseApiController {

    private final TenantRepository tenantRepository;

    public V2ComplianceController(TenantRepository tenantRepository, I18nService i18nService) {
        super(i18nService);
        this.tenantRepository = tenantRepository;
    }

    @GetMapping("/policies")
    public ResponseEntity<?> policies(HttpServletRequest request) {
        String tenantId = currentTenant(request);
        Optional<Tenant> optional = tenantRepository.findById(tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "tenant_not_found", msg(request, "tenant_not_found"), null));
        }
        Tenant tenant = optional.get();

        Map<String, Object> exportPolicy = new LinkedHashMap<String, Object>();
        exportPolicy.put("allow", hasAnyRole(request, "ADMIN", "MANAGER", "ANALYST"));
        exportPolicy.put("maskLevel", tenant.getMaskLevel());
        exportPolicy.put("retentionDays", "GLOBAL".equalsIgnoreCase(tenant.getMarketProfile()) ? 365 : 1800);

        Map<String, Object> dataPolicy = new LinkedHashMap<String, Object>();
        dataPolicy.put("residency", tenant.getDataResidency());
        dataPolicy.put("marketProfile", tenant.getMarketProfile());
        dataPolicy.put("encryption", "AES-256");

        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("tenantId", tenantId);
        body.put("exportPolicy", exportPolicy);
        body.put("dataPolicy", dataPolicy);
        return ResponseEntity.ok(successWithFields(request, "compliance_policies_loaded", body));
    }
}
