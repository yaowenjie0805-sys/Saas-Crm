package com.yao.crm.service;

import com.yao.crm.entity.SubscriptionPlan;
import com.yao.crm.entity.TenantFeatureFlag;
import com.yao.crm.entity.TenantSubscription;
import com.yao.crm.repository.CustomerRepository;
import com.yao.crm.repository.SubscriptionPlanRepository;
import com.yao.crm.repository.TenantFeatureFlagRepository;
import com.yao.crm.repository.TenantRepository;
import com.yao.crm.repository.TenantSubscriptionRepository;
import com.yao.crm.repository.TenantUsageDailyRepository;
import com.yao.crm.repository.UserAccountRepository;
import com.yao.crm.util.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BillingServiceTest {

    private SubscriptionPlanRepository planRepository;
    private TenantSubscriptionRepository subscriptionRepository;
    private TenantFeatureFlagRepository featureFlagRepository;
    private BillingService service;

    @BeforeEach
    void setUp() {
        planRepository = mock(SubscriptionPlanRepository.class);
        subscriptionRepository = mock(TenantSubscriptionRepository.class);
        TenantUsageDailyRepository usageRepository = mock(TenantUsageDailyRepository.class);
        featureFlagRepository = mock(TenantFeatureFlagRepository.class);
        TenantRepository tenantRepository = mock(TenantRepository.class);
        UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        IdGenerator idGenerator = mock(IdGenerator.class);

        when(tenantRepository.existsById("tenant-a")).thenReturn(true);
        when(idGenerator.generate(anyString())).thenAnswer(invocation -> invocation.getArgument(0) + "_1");
        when(planRepository.findByCode("FREE")).thenReturn(Optional.of(plan("FREE", "[\"crm.core\"]")));
        when(planRepository.findByCode("BUSINESS")).thenReturn(Optional.of(plan("BUSINESS", "[\"crm.core\",\"approval\"]")));
        when(subscriptionRepository.save(any(TenantSubscription.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(featureFlagRepository.save(any(TenantFeatureFlag.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(featureFlagRepository.findAllByTenantIdOrderByFlagKeyAsc("tenant-a")).thenReturn(new ArrayList<TenantFeatureFlag>());

        service = new BillingService(
                planRepository,
                subscriptionRepository,
                usageRepository,
                featureFlagRepository,
                tenantRepository,
                userAccountRepository,
                customerRepository,
                idGenerator
        );
    }

    @Test
    void subscriptionShouldCreateFreeDefaultWhenMissing() {
        when(subscriptionRepository.findByTenantId("tenant-a")).thenReturn(Optional.empty());

        Map<String, Object> result = service.subscription("tenant-a");

        assertEquals("FREE", result.get("planCode"));
        verify(subscriptionRepository).save(any(TenantSubscription.class));
    }

    @Test
    void updateSubscriptionShouldSyncPlanFeatureFlags() {
        TenantSubscription existing = new TenantSubscription();
        existing.setId("sub_1");
        existing.setTenantId("tenant-a");
        existing.setPlanCode("FREE");
        existing.setStatus("ACTIVE");
        when(subscriptionRepository.findByTenantId("tenant-a")).thenReturn(Optional.of(existing));
        when(featureFlagRepository.findByTenantIdAndFlagKey("tenant-a", "crm.core")).thenReturn(Optional.empty());
        when(featureFlagRepository.findByTenantIdAndFlagKey("tenant-a", "approval")).thenReturn(Optional.empty());

        Map<String, Object> result = service.updateSubscription("tenant-a", "BUSINESS", "ACTIVE", null, null);

        assertEquals("BUSINESS", result.get("planCode"));
        verify(featureFlagRepository, times(2)).save(any(TenantFeatureFlag.class));
    }

    @Test
    void featuresShouldPreserveManualOverrides() {
        TenantSubscription existing = new TenantSubscription();
        existing.setTenantId("tenant-a");
        existing.setPlanCode("BUSINESS");
        existing.setStatus("ACTIVE");
        TenantFeatureFlag manual = new TenantFeatureFlag();
        manual.setTenantId("tenant-a");
        manual.setFlagKey("approval");
        manual.setEnabled(false);
        manual.setSource("MANUAL");

        when(subscriptionRepository.findByTenantId("tenant-a")).thenReturn(Optional.of(existing));
        when(featureFlagRepository.findByTenantIdAndFlagKey("tenant-a", "crm.core")).thenReturn(Optional.empty());
        when(featureFlagRepository.findByTenantIdAndFlagKey("tenant-a", "approval")).thenReturn(Optional.of(manual));
        when(featureFlagRepository.findAllByTenantIdOrderByFlagKeyAsc("tenant-a")).thenReturn(List.of(manual));

        Map<String, Boolean> result = service.features("tenant-a");

        assertTrue(result.containsKey("approval"));
        assertEquals(false, result.get("approval"));
    }

    @Test
    void updateSubscriptionShouldDisablePlanFlagsThatAreNoLongerIncluded() {
        TenantSubscription existing = new TenantSubscription();
        existing.setId("sub_1");
        existing.setTenantId("tenant-a");
        existing.setPlanCode("BUSINESS");
        existing.setStatus("ACTIVE");

        TenantFeatureFlag approval = feature("approval", true, "PLAN");
        TenantFeatureFlag manualSso = feature("sso", true, "MANUAL");

        when(subscriptionRepository.findByTenantId("tenant-a")).thenReturn(Optional.of(existing));
        when(featureFlagRepository.findByTenantIdAndFlagKey("tenant-a", "crm.core")).thenReturn(Optional.empty());
        when(featureFlagRepository.findAllByTenantIdOrderByFlagKeyAsc("tenant-a")).thenReturn(List.of(approval, manualSso));

        service.updateSubscription("tenant-a", "FREE", "ACTIVE", null, null);

        assertEquals(false, approval.getEnabled());
        assertEquals(true, manualSso.getEnabled());
        verify(featureFlagRepository, times(2)).save(any(TenantFeatureFlag.class));
    }

    @Test
    void updateSubscriptionShouldNotRewriteUnchangedPlanFlags() {
        TenantSubscription existing = new TenantSubscription();
        existing.setId("sub_1");
        existing.setTenantId("tenant-a");
        existing.setPlanCode("FREE");
        existing.setStatus("ACTIVE");

        TenantFeatureFlag core = feature("crm.core", true, "PLAN");

        when(subscriptionRepository.findByTenantId("tenant-a")).thenReturn(Optional.of(existing));
        when(featureFlagRepository.findByTenantIdAndFlagKey("tenant-a", "crm.core")).thenReturn(Optional.of(core));
        when(featureFlagRepository.findAllByTenantIdOrderByFlagKeyAsc("tenant-a")).thenReturn(List.of(core));

        service.updateSubscription("tenant-a", "FREE", "ACTIVE", null, null);

        verify(featureFlagRepository, never()).save(core);
    }

    private SubscriptionPlan plan(String code, String featuresJson) {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setId("plan_" + code.toLowerCase());
        plan.setCode(code);
        plan.setName(code);
        plan.setPriceCentsMonthly(0L);
        plan.setCurrency("CNY");
        plan.setMaxUsers(10);
        plan.setMaxCustomers(1000);
        plan.setMaxStorageMb(1024);
        plan.setFeaturesJson(featuresJson);
        plan.setEnabled(true);
        return plan;
    }

    private TenantFeatureFlag feature(String flagKey, boolean enabled, String source) {
        TenantFeatureFlag flag = new TenantFeatureFlag();
        flag.setId("ff_" + flagKey);
        flag.setTenantId("tenant-a");
        flag.setFlagKey(flagKey);
        flag.setEnabled(enabled);
        flag.setSource(source);
        return flag;
    }
}
