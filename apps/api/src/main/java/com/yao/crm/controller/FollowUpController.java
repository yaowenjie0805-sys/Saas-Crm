package com.yao.crm.controller;

import com.yao.crm.dto.request.CreateFollowUpRequest;
import com.yao.crm.dto.request.UpdateFollowUpRequest;
import com.yao.crm.entity.Customer;
import com.yao.crm.entity.FollowUp;
import com.yao.crm.repository.CustomerRepository;
import com.yao.crm.repository.FollowUpRepository;
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
public class FollowUpController extends BaseApiController {

    private final FollowUpRepository followUpRepository;
    private final CustomerRepository customerRepository;
    private final AuditLogService auditLogService;
    private final ValueNormalizerService valueNormalizerService;

    public FollowUpController(FollowUpRepository followUpRepository,
                              CustomerRepository customerRepository,
                              AuditLogService auditLogService,
                              ValueNormalizerService valueNormalizerService,
                              I18nService i18nService) {
        super(i18nService);
        this.followUpRepository = followUpRepository;
        this.customerRepository = customerRepository;
        this.auditLogService = auditLogService;
        this.valueNormalizerService = valueNormalizerService;
    }

    @GetMapping("/follow-ups/search")
    public ResponseEntity<?> searchFollowUps(
            HttpServletRequest request,
            @RequestParam(defaultValue = "") String customerId,
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
                new HashSet<String>(Arrays.asList("customerId", "author", "summary", "channel", "result", "nextActionDate", "createdAt", "updatedAt")),
                "updatedAt"
        );

        final boolean salesScoped = isSalesScoped(request);
        final String ownerScope = currentOwnerScope(request);
        final String tenantId = currentTenant(request);
        Specification<FollowUp> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<Predicate>();
            predicates.add(cb.equal(root.get("tenantId"), tenantId));
            if (!isBlank(customerId)) {
                predicates.add(cb.equal(root.get("customerId"), customerId));
            }
            if (!isBlank(q)) {
                String pattern = "%" + escapeLike(q.trim().toLowerCase(Locale.ROOT)) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("summary")), pattern, '\\'),
                        cb.like(cb.lower(root.get("result")), pattern, '\\'),
                        cb.like(cb.lower(root.get("author")), pattern, '\\')
                ));
            }
            if (salesScoped) {
                Predicate selfAuthor = cb.equal(root.get("author"), currentUser(request));
                Predicate scopeAuthor = cb.equal(root.get("author"), ownerScope);
                predicates.add(cb.or(selfAuthor, scopeAuthor));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<FollowUp> result = followUpRepository.findAll(spec, pageable);
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("items", result.getContent());
        body.put("total", result.getTotalElements());
        body.put("page", safePage);
        body.put("size", safeSize);
        body.put("totalPages", result.getTotalPages());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/follow-ups")
    public ResponseEntity<?> createFollowUp(HttpServletRequest request, @Valid @RequestBody CreateFollowUpRequest payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }
        String tenantId = currentTenant(request);
        Optional<Customer> customerOpt = customerRepository.findByIdAndTenantId(payload.getCustomerId(), tenantId);
        if (!customerOpt.isPresent()) {
            return ResponseEntity.badRequest().body(legacyErrorByKey(request, "customer_not_found", "BAD_REQUEST", null));
        }
        if (isSalesScoped(request) && !ownerMatchesScope(request, customerOpt.get().getOwner())) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "scope_forbidden", "FORBIDDEN", null));
        }

        FollowUp followUp = new FollowUp();
        followUp.setId(newId("f"));
        followUp.setTenantId(tenantId);
        followUp.setCustomerId(payload.getCustomerId());
        followUp.setAuthor(isSalesScoped(request) ? currentUser(request) : (isBlank(payload.getAuthor()) ? currentUser(request) : payload.getAuthor()));
        followUp.setSummary(payload.getSummary());
        followUp.setChannel(isBlank(payload.getChannel()) ? "Phone" : valueNormalizerService.normalizeFollowUpChannel(payload.getChannel()));
        followUp.setResult(isBlank(payload.getResult()) ? "Pending" : payload.getResult());
        if (!isBlank(payload.getNextActionDate())) {
            LocalDate nextDate = parseLocalDate(request, payload.getNextActionDate());
            followUp.setNextActionDate(nextDate);
        }

        FollowUp saved = followUpRepository.save(followUp);
        auditLogService.record(currentUser(request), currentRole(request), "CREATE", "FOLLOW_UP", saved.getId(), saved.getSummary());
        return ResponseEntity.status(201).body(saved);
    }

    @PatchMapping("/follow-ups/{id}")
    public ResponseEntity<?> updateFollowUp(HttpServletRequest request, @PathVariable String id, @Valid @RequestBody UpdateFollowUpRequest patch) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }
        String tenantId = currentTenant(request);
        Optional<FollowUp> optional = followUpRepository.findByIdAndTenantId(id, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(legacyErrorByKey(request, "follow_up_not_found", "NOT_FOUND", null));
        }
        FollowUp followUp = optional.get();
        if (isSalesScoped(request) && !ownerMatchesScopeByCustomerId(request, followUp.getCustomerId())) {
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
            followUp.setCustomerId(patch.getCustomerId());
        }
        if (patch.getAuthor() != null && !isSalesScoped(request)) followUp.setAuthor(patch.getAuthor());
        if (patch.getSummary() != null) followUp.setSummary(patch.getSummary());
        if (patch.getChannel() != null) followUp.setChannel(valueNormalizerService.normalizeFollowUpChannel(patch.getChannel()));
        if (patch.getResult() != null) followUp.setResult(patch.getResult());
        if (patch.getNextActionDate() != null) {
            if (isBlank(patch.getNextActionDate())) {
                followUp.setNextActionDate(null);
            } else {
                LocalDate nextDate = parseLocalDate(request, patch.getNextActionDate());
                if (nextDate == null) {
                    return ResponseEntity.badRequest().body(legacyErrorByKey(request, "next_action_date_format", "BAD_REQUEST", null));
                }
                followUp.setNextActionDate(nextDate);
            }
        }

        FollowUp saved = followUpRepository.save(followUp);
        auditLogService.record(currentUser(request), currentRole(request), "UPDATE", "FOLLOW_UP", saved.getId(), "Updated follow-up");
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/follow-ups/{id}")
    public ResponseEntity<?> deleteFollowUp(HttpServletRequest request, @PathVariable String id) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }
        String tenantId = currentTenant(request);
        Optional<FollowUp> followUp = followUpRepository.findByIdAndTenantId(id, tenantId);
        if (!followUp.isPresent()) {
            return ResponseEntity.status(404).body(legacyErrorByKey(request, "follow_up_not_found", "NOT_FOUND", null));
        }
        if (isSalesScoped(request) && !ownerMatchesScopeByCustomerId(request, followUp.get().getCustomerId())) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "scope_forbidden", "FORBIDDEN", null));
        }
        followUpRepository.deleteById(id);
        auditLogService.record(currentUser(request), currentRole(request), "DELETE", "FOLLOW_UP", id, "Deleted follow-up");
        return ResponseEntity.noContent().build();
    }

    private boolean ownerMatchesScopeByCustomerId(HttpServletRequest request, String customerId) {
        Optional<Customer> customer = customerRepository.findByIdAndTenantId(customerId, currentTenant(request));
        return customer.isPresent() && ownerMatchesScope(request, customer.get().getOwner());
    }

    private String newId(String prefix) {
        return prefix + "_" + Long.toString(System.currentTimeMillis(), 36) + String.format("%03d", (int) (Math.random() * 1000));
    }
}


