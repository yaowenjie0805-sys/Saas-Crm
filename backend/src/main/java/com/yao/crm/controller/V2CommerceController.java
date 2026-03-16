package com.yao.crm.controller;

import com.yao.crm.entity.Tenant;
import com.yao.crm.repository.OpportunityRepository;
import com.yao.crm.repository.OrderRecordRepository;
import com.yao.crm.repository.PaymentRecordRepository;
import com.yao.crm.repository.QuoteRepository;
import com.yao.crm.repository.TenantRepository;
import com.yao.crm.service.I18nService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
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

    public V2CommerceController(TenantRepository tenantRepository,
                                OpportunityRepository opportunityRepository,
                                QuoteRepository quoteRepository,
                                OrderRecordRepository orderRecordRepository,
                                PaymentRecordRepository paymentRecordRepository,
                                I18nService i18nService) {
        super(i18nService);
        this.tenantRepository = tenantRepository;
        this.opportunityRepository = opportunityRepository;
        this.quoteRepository = quoteRepository;
        this.orderRecordRepository = orderRecordRepository;
        this.paymentRecordRepository = paymentRecordRepository;
    }

    @GetMapping("/overview")
    public ResponseEntity<?> overview(HttpServletRequest request) {
        String tenantId = currentTenant(request);
        Optional<Tenant> optional = tenantRepository.findById(tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "tenant_not_found", msg(request, "tenant_not_found"), null));
        }
        Tenant tenant = optional.get();

        long opportunityTotal = opportunityRepository.findByTenantId(tenantId).size();
        long quoteTotal = quoteRepository.findByTenantId(tenantId).size();
        long orderTotal = orderRecordRepository.findByTenantId(tenantId).size();
        long paymentTotal = paymentRecordRepository.findByTenantId(tenantId).size();
        long inFlightQuotes = quoteRepository.countByTenantIdAndStatus(tenantId, "SUBMITTED")
                + quoteRepository.countByTenantIdAndStatus(tenantId, "APPROVED");
        long completedOrders = orderRecordRepository.countByTenantIdAndStatus(tenantId, "COMPLETED");

        long paymentReceivedAmount = 0L;
        long paymentOutstandingAmount = 0L;
        for (com.yao.crm.entity.PaymentRecord payment : paymentRecordRepository.findByTenantId(tenantId)) {
            long value = payment.getAmount() == null ? 0L : payment.getAmount();
            if ("RECEIVED".equalsIgnoreCase(payment.getStatus()) || "PAID".equalsIgnoreCase(payment.getStatus())) {
                paymentReceivedAmount += value;
            } else {
                paymentOutstandingAmount += value;
            }
        }

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

        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("tenantId", tenantId);
        body.put("marketContext", marketContext);
        body.put("localizedMetrics", metrics);
        return ResponseEntity.ok(successWithFields(request, "commerce_overview_loaded", body));
    }
}
