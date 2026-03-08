package com.yao.crm.controller;

import com.yao.crm.entity.AuditLog;
import com.yao.crm.repository.AuditLogRepository;
import com.yao.crm.service.AuditExportJobService;
import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.I18nService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.Predicate;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api")
public class AuditController extends BaseApiController {

    private final AuditLogService auditLogService;
    private final AuditLogRepository auditLogRepository;
    private final AuditExportJobService auditExportJobService;

    public AuditController(AuditLogService auditLogService,
                           AuditLogRepository auditLogRepository,
                           AuditExportJobService auditExportJobService,
                           I18nService i18nService) {
        super(i18nService);
        this.auditLogService = auditLogService;
        this.auditLogRepository = auditLogRepository;
        this.auditExportJobService = auditExportJobService;
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<?> auditLogs(HttpServletRequest request) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }
        return ResponseEntity.ok(auditLogService.latest());
    }

    @GetMapping("/audit-logs/search")
    public ResponseEntity<?> searchAuditLogs(
            HttpServletRequest request,
            @RequestParam(defaultValue = "") String username,
            @RequestParam(defaultValue = "") String role,
            @RequestParam(defaultValue = "") String action,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "") String from,
            @RequestParam(defaultValue = "") String to,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "ANALYST")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }

        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(100, size));
        Pageable pageable = buildPageable(
                safePage, safeSize, sortBy, sortDir,
                new HashSet<String>(Arrays.asList("username", "role", "action", "resource", "createdAt")),
                "createdAt"
        );

        LocalDateTime fromTime = parseDateStart(from);
        LocalDateTime toTime = parseDateEnd(to);
        if ((!isBlank(from) && fromTime == null) || (!isBlank(to) && toTime == null)) {
            return ResponseEntity.badRequest().body(legacyErrorByKey(request, "invalid_date_format", "BAD_REQUEST", null));
        }

        Page<AuditLog> result = auditLogRepository.findAll(buildAuditSpec(username, role, action, fromTime, toTime), pageable);
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("items", result.getContent());
        body.put("total", result.getTotalElements());
        body.put("page", safePage);
        body.put("size", safeSize);
        body.put("totalPages", result.getTotalPages());
        return ResponseEntity.ok(body);
    }

    @GetMapping(value = "/audit-logs/export", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<?> exportAuditLogs(
            HttpServletRequest request,
            @RequestParam(defaultValue = "") String username,
            @RequestParam(defaultValue = "") String role,
            @RequestParam(defaultValue = "") String action,
            @RequestParam(defaultValue = "") String from,
            @RequestParam(defaultValue = "") String to
    ) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "ANALYST")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }

        LocalDateTime fromTime = parseDateStart(from);
        LocalDateTime toTime = parseDateEnd(to);
        if ((!isBlank(from) && fromTime == null) || (!isBlank(to) && toTime == null)) {
            return ResponseEntity.badRequest().body(legacyErrorByKey(request, "invalid_date_format", "BAD_REQUEST", null));
        }

        List<AuditLog> logs = auditLogRepository.findAll(buildAuditSpec(username, role, action, fromTime, toTime));
        StringBuilder csv = new StringBuilder();
        csv.append("id,username,role,action,resource,resourceId,details,createdAt\n");
        for (AuditLog log : logs) {
            csv.append(escapeCsv(log.getId())).append(',')
                    .append(escapeCsv(log.getUsername())).append(',')
                    .append(escapeCsv(log.getRole())).append(',')
                    .append(escapeCsv(log.getAction())).append(',')
                    .append(escapeCsv(log.getResource())).append(',')
                    .append(escapeCsv(log.getResourceId())).append(',')
                    .append(escapeCsv(log.getDetails())).append(',')
                    .append(escapeCsv(log.getCreatedAt() == null ? "" : log.getCreatedAt().toString()))
                    .append('\n');
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit-logs.csv\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(csv.toString());
    }

    @PostMapping("/audit-logs/export-jobs")
    public ResponseEntity<?> createExportJob(
            HttpServletRequest request,
            @RequestParam(defaultValue = "") String username,
            @RequestParam(defaultValue = "") String role,
            @RequestParam(defaultValue = "") String action,
            @RequestParam(defaultValue = "") String from,
            @RequestParam(defaultValue = "") String to
    ) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "ANALYST")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }
        LocalDateTime fromTime = parseDateStart(from);
        LocalDateTime toTime = parseDateEnd(to);
        if ((!isBlank(from) && fromTime == null) || (!isBlank(to) && toTime == null)) {
            return ResponseEntity.badRequest().body(legacyErrorByKey(request, "invalid_date_format", "BAD_REQUEST", null));
        }
        return ResponseEntity.status(202).body(auditExportJobService.submit(currentUser(request), role, username, action, fromTime, toTime));
    }

    @GetMapping("/audit-logs/export-jobs")
    public ResponseEntity<?> listExportJobs(HttpServletRequest request,
                                            @RequestParam(defaultValue = "8") int limit,
                                            @RequestParam(defaultValue = "") String status) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "ANALYST")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }
        List<Map<String, Object>> items = auditExportJobService.list(
                currentUser(request),
                hasAnyRole(request, "ADMIN", "MANAGER"),
                Math.max(1, Math.min(50, limit)),
                status
        );
        return ResponseEntity.ok(Collections.singletonMap("items", items));
    }

    @PostMapping("/audit-logs/export-jobs/{jobId}/retry")
    public ResponseEntity<?> retryExportJob(HttpServletRequest request, @PathVariable String jobId) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "ANALYST")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }
        try {
            Map<String, Object> job = auditExportJobService.retry(jobId, currentUser(request), hasAnyRole(request, "ADMIN", "MANAGER"));
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

    @GetMapping("/audit-logs/export-jobs/{jobId}")
    public ResponseEntity<?> exportJobStatus(HttpServletRequest request, @PathVariable String jobId) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "ANALYST")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }
        try {
            return ResponseEntity.ok(auditExportJobService.status(jobId, currentUser(request), hasAnyRole(request, "ADMIN", "MANAGER")));
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

    @GetMapping(value = "/audit-logs/export-jobs/{jobId}/download", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<?> downloadExportJob(HttpServletRequest request, @PathVariable String jobId) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "ANALYST")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }
        try {
            String csv = auditExportJobService.download(jobId, currentUser(request), hasAnyRole(request, "ADMIN", "MANAGER"));
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit-logs-" + jobId + ".csv\"")
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

    private Specification<AuditLog> buildAuditSpec(String username, String role, String action, LocalDateTime fromTime, LocalDateTime toTime) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<Predicate>();
            if (!isBlank(username)) predicates.add(cb.equal(root.get("username"), username));
            if (!isBlank(role)) predicates.add(cb.equal(root.get("role"), role));
            if (!isBlank(action)) predicates.add(cb.equal(root.get("action"), action));
            if (fromTime != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromTime));
            if (toTime != null) predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toTime));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}


