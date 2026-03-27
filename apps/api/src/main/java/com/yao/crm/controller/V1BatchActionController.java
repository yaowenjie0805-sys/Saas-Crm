package com.yao.crm.controller;

import com.yao.crm.dto.request.BatchActionRequest;
import com.yao.crm.entity.ContractRecord;
import com.yao.crm.entity.Customer;
import com.yao.crm.entity.Opportunity;
import com.yao.crm.entity.OrderRecord;
import com.yao.crm.entity.PaymentRecord;
import com.yao.crm.entity.Product;
import com.yao.crm.entity.Quote;
import com.yao.crm.repository.ContractRecordRepository;
import com.yao.crm.repository.CustomerRepository;
import com.yao.crm.repository.OpportunityRepository;
import com.yao.crm.repository.OrderRecordRepository;
import com.yao.crm.repository.PaymentRecordRepository;
import com.yao.crm.repository.ProductRepository;
import com.yao.crm.repository.QuoteRepository;
import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.I18nService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
public class V1BatchActionController extends BaseApiController {

    private static final int MAX_BATCH_SIZE = 100;
    private static final String ACTION_DELETE = "DELETE";
    private static final String ACTION_UPDATE_STATUS = "UPDATE_STATUS";
    private static final String ACTION_ASSIGN_OWNER = "ASSIGN_OWNER";

    private final CustomerRepository customerRepository;
    private final OpportunityRepository opportunityRepository;
    private final ProductRepository productRepository;
    private final QuoteRepository quoteRepository;
    private final OrderRecordRepository orderRecordRepository;
    private final ContractRecordRepository contractRecordRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final AuditLogService auditLogService;

