package com.yao.crm.controller;

import com.yao.crm.dto.request.CreatePaymentRequest;
import com.yao.crm.dto.request.UpdatePaymentRequest;
import com.yao.crm.entity.ContractRecord;
import com.yao.crm.entity.Customer;
import com.yao.crm.entity.OrderRecord;
import com.yao.crm.entity.PaymentRecord;
import com.yao.crm.repository.ContractRecordRepository;
import com.yao.crm.repository.CustomerRepository;
import com.yao.crm.repository.OrderRecordRepository;
import com.yao.crm.repository.PaymentRecordRepository;
import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.I18nService;
import com.yao.crm.service.ValueNormalizerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.Predicate;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api")
public class PaymentController extends BaseApiController {

    private final PaymentRecordRepository paymentRepository;
    private final ContractRecordRepository contractRepository;
    private final CustomerRepository customerRepository;
    private final OrderRecordRepository orderRecordRepository;
    private final AuditLogService auditLogService;
    private final ValueNormalizerService valueNormalizerService;

    public PaymentController(PaymentRecordRepository paymentRepository,
                             ContractRecordRepository contractRepository,
                             CustomerRepository customerRepository,
                             OrderRecordRepository orderRecordRepository,
                             AuditLogService auditLogService,
                             ValueNormalizerService valueNormalizerService,
                             I18nService i18nService) {
        super(i18nService);
        this.paymentRepository = paymentRepository;
        this.contractRepository = contractRepository;
        this.customerRepository = customerRepository;
        this.orderRecordRepository = orderRecordRepository;
        this.auditLogService = auditLogService;
        this.valueNormalizerService = valueNormalizerService;
    }

