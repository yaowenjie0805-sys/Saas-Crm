package com.yao.crm.controller;

import com.yao.crm.dto.request.V1TenantUpsertRequest;
import com.yao.crm.entity.Tenant;
import com.yao.crm.entity.UserAccount;
import com.yao.crm.repository.TenantRepository;
import com.yao.crm.repository.UserAccountRepository;
import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.I18nService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/v1")
public class V1TenantController extends BaseApiController {

    private static final Set<String> MARKET_PROFILES = Set.of("CN", "GLOBAL");
    private static final Set<String> APPROVAL_MODES = Set.of("STRICT", "STAGE_GATE");

    private final TenantRepository tenantRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final String bootstrapDefaultPassword;

    public V1TenantController(TenantRepository tenantRepository,
                              UserAccountRepository userAccountRepository,
                              PasswordEncoder passwordEncoder,
                              AuditLogService auditLogService,
                              @Value("${auth.bootstrap.default-password:}") String bootstrapDefaultPassword,
                              I18nService i18nService) {
        super(i18nService);
        this.tenantRepository = tenantRepository;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
        this.bootstrapDefaultPassword = requireBootstrapPassword(bootstrapDefaultPassword);
    }

    @GetMapping("/tenants")
    public ResponseEntity<?> listTenants(HttpServletRequest request) {
        if (!hasAnyRole(request, "ADMIN")) {
            return forbidden(request);
        }
        List<Tenant> rows = tenantRepository.findAllByOrderByIdAsc();
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
            return forbidden(request);
        }

        Tenant tenant = new Tenant();
        tenant.setId("tn_" + Long.toString(System.currentTimeMillis(), 36));
        tenant.setName(normalizeOptional(payload.getName()));
        tenant.setStatus(isBlank(payload.getStatus()) ? "ACTIVE" : payload.getStatus().trim().toUpperCase(Locale.ROOT));
        tenant.setQuotaUsers(payload.getQuotaUsers());
        tenant.setTimezone(isBlank(payload.getTimezone()) ? "Asia/Shanghai" : payload.getTimezone().trim());
        tenant.setCurrency(isBlank(payload.getCurrency()) ? "CNY" : payload.getCurrency().trim().toUpperCase(Locale.ROOT));
        String marketProfile = isBlank(payload.getMarketProfile()) ? "CN" : payload.getMarketProfile().trim().toUpperCase(Locale.ROOT);
        if (!MARKET_PROFILES.contains(marketProfile)) {
            return badRequest(request, "tenant_market_profile_invalid");
        }
        tenant.setMarketProfile(marketProfile);
        tenant.setTaxRule(isBlank(payload.getTaxRule()) ? "VAT_CN" : payload.getTaxRule().trim().toUpperCase());
        String approvalMode = isBlank(payload.getApprovalMode()) ? "STRICT" : payload.getApprovalMode().trim().toUpperCase(Locale.ROOT);
        if (!APPROVAL_MODES.contains(approvalMode)) {
            return badRequest(request, "tenant_approval_mode_invalid");
        }
        tenant.setApprovalMode(approvalMode);
        tenant.setChannelsJson(isBlank(payload.getChannels()) ? "[\"WECOM\",\"DINGTALK\"]" : payload.getChannels().trim());
        tenant.setDataResidency(isBlank(payload.getDataResidency()) ? "CN" : payload.getDataResidency().trim().toUpperCase(Locale.ROOT));
        tenant.setMaskLevel(isBlank(payload.getMaskLevel()) ? "STANDARD" : payload.getMaskLevel().trim().toUpperCase(Locale.ROOT));
        String dateFormat = normalizeTenantDateFormat(payload.getDateFormat());
        if (isBlank(dateFormat)) {
            return badRequest(request, "tenant_date_format_invalid");
        }
        tenant.setDateFormat(dateFormat);
        tenant = tenantRepository.save(tenant);

