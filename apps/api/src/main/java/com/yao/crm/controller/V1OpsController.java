package com.yao.crm.controller;

import com.yao.crm.entity.ApprovalTask;
import com.yao.crm.entity.LeadImportJob;
import com.yao.crm.repository.ApprovalTaskRepository;
import com.yao.crm.repository.LeadImportJobRepository;
import com.yao.crm.repository.NotificationJobRepository;
import com.yao.crm.repository.TenantRepository;
import com.yao.crm.service.ApiRequestMetricsService;
import com.yao.crm.service.AuditExportJobService;
import com.yao.crm.service.I18nService;
import com.yao.crm.service.NotificationJobScheduler;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/ops")
public class V1OpsController extends BaseApiController {
    private static final List<String> APPROVAL_PENDING_STATUSES = Arrays.asList("PENDING", "WAITING");
    private static final List<String> NOTIFICATION_ACTIVE_STATUSES = Arrays.asList("PENDING", "RUNNING", "SUCCESS", "FAILED", "RETRY");

    private final DataSource dataSource;
    private final NotificationJobScheduler notificationJobScheduler;
    private final ApprovalTaskRepository approvalTaskRepository;
    private final NotificationJobRepository notificationJobRepository;
    private final LeadImportJobRepository leadImportJobRepository;
    private final TenantRepository tenantRepository;
    private final HealthController healthController;
    private final AuditExportJobService auditExportJobService;
    private final ApiRequestMetricsService apiRequestMetricsService;
    private final String webhookProviders;
    private final double sloErrorRateMax;
    private final double sloSlowRateMax;
    private final long sloKeyRouteP95MaxMs;
    private final double auditFailedRatioMax;
    private final double auditRetryRatioMax;
    private final double errorBudgetDailyMax;
    private final double errorBudgetWeeklyMax;
    private final double alertP1ErrorRate;
    private final double alertP2ErrorRate;
    private final double alertP3ErrorRate;
    private final double alertP1SlowRate;
    private final double alertP2SlowRate;
    private final double alertP3SlowRate;
    private final String oncallPrimary;
    private final String oncallEscalation;

