package com.yao.crm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yao.crm.dto.request.QuoteCreateRequest;
import com.yao.crm.dto.request.QuoteItemRequest;
import com.yao.crm.dto.request.QuotePatchRequest;
import com.yao.crm.dto.request.QuoteVersionCreateRequest;
import com.yao.crm.entity.*;
import com.yao.crm.repository.*;
import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.CommerceFacadeService;
import com.yao.crm.service.I18nService;
import com.yao.crm.util.IdGenerator;
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
import java.time.LocalDateTime;
import java.util.*;

@Tag(name = "Quotes", description = "Quotes, quote items and quote versions")
@RestController
@RequestMapping("/api/v1")
@Validated
public class V1QuoteController extends CommerceControllerSupport {

    private static final Set<String> QUOTE_STATUSES = Set.of(
            "DRAFT", "SUBMITTED", "APPROVED", "REJECTED", "ACCEPTED", "EXPIRED", "CANCELED"
    );

    private final QuoteRepository quoteRepository;
    private final QuoteItemRepository quoteItemRepository;
    private final QuoteVersionRepository quoteVersionRepository;
    private final CustomerRepository customerRepository;
    private final OpportunityRepository opportunityRepository;
    private final ProductRepository productRepository;
    private final OrderRecordRepository orderRecordRepository;
    private final ApprovalTemplateRepository approvalTemplateRepository;
    private final ApprovalInstanceRepository approvalInstanceRepository;
    private final ApprovalTaskRepository approvalTaskRepository;
    private final ApprovalEventRepository approvalEventRepository;
    private final CommerceFacadeService commerceFacadeService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    private final I18nService i18nService;
    private final IdGenerator idGenerator;

    public V1QuoteController(QuoteRepository quoteRepository,
                             QuoteItemRepository quoteItemRepository,
                             QuoteVersionRepository quoteVersionRepository,
                             CustomerRepository customerRepository,
                             OpportunityRepository opportunityRepository,
                             ProductRepository productRepository,
                             OrderRecordRepository orderRecordRepository,
                             ApprovalTemplateRepository approvalTemplateRepository,
                             ApprovalInstanceRepository approvalInstanceRepository,
                             ApprovalTaskRepository approvalTaskRepository,
                             ApprovalEventRepository approvalEventRepository,
                             CommerceFacadeService commerceFacadeService,
                             AuditLogService auditLogService,
                             ObjectMapper objectMapper,
                             I18nService i18nService,
                             IdGenerator idGenerator) {
        super(i18nService);
        this.quoteRepository = quoteRepository;
        this.quoteItemRepository = quoteItemRepository;
        this.quoteVersionRepository = quoteVersionRepository;
        this.customerRepository = customerRepository;
        this.opportunityRepository = opportunityRepository;
        this.productRepository = productRepository;
        this.orderRecordRepository = orderRecordRepository;
        this.approvalTemplateRepository = approvalTemplateRepository;
        this.approvalInstanceRepository = approvalInstanceRepository;
        this.approvalTaskRepository = approvalTaskRepository;
        this.approvalEventRepository = approvalEventRepository;
        this.commerceFacadeService = commerceFacadeService;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
        this.i18nService = i18nService;
        this.idGenerator = idGenerator;
    }

    @GetMapping("/quotes")
    public ResponseEntity<?> listQuotes(HttpServletRequest request,
                                        @RequestParam(defaultValue = "") String status,
                                        @RequestParam(defaultValue = "") String opportunityId,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        String normalizedStatus = commerceFacadeService.normalizeStatusOrBlank(status, QUOTE_STATUSES);
        if (normalizedStatus == null) {
            return ResponseEntity.badRequest().body(errorBody(request, "quote_status_invalid", msg(request, "quote_status_invalid"), null));
        }
        String normalizedOpportunityId = normalizeId(opportunityId);
        int normalizedSize = commerceFacadeService.normalizePageSize(size);
        Pageable pageable = buildPageable(page, normalizedSize, "updatedAt", "desc",
                Set.of("createdAt", "updatedAt", "quoteNo", "status", "totalAmount"), "updatedAt");
        Collection<String> scopedOwners = null;
        if (isSalesScoped(request)) {
            Set<String> ownerSet = new LinkedHashSet<String>();
            ownerSet.add(currentUser(request));
            ownerSet.add(currentOwnerScope(request));
            scopedOwners = new ArrayList<String>(ownerSet);
        }
        Page<Quote> result = commerceFacadeService.findQuotes(tenantId, normalizedStatus, normalizedOpportunityId, scopedOwners, pageable);
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (Quote row : result.getContent()) {
            items.add(toQuoteView(row, false));
        }
        Map<String, Object> body = pageBody(result, page, normalizedSize);
        body.put("items", items);
        return ResponseEntity.ok(successWithFields(request, "quotes_listed", body));
    }

