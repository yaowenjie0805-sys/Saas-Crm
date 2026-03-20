package com.yao.crm.controller;

import com.yao.crm.security.TraceIdInterceptor;
import com.yao.crm.service.I18nService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

abstract class BaseApiController {
    protected static final String DATE_FORMAT_ISO = "yyyy-MM-dd";
    protected static final String DATE_FORMAT_DMY = "dd/MM/yyyy";
    protected static final String DATE_FORMAT_MDY = "MM-dd-yyyy";

    protected static final Set<String> LEGACY_EXPORT_JOB_ERROR_KEYS = immutableKeys(
            "export_job_not_found",
            "export_job_not_ready"
    );
    protected static final Set<String> LEGACY_PERMISSION_ERROR_KEYS = immutableKeys(
            "invalid_role",
            "invalid_permission",
            "delete_requires_manage_customers",
            "edit_amount_requires_create_opportunity",
            "analyst_should_be_read_only",
            "sales_should_not_have_high_risk_write",
            "permission_rollback_empty"
    );

    private final I18nService i18nService;

    protected BaseApiController(I18nService i18nService) {
        this.i18nService = i18nService;
    }

    protected boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    protected boolean hasAnyRole(HttpServletRequest request, String... roles) {
        Object roleObj = request.getAttribute("authRole");
        if (roleObj == null) {
            return false;
        }
        String role = String.valueOf(roleObj);
        for (String expected : roles) {
            if (expected.equals(role)) {
                return true;
            }
        }
        return false;
    }

    protected String currentUser(HttpServletRequest request) {
        Object username = request.getAttribute("authUsername");
        return username == null ? "unknown" : String.valueOf(username);
    }

    protected String currentRole(HttpServletRequest request) {
        Object role = request.getAttribute("authRole");
        return role == null ? "UNKNOWN" : String.valueOf(role);
    }

    protected String currentOwnerScope(HttpServletRequest request) {
        Object ownerScope = request.getAttribute("authOwnerScope");
        if (ownerScope == null || isBlank(String.valueOf(ownerScope))) {
            return currentUser(request);
        }
        return String.valueOf(ownerScope);
    }

    protected String currentTenant(HttpServletRequest request) {
        Object tenant = request.getAttribute("authTenantId");
        if (tenant == null || isBlank(String.valueOf(tenant))) {
            String headerTenant = request.getHeader("X-Tenant-Id");
            if (!isBlank(headerTenant)) {
                return headerTenant.trim();
            }
            return "tenant_default";
        }
        return String.valueOf(tenant);
    }

    protected boolean isSalesScoped(HttpServletRequest request) {
        return "SALES".equalsIgnoreCase(currentRole(request));
    }

    protected boolean ownerMatchesScope(HttpServletRequest request, String owner) {
        if (!isSalesScoped(request)) {
            return true;
        }
        if (isBlank(owner)) {
            return false;
        }
        String normalizedOwner = owner.trim().toLowerCase(Locale.ROOT);
        String scope = currentOwnerScope(request).trim().toLowerCase(Locale.ROOT);
        String username = currentUser(request).trim().toLowerCase(Locale.ROOT);
        return normalizedOwner.equals(scope) || normalizedOwner.equals(username);
    }

    protected String msg(HttpServletRequest request, String key) {
        return i18nService.msg(request, key);
    }