    public V1OpsController(DataSource dataSource,
                           NotificationJobScheduler notificationJobScheduler,
                           ApprovalTaskRepository approvalTaskRepository,
                           NotificationJobRepository notificationJobRepository,
                           LeadImportJobRepository leadImportJobRepository,
                           TenantRepository tenantRepository,
                           HealthController healthController,
                           AuditExportJobService auditExportJobService,
                           ApiRequestMetricsService apiRequestMetricsService,
                           @Value("${integration.webhooks.providers:}") String webhookProviders,
                           @Value("${ops.slo.error-rate-max:0.02}") double sloErrorRateMax,
                           @Value("${ops.slo.slow-rate-max:0.20}") double sloSlowRateMax,
                           @Value("${ops.slo.key-route-p95-ms:1500}") long sloKeyRouteP95MaxMs,
                           @Value("${ops.audit-export.failed-ratio-max:0.10}") double auditFailedRatioMax,
                           @Value("${ops.audit-export.retry-ratio-max:0.30}") double auditRetryRatioMax,
                           @Value("${ops.error-budget.daily-max:0.02}") double errorBudgetDailyMax,
                           @Value("${ops.error-budget.weekly-max:0.05}") double errorBudgetWeeklyMax,
                           @Value("${ops.alert.p1.error-rate:0.05}") double alertP1ErrorRate,
                           @Value("${ops.alert.p2.error-rate:0.03}") double alertP2ErrorRate,
                           @Value("${ops.alert.p3.error-rate:0.02}") double alertP3ErrorRate,
                           @Value("${ops.alert.p1.slow-rate:0.50}") double alertP1SlowRate,
                           @Value("${ops.alert.p2.slow-rate:0.35}") double alertP2SlowRate,
                           @Value("${ops.alert.p3.slow-rate:0.20}") double alertP3SlowRate,
                           @Value("${ops.oncall.primary:SRE Primary}") String oncallPrimary,
                           @Value("${ops.oncall.escalation:SRE Lead -> Eng Manager}") String oncallEscalation,
                           I18nService i18nService) {
        super(i18nService);
        this.dataSource = dataSource;
        this.notificationJobScheduler = notificationJobScheduler;
        this.approvalTaskRepository = approvalTaskRepository;
        this.notificationJobRepository = notificationJobRepository;
        this.leadImportJobRepository = leadImportJobRepository;
        this.tenantRepository = tenantRepository;
        this.healthController = healthController;
        this.auditExportJobService = auditExportJobService;
        this.apiRequestMetricsService = apiRequestMetricsService;
        this.webhookProviders = webhookProviders == null ? "" : webhookProviders;
        this.sloErrorRateMax = Math.max(0d, sloErrorRateMax);
        this.sloSlowRateMax = Math.max(0d, sloSlowRateMax);
        this.sloKeyRouteP95MaxMs = Math.max(100L, sloKeyRouteP95MaxMs);
        this.auditFailedRatioMax = Math.max(0d, auditFailedRatioMax);
        this.auditRetryRatioMax = Math.max(0d, auditRetryRatioMax);
        this.errorBudgetDailyMax = Math.max(0.001d, errorBudgetDailyMax);
        this.errorBudgetWeeklyMax = Math.max(this.errorBudgetDailyMax, errorBudgetWeeklyMax);
        this.alertP1ErrorRate = Math.max(0.001d, alertP1ErrorRate);
        this.alertP2ErrorRate = Math.max(0.001d, alertP2ErrorRate);
        this.alertP3ErrorRate = Math.max(0.001d, alertP3ErrorRate);
        this.alertP1SlowRate = Math.max(0.01d, alertP1SlowRate);
        this.alertP2SlowRate = Math.max(0.01d, alertP2SlowRate);
        this.alertP3SlowRate = Math.max(0.01d, alertP3SlowRate);
        this.oncallPrimary = oncallPrimary == null ? "" : oncallPrimary.trim();
        this.oncallEscalation = oncallEscalation == null ? "" : oncallEscalation.trim();
    }