    @GetMapping("/payments/search")
    public ResponseEntity<?> searchPayments(
            HttpServletRequest request,
            @RequestParam(defaultValue = "") String customerId,
            @RequestParam(defaultValue = "") String contractId,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "8") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES", "ANALYST")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }
        final String tenantId = currentTenant(request);

        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(50, size));
        Pageable pageable = buildPageable(
                safePage,
                safeSize,
                sortBy,
                sortDir,
                new HashSet<>(Set.of("customerId", "contractId", "orderId", "amount", "status", "method", "owner", "receivedDate", "createdAt", "updatedAt")),
                "updatedAt"
        );

        final boolean salesScoped = isSalesScoped(request);
        final String ownerScope = currentOwnerScope(request);
        Specification<PaymentRecord> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<Predicate>();
            predicates.add(cb.equal(root.get("tenantId"), tenantId));
            if (!isBlank(customerId)) {
                predicates.add(cb.equal(root.get("customerId"), customerId));
            }
            if (!isBlank(contractId)) {
                predicates.add(cb.equal(root.get("contractId"), contractId));
            }
            if (!isBlank(status)) {
                predicates.add(cb.equal(cb.lower(root.get("status")), valueNormalizerService.normalizePaymentStatus(status).toLowerCase(Locale.ROOT)));
            }
            if (salesScoped) {
                Predicate selfOwner = cb.equal(root.get("owner"), currentUser(request));
                Predicate scopeOwner = cb.equal(root.get("owner"), ownerScope);
                predicates.add(cb.or(selfOwner, scopeOwner));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };

        Page<PaymentRecord> result = paymentRepository.findAll(spec, pageable);
        Map<String, Object> body = new HashMap<>();
        body.put("items", result.getContent());
        body.put("total", result.getTotalElements());
        body.put("page", safePage);
        body.put("size", safeSize);
        body.put("totalPages", result.getTotalPages());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/payments")
    public ResponseEntity<?> createPayment(HttpServletRequest request, @Valid @RequestBody CreatePaymentRequest payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }
        String tenantId = currentTenant(request);

        Optional<ContractRecord> contractOpt = contractRepository.findByIdAndTenantId(payload.getContractId(), tenantId);
        if (!contractOpt.isPresent()) {
            return ResponseEntity.badRequest().body(legacyErrorByKey(request, "contract_not_found", "BAD_REQUEST", null));
        }

        ContractRecord contract = contractOpt.get();
        Optional<Customer> customerOpt = customerRepository.findByIdAndTenantId(contract.getCustomerId(), tenantId);
        if (!customerOpt.isPresent()) {
            return ResponseEntity.badRequest().body(legacyErrorByKey(request, "customer_not_found", "BAD_REQUEST", null));
        }
        if (isSalesScoped(request) && !ownerMatchesScope(request, customerOpt.get().getOwner())) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "scope_forbidden", "FORBIDDEN", null));
        }

        PaymentRecord payment = new PaymentRecord();
        payment.setId(newId("pm"));
        payment.setContractId(contract.getId());
        payment.setCustomerId(contract.getCustomerId());
        if (!isBlank(payload.getOrderId())) {
            Optional<OrderRecord> orderOpt = orderRecordRepository.findByIdAndTenantId(payload.getOrderId(), tenantId);
            if (!orderOpt.isPresent()) {
                return ResponseEntity.badRequest().body(legacyErrorByKey(request, "order_not_found", "BAD_REQUEST", null));
            }
            payment.setOrderId(orderOpt.get().getId());
        } else {
            payment.setOrderId(null);
        }
        payment.setTenantId(tenantId);
        payment.setAmount(payload.getAmount() == null ? 0L : payload.getAmount());
        if (!isBlank(payload.getReceivedDate())) {
            LocalDate parsed = parseDateOrNull(request, payload.getReceivedDate());
            if (parsed == null) {
                return ResponseEntity.badRequest().body(legacyErrorByKey(request, "invalid_date_format", "BAD_REQUEST", null));
            }
            payment.setReceivedDate(parsed);
        } else {
            payment.setReceivedDate(null);
        }
        if (isBlank(payload.getMethod())) {
            payment.setMethod("Bank");
        } else {
            if (!valueNormalizerService.isValidPaymentMethod(payload.getMethod())) {
                return ResponseEntity.badRequest().body(legacyErrorByKey(request, "invalid_payment_method", "BAD_REQUEST", null));
            }
            payment.setMethod(valueNormalizerService.normalizePaymentMethod(payload.getMethod()));
        }
        if (isBlank(payload.getStatus())) {
            payment.setStatus("Pending");
        } else {
            if (!valueNormalizerService.isValidPaymentStatus(payload.getStatus())) {
                return ResponseEntity.badRequest().body(legacyErrorByKey(request, "invalid_payment_status", "BAD_REQUEST", null));
            }
            payment.setStatus(valueNormalizerService.normalizePaymentStatus(payload.getStatus()));
        }
        payment.setRemark(payload.getRemark());
        if (isSalesScoped(request)) {
            payment.setOwner(currentOwnerScope(request));
        } else {
            payment.setOwner(customerOpt.get().getOwner());
        }

        PaymentRecord saved = paymentRepository.save(payment);
        auditLogService.record(currentUser(request), currentRole(request), "CREATE", "PAYMENT", saved.getId(), saved.getContractId());
        return ResponseEntity.status(201).body(saved);
    }

    @PatchMapping("/payments/{id}")
    public ResponseEntity<?> updatePayment(HttpServletRequest request, @PathVariable String id, @Valid @RequestBody UpdatePaymentRequest patch) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }
        String tenantId = currentTenant(request);

        Optional<PaymentRecord> optional = paymentRepository.findByIdAndTenantId(id, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(legacyErrorByKey(request, "payment_not_found", "NOT_FOUND", null));
        }

        PaymentRecord payment = optional.get();
        if (isSalesScoped(request) && !ownerMatchesScope(request, payment.getOwner())) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "scope_forbidden", "FORBIDDEN", null));
        }

        if (patch.getContractId() != null) {
            Optional<ContractRecord> contractOpt = contractRepository.findByIdAndTenantId(patch.getContractId(), tenantId);
            if (!contractOpt.isPresent()) {
                return ResponseEntity.badRequest().body(legacyErrorByKey(request, "contract_not_found", "BAD_REQUEST", null));
            }
            ContractRecord contract = contractOpt.get();
            Optional<Customer> customerOpt = customerRepository.findByIdAndTenantId(contract.getCustomerId(), tenantId);
            if (!customerOpt.isPresent()) {
                return ResponseEntity.badRequest().body(legacyErrorByKey(request, "customer_not_found", "BAD_REQUEST", null));
            }
            if (isSalesScoped(request) && !ownerMatchesScope(request, customerOpt.get().getOwner())) {
                return ResponseEntity.status(403).body(legacyErrorByKey(request, "scope_forbidden", "FORBIDDEN", null));
            }
            payment.setContractId(contract.getId());
            payment.setCustomerId(contract.getCustomerId());
            if (isSalesScoped(request)) {
                payment.setOwner(currentOwnerScope(request));
            } else {
                payment.setOwner(customerOpt.get().getOwner());
            }
        }
        if (patch.getOrderId() != null) {
            if (isBlank(patch.getOrderId())) {
                payment.setOrderId(null);
            } else {
                Optional<OrderRecord> orderOpt = orderRecordRepository.findByIdAndTenantId(patch.getOrderId(), tenantId);
                if (!orderOpt.isPresent()) {
                    return ResponseEntity.badRequest().body(legacyErrorByKey(request, "order_not_found", "BAD_REQUEST", null));
                }
                payment.setOrderId(orderOpt.get().getId());
            }
        }

        if (patch.getAmount() != null) payment.setAmount(patch.getAmount());
        if (patch.getReceivedDate() != null) {
            if (isBlank(patch.getReceivedDate())) {
                payment.setReceivedDate(null);
            } else {
                LocalDate parsed = parseDateOrNull(request, patch.getReceivedDate());
                if (parsed == null) {
                    return ResponseEntity.badRequest().body(legacyErrorByKey(request, "invalid_date_format", "BAD_REQUEST", null));
                }
                payment.setReceivedDate(parsed);
            }
        }
        if (patch.getMethod() != null) {
            if (!valueNormalizerService.isValidPaymentMethod(patch.getMethod())) {
                return ResponseEntity.badRequest().body(legacyErrorByKey(request, "invalid_payment_method", "BAD_REQUEST", null));
            }
            payment.setMethod(valueNormalizerService.normalizePaymentMethod(patch.getMethod()));
        }
        if (patch.getStatus() != null) {
            if (!valueNormalizerService.isValidPaymentStatus(patch.getStatus())) {
                return ResponseEntity.badRequest().body(legacyErrorByKey(request, "invalid_payment_status", "BAD_REQUEST", null));
            }
            payment.setStatus(valueNormalizerService.normalizePaymentStatus(patch.getStatus()));
        }
        if (patch.getRemark() != null) payment.setRemark(patch.getRemark());

        PaymentRecord saved = paymentRepository.save(payment);
        auditLogService.record(currentUser(request), currentRole(request), "UPDATE", "PAYMENT", saved.getId(), "Updated payment");
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/payments/{id}")
    public ResponseEntity<?> deletePayment(HttpServletRequest request, @PathVariable String id) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }
        String tenantId = currentTenant(request);

        if (!paymentRepository.existsByIdAndTenantId(id, tenantId)) {
            return ResponseEntity.status(404).body(legacyErrorByKey(request, "payment_not_found", "NOT_FOUND", null));
        }

        paymentRepository.deleteById(id);
        auditLogService.record(currentUser(request), currentRole(request), "DELETE", "PAYMENT", id, "Deleted payment");
        return ResponseEntity.noContent().build();
    }

    private String newId(String prefix) {
        return prefix + "_" + Long.toString(System.currentTimeMillis(), 36) + String.format("%03d", (int) (Math.random() * 1000));
    }

    private LocalDate parseDateOrNull(HttpServletRequest request, String value) {
        return parseLocalDate(request, value);
    }
}


