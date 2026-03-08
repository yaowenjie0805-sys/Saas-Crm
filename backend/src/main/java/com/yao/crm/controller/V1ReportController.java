package com.yao.crm.controller;

import com.yao.crm.service.I18nService;
import com.yao.crm.service.ReportExportJobService;
import com.yao.crm.service.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.Collections;
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

        LocalDate fromDate = parseLocalDate(from);
        LocalDate toDate = parseLocalDate(to);
        if ((fromDate == null && !isBlank(from)) || (toDate == null && !isBlank(to))) {
            return ResponseEntity.badRequest().body(errorBody(request, "invalid_date_format", msg(request, "invalid_date_format"), null));
        }
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            return ResponseEntity.badRequest().body(errorBody(request, "date_range_invalid", msg(request, "date_range_invalid"), null));
        }

        Map<String, Object> data = reportService.overviewByTenant(currentTenant(request), fromDate, toDate, role, owner, department);
        Map<String, Object> filters = new HashMap<String, Object>();
        filters.put("timezone", isBlank(timezone) ? "Asia/Shanghai" : timezone);
        filters.put("currency", isBlank(currency) ? "CNY" : currency.toUpperCase(Locale.ROOT));
        filters.put("role", role);
        filters.put("department", department);
        filters.put("owner", owner);
        filters.put("tenantId", currentTenant(request));
        data.put("filters", filters);
        return ResponseEntity.ok(data);
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
        LocalDate fromDate = parseLocalDate(from);
        LocalDate toDate = parseLocalDate(to);
        if ((fromDate == null && !isBlank(from)) || (toDate == null && !isBlank(to))) {
            return ResponseEntity.badRequest().body(errorBody(request, "invalid_date_format", msg(request, "invalid_date_format"), null));
        }
        return ResponseEntity.accepted().body(reportExportJobService.submitByTenant(
                currentUser(request),
                currentTenant(request),
                role,
                fromDate,
                toDate,
                owner,
                department,
                timezone,
                currency
        ));
    }

    @GetMapping("/export-jobs")
    public ResponseEntity<?> listExportJobs(HttpServletRequest request,
                                            @RequestParam(defaultValue = "8") int limit,
                                            @RequestParam(defaultValue = "") String status) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        return ResponseEntity.ok(Collections.singletonMap("items", reportExportJobService.listByTenant(
                currentUser(request),
                currentTenant(request),
                hasAnyRole(request, "ADMIN", "MANAGER"),
                Math.max(1, Math.min(limit, 100)),
                status
        )));
    }

    @PostMapping("/export-jobs/{jobId}/retry")
    public ResponseEntity<?> retryExport(HttpServletRequest request, @PathVariable String jobId) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        try {
            return ResponseEntity.accepted().body(reportExportJobService.retryByTenant(
                    jobId,
                    currentUser(request),
                    currentTenant(request),
                    hasAnyRole(request, "ADMIN", "MANAGER")
            ));
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
            return ResponseEntity.ok(reportExportJobService.statusByTenant(jobId, currentUser(request), currentTenant(request), hasAnyRole(request, "ADMIN", "MANAGER")));
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
            String filename = "report-overview-" + jobId + ".csv";
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

