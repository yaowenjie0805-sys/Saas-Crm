package com.yao.crm.controller;

import com.yao.crm.service.I18nService;
import com.yao.crm.service.ReportExportJobService;
import com.yao.crm.service.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ReportController extends BaseApiController {

    private final ReportService reportService;
    private final ReportExportJobService reportExportJobService;

    public ReportController(ReportService reportService,
                            ReportExportJobService reportExportJobService,
                            I18nService i18nService) {
        super(i18nService);
        this.reportService = reportService;
        this.reportExportJobService = reportExportJobService;
    }

    @GetMapping("/reports/overview")
    public ResponseEntity<?> reportsOverview(HttpServletRequest request,
                                             @RequestParam(value = "from", required = false) String from,
                                             @RequestParam(value = "to", required = false) String to,
                                             @RequestParam(value = "role", required = false) String role) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "ANALYST")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }

        DateRange range = validateDateRange(request, from, to);
        if (range.errorBody != null) {
            return range.errorBody;
        }

        return ResponseEntity.ok(reportService.overview(range.from, range.to, role));
    }

    @GetMapping("/reports/overview/export")
    public ResponseEntity<?> exportOverviewCsv(HttpServletRequest request,
                                               @RequestParam(value = "from", required = false) String from,
                                               @RequestParam(value = "to", required = false) String to,
                                               @RequestParam(value = "role", required = false) String role) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "ANALYST")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }

        DateRange range = validateDateRange(request, from, to);
        if (range.errorBody != null) {
            return range.errorBody;
        }

        boolean zh = request.getHeader("Accept-Language") != null
                && request.getHeader("Accept-Language").toLowerCase(Locale.ROOT).startsWith("zh");
        String csv = reportService.exportOverviewCsvByTenant("tenant_default", range.from, range.to, role, "", "", zh ? "zh" : "en");
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String fileName = (zh ? "\u62a5\u8868\u603b\u89c8-" : "report-overview-") + date + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(csv);
    }

    @PostMapping("/reports/export-jobs")
    public ResponseEntity<?> createReportExportJob(HttpServletRequest request,
                                                   @RequestParam(value = "from", required = false) String from,
                                                   @RequestParam(value = "to", required = false) String to,
                                                   @RequestParam(value = "role", required = false) String role) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "ANALYST")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }
        DateRange range = validateDateRange(request, from, to);
        if (range.errorBody != null) {
            return range.errorBody;
        }
        return ResponseEntity.status(202).body(reportExportJobService.submit(currentUser(request), role, range.from, range.to));
    }

    @GetMapping("/reports/export-jobs")
    public ResponseEntity<?> listReportExportJobs(HttpServletRequest request,
                                                  @RequestParam(defaultValue = "8") int limit,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "8") int size,
                                                  @RequestParam(defaultValue = "") String status) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "ANALYST")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }
        int safeSize = Math.max(1, Math.min(100, size));
        int safePage = Math.max(1, page);
        if (page <= 1 && size == 8 && limit > 0) {
            safeSize = Math.max(1, Math.min(100, limit));
        }
        Map<String, Object> body = reportExportJobService.listByTenantPaged(
                currentUser(request),
                "tenant_default",
                hasAnyRole(request, "ADMIN", "MANAGER"),
                safePage,
                safeSize,
                status
        );
        return ResponseEntity.ok(body);
    }

    @GetMapping("/reports/export-jobs/{jobId}")
    public ResponseEntity<?> reportExportJobStatus(HttpServletRequest request, @PathVariable String jobId) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "ANALYST")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }
        try {
            return ResponseEntity.ok(reportExportJobService.status(jobId, currentUser(request), hasAnyRole(request, "ADMIN", "MANAGER")));
        } catch (IllegalArgumentException ex) {
            if ("forbidden".equals(ex.getMessage())) {
                return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
            }
            return ResponseEntity.status(404).body(legacyErrorByKnownKey(
                    request,
                    ex.getMessage(),
                    "export_job_not_found",
                    "NOT_FOUND",
                    LEGACY_EXPORT_JOB_ERROR_KEYS,
                    null
            ));
        }
    }

    @PostMapping("/reports/export-jobs/{jobId}/retry")
    public ResponseEntity<?> retryReportExportJob(HttpServletRequest request, @PathVariable String jobId) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "ANALYST")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }
        try {
            Map<String, Object> job = reportExportJobService.retry(jobId, currentUser(request), hasAnyRole(request, "ADMIN", "MANAGER"));
            return ResponseEntity.status(202).body(job);
        } catch (IllegalArgumentException ex) {
            if ("forbidden".equals(ex.getMessage())) {
                return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
            }
            return ResponseEntity.status(404).body(legacyErrorByKnownKey(
                    request,
                    ex.getMessage(),
                    "export_job_not_found",
                    "NOT_FOUND",
                    LEGACY_EXPORT_JOB_ERROR_KEYS,
                    null
            ));
        }
    }

    @GetMapping(value = "/reports/export-jobs/{jobId}/download", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<?> downloadReportExportJob(HttpServletRequest request, @PathVariable String jobId) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "ANALYST")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }
        try {
            String csv = reportExportJobService.download(jobId, currentUser(request), hasAnyRole(request, "ADMIN", "MANAGER"));
            boolean zh = request.getHeader("Accept-Language") != null
                    && request.getHeader("Accept-Language").toLowerCase(Locale.ROOT).startsWith("zh");
            String fileName = (zh ? "\u62a5\u8868\u603b\u89c8-" : "report-overview-") + jobId + ".csv";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                    .body(csv);
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(legacyErrorByKnownKey(
                    request,
                    ex.getMessage(),
                    "export_job_not_ready",
                    "CONFLICT",
                    LEGACY_EXPORT_JOB_ERROR_KEYS,
                    null
            ));
        } catch (IllegalArgumentException ex) {
            if ("forbidden".equals(ex.getMessage())) {
                return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
            }
            return ResponseEntity.status(404).body(legacyErrorByKnownKey(
                    request,
                    ex.getMessage(),
                    "export_job_not_found",
                    "NOT_FOUND",
                    LEGACY_EXPORT_JOB_ERROR_KEYS,
                    null
            ));
        }
    }

    private DateRange validateDateRange(HttpServletRequest request, String fromRaw, String toRaw) {
        LocalDate fromDate = parseLocalDate(request, fromRaw);
        if (fromRaw != null && !fromRaw.trim().isEmpty() && fromDate == null) {
            return DateRange.error(ResponseEntity.badRequest().body(legacyErrorByKey(request, "invalid_date_format", "BAD_REQUEST", null)));
        }

        LocalDate toDate = parseLocalDate(request, toRaw);
        if (toRaw != null && !toRaw.trim().isEmpty() && toDate == null) {
            return DateRange.error(ResponseEntity.badRequest().body(legacyErrorByKey(request, "invalid_date_format", "BAD_REQUEST", null)));
        }

        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            return DateRange.error(ResponseEntity.badRequest().body(legacyErrorByKey(request, "date_range_invalid", "BAD_REQUEST", null)));
        }

        return DateRange.ok(fromDate, toDate);
    }

    private static final class DateRange {
        private final LocalDate from;
        private final LocalDate to;
        private final ResponseEntity<?> errorBody;

        private DateRange(LocalDate from, LocalDate to, ResponseEntity<?> errorBody) {
            this.from = from;
            this.to = to;
            this.errorBody = errorBody;
        }

        private static DateRange ok(LocalDate from, LocalDate to) {
            return new DateRange(from, to, null);
        }

        private static DateRange error(ResponseEntity<?> errorBody) {
            return new DateRange(null, null, errorBody);
        }
    }
}
