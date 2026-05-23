package com.yao.crm.service;

import com.yao.crm.entity.SubscriptionPlan;
import com.yao.crm.entity.TenantFeatureFlag;
import com.yao.crm.entity.TenantSubscription;
import com.yao.crm.entity.TenantUsageDaily;
import com.yao.crm.repository.CustomerRepository;
import com.yao.crm.repository.SubscriptionPlanRepository;
import com.yao.crm.repository.TenantFeatureFlagRepository;
import com.yao.crm.repository.TenantRepository;
import com.yao.crm.repository.TenantSubscriptionRepository;
import com.yao.crm.repository.TenantUsageDailyRepository;
import com.yao.crm.repository.UserAccountRepository;
import com.yao.crm.util.IdGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class BillingService {

    private static final String DEFAULT_PLAN = "FREE";
    private static final Set<String> SUBSCRIPTION_STATUSES = new LinkedHashSet<String>(Arrays.asList(
            "TRIAL", "ACTIVE", "PAST_DUE", "SUSPENDED", "CANCELLED"
    ));
    private static final Set<String> FLAG_SOURCES = new LinkedHashSet<String>(Arrays.asList("PLAN", "MANUAL", "SYSTEM"));

    private final SubscriptionPlanRepository planRepository;
    private final TenantSubscriptionRepository subscriptionRepository;
    private final TenantUsageDailyRepository usageRepository;
    private final TenantFeatureFlagRepository featureFlagRepository;
    private final TenantRepository tenantRepository;
    private final UserAccountRepository userAccountRepository;
    private final CustomerRepository customerRepository;
    private final IdGenerator idGenerator;

    public BillingService(SubscriptionPlanRepository planRepository,
                          TenantSubscriptionRepository subscriptionRepository,
                          TenantUsageDailyRepository usageRepository,
                          TenantFeatureFlagRepository featureFlagRepository,
                          TenantRepository tenantRepository,
                          UserAccountRepository userAccountRepository,
                          CustomerRepository customerRepository,
                          IdGenerator idGenerator) {
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.usageRepository = usageRepository;
        this.featureFlagRepository = featureFlagRepository;
        this.tenantRepository = tenantRepository;
        this.userAccountRepository = userAccountRepository;
        this.customerRepository = customerRepository;
        this.idGenerator = idGenerator;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> plans() {
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (SubscriptionPlan plan : planRepository.findAllByEnabledTrueOrderByPriceCentsMonthlyAscCodeAsc()) {
            out.add(toPlanView(plan));
        }
        return out;
    }

    @Transactional(timeout = 30)
    public Map<String, Object> subscription(String tenantId) {
        requireTenant(tenantId);
        TenantSubscription subscription = ensureSubscription(tenantId);
        SubscriptionPlan plan = requirePlan(subscription.getPlanCode());
        Map<String, Object> out = toSubscriptionView(subscription);
        out.put("plan", toPlanView(plan));
        return out;
    }

    @Transactional(timeout = 30)
    public Map<String, Object> updateSubscription(String tenantId,
                                                  String planCode,
                                                  String status,
                                                  LocalDateTime expiresAt,
                                                  LocalDateTime trialEndsAt) {
        requireTenant(tenantId);
        SubscriptionPlan plan = requirePlan(normalizePlanCode(planCode));
        TenantSubscription subscription = ensureSubscription(tenantId);
        subscription.setPlanCode(plan.getCode());
        subscription.setStatus(normalizeStatus(status));
        subscription.setExpiresAt(expiresAt);
        subscription.setTrialEndsAt(trialEndsAt);
        subscription = subscriptionRepository.save(subscription);
        syncPlanFlags(tenantId, plan, "PLAN");

        Map<String, Object> out = toSubscriptionView(subscription);
        out.put("plan", toPlanView(plan));
        out.put("features", featureMap(tenantId));
        return out;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> usage(String tenantId, LocalDate startDate, LocalDate endDate) {
        requireTenant(tenantId);
        LocalDate safeEnd = endDate == null ? LocalDate.now() : endDate;
        LocalDate safeStart = startDate == null ? safeEnd.minusDays(30) : startDate;
        if (safeStart.isAfter(safeEnd)) {
            throw new IllegalArgumentException("invalid_date_range");
        }
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (TenantUsageDaily row : usageRepository.findAllByTenantIdAndUsageDateBetweenOrderByUsageDateAscMetricKeyAsc(tenantId, safeStart, safeEnd)) {
            out.add(toUsageView(row));
        }
        return out;
    }

    @Transactional(timeout = 30)
    public List<Map<String, Object>> recalculateUsage(String tenantId, LocalDate usageDate) {
        requireTenant(tenantId);
        LocalDate date = usageDate == null ? LocalDate.now() : usageDate;
        upsertUsage(tenantId, date, "users.active", userAccountRepository.findAllByTenantId(tenantId).size());
        upsertUsage(tenantId, date, "customers.total", customerRepository.countByTenantId(tenantId));
        return usage(tenantId, date, date);
    }

    @Transactional(timeout = 30)
    public Map<String, Boolean> features(String tenantId) {
        requireTenant(tenantId);
        TenantSubscription subscription = ensureSubscription(tenantId);
        SubscriptionPlan plan = requirePlan(subscription.getPlanCode());
        syncPlanFlags(tenantId, plan, "PLAN");
        return featureMap(tenantId);
    }

    @Transactional(timeout = 30)
    public Map<String, Object> updateFeature(String tenantId, String flagKey, boolean enabled, String updatedBy) {
        requireTenant(tenantId);
        String key = normalizeFlagKey(flagKey);
        TenantFeatureFlag flag = featureFlagRepository.findByTenantIdAndFlagKey(tenantId, key).orElseGet(() -> {
            TenantFeatureFlag created = new TenantFeatureFlag();
            created.setId(idGenerator.generate("ff"));
            created.setTenantId(tenantId);
            created.setFlagKey(key);
            return created;
        });
        flag.setEnabled(enabled);
        flag.setSource("MANUAL");
        flag.setUpdatedBy(normalizeActor(updatedBy));
        return toFeatureView(featureFlagRepository.save(flag));
    }

    private TenantSubscription ensureSubscription(String tenantId) {
        Optional<TenantSubscription> existing = subscriptionRepository.findByTenantId(tenantId);
        if (existing.isPresent()) {
            return existing.get();
        }
        requirePlan(DEFAULT_PLAN);
        TenantSubscription subscription = new TenantSubscription();
        subscription.setId(idGenerator.generate("sub"));
        subscription.setTenantId(tenantId);
        subscription.setPlanCode(DEFAULT_PLAN);
        subscription.setStatus("ACTIVE");
        subscription.setStartedAt(LocalDateTime.now());
        return subscriptionRepository.save(subscription);
    }

    private Map<String, Boolean> featureMap(String tenantId) {
        Map<String, Boolean> out = new LinkedHashMap<String, Boolean>();
        for (TenantFeatureFlag flag : featureFlagRepository.findAllByTenantIdOrderByFlagKeyAsc(tenantId)) {
            out.put(flag.getFlagKey(), Boolean.TRUE.equals(flag.getEnabled()));
        }
        return out;
    }

    private void syncPlanFlags(String tenantId, SubscriptionPlan plan, String source) {
        Set<String> planFeatures = parseFeatures(plan.getFeaturesJson());
        Set<String> seenFeatures = new LinkedHashSet<String>();
        List<TenantFeatureFlag> existingFlags = featureFlagRepository.findAllByTenantIdOrderByFlagKeyAsc(tenantId);
        for (String feature : planFeatures) {
            seenFeatures.add(feature);
            TenantFeatureFlag flag = featureFlagRepository.findByTenantIdAndFlagKey(tenantId, feature).orElseGet(() -> {
                TenantFeatureFlag created = new TenantFeatureFlag();
                created.setId(idGenerator.generate("ff"));
                created.setTenantId(tenantId);
                created.setFlagKey(feature);
                return created;
            });
            if (!"MANUAL".equalsIgnoreCase(flag.getSource())) {
                String normalizedSource = normalizeFlagSource(source);
                if (!Boolean.TRUE.equals(flag.getEnabled()) || !normalizedSource.equalsIgnoreCase(flag.getSource())) {
                    flag.setEnabled(true);
                    flag.setSource(normalizedSource);
                    featureFlagRepository.save(flag);
                }
            }
        }
        for (TenantFeatureFlag flag : existingFlags) {
            if ("MANUAL".equalsIgnoreCase(flag.getSource())) {
                continue;
            }
            String key = normalizeFlagKey(flag.getFlagKey());
            boolean enabled = seenFeatures.contains(key);
            if (!Boolean.valueOf(enabled).equals(flag.getEnabled())) {
                flag.setEnabled(enabled);
                flag.setSource(normalizeFlagSource(source));
                featureFlagRepository.save(flag);
            }
        }
    }

    private void upsertUsage(String tenantId, LocalDate date, String metricKey, long value) {
        TenantUsageDaily row = usageRepository.findByTenantIdAndUsageDateAndMetricKey(tenantId, date, metricKey).orElseGet(() -> {
            TenantUsageDaily created = new TenantUsageDaily();
            created.setId(idGenerator.generate("use"));
            created.setTenantId(tenantId);
            created.setUsageDate(date);
            created.setMetricKey(metricKey);
            return created;
        });
        row.setMetricValue(Math.max(0L, value));
        usageRepository.save(row);
    }

    private void requireTenant(String tenantId) {
        String normalized = normalizeTenant(tenantId);
        if (normalized == null || !tenantRepository.existsById(normalized)) {
            throw new IllegalArgumentException("tenant_not_found");
        }
    }

    private SubscriptionPlan requirePlan(String planCode) {
        return planRepository.findByCode(normalizePlanCode(planCode))
                .filter(p -> Boolean.TRUE.equals(p.getEnabled()))
                .orElseThrow(() -> new IllegalArgumentException("billing_plan_not_found"));
    }

    private Map<String, Object> toPlanView(SubscriptionPlan plan) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("code", plan.getCode());
        out.put("name", plan.getName());
        out.put("priceCentsMonthly", plan.getPriceCentsMonthly());
        out.put("currency", plan.getCurrency());
        out.put("maxUsers", plan.getMaxUsers());
        out.put("maxCustomers", plan.getMaxCustomers());
        out.put("maxStorageMb", plan.getMaxStorageMb());
        out.put("features", new ArrayList<String>(parseFeatures(plan.getFeaturesJson())));
        out.put("enabled", plan.getEnabled());
        return out;
    }

    private Map<String, Object> toSubscriptionView(TenantSubscription subscription) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("id", subscription.getId());
        out.put("tenantId", subscription.getTenantId());
        out.put("planCode", subscription.getPlanCode());
        out.put("status", subscription.getStatus());
        out.put("startedAt", subscription.getStartedAt());
        out.put("expiresAt", subscription.getExpiresAt());
        out.put("trialEndsAt", subscription.getTrialEndsAt());
        out.put("createdAt", subscription.getCreatedAt());
        out.put("updatedAt", subscription.getUpdatedAt());
        return out;
    }

    private Map<String, Object> toUsageView(TenantUsageDaily row) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("tenantId", row.getTenantId());
        out.put("usageDate", row.getUsageDate());
        out.put("metricKey", row.getMetricKey());
        out.put("metricValue", row.getMetricValue());
        out.put("updatedAt", row.getUpdatedAt());
        return out;
    }

    private Map<String, Object> toFeatureView(TenantFeatureFlag row) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("tenantId", row.getTenantId());
        out.put("flagKey", row.getFlagKey());
        out.put("enabled", row.getEnabled());
        out.put("source", row.getSource());
        out.put("updatedBy", row.getUpdatedBy());
        out.put("updatedAt", row.getUpdatedAt());
        return out;
    }

    private Set<String> parseFeatures(String featuresJson) {
        Set<String> out = new LinkedHashSet<String>();
        String raw = featuresJson == null ? "" : featuresJson.trim();
        if (raw.length() < 2) return out;
        String body = raw;
        if (body.startsWith("[")) body = body.substring(1);
        if (body.endsWith("]")) body = body.substring(0, body.length() - 1);
        for (String item : body.split(",")) {
            String normalized = item.trim().replace("\"", "");
            if (!normalized.isEmpty()) out.add(normalized);
        }
        return out;
    }

    private String normalizeTenant(String tenantId) {
        if (tenantId == null) return null;
        String normalized = tenantId.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizePlanCode(String planCode) {
        if (planCode == null || planCode.trim().isEmpty()) {
            throw new IllegalArgumentException("billing_plan_required");
        }
        return planCode.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeStatus(String status) {
        String normalized = status == null || status.trim().isEmpty()
                ? "ACTIVE"
                : status.trim().toUpperCase(Locale.ROOT);
        if (!SUBSCRIPTION_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("billing_status_invalid");
        }
        return normalized;
    }

    private String normalizeFlagKey(String flagKey) {
        if (flagKey == null || flagKey.trim().isEmpty()) {
            throw new IllegalArgumentException("feature_flag_required");
        }
        String normalized = flagKey.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9][a-z0-9._-]{1,78}[a-z0-9]")) {
            throw new IllegalArgumentException("feature_flag_invalid");
        }
        return normalized;
    }

    private String normalizeFlagSource(String source) {
        String normalized = source == null || source.trim().isEmpty()
                ? "PLAN"
                : source.trim().toUpperCase(Locale.ROOT);
        return FLAG_SOURCES.contains(normalized) ? normalized : "PLAN";
    }

    private String normalizeActor(String actor) {
        if (actor == null || actor.trim().isEmpty()) return "unknown";
        return actor.trim();
    }
}
