package com.yao.crm.controller;

import com.yao.crm.dto.request.OrderCreateRequest;
import com.yao.crm.dto.request.OrderPatchRequest;
import com.yao.crm.entity.ContractRecord;
import com.yao.crm.entity.Customer;
import com.yao.crm.entity.Opportunity;
import com.yao.crm.entity.OrderRecord;
import com.yao.crm.entity.Quote;
import com.yao.crm.enums.OrderStatus;
import com.yao.crm.repository.*;
import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.CommerceFacadeService;
import com.yao.crm.service.I18nService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.*;

@Tag(name = "Orders", description = "Orders and contract management")
@RestController
@RequestMapping("/api/v1")
@Validated
public class V1OrderController extends CommerceControllerSupport {

    private static final Set<String> ORDER_STATUSES = OrderStatus.getValidValues();

    private static final Set<String> CANCELABLE_STATUSES = Set.of(
            OrderStatus.DRAFT.getValue(),
            OrderStatus.CONFIRMED.getValue(),
            OrderStatus.FULFILLING.getValue()
    );

    private final OrderRecordRepository orderRecordRepository;
    private final CustomerRepository customerRepository;
    private final OpportunityRepository opportunityRepository;
    private final QuoteRepository quoteRepository;
    private final ContractRecordRepository contractRecordRepository;
    private final CommerceFacadeService commerceFacadeService;
    private final AuditLogService auditLogService;
    private final I18nService i18nService;

    public V1OrderController(OrderRecordRepository orderRecordRepository,
                             CustomerRepository customerRepository,
                             OpportunityRepository opportunityRepository,
                             QuoteRepository quoteRepository,
                             ContractRecordRepository contractRecordRepository,
                             CommerceFacadeService commerceFacadeService,
                             AuditLogService auditLogService,
                             I18nService i18nService) {
        super(i18nService);
        this.orderRecordRepository = orderRecordRepository;
        this.customerRepository = customerRepository;
        this.opportunityRepository = opportunityRepository;
        this.quoteRepository = quoteRepository;
        this.contractRecordRepository = contractRecordRepository;
        this.commerceFacadeService = commerceFacadeService;
        this.auditLogService = auditLogService;
        this.i18nService = i18nService;
    }

    @GetMapping("/orders")
    public ResponseEntity<?> listOrders(HttpServletRequest request,
                                        @RequestParam(defaultValue = "") String status,
                                        @RequestParam(defaultValue = "") String opportunityId,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        String normalizedStatus = commerceFacadeService.normalizeStatusOrBlank(status, ORDER_STATUSES);
        if (normalizedStatus == null) {
            return ResponseEntity.badRequest().body(errorBody(request, "order_status_invalid", msg(request, "order_status_invalid"), null));
        }
        String normalizedOpportunityId = str(opportunityId);
        int normalizedSize = commerceFacadeService.normalizePageSize(size);
        Pageable pageable = buildPageable(page, normalizedSize, "updatedAt", "desc",
                Set.of("createdAt", "updatedAt", "orderNo", "status", "amount"), "updatedAt");
        Collection<String> scopedOwners = null;
        if (isSalesScoped(request)) {
            Set<String> ownerSet = new LinkedHashSet<String>();
            ownerSet.add(currentUser(request));
            ownerSet.add(currentOwnerScope(request));
            scopedOwners = new ArrayList<String>(ownerSet);
        }
        Page<OrderRecord> result = commerceFacadeService.findOrders(tenantId, normalizedStatus, normalizedOpportunityId, scopedOwners, pageable);
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (OrderRecord row : result.getContent()) {
            items.add(toOrderView(row));
        }
        Map<String, Object> body = pageBody(result, page, normalizedSize);
        body.put("items", items);
        return ResponseEntity.ok(successWithFields(request, "orders_listed", body));
    }

    @PostMapping("/orders")
    public ResponseEntity<?> createOrder(HttpServletRequest request, @Valid @RequestBody OrderCreateRequest payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        OrderRecord row = new OrderRecord();
        row.setId(newId("ord"));
        row.setTenantId(tenantId);
        row.setOrderNo(generateNo("ORD"));
        String validate = applyOrderPayload(request, row, payload, true);
        if (!isBlank(validate)) {
            return ResponseEntity.badRequest().body(errorBody(request, validate, msg(request, validate), null));
        }
        OrderRecord saved = orderRecordRepository.save(row);
        auditLogService.record(currentUser(request), currentRole(request), "CREATE", "ORDER", saved.getId(), "Create order", tenantId);
        return ResponseEntity.status(201).body(successWithFields(request, "order_created", toOrderView(saved)));
    }

