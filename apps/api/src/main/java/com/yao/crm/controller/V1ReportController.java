package com.yao.crm.controller;

import com.yao.crm.service.I18nService;
import com.yao.crm.service.DashboardMetricsCacheService;
import com.yao.crm.service.ReportExportJobService;
import com.yao.crm.service.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports")
public class V1ReportController extends BaseApiController {

    private final ReportService reportService;
    private final ReportExportJobService reportExportJobService;

    public V1ReportController(ReportService reportService,
                              ReportExportJobService reportExportJobService,
                              I18nService i18nService) {
        super(i18nService);
        this.reportService = reportService;
        this.reportExportJobService = reportExportJobService;
    }

    @GetMapping("/overview")
    public ResponseEntity<?> overview(HttpServletRequest request,
                                      @RequestParam(defaultValue = "") String from,
                                      @RequestParam(defaultValue = "") String to,
                                      @RequestParam(defaultValue = "") String role,
                                      @RequestParam(defaultValue = "") String department,
                                      @RequestParam(defaultValue = "") String owner,
                                      @RequestParam(defaultValue = "") String timezone,
                                      @RequestParam(defaultValue = "") String currency) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }

        LocalDate fromDate = parseLocalDate(request, from);
        LocalDate toDate = parseLocalDate(request, to);
        if ((fromDate == null && !isBlank(from)) || (toDate == null && !isBlank(to))) {
            return ResponseEntity.badRequest().body(errorBody(request, "invalid_date_format", msg(request, "invalid_date_format"), null));
        }
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            return ResponseEntity.badRequest().body(errorBody(request, "date_range_invalid", msg(request, "date_range_invalid"), null));
        }
        String normalizedRole = normalizeOptional(role);
        String normalizedOwner = normalizeOptional(owner);
        String normalizedDepartment = normalizeOptional(department);
        String normalizedTimezone = normalizeOptional(timezone);
        String normalizedCurrency = normalizeOptional(currency);

        com.yao.crm.service.DashboardMetricsCacheService.CachedValue<Map<String, Object>> cached =
                reportService.overviewByTenantCached(
                        currentTenant(request),
                        currentUser(request),
                        currentRole(request),
                        fromDate,
                        toDate,
                        normalizedRole,
                        normalizedOwner,
                        normalizedDepartment
                );
        Map<String, Object> data = new HashMap<String, Object>(cached.getValue());
        Map<String, Object> filters = new HashMap<String, Object>();
        filters.put("timezone", isBlank(normalizedTimezone) ? "Asia/Shanghai" : normalizedTimezone);
        filters.put("currency", isBlank(normalizedCurrency) ? "CNY" : normalizedCurrency.toUpperCase(Locale.ROOT));
        filters.put("role", normalizedRole);
        filters.put("department", normalizedDepartment);
        filters.put("owner", normalizedOwner);
        filters.put("tenantId", currentTenant(request));
        data.put("filters", filters);
        return ResponseEntity.ok()
                .header("X-CRM-Cache", cached.isHit() ? "HIT" : "MISS")
                .header("X-CRM-Cache-Tier", cached.getTier())
                .header("X-CRM-Cache-Fallback", cached.isFallback() ? "1" : "0")
                .body(successWithFields(request, "report_overview_loaded", data));
    }

    @GetMapping("/funnel")
    public ResponseEntity<?> funnel(HttpServletRequest request,
                                    @RequestParam(defaultValue = "") String from,
                                    @RequestParam(defaultValue = "") String to,
                                    @RequestParam(defaultValue = "") String owner) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        LocalDate fromDate = parseLocalDate(request, from);
        LocalDate toDate = parseLocalDate(request, to);
        if ((fromDate == null && !isBlank(from)) || (toDate == null && !isBlank(to))) {
            return ResponseEntity.badRequest().body(errorBody(request, "invalid_date_format", msg(request, "invalid_date_format"), null));
        }
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            return ResponseEntity.badRequest().body(errorBody(request, "date_range_invalid", msg(request, "date_range_invalid"), null));
        }
        String normalizedOwner = normalizeOptional(owner);
        DashboardMetricsCacheService.CachedValue<Map<String, Object>> cached = reportService.funnelByTenantCached(
                currentTenant(request),
                currentUser(request),
                currentRole(request),
                fromDate,
                toDate,
                normalizedOwner
        );
        return ResponseEntity.ok()
                .header("X-CRM-Cache", cached.isHit() ? "HIT" : "MISS")
                .header("X-CRM-Cache-Tier", cached.getTier())
                .header("X-CRM-Cache-Fallback", cached.isFallback() ? "1" : "0")
                .body(successWithFields(request, "report_funnel_loaded", cached.getValue()));
    }

    @PostMapping("/export-jobs")
    public ResponseEntity<?> submitExport(HttpServletRequest request,
                                          @RequestParam(defaultValue = "") String role,
                                          @RequestParam(defaultValue = "") String owner,
                                          @RequestParam(defaultValue = "") String department,
                                          @RequestParam(defaultValue = "") String from,
                                          @RequestParam(defaultValue = "") String to,
                                          @RequestParam(defaultValue = "") String timezone,
                                          @RequestParam(defaultValue = "") String currency) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        LocalDate fromDate = parseLocalDate(request, from);
        LocalDate toDate = parseLocalDate(request, to);
        if ((fromDate == null && !isBlank(from)) || (toDate == null && !isBlank(to))) {
            return ResponseEntity.badRequest().body(errorBody(request, "invalid_date_format", msg(request, "invalid_date_format"), null));
        }
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            return ResponseEntity.badRequest().body(errorBody(request, "date_range_invalid", msg(request, "date_range_invalid"), null));
        }
        String normalizedRole = normalizeOptional(role);
        String normalizedOwner = normalizeOptional(owner);
        String normalizedDepartment = normalizeOptional(department);
        String normalizedTimezone = normalizeOptional(timezone);
        String normalizedCurrency = normalizeOptional(currency);
        @SuppressWarnings("unchecked")
        Map<String, Object> job = (Map<String, Object>) reportExportJobService.submitByTenant(
                currentUser(request),
                currentTenant(request),
                normalizedRole,
                fromDate,
                toDate,
                normalizedOwner,
                normalizedDepartment,
                normalizedTimezone,
                normalizedCurrency,
                request.getHeader("Accept-Language")
        );
        return ResponseEntity.accepted().body(successWithFields(request, "report_export_submitted", job));
    }

    @GetMapping("/export-jobs")
    public ResponseEntity<?> listExportJobs(HttpServletRequest request,
                                            @RequestParam(defaultValue = "8") int limit,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "8") int size,
                                            @RequestParam(defaultValue = "") String status) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        int safeSize = Math.max(1, Math.min(100, size));
        int safePage = Math.max(1, page);
        if (page <= 1 && size == 8 && limit > 0) {
            safeSize = Math.max(1, Math.min(100, limit));
        }
        String normalizedStatus = normalizeOptional(status);
        Map<String, Object> body = reportExportJobService.listByTenantPaged(
                currentUser(request),
                currentTenant(request),
                hasAnyRole(request, "ADMIN", "MANAGER"),
                safePage,
                safeSize,
                normalizedStatus
        );
        return ResponseEntity.ok(successWithFields(request, "report_export_jobs_listed", body));
    }

    @PostMapping("/export-jobs/{jobId}/retry")
    public ResponseEntity<?> retryExport(HttpServletRequest request, @PathVariable String jobId) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String normalizedJobId = normalizeOptional(jobId);
        if (isBlank(normalizedJobId)) {
            return ResponseEntity.badRequest().body(errorBody(request, "bad_request", msg(request, "bad_request"), null));
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> retried = (Map<String, Object>) reportExportJobService.retryByTenant(
                    normalizedJobId,
                    currentUser(request),
                    currentTenant(request),
                    hasAnyRole(request, "ADMIN", "MANAGER")
            );
            return ResponseEntity.accepted().body(successWithFields(request, "report_export_retried", retried));
        } catch (IllegalArgumentException ex) {
            return exportJobArgumentError(request, ex);
        } catch (IllegalStateException ex) {
            String code = normalizeCode(ex.getMessage(), "");
            if ("export_job_not_retryable".equals(code)) {
                return ResponseEntity.status(409).body(errorBody(request, code, msg(request, code), null));
            }
            throw ex;
        }
    }

    @GetMapping("/export-jobs/{jobId}")
    public ResponseEntity<?> exportStatus(HttpServletRequest request, @PathVariable String jobId) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String normalizedJobId = normalizeOptional(jobId);
        if (isBlank(normalizedJobId)) {
            return ResponseEntity.badRequest().body(errorBody(request, "bad_request", msg(request, "bad_request"), null));
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> statusBody = (Map<String, Object>) reportExportJobService.statusByTenant(normalizedJobId, currentUser(request), currentTenant(request), hasAnyRole(request, "ADMIN", "MANAGER"));
            return ResponseEntity.ok(successWithFields(request, "report_export_status_loaded", statusBody));
        } catch (IllegalArgumentException ex) {
            return exportJobArgumentError(request, ex);
        }
    }

    @GetMapping("/export-jobs/{jobId}/download")
    public ResponseEntity<?> download(HttpServletRequest request, @PathVariable String jobId) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String normalizedJobId = normalizeOptional(jobId);
        if (isBlank(normalizedJobId)) {
            return ResponseEntity.badRequest().body(errorBody(request, "bad_request", msg(request, "bad_request"), null));
        }
        try {
            String csv = reportExportJobService.downloadByTenant(normalizedJobId, currentUser(request), currentTenant(request), hasAnyRole(request, "ADMIN", "MANAGER"));
            boolean zh = request.getHeader("Accept-Language") != null && request.getHeader("Accept-Language").toLowerCase(Locale.ROOT).startsWith("zh");
            String filename = (zh ? "\u62a5\u8868\u603b\u89c8-" : "report-overview-") + normalizedJobId + ".csv";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(csv);
        } catch (IllegalArgumentException ex) {
            return exportJobArgumentError(request, ex);
        } catch (IllegalStateException ex) {
            String code = normalizeCode(ex.getMessage(), "conflict");
            return ResponseEntity.status(409).body(errorBody(request, code, msg(request, code), null));
        }
    }

    private String normalizeOptional(String value) {
        return isBlank(value) ? "" : value.trim();
    }

    private ResponseEntity<?> exportJobArgumentError(HttpServletRequest request, IllegalArgumentException ex) {
        String code = normalizeCode(ex.getMessage(), "bad_request");
        if ("forbidden".equals(code)) {
            return ResponseEntity.status(403).body(errorBody(request, code, msg(request, code), null));
        }
        if ("export_job_not_found".equals(code)) {
            return ResponseEntity.status(404).body(errorBody(request, code, msg(request, code), null));
        }
        return ResponseEntity.badRequest().body(errorBody(request, code, msg(request, code), null));
    }
}
