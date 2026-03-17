package com.yao.crm.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class ApiRequestMetricsService {

    private final ConcurrentLinkedQueue<RequestSample> samples = new ConcurrentLinkedQueue<RequestSample>();
    private final int maxSamples;
    private final int defaultWindowMinutes;
    private final long slowThresholdMs;

    public ApiRequestMetricsService(
            @Value("${ops.slo.max-samples:5000}") int maxSamples,
            @Value("${ops.slo.window-minutes:30}") int defaultWindowMinutes,
            @Value("${ops.slow-log-threshold-ms:500}") long slowThresholdMs) {
        this.maxSamples = Math.max(500, maxSamples);
        this.defaultWindowMinutes = Math.max(5, defaultWindowMinutes);
        this.slowThresholdMs = Math.max(50L, slowThresholdMs);
    }

    public void observe(String route, int status, long latencyMs, String errorCode) {
        RequestSample sample = new RequestSample();
        sample.route = route == null ? "" : route;
        sample.status = status;
        sample.latencyMs = Math.max(0L, latencyMs);
        sample.errorCode = errorCode == null ? "" : errorCode;
        sample.timestampMs = System.currentTimeMillis();
        samples.add(sample);
        trim(sample.timestampMs);
    }

    public Map<String, Object> snapshot() {
        return snapshot(defaultWindowMinutes);
    }

    public Map<String, Object> snapshot(int windowMinutes) {
        int safeWindowMinutes = Math.max(5, windowMinutes);
        long now = System.currentTimeMillis();
        long cutoff = now - safeWindowMinutes * 60L * 1000L;
        trim(now);

        long total = 0L;
        long error = 0L;
        long slow = 0L;
        RouteAccumulator dashboardAcc = new RouteAccumulator();
        RouteAccumulator customersAcc = new RouteAccumulator();
        RouteAccumulator reportsAcc = new RouteAccumulator();

        for (RequestSample sample : samples) {
            if (sample.timestampMs < cutoff) continue;
            total++;
            if (sample.status >= 500) {
                error++;
            }
            if (sample.latencyMs >= slowThresholdMs) {
                slow++;
            }
            if ("/api/dashboard".equals(sample.route)) {
                dashboardAcc.observe(sample, slowThresholdMs);
            } else if ("/api/customers/search".equals(sample.route)) {
                customersAcc.observe(sample, slowThresholdMs);
            } else if ("/api/v1/reports/overview".equals(sample.route)) {
                reportsAcc.observe(sample, slowThresholdMs);
            }
        }

        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("windowMinutes", safeWindowMinutes);
        out.put("windowSinceMs", cutoff);
        out.put("sampleCount", total);
        out.put("errorCount", error);
        out.put("errorRate", total == 0 ? 0.0d : ((double) error / (double) total));
        out.put("slowCount", slow);
        out.put("slowRate", total == 0 ? 0.0d : ((double) slow / (double) total));
        out.put("slowThresholdMs", slowThresholdMs);

        Map<String, Object> keyRoutes = new LinkedHashMap<String, Object>();
        keyRoutes.put("dashboard", routeSnapshot(dashboardAcc));
        keyRoutes.put("customers", routeSnapshot(customersAcc));
        keyRoutes.put("reports", routeSnapshot(reportsAcc));
        out.put("keyRoutes", keyRoutes);
        return out;
    }

    private Map<String, Object> routeSnapshot(RouteAccumulator acc) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("count", acc.count);
        out.put("p95Ms", percentile(acc.latencies, 95));
        out.put("p99Ms", percentile(acc.latencies, 99));
        out.put("errorRate5xx", acc.count == 0 ? 0.0d : ((double) acc.error / (double) acc.count));
        out.put("slowRate", acc.count == 0 ? 0.0d : ((double) acc.slow / (double) acc.count));
        return out;
    }

    private long percentile(List<Long> values, int p) {
        if (values == null || values.isEmpty()) return 0L;
        List<Long> sorted = new ArrayList<Long>(values);
        Collections.sort(sorted, new Comparator<Long>() {
            @Override
            public int compare(Long a, Long b) {
                return a.compareTo(b);
            }
        });
        int index = (int) Math.ceil((p / 100.0d) * sorted.size()) - 1;
        int safe = Math.max(0, Math.min(sorted.size() - 1, index));
        return sorted.get(safe);
    }

    private void trim(long nowMs) {
        long cutoff = nowMs - defaultWindowMinutes * 60L * 1000L;
        while (samples.size() > maxSamples) {
            samples.poll();
        }
        while (!samples.isEmpty()) {
            RequestSample head = samples.peek();
            if (head == null || head.timestampMs >= cutoff) {
                break;
            }
            samples.poll();
        }
    }

    private static class RequestSample {
        private String route;
        private int status;
        private long latencyMs;
        private String errorCode;
        private long timestampMs;
    }

    private static class RouteAccumulator {
        private long count;
        private long error;
        private long slow;
        private final List<Long> latencies = new ArrayList<Long>();

        private void observe(RequestSample sample, long slowThresholdMs) {
            if (sample == null) return;
            count++;
            latencies.add(sample.latencyMs);
            if (sample.status >= 500) {
                error++;
            }
            if (sample.latencyMs >= slowThresholdMs) {
                slow++;
            }
        }
    }
}
