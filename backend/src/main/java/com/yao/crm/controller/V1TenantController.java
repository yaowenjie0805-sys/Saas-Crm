package com.yao.crm.controller;

import com.yao.crm.dto.request.V1TenantUpsertRequest;
import com.yao.crm.entity.Tenant;
import com.yao.crm.entity.UserAccount;
import com.yao.crm.repository.TenantRepository;
import com.yao.crm.repository.UserAccountRepository;
import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.I18nService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
public class V1TenantController extends BaseApiController {

    private final TenantRepository tenantRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public V1TenantController(TenantRepository tenantRepository,
                              UserAccountRepository userAccountRepository,
                              PasswordEncoder passwordEncoder,
                              AuditLogService auditLogService,
                              I18nService i18nService) {
        super(i18nService);
        this.tenantRepository = tenantRepository;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/tenants")
    public ResponseEntity<?> listTenants(HttpServletRequest request) {
        if (!hasAnyRole(request, "ADMIN")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        List<Tenant> rows = tenantRepository.findAll();
        rows.sort(new Comparator<Tenant>() {
            @Override
            public int compare(Tenant a, Tenant b) {
                return String.valueOf(a.getId()).compareToIgnoreCase(String.valueOf(b.getId()));
            }
        });
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (Tenant row : rows) {
            items.add(toTenantView(row));
        }
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("items", items);
        return ResponseEntity.ok(successWithFields(request, "tenants_listed", body));
    }

    @PostMapping("/tenants")
    public ResponseEntity<?> createTenant(HttpServletRequest request, @Valid @RequestBody V1TenantUpsertRequest payload) {
        if (!hasAnyRole(request, "ADMIN")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }

        Tenant tenant = new Tenant();
        tenant.setId("tn_" + Long.toString(System.currentTimeMillis(), 36));
        tenant.setName(payload.getName());
        tenant.setStatus(isBlank(payload.getStatus()) ? "ACTIVE" : payload.getStatus().trim().toUpperCase());
        tenant.setQuotaUsers(payload.getQuotaUsers());
        tenant.setTimezone(isBlank(payload.getTimezone()) ? "Asia/Shanghai" : payload.getTimezone().trim());
        tenant.setCurrency(isBlank(payload.getCurrency()) ? "CNY" : payload.getCurrency().trim().toUpperCase());
        tenant.setMarketProfile(isBlank(payload.getMarketProfile()) ? "CN" : payload.getMarketProfile().trim().toUpperCase());
        tenant.setTaxRule(isBlank(payload.getTaxRule()) ? "VAT_CN" : payload.getTaxRule().trim().toUpperCase());
        tenant.setApprovalMode(isBlank(payload.getApprovalMode()) ? "STRICT" : payload.getApprovalMode().trim().toUpperCase());
        tenant.setChannelsJson(isBlank(payload.getChannels()) ? "[\"WECOM\",\"DINGTALK\"]" : payload.getChannels().trim());
        tenant.setDataResidency(isBlank(payload.getDataResidency()) ? "CN" : payload.getDataResidency().trim().toUpperCase());
        tenant.setMaskLevel(isBlank(payload.getMaskLevel()) ? "STANDARD" : payload.getMaskLevel().trim().toUpperCase());
        String dateFormat = normalizeTenantDateFormat(payload.getDateFormat());
        if (isBlank(dateFormat)) {
            return ResponseEntity.badRequest().body(errorBody(request, "tenant_date_format_invalid", msg(request, "tenant_date_format_invalid"), null));
        }
        tenant.setDateFormat(dateFormat);
        tenant = tenantRepository.save(tenant);

        UserAccount admin = new UserAccount();
        admin.setId("u_admin_" + tenant.getId());
        admin.setUsername("admin@" + tenant.getId());
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRole("ADMIN");
        admin.setDisplayName("Tenant Admin");
        admin.setOwnerScope("");
        admin.setEnabled(true);
        admin.setTenantId(tenant.getId());
        admin.setDepartment("ADMIN");
        admin.setDataScope("GLOBAL");
        userAccountRepository.save(admin);

        auditLogService.record(currentUser(request), currentRole(request), "CREATE", "TENANT", tenant.getId(), "Created tenant " + tenant.getName(), tenant.getId());
        return ResponseEntity.status(201).body(successWithFields(request, "tenant_created", toTenantView(tenant)));
    }

    @PatchMapping("/tenants/{id}")
    public ResponseEntity<?> updateTenant(HttpServletRequest request, @PathVariable String id, @Valid @RequestBody V1TenantUpsertRequest payload) {
        if (!hasAnyRole(request, "ADMIN")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }

        Optional<Tenant> optional = tenantRepository.findById(id);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "tenant_not_found", msg(request, "tenant_not_found"), null));
        }

        Tenant tenant = optional.get();
        tenant.setName(payload.getName());
        tenant.setStatus(isBlank(payload.getStatus()) ? tenant.getStatus() : payload.getStatus().trim().toUpperCase());
        tenant.setQuotaUsers(payload.getQuotaUsers());
        if (!isBlank(payload.getTimezone())) tenant.setTimezone(payload.getTimezone().trim());
        if (!isBlank(payload.getCurrency())) tenant.setCurrency(payload.getCurrency().trim().toUpperCase());
        if (!isBlank(payload.getMarketProfile())) tenant.setMarketProfile(payload.getMarketProfile().trim().toUpperCase());
        if (!isBlank(payload.getTaxRule())) tenant.setTaxRule(payload.getTaxRule().trim().toUpperCase());
        if (!isBlank(payload.getApprovalMode())) tenant.setApprovalMode(payload.getApprovalMode().trim().toUpperCase());
        if (!isBlank(payload.getChannels())) tenant.setChannelsJson(payload.getChannels().trim());
        if (!isBlank(payload.getDataResidency())) tenant.setDataResidency(payload.getDataResidency().trim().toUpperCase());
        if (!isBlank(payload.getMaskLevel())) tenant.setMaskLevel(payload.getMaskLevel().trim().toUpperCase());
        if (!isBlank(payload.getDateFormat())) {
            String dateFormat = normalizeTenantDateFormat(payload.getDateFormat());
            if (isBlank(dateFormat)) {
                return ResponseEntity.badRequest().body(errorBody(request, "tenant_date_format_invalid", msg(request, "tenant_date_format_invalid"), null));
            }
            tenant.setDateFormat(dateFormat);
        }
        tenant = tenantRepository.save(tenant);

        auditLogService.record(currentUser(request), currentRole(request), "UPDATE", "TENANT", tenant.getId(), "Updated tenant", tenant.getId());
        return ResponseEntity.ok(successWithFields(request, "tenant_updated", toTenantView(tenant)));
    }

    private Map<String, Object> toTenantView(Tenant row) {
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("id", row.getId());
        item.put("name", row.getName());
        item.put("status", row.getStatus());
        item.put("quotaUsers", row.getQuotaUsers());
        item.put("timezone", row.getTimezone());
        item.put("currency", row.getCurrency());
        item.put("dateFormat", row.getDateFormat());
        item.put("marketProfile", row.getMarketProfile());
        item.put("taxRule", row.getTaxRule());
        item.put("approvalMode", row.getApprovalMode());
        item.put("channels", row.getChannelsJson());
        item.put("dataResidency", row.getDataResidency());
        item.put("maskLevel", row.getMaskLevel());
        item.put("createdAt", row.getCreatedAt());
        item.put("updatedAt", row.getUpdatedAt());
        return item;
    }
}

