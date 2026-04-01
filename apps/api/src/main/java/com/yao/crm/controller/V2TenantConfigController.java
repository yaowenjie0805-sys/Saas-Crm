package com.yao.crm.controller;

import com.yao.crm.dto.request.TenantConfigPatchRequest;
import com.yao.crm.entity.Tenant;
import com.yao.crm.repository.TenantRepository;
import com.yao.crm.service.I18nService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/v2")
public class V2TenantConfigController extends BaseApiController {

    private static final Set<String> MARKET_PROFILES = Set.of("CN", "GLOBAL");
    private static final Set<String> APPROVAL_MODES = Set.of("STRICT", "STAGE_GATE");
    private final TenantRepository tenantRepository;

    public V2TenantConfigController(TenantRepository tenantRepository, I18nService i18nService) {
        super(i18nService);
        this.tenantRepository = tenantRepository;
    }

    @GetMapping("/tenant-config")
    public ResponseEntity<?> getTenantConfig(HttpServletRequest request) {
        String tenantId = resolveTenantId(request);
        if (isBlank(tenantId)) {
            return badRequest(
                    request,
                    "bad_request",
                    badRequestDetails("tenantId")
            );
        }
        Optional<Tenant> optional = tenantRepository.findById(tenantId);
        if (!optional.isPresent()) {
            return notFound(request, "tenant_not_found");
        }
        return ResponseEntity.ok(successWithFields(request, "tenant_config_loaded", toView(optional.get())));
    }

    @PatchMapping("/tenant-config")
    public ResponseEntity<?> patchTenantConfig(HttpServletRequest request, @RequestBody TenantConfigPatchRequest payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
            return forbidden(request);
        }
        if (payload == null) {
            return badRequest(request, "bad_request", null);
        }
        String tenantId = resolveTenantId(request);
        if (isBlank(tenantId)) {
            return badRequest(
                    request,
                    "bad_request",
                    badRequestDetails("tenantId")
            );
        }
        Optional<Tenant> optional = tenantRepository.findById(tenantId);
        if (!optional.isPresent()) {
            return notFound(request, "tenant_not_found");
        }
        Tenant tenant = optional.get();

        String marketProfile = upper(payload.getMarketProfile());
        if (!isBlank(marketProfile)) {
            if (!MARKET_PROFILES.contains(marketProfile)) {
                return badRequest(request, "tenant_market_profile_invalid", null);
            }
            tenant.setMarketProfile(marketProfile);
        }

        String taxRule = upper(payload.getTaxRule());
        if (!isBlank(taxRule)) tenant.setTaxRule(taxRule);

        String approvalMode = upper(payload.getApprovalMode());
        if (!isBlank(approvalMode)) {
            if (!APPROVAL_MODES.contains(approvalMode)) {
                return badRequest(request, "tenant_approval_mode_invalid", null);
            }
            tenant.setApprovalMode(approvalMode);
        }

        List<String> channels = payload.getChannels();
        if (channels != null) {
            StringBuilder merged = new StringBuilder("[");
            boolean first = true;
            for (String item : channels) {
                String text = upper(item);
                if (isBlank(text)) continue;
                if (!first) merged.append(',');
                merged.append('"').append(text).append('"');
                first = false;
            }
            merged.append(']');
            tenant.setChannelsJson(merged.toString());
        }

        String dataResidency = upper(payload.getDataResidency());
        if (!isBlank(dataResidency)) tenant.setDataResidency(dataResidency);

        String maskLevel = upper(payload.getMaskLevel());
        if (!isBlank(maskLevel)) tenant.setMaskLevel(maskLevel);

        tenant = tenantRepository.save(tenant);
        return ResponseEntity.ok(successWithFields(request, "tenant_config_updated", toView(tenant)));
    }

    private Map<String, Object> toView(Tenant tenant) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("tenantId", tenant.getId());
        out.put("marketProfile", tenant.getMarketProfile());
        out.put("timezone", tenant.getTimezone());
        out.put("currency", tenant.getCurrency());
        out.put("dateFormat", tenant.getDateFormat());
        out.put("taxRule", tenant.getTaxRule());
        out.put("approvalMode", tenant.getApprovalMode());
        out.put("channels", tenant.getChannelsJson());
        out.put("dataResidency", tenant.getDataResidency());
        out.put("maskLevel", tenant.getMaskLevel());
        out.put("updatedAt", tenant.getUpdatedAt());
        return out;
    }

    private String upper(String value) {
        if (isBlank(value)) return "";
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String resolveTenantId(HttpServletRequest request) {
        Object tenant = request.getAttribute("authTenantId");
        if (tenant != null && !isBlank(String.valueOf(tenant))) {
            return String.valueOf(tenant).trim();
        }
        String headerTenant = request.getHeader("X-Tenant-Id");
        if (!isBlank(headerTenant)) {
            return headerTenant.trim();
        }
        return "";
    }

    private Map<String, Object> badRequestDetails(String field) {
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        details.put("field", field);
        return details;
    }

    private ResponseEntity<?> forbidden(HttpServletRequest request) {
        String code = normalizeCode("forbidden", "forbidden");
        return ResponseEntity.status(403).body(errorBody(request, code, msg(request, code), null));
    }

    private ResponseEntity<?> badRequest(HttpServletRequest request, String msgKey, Map<String, Object> details) {
        String code = normalizeCode(msgKey, "bad_request");
        return ResponseEntity.badRequest().body(errorBody(request, code, msg(request, code), details));
    }

    private ResponseEntity<?> notFound(HttpServletRequest request, String msgKey) {
        String code = normalizeCode(msgKey, "not_found");
        return ResponseEntity.status(404).body(errorBody(request, code, msg(request, code), null));
    }
}