    @GetMapping("/health")
    public ResponseEntity<?> health(HttpServletRequest request,
                                    @RequestParam(defaultValue = "") String timezone) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        OpsRequestContext context = resolveContext(request, timezone);
        if (context.hasError()) {
            return context.getErrorResponse();
        }
        String tenantId = context.getTenantId();
        ZoneId zoneId = context.getZoneId();
        List<String> providerList = normalizeWebhookProviders(webhookProviders);

        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("requestId", traceId(request));
        out.put("tenantId", tenantId);
        out.put("time", nowAt(zoneId));
        out.put("timezone", zoneId.getId());
        out.put("database", databaseHealth());
        out.put("notificationScheduler", schedulerHealth());
        out.put("webhookProviders", String.join(",", providerList));
        out.put("webhookProviderList", providerList);
        out.put("webhookConfigured", !providerList.isEmpty());
        out.put("status", "UP");
        return ResponseEntity.ok(successWithFields(request, "ops_health_loaded", out));
    }

    @GetMapping("/metrics/summary")
    public ResponseEntity<?> metricsSummary(HttpServletRequest request,
                                            @RequestParam(defaultValue = "") String timezone) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        OpsRequestContext context = resolveContext(request, timezone);
        if (context.hasError()) {
            return context.getErrorResponse();
        }
        String tenantId = context.getTenantId();
        ZoneId zoneId = context.getZoneId();

        LocalDateTime now = nowAt(zoneId);
        long approvalBacklog = approvalTaskRepository.countByTenantIdAndStatusIn(tenantId, APPROVAL_PENDING_STATUSES);
        long approvalOverdue = approvalTaskRepository.countByTenantIdAndStatusInAndDeadlineAtBefore(tenantId, APPROVAL_PENDING_STATUSES, now);
        Map<String, Long> groupedStatus = toLongCountMap(notificationJobRepository.countGroupedByStatus(tenantId));
        long notifySuccess = groupedStatus.containsKey("SUCCESS") ? groupedStatus.get("SUCCESS") : 0L;
        long notifyFailed = groupedStatus.containsKey("FAILED") ? groupedStatus.get("FAILED") : 0L;
        long notifyRetry = groupedStatus.containsKey("RETRY") ? groupedStatus.get("RETRY") : 0L;
        long notifyTotal = 0L;
        for (String status : NOTIFICATION_ACTIVE_STATUSES) {
            notifyTotal += groupedStatus.containsKey(status) ? groupedStatus.get(status) : 0L;
        }
        double successRate = notifyTotal == 0 ? 1.0 : ((double) notifySuccess / (double) notifyTotal);
        Map<String, Object> retryBuckets = buildRetryDistribution(tenantId);

        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("requestId", traceId(request));
        out.put("tenantId", tenantId);
        out.put("approvalBacklog", approvalBacklog);
        out.put("approvalOverdue", approvalOverdue);
        out.put("notificationTotal", notifyTotal);
        out.put("notificationSuccess", notifySuccess);
        out.put("notificationFailed", notifyFailed);
        out.put("notificationRetry", notifyRetry);
        out.put("notificationSuccessRate", successRate);
        out.put("notificationRetryDistribution", retryBuckets);
        out.put("importMetrics", buildImportMetrics(tenantId, zoneId));
        out.put("timezone", zoneId.getId());
        out.put("scheduler", schedulerHealth());
        return ResponseEntity.ok(successWithFields(request, "ops_metrics_loaded", out));
    }

    @GetMapping("/slo-snapshot")
    public ResponseEntity<?> sloSnapshot(HttpServletRequest request,
                                         @RequestParam(defaultValue = "") String timezone) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        OpsRequestContext context = resolveContext(request, timezone);
        if (context.hasError()) {
            return context.getErrorResponse();
        }
        String tenantId = context.getTenantId();
        ZoneId zoneId = context.getZoneId();
        LocalDateTime now = nowAt(zoneId);

        Map<String, Object> readiness = healthController.ready();
        Map<String, Object> dependencies = healthController.dependencies();
        Map<String, Object> api = apiRequestMetricsService.snapshot();
        Map<String, Object> audit = auditExportJobService.metricsSnapshot(tenantId);

        double errorRate = asDouble(api.get("errorRate"));
        double slowRate = asDouble(api.get("slowRate"));
        Map<String, Object> keyRoutes = asMap(api.get("keyRoutes"));
        long dashboardP95 = readRouteP95(keyRoutes, "dashboard");
        long dashboardP99 = readRouteP99(keyRoutes, "dashboard");
        long customersP95 = readRouteP95(keyRoutes, "customers");
        long customersP99 = readRouteP99(keyRoutes, "customers");
        long reportsP95 = readRouteP95(keyRoutes, "reports");
        long reportsP99 = readRouteP99(keyRoutes, "reports");

        long totalDone = asLong(audit.get("totalDone"));
        long totalFailed = asLong(audit.get("totalFailed"));
        long totalRetried = asLong(audit.get("totalRetried"));
        long completed = totalDone + totalFailed;
        double auditFailedRatio = completed <= 0 ? 0.0d : ((double) totalFailed / (double) completed);
        double auditRetryRatio = completed <= 0 ? 0.0d : ((double) totalRetried / (double) completed);

        List<String> legacyAlerts = new ArrayList<String>();
        boolean readinessOk = Boolean.TRUE.equals(readiness.get("ok"));
        if (!readinessOk) legacyAlerts.add("readiness_degraded");
        if (errorRate > sloErrorRateMax) legacyAlerts.add("api_error_rate_high");
        if (slowRate > sloSlowRateMax) legacyAlerts.add("api_slow_rate_high");
        if (dashboardP95 > sloKeyRouteP95MaxMs) legacyAlerts.add("dashboard_p95_high");
        if (customersP95 > sloKeyRouteP95MaxMs) legacyAlerts.add("customers_p95_high");
        if (reportsP95 > sloKeyRouteP95MaxMs) legacyAlerts.add("reports_p95_high");
        if (auditFailedRatio > auditFailedRatioMax) legacyAlerts.add("audit_export_failed_ratio_high");
        if (auditRetryRatio > auditRetryRatioMax) legacyAlerts.add("audit_export_retry_ratio_high");

        List<Map<String, Object>> leveledAlerts = buildLeveledAlerts(readinessOk, errorRate, slowRate, auditFailedRatio, now);
        String highestLevel = detectHighestAlertLevel(leveledAlerts);
        double errorBudgetConsumedDaily = errorRate;
        double errorBudgetConsumedWeekly = Math.min(1.0d, errorRate * 7.0d);
        Map<String, Object> errorBudget = buildErrorBudget(errorBudgetConsumedDaily, errorBudgetConsumedWeekly);
        Map<String, Object> oncall = buildOncall(now);

        Map<String, Object> thresholds = new LinkedHashMap<String, Object>();
        thresholds.put("errorRateMax", sloErrorRateMax);
        thresholds.put("slowRateMax", sloSlowRateMax);
        thresholds.put("keyRouteP95MaxMs", sloKeyRouteP95MaxMs);
        thresholds.put("auditFailedRatioMax", auditFailedRatioMax);
        thresholds.put("auditRetryRatioMax", auditRetryRatioMax);
        thresholds.put("alertP1ErrorRate", alertP1ErrorRate);
        thresholds.put("alertP2ErrorRate", alertP2ErrorRate);
        thresholds.put("alertP3ErrorRate", alertP3ErrorRate);

        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("errorRate", errorRate);
        summary.put("slowRate", slowRate);
        summary.put("requestCount", asLong(api.get("sampleCount")));
        summary.put("dashboardP95Ms", dashboardP95);
        summary.put("dashboardP99Ms", dashboardP99);
        summary.put("customersP95Ms", customersP95);
        summary.put("customersP99Ms", customersP99);
        summary.put("reportsP95Ms", reportsP95);
        summary.put("reportsP99Ms", reportsP99);
        summary.put("auditExportFailedRatio", auditFailedRatio);
        summary.put("auditExportRetryRatio", auditRetryRatio);

        Map<String, Object> performanceWindow = new LinkedHashMap<String, Object>();
        performanceWindow.put("requestCount", asLong(api.get("sampleCount")));
        performanceWindow.put("errorRate5xx", errorRate);
        performanceWindow.put("slowRate", slowRate);
        performanceWindow.put("keyRoutes", keyRoutes);
        performanceWindow.put("windowMinutes", asLong(api.get("windowMinutes")));

        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("requestId", traceId(request));
        out.put("tenantId", tenantId);
        out.put("generatedAt", now);
        out.put("timezone", zoneId.getId());
        out.put("overallStatus", legacyAlerts.isEmpty() ? "PASS" : "FAIL");
        out.put("alerts", legacyAlerts);
        out.put("alertsLevel", highestLevel);
        out.put("alertsDetailed", leveledAlerts);
        out.put("errorBudget", errorBudget);
        out.put("oncall", oncall);
        out.put("thresholds", thresholds);
        out.put("summary", summary);
        out.put("performanceWindow", performanceWindow);
        out.put("readiness", readiness);
        out.put("dependencies", dependencies);
        out.put("api", api);
        out.put("auditExport", audit);
        return ResponseEntity.ok(successWithFields(request, "ops_slo_snapshot_loaded", out));
    }

    private Map<String, Object> buildImportMetrics(String tenantId, ZoneId zoneId) {
        LocalDateTime now = nowAt(zoneId);
        ZonedDateTime zoneNow = ZonedDateTime.now(zoneId);
        LocalDateTime from = zoneNow.minusHours(24).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        long total = 0L;
        long running = 0L;
        long success = 0L;
        long partial = 0L;
        long failed = 0L;
        long canceled = 0L;
        long processedRows = 0L;
        long failedRows = 0L;
        List<Long> durations = new ArrayList<Long>();
        final int batchSize = 500;
        for (int page = 0; ; page++) {
            List<LeadImportJob> jobs = leadImportJobRepository.findByTenantIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                    tenantId,
                    from,
                    PageRequest.of(page, batchSize)
            );
            if (jobs.isEmpty()) {
                break;
            }
            for (LeadImportJob row : jobs) {
                total++;
                String status = row.getStatus() == null ? "" : row.getStatus().trim().toUpperCase(Locale.ROOT);
                if ("PENDING".equals(status) || "RUNNING".equals(status)) running++;
                if ("SUCCESS".equals(status)) success++;
                if ("PARTIAL_SUCCESS".equals(status)) partial++;
                if ("FAILED".equals(status)) failed++;
                if ("CANCELED".equals(status)) canceled++;
                processedRows += row.getProcessedRows() == null ? 0L : row.getProcessedRows();
                failedRows += row.getFailCount() == null ? 0L : row.getFailCount();

                if (row.getCreatedAt() == null) continue;
                if (!isTerminal(status)) continue;
                LocalDateTime end = row.getUpdatedAt() == null ? now : row.getUpdatedAt();
                long ms = Math.max(0L, Duration.between(row.getCreatedAt(), end).toMillis());
                durations.add(ms);
            }
            if (jobs.size() < batchSize) {
                break;
            }
        }

        long completed = success + partial + failed + canceled;
        double successRate = completed == 0 ? 1.0 : ((double) (success + partial) / (double) completed);
        double failureRate = completed == 0 ? 0.0 : ((double) failed / (double) completed);
        Collections.sort(durations);
        long avgDurationMs = durations.isEmpty() ? 0L : (long) durations.stream().mapToLong(Long::longValue).average().orElse(0.0);
        long p95DurationMs = durations.isEmpty() ? 0L : durations.get(Math.max(0, (int) Math.ceil(durations.size() * 0.95) - 1));

        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("windowHours", 24);
        out.put("windowFrom", from);
        out.put("windowTo", now);
        out.put("timezone", zoneId.getId());
        out.put("importJobTotal", total);
        out.put("importRunning", running);
        out.put("importSuccess", success);
        out.put("importPartialSuccess", partial);
        out.put("importFailed", failed);
        out.put("importCanceled", canceled);
        out.put("importSuccessRate", successRate);
        out.put("importFailureRate", failureRate);
        out.put("importAvgDurationMs", avgDurationMs);
        out.put("importP95DurationMs", p95DurationMs);
        out.put("importProcessedRows", processedRows);
        out.put("importFailedRows", failedRows);
        return out;
    }

    private boolean isTerminal(String status) {
        return "SUCCESS".equalsIgnoreCase(status)
                || "PARTIAL_SUCCESS".equalsIgnoreCase(status)
                || "FAILED".equalsIgnoreCase(status)
                || "CANCELED".equalsIgnoreCase(status);
    }

    private Map<String, Object> buildRetryDistribution(String tenantId) {
        Map<String, Long> groupedBuckets = toLongCountMap(notificationJobRepository.countRetryBuckets(tenantId));
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("retry0", groupedBuckets.containsKey("RETRY0") ? groupedBuckets.get("RETRY0") : 0L);
        out.put("retry1to2", groupedBuckets.containsKey("RETRY1TO2") ? groupedBuckets.get("RETRY1TO2") : 0L);
        out.put("retry3plus", groupedBuckets.containsKey("RETRY3PLUS") ? groupedBuckets.get("RETRY3PLUS") : 0L);
        return out;
    }

    private Map<String, Long> toLongCountMap(List<Object[]> rows) {
        Map<String, Long> out = new LinkedHashMap<String, Long>();
        if (rows == null) return out;
        for (Object[] row : rows) {
            if (row == null || row.length < 2) continue;
            String key = row[0] == null ? "UNKNOWN" : String.valueOf(row[0]).trim().toUpperCase(Locale.ROOT);
            Number count = row[1] instanceof Number ? (Number) row[1] : 0;
            out.put(key, count.longValue());
        }
        return out;
    }

    private Map<String, Object> databaseHealth() {
        try (Connection ignored = dataSource.getConnection()) {
            return Collections.<String, Object>singletonMap("status", "UP");
        } catch (Exception ex) {
            Map<String, Object> out = new LinkedHashMap<String, Object>();
            out.put("status", "DOWN");
            out.put("message", ex.getMessage());
            return out;
        }
    }

    private Map<String, Object> schedulerHealth() {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("lastRunAt", notificationJobScheduler.getLastRunAt());
        out.put("lastProcessed", notificationJobScheduler.getLastProcessed());
        out.put("lastError", notificationJobScheduler.getLastError());
        out.put("status", notificationJobScheduler.getLastError() == null ? "UP" : "DEGRADED");
        return out;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return Collections.emptyMap();
    }

    private double asDouble(Object value) {
        if (value == null) return 0.0d;
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ex) {
            return 0.0d;
        }
    }

    private long asLong(Object value) {
        if (value == null) return 0L;
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ex) {
            return 0L;
        }
    }

    private long readRouteP95(Map<String, Object> keyRoutes, String route) {
        Map<String, Object> routeValue = asMap(keyRoutes.get(route));
        return asLong(routeValue.get("p95Ms"));
    }

    private long readRouteP99(Map<String, Object> keyRoutes, String route) {
        Map<String, Object> routeValue = asMap(keyRoutes.get(route));
        return asLong(routeValue.get("p99Ms"));
    }

    private List<Map<String, Object>> buildLeveledAlerts(boolean readinessOk, double errorRate, double slowRate, double auditFailedRatio, LocalDateTime now) {
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        if (!readinessOk) {
            out.add(alert("P1", "readiness_degraded", "readiness endpoint not healthy", now));
        }
        if (errorRate >= alertP1ErrorRate || slowRate >= alertP1SlowRate) {
            out.add(alert("P1", "api_critical", "error/slow rate reached p1 threshold", now));
        } else if (errorRate >= alertP2ErrorRate || slowRate >= alertP2SlowRate) {
            out.add(alert("P2", "api_degraded", "error/slow rate reached p2 threshold", now));
        } else if (errorRate >= alertP3ErrorRate || slowRate >= alertP3SlowRate) {
            out.add(alert("P3", "api_warning", "error/slow rate reached p3 threshold", now));
        }
        if (auditFailedRatio > auditFailedRatioMax) {
            out.add(alert("P2", "audit_export_failed_ratio_high", "audit export failure ratio is high", now));
        }
        return out;
    }

    private String detectHighestAlertLevel(List<Map<String, Object>> alerts) {
        if (alerts == null || alerts.isEmpty()) return "NONE";
        for (Map<String, Object> alert : alerts) {
            if ("P1".equals(String.valueOf(alert.get("level")))) return "P1";
        }
        for (Map<String, Object> alert : alerts) {
            if ("P2".equals(String.valueOf(alert.get("level")))) return "P2";
        }
        return "P3";
    }

    private Map<String, Object> buildErrorBudget(double consumedDaily, double consumedWeekly) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("daily", budgetWindow(errorBudgetDailyMax, consumedDaily));
        out.put("weekly", budgetWindow(errorBudgetWeeklyMax, consumedWeekly));
        return out;
    }

    private Map<String, Object> budgetWindow(double budgetMax, double consumed) {
        double safeConsumed = Math.max(0.0d, consumed);
        double remaining = Math.max(0.0d, budgetMax - safeConsumed);
        double burnRate = budgetMax <= 0 ? 0.0d : (safeConsumed / budgetMax);
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("budget", budgetMax);
        out.put("consumed", safeConsumed);
        out.put("remaining", remaining);
        out.put("burnRate", burnRate);
        out.put("pass", safeConsumed <= budgetMax);
        return out;
    }

    private Map<String, Object> buildOncall(LocalDateTime now) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("primary", oncallPrimary.isEmpty() ? "UNASSIGNED" : oncallPrimary);
        out.put("escalation", oncallEscalation.isEmpty() ? "UNDEFINED" : oncallEscalation);
        out.put("lastRotationAt", now.minusDays(1));
        return out;
    }

    private Map<String, Object> alert(String level, String reason, String message, LocalDateTime now) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("level", level);
        out.put("reason", reason);
        out.put("message", message);
        out.put("triggeredAt", now);
        return out;
    }

    private OpsRequestContext resolveContext(HttpServletRequest request, String timezoneParam) {
        String authTenantId = normalize(readAttr(request, "authTenantId"));
        String headerTenantId = normalize(request.getHeader("X-Tenant-Id"));
        if (!isBlank(authTenantId) && !isBlank(headerTenantId) && !authTenantId.equals(headerTenantId)) {
            Map<String, Object> details = new LinkedHashMap<String, Object>();
            details.put("authTenantId", authTenantId);
            details.put("headerTenantId", headerTenantId);
            return OpsRequestContext.error(ResponseEntity.status(409).body(errorBody(request, "tenant_conflict", msg(request, "tenant_conflict"), details)));
        }

        String tenantId = normalize(currentTenant(request));
        if (isBlank(tenantId)) {
            Map<String, Object> details = new LinkedHashMap<String, Object>();
            details.put("reason", "tenant_required");
            return OpsRequestContext.error(ResponseEntity.badRequest().body(errorBody(request, "tenant_required", msg(request, "tenant_required"), details)));
        }

        Optional<com.yao.crm.entity.Tenant> tenantOptional = tenantRepository.findById(tenantId);
        if (!tenantOptional.isPresent()) {
            return OpsRequestContext.error(ResponseEntity.status(404).body(errorBody(request, "tenant_not_found", msg(request, "tenant_not_found"), null)));
        }

        String requestedTimezone = normalize(timezoneParam);
        if (!isBlank(requestedTimezone)) {
            try {
                return OpsRequestContext.success(tenantId, ZoneId.of(requestedTimezone));
            } catch (Exception ex) {
                Map<String, Object> details = new LinkedHashMap<String, Object>();
                details.put("reason", "invalid_timezone");
                details.put("timezone", requestedTimezone);
                return OpsRequestContext.error(ResponseEntity.badRequest().body(errorBody(request, "invalid_timezone", msg(request, "invalid_timezone"), details)));
            }
        }

        ZoneId zoneId = parseZoneOrDefault(tenantOptional.get().getTimezone());
        return OpsRequestContext.success(tenantId, zoneId);
    }

    private ZoneId parseZoneOrDefault(String timezone) {
        String normalized = normalize(timezone);
        if (isBlank(normalized)) {
            return ZoneId.systemDefault();
        }
        try {
            return ZoneId.of(normalized);
        } catch (Exception ex) {
            return ZoneId.systemDefault();
        }
    }

    private LocalDateTime nowAt(ZoneId zoneId) {
        return ZonedDateTime.now(zoneId).toLocalDateTime();
    }

    private List<String> normalizeWebhookProviders(String configuredProviders) {
        if (isBlank(configuredProviders)) {
            return Collections.emptyList();
        }
        String[] segments = configuredProviders.split("[,;\\s]+");
        LinkedHashSet<String> normalized = new LinkedHashSet<String>();
        for (String segment : segments) {
            String token = normalize(segment).toUpperCase(Locale.ROOT);
            if (!isBlank(token)) {
                normalized.add(token);
            }
        }
        return new ArrayList<String>(normalized);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String readAttr(HttpServletRequest request, String key) {
        Object value = request.getAttribute(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static class OpsRequestContext {
        private final String tenantId;
        private final ZoneId zoneId;
        private final ResponseEntity<?> errorResponse;

        private OpsRequestContext(String tenantId, ZoneId zoneId, ResponseEntity<?> errorResponse) {
            this.tenantId = tenantId;
            this.zoneId = zoneId;
            this.errorResponse = errorResponse;
        }

        static OpsRequestContext success(String tenantId, ZoneId zoneId) {
            return new OpsRequestContext(tenantId, zoneId, null);
        }

        static OpsRequestContext error(ResponseEntity<?> response) {
            return new OpsRequestContext("", ZoneId.systemDefault(), response);
        }

        boolean hasError() {
            return errorResponse != null;
        }

        String getTenantId() {
            return tenantId;
        }

        ZoneId getZoneId() {
            return zoneId;
        }

        ResponseEntity<?> getErrorResponse() {
            return errorResponse;
        }
    }
}
