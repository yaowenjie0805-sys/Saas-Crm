package com.yao.crm.controller;

import com.yao.crm.dto.request.CreateCustomerRequest;
import com.yao.crm.dto.request.UpdateCustomerRequest;
import com.yao.crm.entity.Customer;
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
import java.util.*;

@RestController
@RequestMapping("/api")
public class CustomerController extends BaseApiController {

    private final CustomerRepository customerRepository;
    private final AuditLogService auditLogService;
    private final ValueNormalizerService valueNormalizerService;

    public CustomerController(CustomerRepository customerRepository,
                              AuditLogService auditLogService,
                              ValueNormalizerService valueNormalizerService,
                              I18nService i18nService) {
        super(i18nService);
        this.customerRepository = customerRepository;
        this.auditLogService = auditLogService;
        this.valueNormalizerService = valueNormalizerService;
    }

    @GetMapping("/customers")
    public ResponseEntity<?> customers(HttpServletRequest request,
                                       @RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "50") int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(50, size));
        Pageable pageable = buildPageable(safePage, safeSize, "updatedAt", "desc",
                new HashSet<String>(Arrays.asList("name", "owner", "tag", "value", "status", "createdAt", "updatedAt")),
                "updatedAt");
        Page<Customer> result = customerRepository.findByTenantId(currentTenant(request), pageable);
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("items", result.getContent());
        body.put("total", result.getTotalElements());
        body.put("page", safePage);
        body.put("size", safeSize);
        body.put("totalPages", result.getTotalPages());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/customers/search")
    public ResponseEntity<?> searchCustomers(
            HttpServletRequest request,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String status,
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
                new HashSet<String>(Arrays.asList("name", "owner", "tag", "value", "status", "createdAt", "updatedAt")),
                "updatedAt"
        );

        final String ownerScope = currentOwnerScope(request);
        final boolean salesScoped = isSalesScoped(request);
        final String tenantId = currentTenant(request);
        Specification<Customer> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<Predicate>();
            predicates.add(cb.equal(root.get("tenantId"), tenantId));
            if (!isBlank(q)) {
                String pattern = "%" + escapeLike(q.trim().toLowerCase(Locale.ROOT)) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern, '\\'),
                        cb.like(cb.lower(root.get("owner")), pattern, '\\'),
                        cb.like(cb.lower(root.get("tag")), pattern, '\\')
                ));
            }
            if (!isBlank(status)) {
                String normalizedStatus = valueNormalizerService.normalizeCustomerStatus(status);
                predicates.add(cb.equal(cb.lower(root.get("status")), normalizedStatus.toLowerCase(Locale.ROOT)));
            }
            if (salesScoped) {
                predicates.add(cb.equal(root.get("owner"), ownerScope));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Customer> result = customerRepository.findAll(spec, pageable);
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("items", result.getContent());
        body.put("total", result.getTotalElements());
        body.put("page", safePage);
        body.put("size", safeSize);
        body.put("totalPages", result.getTotalPages());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/customers")
    public ResponseEntity<?> createCustomer(HttpServletRequest request, @Valid @RequestBody CreateCustomerRequest payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }

        Customer customer = new Customer();
        customer.setId(newId("c"));
        customer.setTenantId(currentTenant(request));
        customer.setName(payload.getName());
        if (isSalesScoped(request)) {
            customer.setOwner(currentOwnerScope(request));
        } else {
            customer.setOwner(payload.getOwner());
        }
        if (!valueNormalizerService.isValidCustomerStatus(payload.getStatus())) {
            return ResponseEntity.badRequest().body(legacyErrorByKey(request, "invalid_customer_status", "BAD_REQUEST", null));
        }
        customer.setStatus(valueNormalizerService.normalizeCustomerStatus(payload.getStatus()));
        customer.setTag(isBlank(payload.getTag()) ? "New Lead" : payload.getTag());
        customer.setValue(payload.getValue() == null ? 0L : payload.getValue());

        Customer saved = customerRepository.save(customer);
        auditLogService.record(currentUser(request), currentRole(request), "CREATE", "CUSTOMER", saved.getId(), saved.getName());
        return ResponseEntity.status(201).body(saved);
    }

    @PatchMapping("/customers/{id}")
    public ResponseEntity<?> updateCustomer(HttpServletRequest request, @PathVariable String id, @Valid @RequestBody UpdateCustomerRequest patch) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }
        String tenantId = currentTenant(request);
        Optional<Customer> optional = customerRepository.findByIdAndTenantId(id, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(legacyErrorByKey(request, "customer_not_found", "NOT_FOUND", null));
        }
        Customer customer = optional.get();
        if (isSalesScoped(request) && !ownerMatchesScope(request, customer.getOwner())) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "scope_forbidden", "FORBIDDEN", null));
        }

        if (patch.getName() != null) customer.setName(patch.getName());
        if (patch.getOwner() != null && !isSalesScoped(request)) customer.setOwner(patch.getOwner());
        if (patch.getTag() != null) customer.setTag(patch.getTag());
        if (patch.getStatus() != null) {
            if (!valueNormalizerService.isValidCustomerStatus(patch.getStatus())) {
                return ResponseEntity.badRequest().body(legacyErrorByKey(request, "invalid_customer_status", "BAD_REQUEST", null));
            }
            customer.setStatus(valueNormalizerService.normalizeCustomerStatus(patch.getStatus()));
        }
        if (patch.getValue() != null) customer.setValue(patch.getValue());

        Customer saved = customerRepository.save(customer);
        auditLogService.record(currentUser(request), currentRole(request), "UPDATE", "CUSTOMER", saved.getId(), "Updated customer fields");
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/customers/{id}")
    public ResponseEntity<?> deleteCustomer(HttpServletRequest request, @PathVariable String id) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }
        String tenantId = currentTenant(request);
        if (!customerRepository.existsByIdAndTenantId(id, tenantId)) {
            return ResponseEntity.status(404).body(legacyErrorByKey(request, "customer_not_found", "NOT_FOUND", null));
        }
        customerRepository.deleteById(id);
        auditLogService.record(currentUser(request), currentRole(request), "DELETE", "CUSTOMER", id, "Deleted customer");
        return ResponseEntity.noContent().build();
    }

    private String newId(String prefix) {
        return prefix + "_" + Long.toString(System.currentTimeMillis(), 36) + String.format("%03d", (int) (Math.random() * 1000));
    }
}