    public V1BatchActionController(CustomerRepository customerRepository,
                                   OpportunityRepository opportunityRepository,
                                   ProductRepository productRepository,
                                   QuoteRepository quoteRepository,
                                   OrderRecordRepository orderRecordRepository,
                                   ContractRecordRepository contractRecordRepository,
                                   PaymentRecordRepository paymentRecordRepository,
                                   AuditLogService auditLogService,
                                   I18nService i18nService) {
        super(i18nService);
        this.customerRepository = customerRepository;
        this.opportunityRepository = opportunityRepository;
        this.productRepository = productRepository;
        this.quoteRepository = quoteRepository;
        this.orderRecordRepository = orderRecordRepository;
        this.contractRecordRepository = contractRecordRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/customers/batch-actions")
    public ResponseEntity<?> batchCustomers(HttpServletRequest request, @Valid @RequestBody BatchActionRequest payload) {
        String tenantId = currentTenant(request);
        String action = normalizeAction(payload.getAction());
        List<String> ids = payload.getIds();
        if (!validateBatchRequest(action, ids)) return validationResponse(request, action, ids);
        MutableSummary summary = new MutableSummary(ids.size());
        String statusValue = payload.getStatus();
        String ownerValue = payload.getOwner();
        Map<String, Customer> rowsById = new LinkedHashMap<String, Customer>();
        for (Customer row : customerRepository.findByTenantIdAndIdIn(tenantId, ids)) {
            rowsById.put(row.getId(), row);
        }
        for (String id : ids) {
            Customer row = rowsById.get(id);
            if (row == null) {
                summary.notFound(id, msg(request, "customer_not_found"), traceId(request));
                continue;
            }
            if (isSalesScoped(request) && !ownerMatchesScope(request, row.getOwner())) {
                summary.forbidden(id, msg(request, "scope_forbidden"), traceId(request));
                continue;
            }
            if (ACTION_DELETE.equals(action)) {
                if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
                    summary.forbidden(id, msg(request, "forbidden"), traceId(request));
                    continue;
                }
                customerRepository.delete(row);
                auditLogService.record(currentUser(request), currentRole(request), "DELETE", "CUSTOMER", row.getId(), "Batch delete customer", tenantId);
                summary.succeeded++;
                continue;
            }
            if (ACTION_UPDATE_STATUS.equals(action)) {
                if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
                    summary.forbidden(id, msg(request, "forbidden"), traceId(request));
                    continue;
                }
                if (isBlank(statusValue)) {
                    summary.skipped(id, msg(request, "invalid_customer_status"), traceId(request));
                    continue;
                }
                row.setStatus(statusValue.trim());
                customerRepository.save(row);
                auditLogService.record(currentUser(request), currentRole(request), "UPDATE", "CUSTOMER", row.getId(), "Batch update customer status", tenantId);
                summary.succeeded++;
                continue;
            }
            if (ACTION_ASSIGN_OWNER.equals(action)) {
                if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
                    summary.forbidden(id, msg(request, "forbidden"), traceId(request));
                    continue;
                }
                if (isBlank(ownerValue)) {
                    summary.skipped(id, msg(request, "bad_request"), traceId(request));
                    continue;
                }
                if (isSalesScoped(request) && !ownerMatchesScope(request, ownerValue)) {
                    summary.forbidden(id, msg(request, "scope_forbidden"), traceId(request));
                    continue;
                }
                row.setOwner(ownerValue.trim());
                customerRepository.save(row);
                auditLogService.record(currentUser(request), currentRole(request), "UPDATE", "CUSTOMER", row.getId(), "Batch assign customer owner", tenantId);
                summary.succeeded++;
                continue;
            }
            summary.skipped(id, msg(request, "bad_request"), traceId(request));
        }
        return ResponseEntity.ok(successWithFields(request, "batch_action_completed", summary.toBody()));
    }

    @PostMapping("/opportunities/batch-actions")
    public ResponseEntity<?> batchOpportunities(HttpServletRequest request, @Valid @RequestBody BatchActionRequest payload) {
        String tenantId = currentTenant(request);
        String action = normalizeAction(payload.getAction());
        List<String> ids = payload.getIds();
        if (!validateBatchRequest(action, ids)) return validationResponse(request, action, ids);
        MutableSummary summary = new MutableSummary(ids.size());
        String stageValue = payload.getStatus();
        if (isBlank(stageValue)) stageValue = payload.getStage();
        String ownerValue = payload.getOwner();
        Map<String, Opportunity> rowsById = new LinkedHashMap<String, Opportunity>();
        for (Opportunity row : opportunityRepository.findByTenantIdAndIdIn(tenantId, ids)) {
            rowsById.put(row.getId(), row);
        }
        for (String id : ids) {
            Opportunity row = rowsById.get(id);
            if (row == null) {
                summary.notFound(id, msg(request, "opportunity_not_found"), traceId(request));
                continue;
            }
            if (isSalesScoped(request) && !ownerMatchesScope(request, row.getOwner())) {
                summary.forbidden(id, msg(request, "scope_forbidden"), traceId(request));
                continue;
            }
            if (ACTION_DELETE.equals(action)) {
                if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
                    summary.forbidden(id, msg(request, "forbidden"), traceId(request));
                    continue;
                }
                opportunityRepository.delete(row);
                auditLogService.record(currentUser(request), currentRole(request), "DELETE", "OPPORTUNITY", row.getId(), "Batch delete opportunity", tenantId);
                summary.succeeded++;
                continue;
            }
            if (ACTION_UPDATE_STATUS.equals(action)) {
                if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
                    summary.forbidden(id, msg(request, "forbidden"), traceId(request));
                    continue;
                }
                if (isBlank(stageValue)) {
                    summary.skipped(id, msg(request, "invalid_opportunity_stage"), traceId(request));
                    continue;
                }
                row.setStage(stageValue.trim());
                opportunityRepository.save(row);
                auditLogService.record(currentUser(request), currentRole(request), "UPDATE", "OPPORTUNITY", row.getId(), "Batch update opportunity stage", tenantId);
                summary.succeeded++;
                continue;
            }
            if (ACTION_ASSIGN_OWNER.equals(action)) {
                if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
                    summary.forbidden(id, msg(request, "forbidden"), traceId(request));
                    continue;
                }
                if (isBlank(ownerValue)) {
                    summary.skipped(id, msg(request, "bad_request"), traceId(request));
                    continue;
                }
                if (isSalesScoped(request) && !ownerMatchesScope(request, ownerValue)) {
                    summary.forbidden(id, msg(request, "scope_forbidden"), traceId(request));
                    continue;
                }
                row.setOwner(ownerValue.trim());
                opportunityRepository.save(row);
                auditLogService.record(currentUser(request), currentRole(request), "UPDATE", "OPPORTUNITY", row.getId(), "Batch assign opportunity owner", tenantId);
                summary.succeeded++;
                continue;
            }
            summary.skipped(id, msg(request, "bad_request"), traceId(request));
        }
        return ResponseEntity.ok(successWithFields(request, "batch_action_completed", summary.toBody()));
    }

    @PostMapping("/products/batch-actions")
    public ResponseEntity<?> batchProducts(HttpServletRequest request, @Valid @RequestBody BatchActionRequest payload) {
        String tenantId = currentTenant(request);
        String action = normalizeAction(payload.getAction());
        List<String> ids = payload.getIds();
        if (!validateBatchRequest(action, ids)) return validationResponse(request, action, ids);
        MutableSummary summary = new MutableSummary(ids.size());
        String statusValue = payload.getStatus();
        Map<String, Product> rowsById = new LinkedHashMap<String, Product>();
        for (Product row : productRepository.findByTenantIdAndIdIn(tenantId, ids)) {
            rowsById.put(row.getId(), row);
        }
        for (String id : ids) {
            Product row = rowsById.get(id);
            if (row == null) {
                summary.notFound(id, msg(request, "product_not_found"), traceId(request));
                continue;
            }
            if (ACTION_DELETE.equals(action)) {
                if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
                    summary.forbidden(id, msg(request, "forbidden"), traceId(request));
                    continue;
                }
                productRepository.delete(row);
                auditLogService.record(currentUser(request), currentRole(request), "DELETE", "PRODUCT", row.getId(), "Batch delete product", tenantId);
                summary.succeeded++;
                continue;
            }
            if (ACTION_UPDATE_STATUS.equals(action)) {
                if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
                    summary.forbidden(id, msg(request, "forbidden"), traceId(request));
                    continue;
                }
                if (isBlank(statusValue)) {
                    summary.skipped(id, msg(request, "product_status_invalid"), traceId(request));
                    continue;
                }
                row.setStatus(statusValue.trim().toUpperCase(Locale.ROOT));
                productRepository.save(row);
                auditLogService.record(currentUser(request), currentRole(request), "UPDATE", "PRODUCT", row.getId(), "Batch update product status", tenantId);
                summary.succeeded++;
                continue;
            }
            summary.skipped(id, msg(request, "batch_action_not_supported"), traceId(request));
        }
        return ResponseEntity.ok(successWithFields(request, "batch_action_completed", summary.toBody()));
    }

    @PostMapping("/quotes/batch-actions")
    public ResponseEntity<?> batchQuotes(HttpServletRequest request, @Valid @RequestBody BatchActionRequest payload) {
        String tenantId = currentTenant(request);
        String action = normalizeAction(payload.getAction());
        List<String> ids = payload.getIds();
        if (!validateBatchRequest(action, ids)) return validationResponse(request, action, ids);
        MutableSummary summary = new MutableSummary(ids.size());
        String statusValue = payload.getStatus();
        String ownerValue = payload.getOwner();
        Map<String, Quote> rowsById = new LinkedHashMap<String, Quote>();
        for (Quote row : quoteRepository.findByTenantIdAndIdIn(tenantId, ids)) {
            rowsById.put(row.getId(), row);
        }
        for (String id : ids) {
            Quote row = rowsById.get(id);
            if (row == null) {
                summary.notFound(id, msg(request, "quote_not_found"), traceId(request));
                continue;
            }
            if (isSalesScoped(request) && !ownerMatchesScope(request, row.getOwner())) {
                summary.forbidden(id, msg(request, "scope_forbidden"), traceId(request));
                continue;
            }
            if (ACTION_DELETE.equals(action)) {
                if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
                    summary.forbidden(id, msg(request, "forbidden"), traceId(request));
                    continue;
                }
                quoteRepository.delete(row);
                auditLogService.record(currentUser(request), currentRole(request), "DELETE", "QUOTE", row.getId(), "Batch delete quote", tenantId);
                summary.succeeded++;
                continue;
            }
            if (ACTION_UPDATE_STATUS.equals(action)) {
                if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
                    summary.forbidden(id, msg(request, "forbidden"), traceId(request));
                    continue;
                }
                if (isBlank(statusValue)) {
                    summary.skipped(id, msg(request, "quote_status_invalid"), traceId(request));
                    continue;
                }
                row.setStatus(statusValue.trim().toUpperCase(Locale.ROOT));
                quoteRepository.save(row);
                auditLogService.record(currentUser(request), currentRole(request), "UPDATE", "QUOTE", row.getId(), "Batch update quote status", tenantId);
                summary.succeeded++;
                continue;
            }
            if (ACTION_ASSIGN_OWNER.equals(action)) {
                if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
                    summary.forbidden(id, msg(request, "forbidden"), traceId(request));
                    continue;
                }
                if (isBlank(ownerValue)) {
                    summary.skipped(id, msg(request, "bad_request"), traceId(request));
                    continue;
                }
                if (isSalesScoped(request) && !ownerMatchesScope(request, ownerValue)) {
                    summary.forbidden(id, msg(request, "scope_forbidden"), traceId(request));
                    continue;
                }
                row.setOwner(ownerValue.trim());
                quoteRepository.save(row);
                auditLogService.record(currentUser(request), currentRole(request), "UPDATE", "QUOTE", row.getId(), "Batch assign quote owner", tenantId);
                summary.succeeded++;
                continue;
            }
            summary.skipped(id, msg(request, "bad_request"), traceId(request));
        }
        return ResponseEntity.ok(successWithFields(request, "batch_action_completed", summary.toBody()));
    }

    @PostMapping("/orders/batch-actions")
    public ResponseEntity<?> batchOrders(HttpServletRequest request, @Valid @RequestBody BatchActionRequest payload) {
        String tenantId = currentTenant(request);
        String action = normalizeAction(payload.getAction());
        List<String> ids = payload.getIds();
        if (!validateBatchRequest(action, ids)) return validationResponse(request, action, ids);
        MutableSummary summary = new MutableSummary(ids.size());
        String statusValue = payload.getStatus();
        String ownerValue = payload.getOwner();
        Map<String, OrderRecord> rowsById = new LinkedHashMap<String, OrderRecord>();
        for (OrderRecord row : orderRecordRepository.findByTenantIdAndIdIn(tenantId, ids)) {
            rowsById.put(row.getId(), row);
        }
        for (String id : ids) {
            OrderRecord row = rowsById.get(id);
            if (row == null) {
                summary.notFound(id, msg(request, "order_not_found"), traceId(request));
                continue;
            }
            if (isSalesScoped(request) && !ownerMatchesScope(request, row.getOwner())) {
                summary.forbidden(id, msg(request, "scope_forbidden"), traceId(request));
                continue;
            }
            if (ACTION_DELETE.equals(action)) {
                if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
                    summary.forbidden(id, msg(request, "forbidden"), traceId(request));
                    continue;
                }
                orderRecordRepository.delete(row);
                auditLogService.record(currentUser(request), currentRole(request), "DELETE", "ORDER", row.getId(), "Batch delete order", tenantId);
                summary.succeeded++;
                continue;
            }
            if (ACTION_UPDATE_STATUS.equals(action)) {
                if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
                    summary.forbidden(id, msg(request, "forbidden"), traceId(request));
                    continue;
                }
                if (isBlank(statusValue)) {
                    summary.skipped(id, msg(request, "order_status_invalid"), traceId(request));
                    continue;
                }
                row.setStatus(statusValue.trim().toUpperCase(Locale.ROOT));
                orderRecordRepository.save(row);
                auditLogService.record(currentUser(request), currentRole(request), "UPDATE", "ORDER", row.getId(), "Batch update order status", tenantId);
                summary.succeeded++;
                continue;
            }
            if (ACTION_ASSIGN_OWNER.equals(action)) {
                if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
                    summary.forbidden(id, msg(request, "forbidden"), traceId(request));
                    continue;
                }
                if (isBlank(ownerValue)) {
                    summary.skipped(id, msg(request, "bad_request"), traceId(request));
                    continue;
                }
                if (isSalesScoped(request) && !ownerMatchesScope(request, ownerValue)) {
                    summary.forbidden(id, msg(request, "scope_forbidden"), traceId(request));
                    continue;
                }
                row.setOwner(ownerValue.trim());
                orderRecordRepository.save(row);
                auditLogService.record(currentUser(request), currentRole(request), "UPDATE", "ORDER", row.getId(), "Batch assign order owner", tenantId);
                summary.succeeded++;
                continue;
            }
            summary.skipped(id, msg(request, "bad_request"), traceId(request));
        }
        return ResponseEntity.ok(successWithFields(request, "batch_action_completed", summary.toBody()));
    }

    @PostMapping("/contracts/batch-actions")
    public ResponseEntity<?> batchContracts(HttpServletRequest request, @Valid @RequestBody BatchActionRequest payload) {
        String tenantId = currentTenant(request);
        String action = normalizeAction(payload.getAction());
        List<String> ids = payload.getIds();
        if (!validateBatchRequest(action, ids)) return validationResponse(request, action, ids);
        MutableSummary summary = new MutableSummary(ids.size());
        String statusValue = payload.getStatus();
        String ownerValue = payload.getOwner();
        Map<String, ContractRecord> rowsById = new LinkedHashMap<String, ContractRecord>();
        for (ContractRecord row : contractRecordRepository.findByTenantIdAndIdIn(tenantId, ids)) {
            rowsById.put(row.getId(), row);
        }
        for (String id : ids) {
            ContractRecord row = rowsById.get(id);
            if (row == null) {
                summary.notFound(id, msg(request, "contract_not_found"), traceId(request));
                continue;
            }
            if (isSalesScoped(request) && !ownerMatchesScope(request, row.getOwner())) {
                summary.forbidden(id, msg(request, "scope_forbidden"), traceId(request));
                continue;
            }
            if (ACTION_DELETE.equals(action)) {
                if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
                    summary.forbidden(id, msg(request, "forbidden"), traceId(request));
                    continue;
                }
                contractRecordRepository.delete(row);
                auditLogService.record(currentUser(request), currentRole(request), "DELETE", "CONTRACT", row.getId(), "Batch delete contract", tenantId);
                summary.succeeded++;
                continue;
            }
            if (ACTION_UPDATE_STATUS.equals(action)) {
                if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
                    summary.forbidden(id, msg(request, "forbidden"), traceId(request));
                    continue;
                }
                if (isBlank(statusValue)) {
                    summary.skipped(id, msg(request, "invalid_contract_status"), traceId(request));
                    continue;
                }
                row.setStatus(statusValue.trim());
                contractRecordRepository.save(row);
                auditLogService.record(currentUser(request), currentRole(request), "UPDATE", "CONTRACT", row.getId(), "Batch update contract status", tenantId);
                summary.succeeded++;
                continue;
            }
            if (ACTION_ASSIGN_OWNER.equals(action)) {
                if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
                    summary.forbidden(id, msg(request, "forbidden"), traceId(request));
                    continue;
                }
                if (isBlank(ownerValue)) {
                    summary.skipped(id, msg(request, "bad_request"), traceId(request));
                    continue;
                }
                if (isSalesScoped(request) && !ownerMatchesScope(request, ownerValue)) {
                    summary.forbidden(id, msg(request, "scope_forbidden"), traceId(request));
                    continue;
                }
                row.setOwner(ownerValue.trim());
                contractRecordRepository.save(row);
                auditLogService.record(currentUser(request), currentRole(request), "UPDATE", "CONTRACT", row.getId(), "Batch assign contract owner", tenantId);
                summary.succeeded++;
                continue;
            }
            summary.skipped(id, msg(request, "bad_request"), traceId(request));
        }
        return ResponseEntity.ok(successWithFields(request, "batch_action_completed", summary.toBody()));
    }

    @PostMapping("/payments/batch-actions")
    public ResponseEntity<?> batchPayments(HttpServletRequest request, @Valid @RequestBody BatchActionRequest payload) {
        String tenantId = currentTenant(request);
        String action = normalizeAction(payload.getAction());
        List<String> ids = payload.getIds();
        if (!validateBatchRequest(action, ids)) return validationResponse(request, action, ids);
        MutableSummary summary = new MutableSummary(ids.size());
        String statusValue = payload.getStatus();
        String ownerValue = payload.getOwner();
        Map<String, PaymentRecord> rowsById = new LinkedHashMap<String, PaymentRecord>();
        for (PaymentRecord row : paymentRecordRepository.findByTenantIdAndIdIn(tenantId, ids)) {
            rowsById.put(row.getId(), row);
        }
        for (String id : ids) {
            PaymentRecord row = rowsById.get(id);
            if (row == null) {
                summary.notFound(id, msg(request, "payment_not_found"), traceId(request));
                continue;
            }
            if (isSalesScoped(request) && !ownerMatchesScope(request, row.getOwner())) {
                summary.forbidden(id, msg(request, "scope_forbidden"), traceId(request));
                continue;
            }
            if (ACTION_DELETE.equals(action)) {
                if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
                    summary.forbidden(id, msg(request, "forbidden"), traceId(request));
                    continue;
                }
                paymentRecordRepository.delete(row);
                auditLogService.record(currentUser(request), currentRole(request), "DELETE", "PAYMENT", row.getId(), "Batch delete payment", tenantId);
                summary.succeeded++;
                continue;
            }
            if (ACTION_UPDATE_STATUS.equals(action)) {
                if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
                    summary.forbidden(id, msg(request, "forbidden"), traceId(request));
                    continue;
                }
                if (isBlank(statusValue)) {
                    summary.skipped(id, msg(request, "invalid_payment_status"), traceId(request));
                    continue;
                }
                row.setStatus(statusValue.trim());
                paymentRecordRepository.save(row);
                auditLogService.record(currentUser(request), currentRole(request), "UPDATE", "PAYMENT", row.getId(), "Batch update payment status", tenantId);
                summary.succeeded++;
                continue;
            }
            if (ACTION_ASSIGN_OWNER.equals(action)) {
                if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
                    summary.forbidden(id, msg(request, "forbidden"), traceId(request));
                    continue;
                }
                if (isBlank(ownerValue)) {
                    summary.skipped(id, msg(request, "bad_request"), traceId(request));
                    continue;
                }
                if (isSalesScoped(request) && !ownerMatchesScope(request, ownerValue)) {
                    summary.forbidden(id, msg(request, "scope_forbidden"), traceId(request));
                    continue;
                }
                row.setOwner(ownerValue.trim());
                paymentRecordRepository.save(row);
                auditLogService.record(currentUser(request), currentRole(request), "UPDATE", "PAYMENT", row.getId(), "Batch assign payment owner", tenantId);
                summary.succeeded++;
                continue;
            }
            summary.skipped(id, msg(request, "bad_request"), traceId(request));
        }
        return ResponseEntity.ok(successWithFields(request, "batch_action_completed", summary.toBody()));
    }

    private boolean validateBatchRequest(String action, List<String> ids) {
        if (isBlank(action) || ids == null || ids.isEmpty()) return false;
        if (!Arrays.asList(ACTION_DELETE, ACTION_UPDATE_STATUS, ACTION_ASSIGN_OWNER).contains(action)) return false;
        return ids.size() <= MAX_BATCH_SIZE;
    }

    private ResponseEntity<?> validationResponse(HttpServletRequest request, String action, List<String> ids) {
        if (ids != null && ids.size() > MAX_BATCH_SIZE) {
            Map<String, Object> details = new LinkedHashMap<String, Object>();
            details.put("maxBatchSize", MAX_BATCH_SIZE);
            return ResponseEntity.badRequest().body(errorBody(request, "batch_limit_exceeded", msg(request, "batch_limit_exceeded"), details));
        }
        if (isBlank(action)) {
            return ResponseEntity.badRequest().body(errorBody(request, "batch_action_required", msg(request, "batch_action_required"), null));
        }
        if (!Arrays.asList(ACTION_DELETE, ACTION_UPDATE_STATUS, ACTION_ASSIGN_OWNER).contains(action)) {
            return ResponseEntity.badRequest().body(errorBody(request, "batch_action_invalid", msg(request, "batch_action_invalid"), null));
        }
        return ResponseEntity.badRequest().body(errorBody(request, "batch_ids_required", msg(request, "batch_ids_required"), null));
    }

    private String normalizeAction(Object value) {
        return str(value).trim().toUpperCase(Locale.ROOT);
    }

    private List<String> parseIds(Object raw) {
        List<String> ids = new ArrayList<String>();
        if (!(raw instanceof List)) return ids;
        @SuppressWarnings("unchecked")
        List<Object> src = (List<Object>) raw;
        for (Object one : src) {
            String id = str(one).trim();
            if (!id.isEmpty()) ids.add(id);
        }
        return ids;
    }

    private String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static class MutableSummary {
        private final int requested;
        private int succeeded;
        private int skipped;
        private int notFound;
        private int forbidden;
        private final List<Map<String, Object>> failures = new ArrayList<Map<String, Object>>();

        private MutableSummary(int requested) {
            this.requested = requested;
        }

        private void skipped(String id, String message, String requestId) {
            skipped++;
            failures.add(failure(id, message, requestId));
        }

        private void notFound(String id, String message, String requestId) {
            notFound++;
            failures.add(failure(id, message, requestId));
        }

        private void forbidden(String id, String message, String requestId) {
            forbidden++;
            failures.add(failure(id, message, requestId));
        }

        private Map<String, Object> failure(String id, String message, String requestId) {
            Map<String, Object> out = new LinkedHashMap<String, Object>();
            out.put("id", id);
            out.put("message", message);
            out.put("requestId", requestId);
            return out;
        }

        private Map<String, Object> toBody() {
            Map<String, Object> body = new LinkedHashMap<String, Object>();
            body.put("requested", requested);
            body.put("succeeded", succeeded);
            body.put("failed", failures.size());
            body.put("skipped", skipped);
            body.put("notFound", notFound);
            body.put("forbidden", forbidden);
            body.put("failures", failures);
            return body;
        }
    }
}