    protected Long parseLong(Object value) {
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ex) {
            return null;
        }
    }

    protected Integer parseInt(Object value) {
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ex) {
            return null;
        }
    }

    protected Pageable buildPageable(int page, int size, String sortBy, String sortDir, Set<String> allowedFields, String defaultField) {
        String finalSortBy = allowedFields.contains(sortBy) ? sortBy : defaultField;
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(Math.max(0, page - 1), Math.max(1, size), Sort.by(direction, finalSortBy));
    }

    protected LocalDateTime parseDateStart(String value) {
        if (isBlank(value)) return null;
        LocalDate parsed = parseLocalDate(value);
        return parsed == null ? null : parsed.atStartOfDay();
    }

    protected LocalDateTime parseDateStart(HttpServletRequest request, String value) {
        if (isBlank(value)) return null;
        LocalDate parsed = parseLocalDate(request, value);
        return parsed == null ? null : parsed.atStartOfDay();
    }

    protected LocalDateTime parseDateEnd(String value) {
        if (isBlank(value)) return null;
        LocalDate parsed = parseLocalDate(value);
        return parsed == null ? null : parsed.atTime(23, 59, 59);
    }

    protected LocalDateTime parseDateEnd(HttpServletRequest request, String value) {
        if (isBlank(value)) return null;
        LocalDate parsed = parseLocalDate(request, value);
        return parsed == null ? null : parsed.atTime(23, 59, 59);
    }

    protected LocalDate parseLocalDate(Object value) {
        return parseLocalDate(null, value);
    }

    protected LocalDate parseLocalDate(HttpServletRequest request, Object value) {
        if (value == null) return null;
        String str = String.valueOf(value);
        if (isBlank(str)) return null;
        String normalized = str.trim();
        String tenantFormat = request == null ? DATE_FORMAT_ISO : currentTenantDateFormat(request);
        LocalDate tenantParsed = parseByFormat(normalized, tenantFormat);
        if (tenantParsed != null) {
            return tenantParsed;
        }
        for (String format : supportedDateFormats()) {
            LocalDate parsed = parseByFormat(normalized, format);
            if (parsed != null) return parsed;
        }
        return null;
    }

    protected String normalizeTenantDateFormat(String input) {
        if (isBlank(input)) return DATE_FORMAT_ISO;
        String normalized = input.trim();
        if ("YYYY-MM-DD".equalsIgnoreCase(normalized)) return DATE_FORMAT_ISO;
        for (String format : supportedDateFormats()) {
            if (format.equals(normalized)) return format;
        }
        return "";
    }

    protected Set<String> supportedDateFormats() {
        return new HashSet<String>(Arrays.asList(DATE_FORMAT_ISO, DATE_FORMAT_DMY, DATE_FORMAT_MDY));
    }

    protected String currentTenantDateFormat(HttpServletRequest request) {
        Object formatObj = request.getAttribute("authTenantDateFormat");
        String format = formatObj == null ? DATE_FORMAT_ISO : String.valueOf(formatObj);
        String normalized = normalizeTenantDateFormat(format);
        return isBlank(normalized) ? DATE_FORMAT_ISO : normalized;
    }

    private LocalDate parseByFormat(String input, String format) {
        try {
            return LocalDate.parse(input, DateTimeFormatter.ofPattern(format));
        } catch (Exception ignore) {
            return null;
        }
    }

    protected String escapeCsv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    protected String traceId(HttpServletRequest request) {
        Object trace = request.getAttribute(TraceIdInterceptor.TRACE_ID_ATTR);
        return trace == null ? "" : String.valueOf(trace);
    }

    protected Map<String, Object> errorBody(HttpServletRequest request, String code, String message, Map<String, Object> details) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("code", code);
        body.put("message", message);
        body.put("requestId", traceId(request));
        body.put("details", details == null ? new LinkedHashMap<String, Object>() : details);
        return body;
    }

    protected Map<String, Object> successBody(HttpServletRequest request, String code, String message, Map<String, Object> details) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("code", normalizeCode(code, "success"));
        body.put("message", message);
        body.put("requestId", traceId(request));
        body.put("details", details == null ? new LinkedHashMap<String, Object>() : details);
        return body;
    }

    protected Map<String, Object> successByKey(HttpServletRequest request, String key, Map<String, Object> details) {
        String normalized = normalizeCode(key, "success");
        return successBody(request, normalized, msg(request, normalized), details);
    }

    protected Map<String, Object> successWithFields(HttpServletRequest request, String key, Map<String, Object> fields) {
        Map<String, Object> out = successByKey(request, key, null);
        if (fields != null) {
            for (Map.Entry<String, Object> entry : fields.entrySet()) {
                String field = entry.getKey();
                if ("code".equals(field) || "message".equals(field) || "requestId".equals(field) || "details".equals(field)) {
                    continue;
                }
                out.put(field, entry.getValue());
            }
        }
        return out;
    }

    protected Map<String, Object> legacyErrorBody(HttpServletRequest request, String code, String message, Map<String, Object> details) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("message", message);
        body.put("code", legacyCode(code, "BAD_REQUEST"));
        body.put("requestId", traceId(request));
        body.put("details", details == null ? new LinkedHashMap<String, Object>() : details);
        return body;
    }

    protected Map<String, Object> legacyErrorByKey(HttpServletRequest request, String key, String fallbackCode, Map<String, Object> details) {
        String msgKey = isBlank(key) ? "bad_request" : key;
        String code = legacyCode(msgKey, fallbackCode);
        return legacyErrorBody(request, code, msg(request, msgKey), details);
    }

    protected String normalizeCode(String value, String fallback) {
        String base = isBlank(value) ? fallback : value.trim().toLowerCase(Locale.ROOT);
        if (isBlank(base)) return "bad_request";
        String normalized = base.replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        return isBlank(normalized) ? "bad_request" : normalized;
    }

    protected String legacyCode(String value, String fallback) {
        String normalized = normalizeCode(value, fallback == null ? "bad_request" : fallback.toLowerCase(Locale.ROOT));
        return normalized.toUpperCase(Locale.ROOT);
    }

    protected Map<String, Object> legacyErrorByKnownKey(
            HttpServletRequest request,
            String keyCandidate,
            String fallbackKey,
            String fallbackCode,
            Set<String> allowedKeys,
            Map<String, Object> details
    ) {
        String normalizedKey = normalizeCode(keyCandidate, "");
        boolean known = !isBlank(normalizedKey) && allowedKeys != null && allowedKeys.contains(normalizedKey);
        String code = known ? legacyCode(normalizedKey, fallbackCode) : legacyCode(fallbackCode, fallbackCode);
        String messageKey = known ? normalizedKey : normalizeCode(fallbackKey, "bad_request");
        return legacyErrorBody(request, code, msg(request, messageKey), details);
    }

    private static Set<String> immutableKeys(String... keys) {
        return Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(keys)));
    }
}