    @PatchMapping("/orders")
    public ResponseEntity<?> patchOrder(HttpServletRequest request, @Valid @RequestBody OrderPatchRequest payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String id = payload.getId();
        if (isBlank(id)) {
            return ResponseEntity.badRequest().body(errorBody(request, "id_required", msg(request, "id_required"), null));
        }
        String tenantId = currentTenant(request);
        Optional<OrderRecord> optional = orderRecordRepository.findByIdAndTenantId(id, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "order_not_found", msg(request, "order_not_found"), null));
        }
        OrderRecord row = optional.get();
        if (isSalesScoped(request) && !ownerMatchesScope(request, row.getOwner())) {
            return ResponseEntity.status(403).body(errorBody(request, "scope_forbidden", msg(request, "scope_forbidden"), null));
        }
        String validate = applyOrderPayload(request, row, payload, false);
        if (!isBlank(validate)) {
            return ResponseEntity.badRequest().body(errorBody(request, validate, msg(request, validate), null));
        }
        OrderRecord saved = orderRecordRepository.save(row);
        auditLogService.record(currentUser(request), currentRole(request), "UPDATE", "ORDER", saved.getId(), "Update order", tenantId);
        return ResponseEntity.ok(successWithFields(request, "order_updated", toOrderView(saved)));
    }

    @PostMapping("/orders/{id}/confirm")
    public ResponseEntity<?> confirmOrder(HttpServletRequest request, @PathVariable @NotBlank String id) {
        return transitionOrder(request, id, OrderStatus.DRAFT, OrderStatus.CONFIRMED, "order_confirmed");
    }

    @PostMapping("/orders/{id}/fulfill")
    public ResponseEntity<?> fulfillOrder(HttpServletRequest request, @PathVariable @NotBlank String id) {
        return transitionOrder(request, id, OrderStatus.CONFIRMED, OrderStatus.FULFILLING, "order_fulfilling");
    }

    @PostMapping("/orders/{id}/complete")
    public ResponseEntity<?> completeOrder(HttpServletRequest request, @PathVariable @NotBlank String id) {
        return transitionOrder(request, id, OrderStatus.FULFILLING, OrderStatus.COMPLETED, "order_completed");
    }

    @PostMapping("/orders/{id}/cancel")
    public ResponseEntity<?> cancelOrder(HttpServletRequest request, @PathVariable @NotBlank String id) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        Optional<OrderRecord> optional = orderRecordRepository.findByIdAndTenantId(id, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "order_not_found", msg(request, "order_not_found"), null));
        }
        OrderRecord row = optional.get();
        if (isSalesScoped(request) && !ownerMatchesScope(request, row.getOwner())) {
            return ResponseEntity.status(403).body(errorBody(request, "scope_forbidden", msg(request, "scope_forbidden"), null));
        }
        if (!OrderStatus.isCancelable(row.getStatus())) {
            return ResponseEntity.status(409).body(errorBody(request, "order_status_transition_invalid", msg(request, "order_status_transition_invalid"), null));
        }
        row.setStatus(OrderStatus.CANCELED.getValue());
        row = orderRecordRepository.save(row);
        auditLogService.record(currentUser(request), currentRole(request), "STATUS", "ORDER", row.getId(), "Order CANCELED", tenantId);
        return ResponseEntity.ok(successWithFields(request, "order_canceled", toOrderView(row)));
    }

    @PostMapping("/orders/{id}/to-contract")
    public ResponseEntity<?> orderToContract(HttpServletRequest request, @PathVariable @NotBlank String id) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        Optional<OrderRecord> optional = orderRecordRepository.findByIdAndTenantId(id, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "order_not_found", msg(request, "order_not_found"), null));
        }
        OrderRecord order = optional.get();
        if (isSalesScoped(request) && !ownerMatchesScope(request, order.getOwner())) {
            return ResponseEntity.status(403).body(errorBody(request, "scope_forbidden", msg(request, "scope_forbidden"), null));
        }
        String approvalMode = commerceFacadeService.resolveOrderApprovalMode(order, tenantId);
        if ("STAGE_GATE".equals(approvalMode) && !OrderStatus.FULFILLING.getValue().equalsIgnoreCase(order.getStatus())) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("requiredStatus", OrderStatus.FULFILLING.getValue());
            details.put("currentStatus", order.getStatus());
            details.put("approvalMode", approvalMode);
            auditLogService.record(currentUser(request), currentRole(request), "STAGE_GATE_BLOCK", "ORDER", order.getId(), "Order to-contract blocked by stage gate", tenantId);
            return ResponseEntity.status(409).body(errorBody(request, "order_stage_gate_requires_fulfilling", msg(request, "order_stage_gate_requires_fulfilling"), details));
        }
        if (!"STAGE_GATE".equals(approvalMode) && !OrderStatus.canTransitionToContract(order.getStatus())) {
            return ResponseEntity.status(409).body(errorBody(request, "order_not_confirmed", msg(request, "order_not_confirmed"), null));
        }
        ContractRecord contract = new ContractRecord();
        contract.setId(newId("ctr"));
        contract.setTenantId(tenantId);
        contract.setCustomerId(order.getCustomerId());
        contract.setContractNo(generateNo("CTR"));
        contract.setTitle("Contract from " + order.getOrderNo());
        contract.setAmount(order.getAmount());
        contract.setStatus("Draft");
        contract.setOwner(order.getOwner());
        contract.setSignDate(order.getSignDate());
        ContractRecord saved = contractRecordRepository.save(contract);
        auditLogService.record(currentUser(request), currentRole(request), "CONVERT", "ORDER", order.getId(), "Order to contract", tenantId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("orderId", order.getId());
        body.put("contractId", saved.getId());
        body.put("contractNo", saved.getContractNo());
        body.put("approvalMode", approvalMode);
        auditLogService.record(currentUser(request), currentRole(request), "STAGE_GATE_PASS", "ORDER", order.getId(), "Order to-contract pass by mode " + approvalMode, tenantId);
        return ResponseEntity.status(201).body(successWithFields(request, "order_contract_created", body));
    }

    // ========== Private Helper Methods ==========

    /**
     * 统一的订单载荷应用方法 - 合并 OrderCreateRequest 和 OrderPatchRequest 处理
     */
    private String applyOrderPayload(HttpServletRequest request, OrderRecord row, Object payload, boolean creating) {
        String customerId;
        String opportunityId;
        String quoteId;
        String owner;
        String status;
        Long totalAmount;
        // 扩展字段
        String settlementCurrency = null;
        String exchangeRateSnapshot = null;
        String invoiceStatus = null;
        String taxDisplayMode = null;
        String complianceTag = null;

        if (payload instanceof OrderCreateRequest) {
            OrderCreateRequest createPayload = (OrderCreateRequest) payload;
            customerId = createPayload.getCustomerId();
            opportunityId = createPayload.getOpportunityId();
            quoteId = createPayload.getQuoteId();
            owner = createPayload.getOwner();
            status = createPayload.getStatus();
            totalAmount = createPayload.getTotalAmount();
            // 扩展字段
            settlementCurrency = createPayload.getSettlementCurrency();
            exchangeRateSnapshot = createPayload.getExchangeRateSnapshot();
            invoiceStatus = createPayload.getInvoiceStatus();
            taxDisplayMode = createPayload.getTaxDisplayMode();
            complianceTag = createPayload.getComplianceTag();
        } else if (payload instanceof OrderPatchRequest) {
            OrderPatchRequest patchPayload = (OrderPatchRequest) payload;
            customerId = patchPayload.getCustomerId();
            opportunityId = patchPayload.getOpportunityId();
            quoteId = patchPayload.getQuoteId();
            owner = patchPayload.getOwner();
            status = patchPayload.getStatus();
            totalAmount = patchPayload.getTotalAmount();
        } else {
            return "invalid_payload_type";
        }

        // 客户ID验证
        if (creating && isBlank(customerId)) return "order_customer_required";
        if (!isBlank(customerId)) {
            if (!belongsToTenant(customerRepository.findById(customerId).orElse(null), row.getTenantId())) return "customer_not_found";
            row.setCustomerId(customerId);
        }

        // 商机ID验证
        if (opportunityId != null) {
            if (!isBlank(opportunityId) && !belongsToTenant(opportunityRepository.findById(opportunityId).orElse(null), row.getTenantId())) {
                return "opportunity_not_found";
            }
            row.setOpportunityId(opportunityId);
        }

        // 报价ID设置
        if (quoteId != null) row.setQuoteId(quoteId);

        // 负责人设置
        if (owner != null) {
            if (!isBlank(owner)) {
                if (isSalesScoped(request) && !ownerMatchesScope(request, owner)) return "scope_forbidden";
                row.setOwner(owner.trim());
            }
        } else if (creating) {
            row.setOwner(currentUser(request));
        }

        // 状态验证
        if (status != null) {
            String normalizedStatus = upperOrDefault(status, row.getStatus());
            if (!ORDER_STATUSES.contains(normalizedStatus)) return "order_status_invalid";
            row.setStatus(normalizedStatus);
        }

        // 金额验证
        if (totalAmount != null) row.setAmount(Math.max(0L, totalAmount));

        // 扩展字段设置（仅创建时有效）
        if (creating) {
            if (!isBlank(settlementCurrency)) row.setSettlementCurrency(settlementCurrency.trim());
            if (!isBlank(exchangeRateSnapshot)) row.setExchangeRateSnapshot(exchangeRateSnapshot.trim());
            if (!isBlank(invoiceStatus)) row.setInvoiceStatus(invoiceStatus.trim());
            if (!isBlank(taxDisplayMode)) row.setTaxDisplayMode(taxDisplayMode.trim());
            if (!isBlank(complianceTag)) row.setComplianceTag(complianceTag.trim());
        }

        return "";
    }

    private ResponseEntity<?> transitionOrder(HttpServletRequest request, String id, OrderStatus from, OrderStatus to, String successCode) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        Optional<OrderRecord> optional = orderRecordRepository.findByIdAndTenantId(id, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "order_not_found", msg(request, "order_not_found"), null));
        }
        OrderRecord row = optional.get();
        String approvalMode = commerceFacadeService.resolveOrderApprovalMode(row, tenantId);
        if ("STAGE_GATE".equals(approvalMode) && OrderStatus.DRAFT.equals(from)) {
            ResponseEntity<?> guard = ensureOrderConfirmStageGate(request, row, tenantId, approvalMode);
            if (guard != null) {
                return guard;
            }
        }
        if (!from.getValue().equalsIgnoreCase(row.getStatus())) {
            return ResponseEntity.status(409).body(errorBody(request, "order_status_transition_invalid", msg(request, "order_status_transition_invalid"), null));
        }
        row.setStatus(to.getValue());
        OrderRecord saved = orderRecordRepository.save(row);
        auditLogService.record(currentUser(request), currentRole(request), "STATUS", "ORDER", saved.getId(), "Order " + to.getValue(), tenantId);
        Map<String, Object> body = toOrderView(saved);
        body.put("approvalMode", approvalMode);
        return ResponseEntity.ok(successWithFields(request, successCode, body));
    }

    private ResponseEntity<?> ensureOrderConfirmStageGate(HttpServletRequest request, OrderRecord order, String tenantId, String approvalMode) {
        if (isBlank(order.getQuoteId())) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("requiredStatus", "ACCEPTED_QUOTE");
            details.put("currentStatus", "NO_QUOTE");
            details.put("approvalMode", approvalMode);
            auditLogService.record(currentUser(request), currentRole(request), "STAGE_GATE_BLOCK", "ORDER", order.getId(), "Order confirm blocked: quote link missing", tenantId);
            return ResponseEntity.status(409).body(errorBody(request, "order_stage_gate_requires_quote_accepted", msg(request, "order_stage_gate_requires_quote_accepted"), details));
        }
        Optional<Quote> quoteOpt = quoteRepository.findByIdAndTenantId(order.getQuoteId(), tenantId);
        if (!quoteOpt.isPresent()) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("requiredStatus", "ACCEPTED_QUOTE");
            details.put("currentStatus", "QUOTE_NOT_FOUND");
            details.put("approvalMode", approvalMode);
            auditLogService.record(currentUser(request), currentRole(request), "STAGE_GATE_BLOCK", "ORDER", order.getId(), "Order confirm blocked: quote not found", tenantId);
            return ResponseEntity.status(409).body(errorBody(request, "order_stage_gate_requires_quote_accepted", msg(request, "order_stage_gate_requires_quote_accepted"), details));
        }
        String quoteStatus = quoteOpt.get().getStatus();
        if (!"ACCEPTED".equalsIgnoreCase(quoteStatus)) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("requiredStatus", "ACCEPTED_QUOTE");
            details.put("currentStatus", quoteStatus);
            details.put("approvalMode", approvalMode);
            auditLogService.record(currentUser(request), currentRole(request), "STAGE_GATE_BLOCK", "ORDER", order.getId(), "Order confirm blocked by quote stage", tenantId);
            return ResponseEntity.status(409).body(errorBody(request, "order_stage_gate_requires_quote_accepted", msg(request, "order_stage_gate_requires_quote_accepted"), details));
        }
        auditLogService.record(currentUser(request), currentRole(request), "STAGE_GATE_PASS", "ORDER", order.getId(), "Order confirm pass by stage gate", tenantId);
        return null;
    }
}
