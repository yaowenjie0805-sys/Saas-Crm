package com.yao.crm.controller;

import com.yao.crm.entity.Tenant;
import com.yao.crm.repository.OpportunityRepository;
import com.yao.crm.repository.OrderRecordRepository;
import com.yao.crm.repository.PaymentRecordRepository;
import com.yao.crm.repository.QuoteRepository;
import com.yao.crm.repository.TenantRepository;
import com.yao.crm.service.DashboardMetricsCacheService;
import com.yao.crm.service.I18nService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v2/commerce")
public class V2CommerceController extends BaseApiController {

    private final TenantRepository tenantRepository;
    private final OpportunityRepository opportunityRepository;
    private final QuoteRepository quoteRepository;
    private final OrderRecordRepository orderRecordRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final DashboardMetricsCacheService dashboardMetricsCacheService;

    public V2CommerceController(TenantRepository tenantRepository,
                                OpportunityRepository opportunityRepository,
                                QuoteRepository quoteRepository,
                                OrderRecordRepository orderRecordRepository,
                                PaymentRecordRepository paymentRecordRepository,
                                DashboardMetricsCacheService dashboardMetricsCacheService,
                                I18nService i18nService) {
        super(i18nService);
        this.tenantRepository = tenantRepository;
        this.opportunityRepository = opportunityRepository;
        this.quoteRepository = quoteRepository;
        this.orderRecordRepository = orderRecordRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.dashboardMetricsCacheService = dashboardMetricsCacheService;
    }

    @GetMapping("/overview")
    public ResponseEntity<?> overview(HttpServletRequest request) {
        String tenantId = currentTenant(request);
        String cacheKey = currentUser(request) + "|" + currentRole(request);
        DashboardMetricsCacheService.CachedValue<Map<String, Object>> cached = dashboardMetricsCacheService.getOrLoad(
                tenantId,
                "commerce-overview",
                cacheKey,
                () -> {
                    Optional<Tenant> optional = tenantRepository.findById(tenantId);
                    if (!optional.isPresent()) {
                        return null;
                    }
                    Tenant tenant = optional.get();

                    long opportunityTotal = opportunityRepository.countByTenantId(tenantId);
                    long quoteTotal = quoteRepository.countByTenantId(tenantId);
                    long orderTotal = orderRecordRepository.countByTenantId(tenantId);
                    long paymentTotal = paymentRecordRepository.countByTenantId(tenantId);
                    long inFlightQuotes = quoteRepository.countByTenantIdAndStatus(tenantId, "SUBMITTED")
                            + quoteRepository.countByTenantIdAndStatus(tenantId, "APPROVED");
                    long completedOrders = orderRecordRepository.countByTenantIdAndStatus(tenantId, "COMPLETED");

                    Long paymentReceivedValue = paymentRecordRepository.sumAmountByTenantIdAndStatusInUppercase(
                            tenantId,
                            Arrays.asList("RECEIVED", "PAID")
                    );
                    Long paymentOutstandingValue = paymentRecordRepository.sumAmountByTenantIdAndStatusNotInUppercase(
                            tenantId,
                            Arrays.asList("RECEIVED", "PAID")
                    );
                    long paymentReceivedAmount = paymentReceivedValue == null ? 0L : paymentReceivedValue.longValue();
                    long paymentOutstandingAmount = paymentOutstandingValue == null ? 0L : paymentOutstandingValue.longValue();

                    Map<String, Object> marketContext = new LinkedHashMap<String, Object>();
                    marketContext.put("marketProfile", tenant.getMarketProfile());
                    marketContext.put("currency", tenant.getCurrency());
                    marketContext.put("timezone", tenant.getTimezone());
                    marketContext.put("taxRule", tenant.getTaxRule());
                    marketContext.put("taxDisplayMode", "CN".equalsIgnoreCase(tenant.getMarketProfile()) ? "TAX_INCLUSIVE" : "TAX_EXCLUSIVE");

                    Map<String, Object> metrics = new LinkedHashMap<String, Object>();
                    metrics.put("opportunityTotal", opportunityTotal);
                    metrics.put("quoteTotal", quoteTotal);
                    metrics.put("orderTotal", orderTotal);
                    metrics.put("paymentTotal", paymentTotal);
                    metrics.put("inFlightQuotes", inFlightQuotes);
                    metrics.put("completedOrders", completedOrders);
                    metrics.put("paymentReceivedAmount", paymentReceivedAmount);
                    metrics.put("paymentOutstandingAmount", paymentOutstandingAmount);
                    metrics.put("pipelineHealth", opportunityTotal == 0 ? 0 : (int) Math.round((completedOrders * 100.0) / opportunityTotal));
                    metrics.put("winRate", quoteTotal == 0 ? 0 : (int) Math.round((completedOrders * 100.0) / quoteTotal));
                    metrics.put("arrLike", "GLOBAL".equalsIgnoreCase(tenant.getMarketProfile()) ? paymentReceivedAmount * 12 : 0L);

                    Map<String, Object> out = new LinkedHashMap<String, Object>();
                    out.put("tenantId", tenantId);
                    out.put("marketContext", marketContext);
                    out.put("localizedMetrics", metrics);
                    return out;
                }
        );

        Map<String, Object> body = cached.getValue();
        if (body == null) {
            return ResponseEntity.status(404).body(errorBody(request, "tenant_not_found", msg(request, "tenant_not_found"), null));
        }
        return ResponseEntity.ok()
                .header("X-CRM-Cache", cached.isHit() ? "HIT" : "MISS")
                .header("X-CRM-Cache-Tier", cached.getTier())
                .header("X-CRM-Cache-Fallback", cached.isFallback() ? "1" : "0")
                .body(successWithFields(request, "commerce_overview_loaded", body));
    }
}