        UserAccount admin = new UserAccount();
        admin.setId("u_admin_" + tenant.getId());
        admin.setUsername("admin@" + tenant.getId());
        admin.setPassword(passwordEncoder.encode(bootstrapDefaultPassword));
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
            return forbidden(request);
        }
        String tenantId = normalizeOptional(id);
        if (isBlank(tenantId)) {
            return badRequest(request, "bad_request");
        }

        Optional<Tenant> optional = tenantRepository.findById(tenantId);
        if (!optional.isPresent()) {
            return notFound(request, "tenant_not_found");
        }

        Tenant tenant = optional.get();
        tenant.setName(normalizeOptional(payload.getName()));
        tenant.setStatus(isBlank(payload.getStatus()) ? tenant.getStatus() : payload.getStatus().trim().toUpperCase(Locale.ROOT));
        tenant.setQuotaUsers(payload.getQuotaUsers());
        if (!isBlank(payload.getTimezone())) tenant.setTimezone(payload.getTimezone().trim());
        if (!isBlank(payload.getCurrency())) tenant.setCurrency(payload.getCurrency().trim().toUpperCase(Locale.ROOT));
        if (!isBlank(payload.getMarketProfile())) {
            String marketProfile = payload.getMarketProfile().trim().toUpperCase(Locale.ROOT);
            if (!MARKET_PROFILES.contains(marketProfile)) {
                return badRequest(request, "tenant_market_profile_invalid");
            }
            tenant.setMarketProfile(marketProfile);
        }
        if (!isBlank(payload.getTaxRule())) tenant.setTaxRule(payload.getTaxRule().trim().toUpperCase());
        if (!isBlank(payload.getApprovalMode())) {
            String approvalMode = payload.getApprovalMode().trim().toUpperCase(Locale.ROOT);
            if (!APPROVAL_MODES.contains(approvalMode)) {
                return badRequest(request, "tenant_approval_mode_invalid");
            }
            tenant.setApprovalMode(approvalMode);
        }
        if (!isBlank(payload.getChannels())) tenant.setChannelsJson(payload.getChannels().trim());
        if (!isBlank(payload.getDataResidency())) tenant.setDataResidency(payload.getDataResidency().trim().toUpperCase(Locale.ROOT));
        if (!isBlank(payload.getMaskLevel())) tenant.setMaskLevel(payload.getMaskLevel().trim().toUpperCase(Locale.ROOT));
        if (!isBlank(payload.getDateFormat())) {
            String dateFormat = normalizeTenantDateFormat(payload.getDateFormat());
            if (isBlank(dateFormat)) {
                return badRequest(request, "tenant_date_format_invalid");
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

    private String requireBootstrapPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalStateException(
                    "auth.bootstrap.default-password must be explicitly configured. "
                    + "Set AUTH_BOOTSTRAP_DEFAULT_PASSWORD to a strong value.");
        }
        String trimmed = password.trim();
        if (trimmed.length() < 8) {
            throw new IllegalStateException(
                    "auth.bootstrap.default-password is too short (< 8 chars). "
                    + "Use a stronger password.");
        }
        return trimmed;
    }

    private String normalizeOptional(String value) {
        return isBlank(value) ? "" : value.trim();
    }

    private ResponseEntity<?> forbidden(HttpServletRequest request) {
        String code = normalizeCode("forbidden", "forbidden");
        return ResponseEntity.status(403).body(errorBody(request, code, msg(request, code), null));
    }

    private ResponseEntity<?> badRequest(HttpServletRequest request, String msgKey) {
        String code = normalizeCode(msgKey, "bad_request");
        return ResponseEntity.badRequest().body(errorBody(request, code, msg(request, code), null));
    }

    private ResponseEntity<?> notFound(HttpServletRequest request, String msgKey) {
        String code = normalizeCode(msgKey, "not_found");
        return ResponseEntity.status(404).body(errorBody(request, code, msg(request, code), null));
    }
}

