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

        com.yao.crm.service.DashboardMetricsCacheService.CachedValue<Map<String, Object>> cached =
                reportService.overviewByTenantCached(
                        currentTenant(request),
                        currentUser(request),
                        currentRole(request),
                        fromDate,
                        toDate,
                        role,
                        owner,
                        department
                );
        Map<String, Object> data = new HashMap<String, Object>(cached.getValue());
        Map<String, Object> filters = new HashMap<String, Object>();
        filters.put("timezone", isBlank(timezone) ? "Asia/Shanghai" : timezone);
        filters.put("currency", isBlank(currency) ? "CNY" : currency.toUpperCase(Locale.ROOT));
        filters.put("role", role);
        filters.put("department", department);
        filters.put("owner", owner);
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
        DashboardMetricsCacheService.CachedValue<Map<String, Object>> cached = reportService.funnelByTenantCached(
                currentTenant(request),
                currentUser(request),
                currentRole(request),
                fromDate,
                toDate,
                owner
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
        @SuppressWarnings("unchecked")
        Map<String, Object> job = (Map<String, Object>) reportExportJobService.submitByTenant(
                currentUser(request),
                currentTenant(request),
                role,
                fromDate,
                toDate,
                owner,
                department,
                timezone,
                currency,
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
        Map<String, Object> body = reportExportJobService.listByTenantPaged(
                currentUser(request),
                currentTenant(request),
                hasAnyRole(request, "ADMIN", "MANAGER"),
                safePage,
                safeSize,
                status
        );
        return ResponseEntity.ok(successWithFields(request, "report_export_jobs_listed", body));
    }

    @PostMapping("/export-jobs/{jobId}/retry")
    public ResponseEntity<?> retryExport(HttpServletRequest request, @PathVariable String jobId) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> retried = (Map<String, Object>) reportExportJobService.retryByTenant(
                    jobId,
                    currentUser(request),
                    currentTenant(request),
                    hasAnyRole(request, "ADMIN", "MANAGER")
            );
            return ResponseEntity.accepted().body(successWithFields(request, "report_export_retried", retried));
        } catch (IllegalArgumentException ex) {
            String code = normalizeCode(ex.getMessage(), "bad_request");
            return ResponseEntity.badRequest().body(errorBody(request, code, msg(request, code), null));
        }
    }

    @GetMapping("/export-jobs/{jobId}")
    public ResponseEntity<?> exportStatus(HttpServletRequest request, @PathVariable String jobId) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> statusBody = (Map<String, Object>) reportExportJobService.statusByTenant(jobId, currentUser(request), currentTenant(request), hasAnyRole(request, "ADMIN", "MANAGER"));
            return ResponseEntity.ok(successWithFields(request, "report_export_status_loaded", statusBody));
        } catch (IllegalArgumentException ex) {
            String code = normalizeCode(ex.getMessage(), "bad_request");
            return ResponseEntity.badRequest().body(errorBody(request, code, msg(request, code), null));
        }
    }

    @GetMapping("/export-jobs/{jobId}/download")
    public ResponseEntity<?> download(HttpServletRequest request, @PathVariable String jobId) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        try {
            String csv = reportExportJobService.downloadByTenant(jobId, currentUser(request), currentTenant(request), hasAnyRole(request, "ADMIN", "MANAGER"));
            boolean zh = request.getHeader("Accept-Language") != null && request.getHeader("Accept-Language").toLowerCase(Locale.ROOT).startsWith("zh");
            String filename = (zh ? "\u62a5\u8868\u603b\u89c8-" : "report-overview-") + jobId + ".csv";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(csv);
        } catch (IllegalArgumentException ex) {
            String code = normalizeCode(ex.getMessage(), "bad_request");
            return ResponseEntity.badRequest().body(errorBody(request, code, msg(request, code), null));
        } catch (IllegalStateException ex) {
            String code = normalizeCode(ex.getMessage(), "conflict");
            return ResponseEntity.status(409).body(errorBody(request, code, msg(request, code), null));
        }
    }
}