    @PostMapping("/quotes")
    public ResponseEntity<?> createQuote(HttpServletRequest request, @Valid @RequestBody QuoteCreateRequest payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        Quote row = new Quote();
        row.setId(newId("qt"));
        row.setTenantId(tenantId);
        row.setQuoteNo(generateNo("QUO"));
        String validate = applyQuotePayload(request, row, payload, true);
        if (!isBlank(validate)) {
            return validationError(request, validate);
        }
        Quote saved = quoteRepository.save(row);
        recomputeQuoteAmounts(saved);
        auditLogService.record(currentUser(request), currentRole(request), "CREATE", "QUOTE", saved.getId(), "Create quote", tenantId);
        return ResponseEntity.status(201).body(successWithFields(request, "quote_created", toQuoteView(saved, true)));
    }

    @PatchMapping("/quotes")
    public ResponseEntity<?> patchQuote(HttpServletRequest request, @Valid @RequestBody QuotePatchRequest payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String normalizedId = normalizeId(payload.getId());
        if (isBlank(normalizedId)) {
            return ResponseEntity.badRequest().body(errorBody(request, "id_required", msg(request, "id_required"), null));
        }
        String tenantId = currentTenant(request);
        Optional<Quote> optional = quoteRepository.findByIdAndTenantId(normalizedId, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "quote_not_found", msg(request, "quote_not_found"), null));
        }
        Quote row = optional.get();
        if (isSalesScoped(request) && !ownerMatchesScope(request, row.getOwner())) {
            return ResponseEntity.status(403).body(errorBody(request, "scope_forbidden", msg(request, "scope_forbidden"), null));
        }
        if (!"DRAFT".equalsIgnoreCase(row.getStatus())) {
            return ResponseEntity.status(409).body(errorBody(request, "quote_only_draft_editable", msg(request, "quote_only_draft_editable"), null));
        }
        String validate = applyQuotePayload(request, row, payload, false);
        if (!isBlank(validate)) {
            return validationError(request, validate);
        }
        Quote saved = quoteRepository.save(row);
        recomputeQuoteAmounts(saved);
        auditLogService.record(currentUser(request), currentRole(request), "UPDATE", "QUOTE", saved.getId(), "Update quote", tenantId);
        return ResponseEntity.ok(successWithFields(request, "quote_updated", toQuoteView(saved, true)));
    }

    @GetMapping("/quotes/{id}/items")
    public ResponseEntity<?> listQuoteItems(HttpServletRequest request, @PathVariable @NotBlank String id) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String normalizedId = normalizeId(id);
        if (isBlank(normalizedId)) {
            return ResponseEntity.badRequest().body(errorBody(request, "id_required", msg(request, "id_required"), null));
        }
        String tenantId = currentTenant(request);
        Optional<Quote> quoteOptional = quoteRepository.findByIdAndTenantId(normalizedId, tenantId);
        if (!quoteOptional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "quote_not_found", msg(request, "quote_not_found"), null));
        }
        Quote quote = quoteOptional.get();
        if (isSalesScoped(request) && !ownerMatchesScope(request, quote.getOwner())) {
            return ResponseEntity.status(403).body(errorBody(request, "scope_forbidden", msg(request, "scope_forbidden"), null));
        }
        List<QuoteItem> items = quoteItemRepository.findByTenantIdAndQuoteIdOrderByCreatedAtAsc(tenantId, normalizedId);
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (QuoteItem item : items) out.add(toQuoteItemView(item));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("quoteId", normalizedId);
        body.put("items", out);
        body.put("total", out.size());
        return ResponseEntity.ok(successWithFields(request, "quote_items_listed", body));
    }

    @PostMapping("/quotes/{id}/items")
    public ResponseEntity<?> replaceQuoteItems(HttpServletRequest request, @PathVariable @NotBlank String id, @Valid @RequestBody List<QuoteItemRequest> payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String normalizedId = normalizeId(id);
        if (isBlank(normalizedId)) {
            return ResponseEntity.badRequest().body(errorBody(request, "id_required", msg(request, "id_required"), null));
        }
        String tenantId = currentTenant(request);
        Optional<Quote> quoteOptional = quoteRepository.findByIdAndTenantId(normalizedId, tenantId);
        if (!quoteOptional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "quote_not_found", msg(request, "quote_not_found"), null));
        }
        Quote quote = quoteOptional.get();
        if (isSalesScoped(request) && !ownerMatchesScope(request, quote.getOwner())) {
            return ResponseEntity.status(403).body(errorBody(request, "scope_forbidden", msg(request, "scope_forbidden"), null));
        }
        if (!"DRAFT".equalsIgnoreCase(quote.getStatus())) {
            return ResponseEntity.status(409).body(errorBody(request, "quote_only_draft_editable", msg(request, "quote_only_draft_editable"), null));
        }
        quoteItemRepository.deleteByTenantIdAndQuoteId(tenantId, normalizedId);
        List<Map<String, Object>> savedItems = new ArrayList<Map<String, Object>>();
        if (payload != null) {
            Set<String> productIds = new LinkedHashSet<String>();
            for (QuoteItemRequest itemPayload : payload) {
                String productId = normalizeId(itemPayload.getProductId());
                if (!isBlank(productId)) {
                    productIds.add(productId);
                }
            }
            Map<String, Product> productsById = new LinkedHashMap<>();
            if (!productIds.isEmpty()) {
                for (Product product : productRepository.findByTenantIdAndIdIn(tenantId, new ArrayList<String>(productIds))) {
                    productsById.put(product.getId(), product);
                }
            }
            List<QuoteItem> toSave = new ArrayList<QuoteItem>();
            for (QuoteItemRequest itemPayload : payload) {
                String productId = normalizeId(itemPayload.getProductId());
                if (isBlank(productId)) continue;
                Product product = productsById.get(productId);
                if (product == null) continue;
                QuoteItem item = new QuoteItem();
                item.setId(newId("qti"));
                item.setTenantId(tenantId);
                item.setQuoteId(normalizedId);
                item.setProductId(productId);
                item.setProductName(isBlank(itemPayload.getProductName()) ? product.getName() : itemPayload.getProductName().trim());
                item.setQuantity(itemPayload.getQuantity() != null ? Math.max(1, itemPayload.getQuantity()) : 1);
                item.setUnitPrice(itemPayload.getUnitPrice() != null ? Math.max(0L, itemPayload.getUnitPrice()) : (product.getStandardPrice() == null ? 0L : product.getStandardPrice()));
                item.setDiscountRate(itemPayload.getDiscountRate() != null ? Math.max(0.0, itemPayload.getDiscountRate()) : 0.0);
                item.setTaxRate(itemPayload.getTaxRate() != null ? Math.max(0.0, itemPayload.getTaxRate()) : (product.getTaxRate() == null ? 0.0 : product.getTaxRate()));
                calculateLine(item);
                toSave.add(item);
            }
            if (!toSave.isEmpty()) {
                for (QuoteItem savedItem : quoteItemRepository.saveAll(toSave)) {
                    savedItems.add(toQuoteItemView(savedItem));
                }
            }
        }
        Quote updated = recomputeQuoteAmounts(quote);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("quote", toQuoteView(updated, false));
        body.put("items", savedItems);
        auditLogService.record(currentUser(request), currentRole(request), "UPDATE", "QUOTE_ITEM", normalizedId, "Replace quote items", tenantId);
        return ResponseEntity.ok(successWithFields(request, "quote_items_saved", body));
    }

    @PostMapping("/quotes/{id}/submit")
    public ResponseEntity<?> submitQuote(HttpServletRequest request, @PathVariable @NotBlank String id) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String normalizedId = normalizeId(id);
        if (isBlank(normalizedId)) {
            return ResponseEntity.badRequest().body(errorBody(request, "id_required", msg(request, "id_required"), null));
        }
        String tenantId = currentTenant(request);
        Optional<Quote> optional = quoteRepository.findByIdAndTenantId(normalizedId, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "quote_not_found", msg(request, "quote_not_found"), null));
        }
        Quote quote = optional.get();
        if (isSalesScoped(request) && !ownerMatchesScope(request, quote.getOwner())) {
            return ResponseEntity.status(403).body(errorBody(request, "scope_forbidden", msg(request, "scope_forbidden"), null));
        }
        if (!"DRAFT".equalsIgnoreCase(quote.getStatus())) {
            return ResponseEntity.status(409).body(errorBody(request, "quote_status_transition_invalid", msg(request, "quote_status_transition_invalid"), null));
        }
        String approvalMode = commerceFacadeService.resolveApprovalMode(tenantId);
        List<QuoteItem> rows = quoteItemRepository.findByTenantIdAndQuoteIdOrderByCreatedAtAsc(tenantId, quote.getId());
        boolean triggerByAmount = (quote.getTotalAmount() == null ? 0L : quote.getTotalAmount()) >= 500000L;
        boolean triggerByDiscount = false;
        for (QuoteItem row : rows) {
            if (row.getDiscountRate() != null && row.getDiscountRate() >= 0.20d) {
                triggerByDiscount = true;
                break;
            }
        }
        boolean approvalTriggered = "STAGE_GATE".equals(approvalMode) || triggerByAmount || triggerByDiscount;
        String approvalInstanceId = "";
        if (approvalTriggered) {
            ApprovalInstance instance = createQuoteApprovalInstance(request, quote);
            if (instance == null) {
                return ResponseEntity.status(409).body(errorBody(request, "approval_template_not_found", msg(request, "approval_template_not_found"), null));
            }
            approvalInstanceId = instance.getId();
        }
        quote.setStatus("SUBMITTED");
        quoteRepository.save(quote);
        Map<String, Object> body = toQuoteView(quote, false);
        body.put("approvalTriggered", approvalTriggered);
        body.put("approvalInstanceId", approvalInstanceId);
        body.put("approvalReason", approvalTriggered ? ("STAGE_GATE".equals(approvalMode) ? "STAGE_GATE" : (triggerByAmount ? "AMOUNT" : "DISCOUNT")) : "NONE");
        body.put("approvalMode", approvalMode);
        auditLogService.record(currentUser(request), currentRole(request), "SUBMIT", "QUOTE", quote.getId(), approvalTriggered ? ("submit with approval trigger (" + approvalMode + ")") : "submit without approval trigger", tenantId);
        return ResponseEntity.ok(successWithFields(request, "quote_submitted", body));
    }

    @PostMapping("/quotes/{id}/accept")
    public ResponseEntity<?> acceptQuote(HttpServletRequest request, @PathVariable @NotBlank String id) {
        return transitionQuote(request, id, Collections.singletonList("APPROVED"), "ACCEPTED", "quote_accepted");
    }

    @PostMapping("/quotes/{id}/to-order")
    public ResponseEntity<?> quoteToOrder(HttpServletRequest request, @PathVariable @NotBlank String id) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String normalizedId = normalizeId(id);
        if (isBlank(normalizedId)) {
            return ResponseEntity.badRequest().body(errorBody(request, "id_required", msg(request, "id_required"), null));
        }
        String tenantId = currentTenant(request);
        Optional<Quote> optional = quoteRepository.findByIdAndTenantId(normalizedId, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "quote_not_found", msg(request, "quote_not_found"), null));
        }
        Quote quote = optional.get();
        if (isSalesScoped(request) && !ownerMatchesScope(request, quote.getOwner())) {
            return ResponseEntity.status(403).body(errorBody(request, "scope_forbidden", msg(request, "scope_forbidden"), null));
        }
        String approvalMode = commerceFacadeService.resolveApprovalMode(tenantId);
        if ("STAGE_GATE".equals(approvalMode) && !"APPROVED".equalsIgnoreCase(quote.getStatus())) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("requiredStatus", "APPROVED");
            details.put("currentStatus", quote.getStatus());
            details.put("approvalMode", approvalMode);
            return ResponseEntity.status(409).body(errorBody(request, "quote_stage_gate_requires_approval", msg(request, "quote_stage_gate_requires_approval"), details));
        }
        if (!"STAGE_GATE".equals(approvalMode) && !"ACCEPTED".equalsIgnoreCase(quote.getStatus()) && !"APPROVED".equalsIgnoreCase(quote.getStatus())) {
            return ResponseEntity.status(409).body(errorBody(request, "quote_not_accepted", msg(request, "quote_not_accepted"), null));
        }
        OrderRecord order = new OrderRecord();
        order.setId(newId("ord"));
        order.setTenantId(tenantId);
        order.setOrderNo(generateNo("ORD"));
        order.setCustomerId(quote.getCustomerId());
        order.setOpportunityId(quote.getOpportunityId());
        order.setQuoteId(quote.getId());
        order.setOwner(quote.getOwner());
        order.setStatus("DRAFT");
        order.setAmount(quote.getTotalAmount());
        order.setNotes(commerceFacadeService.buildOrderCreationNotes(quote.getQuoteNo(), approvalMode));
        OrderRecord saved = orderRecordRepository.save(order);
        quote.setStatus("ACCEPTED");
        quoteRepository.save(quote);
        auditLogService.record(currentUser(request), currentRole(request), "CONVERT", "QUOTE", quote.getId(), "Quote to order", tenantId);
        Map<String, Object> body = toOrderView(saved);
        body.put("orderId", saved.getId());
        body.put("orderStatus", saved.getStatus());
        body.put("sourceQuoteId", quote.getId());
        body.put("sourceQuoteStatus", quote.getStatus());
        body.put("approvalMode", approvalMode);
        return ResponseEntity.status(201).body(successWithFields(request, "quote_order_created", body));
    }

    @GetMapping("/quotes/{id}/versions")
    public ResponseEntity<?> listQuoteVersions(HttpServletRequest request, @PathVariable @NotBlank String id) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String normalizedId = normalizeId(id);
        if (isBlank(normalizedId)) {
            return ResponseEntity.badRequest().body(errorBody(request, "id_required", msg(request, "id_required"), null));
        }
        String tenantId = currentTenant(request);
        Optional<Quote> optional = quoteRepository.findByIdAndTenantId(normalizedId, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "quote_not_found", msg(request, "quote_not_found"), null));
        }
        Quote quote = optional.get();
        if (isSalesScoped(request) && !ownerMatchesScope(request, quote.getOwner())) {
            return ResponseEntity.status(403).body(errorBody(request, "scope_forbidden", msg(request, "scope_forbidden"), null));
        }
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (QuoteVersion row : quoteVersionRepository.findByTenantIdAndQuoteIdOrderByVersionNoDesc(tenantId, normalizedId)) {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("id", row.getId());
            out.put("quoteId", row.getQuoteId());
            out.put("versionNo", row.getVersionNo());
            out.put("status", row.getStatus());
            out.put("totalAmount", row.getTotalAmount());
            out.put("createdBy", row.getCreatedBy());
            out.put("createdAt", row.getCreatedAt());
            out.put("snapshotJson", row.getSnapshotJson());
            items.add(out);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("quoteId", normalizedId);
        body.put("items", items);
        body.put("total", items.size());
        return ResponseEntity.ok(successWithFields(request, "quote_versions_listed", body));
    }

    @PostMapping("/quotes/{id}/versions")
    public ResponseEntity<?> createQuoteVersion(HttpServletRequest request,
                                                @PathVariable @NotBlank String id,
                                                @Valid @RequestBody(required = false) QuoteVersionCreateRequest payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String normalizedId = normalizeId(id);
        if (isBlank(normalizedId)) {
            return ResponseEntity.badRequest().body(errorBody(request, "id_required", msg(request, "id_required"), null));
        }
        String tenantId = currentTenant(request);
        Optional<Quote> optional = quoteRepository.findByIdAndTenantId(normalizedId, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "quote_not_found", msg(request, "quote_not_found"), null));
        }
        Quote quote = optional.get();
        if (isSalesScoped(request) && !ownerMatchesScope(request, quote.getOwner())) {
            return ResponseEntity.status(403).body(errorBody(request, "scope_forbidden", msg(request, "scope_forbidden"), null));
        }
        int nextVersion = quoteVersionRepository.findTopByTenantIdAndQuoteIdOrderByVersionNoDesc(tenantId, normalizedId)
                .map(v -> (v.getVersionNo() == null ? 0 : v.getVersionNo()) + 1)
                .orElse(1);
        List<Map<String, Object>> lineItems = new ArrayList<Map<String, Object>>();
        for (QuoteItem item : quoteItemRepository.findByTenantIdAndQuoteIdOrderByCreatedAtAsc(tenantId, normalizedId)) {
            lineItems.add(toQuoteItemView(item));
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("quote", toQuoteView(quote, false));
        snapshot.put("items", lineItems);
        snapshot.put("remark", payload == null ? "" : (payload.getRemark() == null ? "" : payload.getRemark()));
        snapshot.put("requestId", traceId(request));
        String snapshotJson;
        try {
            snapshotJson = objectMapper.writeValueAsString(snapshot);
        } catch (Exception ex) {
            snapshotJson = "{\"quoteId\":\"" + normalizedId + "\"}";
        }
        QuoteVersion row = new QuoteVersion();
        row.setId(newId("qtv"));
        row.setTenantId(tenantId);
        row.setQuoteId(normalizedId);
        row.setVersionNo(nextVersion);
        row.setStatus(quote.getStatus());
        row.setTotalAmount(quote.getTotalAmount() == null ? 0L : quote.getTotalAmount());
        row.setCreatedBy(currentUser(request));
        row.setSnapshotJson(snapshotJson);
        QuoteVersion saved = quoteVersionRepository.save(row);
        auditLogService.record(currentUser(request), currentRole(request), "VERSION", "QUOTE", quote.getId(), "Create quote version " + nextVersion, tenantId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", saved.getId());
        body.put("quoteId", saved.getQuoteId());
        body.put("versionNo", saved.getVersionNo());
        body.put("status", saved.getStatus());
        body.put("totalAmount", saved.getTotalAmount());
        body.put("createdBy", saved.getCreatedBy());
        body.put("createdAt", saved.getCreatedAt());
        return ResponseEntity.status(201).body(successWithFields(request, "quote_version_created", body));
    }

    // ========== Private Helper Methods ==========

    private ResponseEntity<?> transitionQuote(HttpServletRequest request, String id, List<String> fromStatuses, String to, String successCode) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String normalizedId = normalizeId(id);
        if (isBlank(normalizedId)) {
            return ResponseEntity.badRequest().body(errorBody(request, "id_required", msg(request, "id_required"), null));
        }
        String tenantId = currentTenant(request);
        Optional<Quote> optional = quoteRepository.findByIdAndTenantId(normalizedId, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "quote_not_found", msg(request, "quote_not_found"), null));
        }
        Quote row = optional.get();
        if (isSalesScoped(request) && !ownerMatchesScope(request, row.getOwner())) {
            return ResponseEntity.status(403).body(errorBody(request, "scope_forbidden", msg(request, "scope_forbidden"), null));
        }
        boolean validFrom = false;
        for (String from : fromStatuses) {
            if (from.equalsIgnoreCase(row.getStatus())) {
                validFrom = true;
                break;
            }
        }
        if (!validFrom) {
            return ResponseEntity.status(409).body(errorBody(request, "quote_status_transition_invalid", msg(request, "quote_status_transition_invalid"), null));
        }
        row.setStatus(to);
        Quote saved = quoteRepository.save(row);
        auditLogService.record(currentUser(request), currentRole(request), "STATUS", "QUOTE", saved.getId(), "Quote " + to, tenantId);
        return ResponseEntity.ok(successWithFields(request, successCode, toQuoteView(saved, false)));
    }

    private Quote recomputeQuoteAmounts(Quote quote) {
        List<QuoteItem> items = quoteItemRepository.findByTenantIdAndQuoteIdOrderByCreatedAtAsc(quote.getTenantId(), quote.getId());
        long subtotal = 0L;
        long tax = 0L;
        long total = 0L;
        for (QuoteItem item : items) {
            subtotal += item.getSubtotalAmount() == null ? 0L : item.getSubtotalAmount();
            tax += item.getTaxAmount() == null ? 0L : item.getTaxAmount();
            total += item.getTotalAmount() == null ? 0L : item.getTotalAmount();
        }
        quote.setSubtotalAmount(subtotal);
        quote.setTaxAmount(tax);
        quote.setTotalAmount(total);
        return quoteRepository.save(quote);
    }

    private void calculateLine(QuoteItem item) {
        int qty = item.getQuantity() == null ? 1 : Math.max(1, item.getQuantity());
        long unitPrice = item.getUnitPrice() == null ? 0L : Math.max(0L, item.getUnitPrice());
        double discountRate = item.getDiscountRate() == null ? 0.0 : Math.max(0.0, item.getDiscountRate());
        if (discountRate > 1) discountRate = 1;
        double taxRate = item.getTaxRate() == null ? 0.0 : Math.max(0.0, item.getTaxRate());
        long subtotal = Math.max(0L, Math.round(qty * unitPrice * (1 - discountRate)));
        long tax = Math.max(0L, Math.round(subtotal * taxRate));
        item.setQuantity(qty);
        item.setUnitPrice(unitPrice);
        item.setDiscountRate(discountRate);
        item.setTaxRate(taxRate);
        item.setSubtotalAmount(subtotal);
        item.setTaxAmount(tax);
        item.setTotalAmount(subtotal + tax);
    }

    private String applyQuotePayload(HttpServletRequest request, Quote row, QuoteCreateRequest payload, boolean creating) {
        String customerId = normalizeId(payload.getCustomerId());
        if (creating && isBlank(customerId)) return "quote_customer_required";
        if (!isBlank(customerId)) {
            if (!customerRepository.findByIdAndTenantId(customerId, row.getTenantId()).isPresent()) return "customer_not_found";
            row.setCustomerId(customerId);
        }
        String opportunityId = normalizeId(payload.getOpportunityId());
        if (!isBlank(opportunityId)) {
            if (!opportunityRepository.findByIdAndTenantId(opportunityId, row.getTenantId()).isPresent()) return "opportunity_not_found";
            row.setOpportunityId(opportunityId);
        }
        if (payload.getOwner() != null) {
            String owner = normalizeId(payload.getOwner());
            if (!isBlank(owner)) {
                if (isSalesScoped(request) && !ownerMatchesScope(request, owner)) return "scope_forbidden";
                row.setOwner(owner);
            }
        } else if (creating) {
            row.setOwner(currentUser(request));
        }
        if (payload.getStatus() != null) {
            String status = normalizeStatus(payload.getStatus(), row.getStatus());
            if (!QUOTE_STATUSES.contains(status)) return "quote_status_invalid";
            row.setStatus(status);
        }
        if (payload.getValidUntil() != null) row.setValidUntil(parseLocalDate(payload.getValidUntil()));
        return "";
    }

    private String applyQuotePayload(HttpServletRequest request, Quote row, QuotePatchRequest payload, boolean creating) {
        String customerId = normalizeId(payload.getCustomerId());
        if (creating && isBlank(customerId)) return "quote_customer_required";
        if (!isBlank(customerId)) {
            if (!customerRepository.findByIdAndTenantId(customerId, row.getTenantId()).isPresent()) return "customer_not_found";
            row.setCustomerId(customerId);
        }
        String opportunityId = normalizeId(payload.getOpportunityId());
        if (!isBlank(opportunityId)) {
            if (!opportunityRepository.findByIdAndTenantId(opportunityId, row.getTenantId()).isPresent()) return "opportunity_not_found";
            row.setOpportunityId(opportunityId);
        }
        if (payload.getOwner() != null) {
            String owner = normalizeId(payload.getOwner());
            if (!isBlank(owner)) {
                if (isSalesScoped(request) && !ownerMatchesScope(request, owner)) return "scope_forbidden";
                row.setOwner(owner);
            }
        } else if (creating) {
            row.setOwner(currentUser(request));
        }
        if (payload.getStatus() != null) {
            String status = normalizeStatus(payload.getStatus(), row.getStatus());
            if (!QUOTE_STATUSES.contains(status)) return "quote_status_invalid";
            row.setStatus(status);
        }
        if (payload.getValidUntil() != null) row.setValidUntil(parseLocalDate(payload.getValidUntil()));
        return "";
    }

    private ApprovalInstance createQuoteApprovalInstance(HttpServletRequest request, Quote quote) {
        String tenantId = quote.getTenantId();
        String role = currentRole(request);
        List<ApprovalTemplate> templates = approvalTemplateRepository.findByTenantIdAndBizTypeAndEnabledTrueOrderByCreatedAtAsc(tenantId, "QUOTE");
        ApprovalTemplate selected = null;
        for (ApprovalTemplate t : templates) {
            long amount = quote.getTotalAmount() == null ? 0L : quote.getTotalAmount();
            if (t.getAmountMin() != null && amount < t.getAmountMin()) continue;
            if (t.getAmountMax() != null && amount > t.getAmountMax()) continue;
            if (!isBlank(t.getRole()) && !t.getRole().equalsIgnoreCase(role)) continue;
            if ("INACTIVE".equalsIgnoreCase(t.getStatus())) continue;
            selected = t;
            break;
        }
        if (selected == null) {
            return null;
        }
        ApprovalInstance instance = new ApprovalInstance();
        instance.setId(newId("api"));
        instance.setTenantId(tenantId);
        instance.setTemplateId(selected.getId());
        instance.setTemplateVersion(selected.getVersion());
        instance.setBizType("QUOTE");
        instance.setBizId(quote.getId());
        instance.setSubmitter(currentUser(request));
        instance.setComment("Auto submitted by quote threshold");
        instance.setStatus("PENDING");
        instance.setCurrentSeq(1);
        instance = approvalInstanceRepository.save(instance);

        String[] roles = (selected.getApproverRoles() == null ? "" : selected.getApproverRoles()).split(",");
        int seq = 1;
        for (String r : roles) {
            String roleItem = isBlank(r) ? "" : r.trim().toUpperCase(Locale.ROOT);
            if (roleItem.isEmpty()) continue;
            ApprovalTask task = new ApprovalTask();
            task.setId(newId("aptk"));
            task.setTenantId(tenantId);
            task.setInstanceId(instance.getId());
            task.setApproverRole(roleItem);
            task.setSeq(seq);
            task.setStatus(seq == 1 ? "PENDING" : "WAITING");
            task.setSlaMinutes(240);
            task.setDeadlineAt(LocalDateTime.now().plusMinutes(240));
            approvalTaskRepository.save(task);

            ApprovalEvent event = new ApprovalEvent();
            event.setId(newId("apev"));
            event.setTenantId(tenantId);
            event.setInstanceId(instance.getId());
            event.setTaskId(task.getId());
            event.setEventType("TASK_CREATED");
            event.setOperatorUser(currentUser(request));
            event.setDetail("quote_auto_submit");
            event.setRequestId(traceId(request));
            approvalEventRepository.save(event);
            seq++;
        }

        ApprovalEvent submitEvent = new ApprovalEvent();
        submitEvent.setId(newId("apev"));
        submitEvent.setTenantId(tenantId);
        submitEvent.setInstanceId(instance.getId());
        submitEvent.setTaskId(null);
        submitEvent.setEventType("SUBMITTED");
        submitEvent.setOperatorUser(currentUser(request));
        submitEvent.setDetail("quote_auto_submit");
        submitEvent.setRequestId(traceId(request));
        approvalEventRepository.save(submitEvent);
        return instance;
    }

    private Map<String, Object> toQuoteView(Quote row, boolean withItems) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", row.getId());
        out.put("quoteNo", row.getQuoteNo());
        out.put("customerId", row.getCustomerId());
        out.put("opportunityId", row.getOpportunityId());
        out.put("priceBookId", row.getPriceBookId());
        out.put("owner", row.getOwner());
        out.put("status", row.getStatus());
        out.put("subtotalAmount", row.getSubtotalAmount());
        out.put("taxAmount", row.getTaxAmount());
        out.put("totalAmount", row.getTotalAmount());
        out.put("version", row.getVersion());
        out.put("validUntil", row.getValidUntil());
        out.put("notes", row.getNotes());
        out.put("tenantId", row.getTenantId());
        out.put("updatedAt", row.getUpdatedAt());
        if (withItems) {
            List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
            for (QuoteItem item : quoteItemRepository.findByTenantIdAndQuoteIdOrderByCreatedAtAsc(row.getTenantId(), row.getId())) {
                items.add(toQuoteItemView(item));
            }
            out.put("items", items);
        }
        return out;
    }

    private Map<String, Object> toQuoteItemView(QuoteItem row) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", row.getId());
        out.put("quoteId", row.getQuoteId());
        out.put("productId", row.getProductId());
        out.put("productName", row.getProductName());
        out.put("quantity", row.getQuantity());
        out.put("unitPrice", row.getUnitPrice());
        out.put("discountRate", row.getDiscountRate());
        out.put("taxRate", row.getTaxRate());
        out.put("subtotalAmount", row.getSubtotalAmount());
        out.put("taxAmount", row.getTaxAmount());
        out.put("totalAmount", row.getTotalAmount());
        return out;
    }

    private ResponseEntity<?> validationError(HttpServletRequest request, String code) {
        String normalizedCode = normalizeCode(code, "bad_request");
        if ("scope_forbidden".equals(normalizedCode) || "forbidden".equals(normalizedCode)) {
            return ResponseEntity.status(403).body(errorBody(request, normalizedCode, msg(request, normalizedCode), null));
        }
        return ResponseEntity.badRequest().body(errorBody(request, normalizedCode, msg(request, normalizedCode), null));
    }

    private String normalizeId(String value) {
        return isBlank(value) ? "" : value.trim();
    }

    private String normalizeStatus(String value, String fallback) {
        String normalized = normalizeId(value);
        if (!normalized.isEmpty()) {
            return normalized.toUpperCase(Locale.ROOT);
        }
        String normalizedFallback = normalizeId(fallback);
        return normalizedFallback.isEmpty() ? "" : normalizedFallback.toUpperCase(Locale.ROOT);
    }
}
