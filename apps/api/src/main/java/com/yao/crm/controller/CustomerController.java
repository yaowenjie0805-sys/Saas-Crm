package com.yao.crm.controller;

import com.yao.crm.dto.request.CreateCustomerRequest;
import com.yao.crm.dto.request.UpdateCustomerRequest;
import com.yao.crm.entity.Customer;
import com.yao.crm.repository.CustomerRepository;
import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.I18nService;
import com.yao.crm.service.ValueNormalizerService;
import com.yao.crm.util.IdGenerator;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.Predicate;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.*;
import org.springframework.transaction.annotation.Transactional;

@Tag(name = "Customers", description = "Customer management operations")
@RestController
@RequestMapping({"/api", "/api/v1"})
public class CustomerController extends BaseApiController {

    private final CustomerRepository customerRepository;
    private final AuditLogService auditLogService;
    private final ValueNormalizerService valueNormalizerService;
    private final IdGenerator idGenerator;

    public CustomerController(CustomerRepository customerRepository,
                              AuditLogService auditLogService,
                              ValueNormalizerService valueNormalizerService,
                              I18nService i18nService,
                              IdGenerator idGenerator) {
        super(i18nService);
        this.customerRepository = customerRepository;
        this.auditLogService = auditLogService;
        this.valueNormalizerService = valueNormalizerService;
        this.idGenerator = idGenerator;
    }

    @GetMapping("/customers")
    public ResponseEntity<?> customers(HttpServletRequest request,
                                       @RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "50") int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(50, size));
        Pageable pageable = buildPageable(safePage, safeSize, "updatedAt", "desc",
                new HashSet<>(Set.of("name", "owner", "tag", "value", "status", "createdAt", "updatedAt")),
                "updatedAt");
        Page<Customer> result = customerRepository.findByTenantId(currentTenant(request), pageable);
        Map<String, Object> body = new HashMap<>();
        body.put("items", result.getContent());
        body.put("data", result.getContent());
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
                new HashSet<>(Set.of("name", "owner", "tag", "value", "status", "createdAt", "updatedAt")),
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
            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };

        Page<Customer> result = customerRepository.findAll(spec, pageable);
        Map<String, Object> body = new HashMap<>();
        body.put("items", result.getContent());
        body.put("data", result.getContent());
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
        customer.setId(idGenerator.generate("c"));
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
        auditLogService.record(currentUser(request), currentRole(request), "CREATE", "CUSTOMER", saved.getId(), saved.getName(), currentTenant(request));
        return ResponseEntity.status(201).body(saved);
    }

    @PutMapping("/customers/{id}")
    @PatchMapping("/customers/{id}")
    public ResponseEntity<?> updateCustomer(HttpServletRequest request, @PathVariable String id, @Valid @RequestBody UpdateCustomerRequest patch) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }
        if (isBlank(id)) {
            return ResponseEntity.badRequest().body(legacyErrorByKey(request, "bad_request", "BAD_REQUEST", null));
        }
        String normalizedId = id.trim();
        String tenantId = currentTenant(request);
        Optional<Customer> optional = customerRepository.findByIdAndTenantId(normalizedId, tenantId);
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
        auditLogService.record(currentUser(request), currentRole(request), "UPDATE", "CUSTOMER", saved.getId(), "Updated customer fields", tenantId);
        return ResponseEntity.ok(saved);
    }

    @Transactional
    @DeleteMapping("/customers/{id}")
    public ResponseEntity<?> deleteCustomer(HttpServletRequest request, @PathVariable String id) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }
        if (isBlank(id)) {
            return ResponseEntity.badRequest().body(legacyErrorByKey(request, "bad_request", "BAD_REQUEST", null));
        }
        return deleteCustomerInTenant(request, id.trim());
    }

    private ResponseEntity<?> deleteCustomerInTenant(HttpServletRequest request, String id) {
        // Fast path: delete directly in the current tenant to avoid a read-before-delete round trip.
        String tenantId = currentTenant(request);
        long deleted = customerRepository.deleteByIdAndTenantId(id, tenantId);
        if (deleted == 0L) {
            return ResponseEntity.status(404).body(legacyErrorByKey(request, "customer_not_found", "NOT_FOUND", null));
        }
        auditLogService.record(currentUser(request), currentRole(request), "DELETE", "CUSTOMER", id, "Deleted customer", tenantId);
        return ResponseEntity.noContent().build();
    }

}
