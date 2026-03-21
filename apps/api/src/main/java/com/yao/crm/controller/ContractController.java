package com.yao.crm.controller;

import com.yao.crm.dto.request.CreateContractRequest;
import com.yao.crm.dto.request.UpdateContractRequest;
import com.yao.crm.entity.ContractRecord;
import com.yao.crm.entity.Customer;
import com.yao.crm.repository.ContractRecordRepository;
import com.yao.crm.repository.CustomerRepository;
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
public class ContractController extends BaseApiController {

    private final ContractRecordRepository contractRepository;
    private final CustomerRepository customerRepository;
    private final AuditLogService auditLogService;
    private final ValueNormalizerService valueNormalizerService;

    public ContractController(ContractRecordRepository contractRepository,
                              CustomerRepository customerRepository,
                              AuditLogService auditLogService,
                              ValueNormalizerService valueNormalizerService,
                              I18nService i18nService) {
        super(i18nService);
        this.contractRepository = contractRepository;
        this.customerRepository = customerRepository;
        this.auditLogService = auditLogService;
        this.valueNormalizerService = valueNormalizerService;
    }

    @GetMapping("/contracts/search")
    public ResponseEntity<?> searchContracts(
            HttpServletRequest request,
            @RequestParam(defaultValue = "") String customerId,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "8") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES", "ANALYST")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }

        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(50, size));
        Pageable pageable = buildPageable(
                safePage,
                safeSize,
                sortBy,
                sortDir,
                new HashSet<String>(Arrays.asList("customerId", "contractNo", "title", "amount", "status", "owner", "signDate", "createdAt", "updatedAt")),
                "updatedAt"
        );

        final boolean salesScoped = isSalesScoped(request);
        final String ownerScope = currentOwnerScope(request);
        final String tenantId = currentTenant(request);
        Specification<ContractRecord> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<Predicate>();
            predicates.add(cb.equal(root.get("tenantId"), tenantId));
            if (!isBlank(customerId)) {
                predicates.add(cb.equal(root.get("customerId"), customerId));
            }
            if (!isBlank(status)) {
                predicates.add(cb.equal(cb.lower(root.get("status")), valueNormalizerService.normalizeContractStatus(status).toLowerCase(Locale.ROOT)));
            }
            if (!isBlank(q)) {
                String pattern = "%" + escapeLike(q.trim().toLowerCase(Locale.ROOT)) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("contractNo")), pattern, '\\'),
                        cb.like(cb.lower(root.get("title")), pattern, '\\')
                ));
            }
            if (salesScoped) {
                Predicate selfOwner = cb.equal(root.get("owner"), currentUser(request));
                Predicate scopeOwner = cb.equal(root.get("owner"), ownerScope);
                predicates.add(cb.or(selfOwner, scopeOwner));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<ContractRecord> result = contractRepository.findAll(spec, pageable);
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("items", result.getContent());
        body.put("total", result.getTotalElements());
        body.put("page", safePage);
        body.put("size", safeSize);
        body.put("totalPages", result.getTotalPages());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/contracts")
    public ResponseEntity<?> createContract(HttpServletRequest request, @Valid @RequestBody CreateContractRequest payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }

        String tenantId = currentTenant(request);
        Optional<Customer> customer = customerRepository.findByIdAndTenantId(payload.getCustomerId(), tenantId);
        if (!customer.isPresent()) {
            return ResponseEntity.badRequest().body(legacyErrorByKey(request, "customer_not_found", "BAD_REQUEST", null));
        }
        if (isSalesScoped(request) && !ownerMatchesScope(request, customer.get().getOwner())) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "scope_forbidden", "FORBIDDEN", null));
        }

        ContractRecord contract = new ContractRecord();
        contract.setId(newId("cr"));
        contract.setTenantId(tenantId);
        contract.setCustomerId(payload.getCustomerId());
        contract.setContractNo(isBlank(payload.getContractNo()) ? newContractNo() : payload.getContractNo().trim());
        contract.setTitle(payload.getTitle());
        contract.setAmount(payload.getAmount() == null ? 0L : payload.getAmount());
        if (!valueNormalizerService.isValidContractStatus(payload.getStatus())) {
            return ResponseEntity.badRequest().body(legacyErrorByKey(request, "invalid_contract_status", "BAD_REQUEST", null));
        }
        contract.setStatus(valueNormalizerService.normalizeContractStatus(payload.getStatus()));
        if (!isBlank(payload.getSignDate())) {
            LocalDate parsed = parseDateOrNull(request, payload.getSignDate());
            if (parsed == null) {
                return ResponseEntity.badRequest().body(legacyErrorByKey(request, "invalid_date_format", "BAD_REQUEST", null));
            }
            contract.setSignDate(parsed);
        } else {
            contract.setSignDate(null);
        }
        if (isSalesScoped(request)) {
            contract.setOwner(currentOwnerScope(request));
        } else {
            contract.setOwner(isBlank(payload.getOwner()) ? customer.get().getOwner() : payload.getOwner());
        }

        ContractRecord saved = contractRepository.save(contract);
        auditLogService.record(currentUser(request), currentRole(request), "CREATE", "CONTRACT", saved.getId(), saved.getContractNo());
        return ResponseEntity.status(201).body(saved);
    }

    @PatchMapping("/contracts/{id}")
    public ResponseEntity<?> updateContract(HttpServletRequest request, @PathVariable String id, @Valid @RequestBody UpdateContractRequest patch) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }

        String tenantId = currentTenant(request);
        Optional<ContractRecord> optional = contractRepository.findByIdAndTenantId(id, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(legacyErrorByKey(request, "contract_not_found", "NOT_FOUND", null));
        }
        ContractRecord contract = optional.get();
        if (isSalesScoped(request) && !ownerMatchesScope(request, contract.getOwner())) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "scope_forbidden", "FORBIDDEN", null));
        }

        if (patch.getCustomerId() != null) {
            Optional<Customer> customer = customerRepository.findByIdAndTenantId(patch.getCustomerId(), tenantId);
            if (!customer.isPresent()) {
                return ResponseEntity.badRequest().body(legacyErrorByKey(request, "customer_not_found", "BAD_REQUEST", null));
            }
            if (isSalesScoped(request) && !ownerMatchesScope(request, customer.get().getOwner())) {
                return ResponseEntity.status(403).body(legacyErrorByKey(request, "scope_forbidden", "FORBIDDEN", null));
            }
            contract.setCustomerId(patch.getCustomerId());
            if (isSalesScoped(request)) {
                contract.setOwner(currentOwnerScope(request));
            }
        }

        if (patch.getContractNo() != null) contract.setContractNo(patch.getContractNo());
        if (patch.getTitle() != null) contract.setTitle(patch.getTitle());
        if (patch.getAmount() != null) contract.setAmount(patch.getAmount());
        if (patch.getStatus() != null) {
            if (!valueNormalizerService.isValidContractStatus(patch.getStatus())) {
                return ResponseEntity.badRequest().body(legacyErrorByKey(request, "invalid_contract_status", "BAD_REQUEST", null));
            }
            contract.setStatus(valueNormalizerService.normalizeContractStatus(patch.getStatus()));
        }
        if (patch.getSignDate() != null) {
            if (isBlank(patch.getSignDate())) {
                contract.setSignDate(null);
            } else {
                LocalDate parsed = parseDateOrNull(request, patch.getSignDate());
                if (parsed == null) {
                    return ResponseEntity.badRequest().body(legacyErrorByKey(request, "invalid_date_format", "BAD_REQUEST", null));
                }
                contract.setSignDate(parsed);
            }
        }
        if (patch.getOwner() != null && !isSalesScoped(request)) contract.setOwner(patch.getOwner());

        ContractRecord saved = contractRepository.save(contract);
        auditLogService.record(currentUser(request), currentRole(request), "UPDATE", "CONTRACT", saved.getId(), "Updated contract");
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/contracts/{id}")
    public ResponseEntity<?> deleteContract(HttpServletRequest request, @PathVariable String id) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }

        String tenantId = currentTenant(request);
        if (!contractRepository.existsByIdAndTenantId(id, tenantId)) {
            return ResponseEntity.status(404).body(legacyErrorByKey(request, "contract_not_found", "NOT_FOUND", null));
        }

        contractRepository.deleteById(id);
        auditLogService.record(currentUser(request), currentRole(request), "DELETE", "CONTRACT", id, "Deleted contract");
        return ResponseEntity.noContent().build();
    }

    private String newId(String prefix) {
        return prefix + "_" + Long.toString(System.currentTimeMillis(), 36) + String.format("%03d", (int) (Math.random() * 1000));
    }

    private String newContractNo() {
        return "CT-" + System.currentTimeMillis();
    }

    private LocalDate parseDateOrNull(HttpServletRequest request, String value) {
        return parseLocalDate(request, value);
    }
}


