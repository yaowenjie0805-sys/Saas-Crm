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
import com.yao.crm.util.CollectionsUtil;
import com.yao.crm.util.IdGenerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.Predicate;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api")
public class FollowUpController extends BaseApiController {

    private final FollowUpRepository followUpRepository;
    private final CustomerRepository customerRepository;
    private final AuditLogService auditLogService;
    private final ValueNormalizerService valueNormalizerService;
    private final IdGenerator idGenerator;

    public FollowUpController(FollowUpRepository followUpRepository,
                              CustomerRepository customerRepository,
                              AuditLogService auditLogService,
                              ValueNormalizerService valueNormalizerService,
                              I18nService i18nService,
                              IdGenerator idGenerator) {
        super(i18nService);
        this.followUpRepository = followUpRepository;
        this.customerRepository = customerRepository;
        this.auditLogService = auditLogService;
        this.valueNormalizerService = valueNormalizerService;
        this.idGenerator = idGenerator;
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
                CollectionsUtil.setOf("customerId", "author", "summary", "channel", "result", "nextActionDate", "createdAt", "updatedAt"),
                "updatedAt"
        );

        final boolean salesScoped = isSalesScoped(request);
        final String ownerScope = currentOwnerScope(request);
        final String tenantId = currentTenant(request);
        final String normalizedCustomerId = normalizeOptional(customerId);
        final String normalizedQuery = normalizeOptional(q);
        Specification<FollowUp> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<Predicate>();
            predicates.add(cb.equal(root.get("tenantId"), tenantId));
            if (normalizedCustomerId != null) {
                predicates.add(cb.equal(root.get("customerId"), normalizedCustomerId));
            }
            if (normalizedQuery != null) {
                String pattern = "%" + escapeLike(normalizedQuery.toLowerCase(Locale.ROOT)) + "%";
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
            return cb.and(predicates.toArray(new Predicate[predicates.size()]));

        };

        Page<FollowUp> result = followUpRepository.findAll(spec, pageable);
        Map<String, Object> body = new HashMap<>();
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
        String normalizedCustomerId = normalizeRequired(payload.getCustomerId());
        if (normalizedCustomerId == null) {
            return ResponseEntity.badRequest().body(legacyErrorByKey(request, "bad_request", "BAD_REQUEST", null));
        }
        Optional<Customer> customerOpt = customerRepository.findByIdAndTenantId(normalizedCustomerId, tenantId);
        if (!customerOpt.isPresent()) {
            return ResponseEntity.badRequest().body(legacyErrorByKey(request, "customer_not_found", "BAD_REQUEST", null));
        }
        if (isSalesScoped(request) && !ownerMatchesScope(request, customerOpt.get().getOwner())) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "scope_forbidden", "FORBIDDEN", null));
        }

        FollowUp followUp = new FollowUp();
        followUp.setId(idGenerator.generate("f"));
        followUp.setTenantId(tenantId);
        followUp.setCustomerId(normalizedCustomerId);
        followUp.setAuthor(isSalesScoped(request) ? currentUser(request) : (isBlank(payload.getAuthor()) ? currentUser(request) : payload.getAuthor().trim()));
        followUp.setSummary(payload.getSummary().trim());
        followUp.setChannel(isBlank(payload.getChannel()) ? "Phone" : valueNormalizerService.normalizeFollowUpChannel(payload.getChannel()));
        followUp.setResult(isBlank(payload.getResult()) ? "Pending" : payload.getResult().trim());
        if (!isBlank(payload.getNextActionDate())) {
            LocalDate nextDate = parseNextActionDateOrNull(request, payload.getNextActionDate());
            if (nextDate == null) {
                return ResponseEntity.badRequest().body(legacyErrorByKey(request, "next_action_date_format", "BAD_REQUEST", null));
            }
            followUp.setNextActionDate(nextDate);
        }

        FollowUp saved = followUpRepository.save(followUp);
        auditLogService.record(currentUser(request), currentRole(request), "CREATE", "FOLLOW_UP", saved.getId(), saved.getSummary());
        return ResponseEntity.status(201).body(saved);
    }

    @PatchMapping("/follow-ups/{id}")
    public ResponseEntity<?> updateFollowUp(HttpServletRequest request, @PathVariable String id, @Valid @RequestBody UpdateFollowUpRequest patch) {
        String normalizedId = normalizeRequired(id);
        if (normalizedId == null) {
            return ResponseEntity.badRequest().body(legacyErrorByKey(request, "bad_request", "BAD_REQUEST", null));
        }
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }
        String tenantId = currentTenant(request);
        Optional<FollowUp> optional = followUpRepository.findByIdAndTenantId(normalizedId, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(legacyErrorByKey(request, "follow_up_not_found", "NOT_FOUND", null));
        }
        FollowUp followUp = optional.get();
        if (isSalesScoped(request) && !ownerMatchesScopeByCustomerId(request, followUp.getCustomerId())) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "scope_forbidden", "FORBIDDEN", null));
        }

        if (patch.getCustomerId() != null) {
            String normalizedCustomerId = normalizeRequired(patch.getCustomerId());
            if (normalizedCustomerId == null) {
                return ResponseEntity.badRequest().body(legacyErrorByKey(request, "bad_request", "BAD_REQUEST", null));
            }
            Optional<Customer> customer = customerRepository.findByIdAndTenantId(normalizedCustomerId, tenantId);
            if (!customer.isPresent()) {
                return ResponseEntity.badRequest().body(legacyErrorByKey(request, "customer_not_found", "BAD_REQUEST", null));
            }
            if (isSalesScoped(request) && !ownerMatchesScope(request, customer.get().getOwner())) {
                return ResponseEntity.status(403).body(legacyErrorByKey(request, "scope_forbidden", "FORBIDDEN", null));
            }
            followUp.setCustomerId(normalizedCustomerId);
        }
        if (patch.getAuthor() != null && !isSalesScoped(request)) followUp.setAuthor(patch.getAuthor().trim());
        if (patch.getSummary() != null) followUp.setSummary(patch.getSummary().trim());
        if (patch.getChannel() != null) followUp.setChannel(valueNormalizerService.normalizeFollowUpChannel(patch.getChannel().trim()));
        if (patch.getResult() != null) followUp.setResult(patch.getResult().trim());
        if (patch.getNextActionDate() != null) {
            if (isBlank(patch.getNextActionDate())) {
                followUp.setNextActionDate(null);
            } else {
                LocalDate nextDate = parseNextActionDateOrNull(request, patch.getNextActionDate());
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
        String normalizedId = normalizeRequired(id);
        if (normalizedId == null) {
            return ResponseEntity.badRequest().body(legacyErrorByKey(request, "bad_request", "BAD_REQUEST", null));
        }
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }
        String tenantId = currentTenant(request);
        if (isSalesScoped(request)) {
            Optional<FollowUp> optional = followUpRepository.findByIdAndTenantId(normalizedId, tenantId);
            if (!optional.isPresent()) {
                return ResponseEntity.status(404).body(legacyErrorByKey(request, "follow_up_not_found", "NOT_FOUND", null));
            }
            if (!ownerMatchesScopeByCustomerId(request, optional.get().getCustomerId())) {
                return ResponseEntity.status(403).body(legacyErrorByKey(request, "scope_forbidden", "FORBIDDEN", null));
            }
        }
        long deleted = followUpRepository.deleteByIdAndTenantId(normalizedId, tenantId);
        if (deleted == 0L) {
            return ResponseEntity.status(404).body(legacyErrorByKey(request, "follow_up_not_found", "NOT_FOUND", null));
        }
        auditLogService.record(currentUser(request), currentRole(request), "DELETE", "FOLLOW_UP", normalizedId, "Deleted follow-up");
        return ResponseEntity.noContent().build();
    }

    private boolean ownerMatchesScopeByCustomerId(HttpServletRequest request, String customerId) {
        Optional<Customer> customer = customerRepository.findByIdAndTenantId(customerId, currentTenant(request));
        return customer.isPresent() && ownerMatchesScope(request, customer.get().getOwner());
    }


    private LocalDate parseNextActionDateOrNull(HttpServletRequest request, String value) {
        String normalized = normalizeRequired(value);
        if (normalized == null) {
            return null;
        }
        LocalDate parsed = parseLocalDate(request, normalized);
        if (parsed == null) {
            return null;
        }
        if (!isCanonicalDate(request, normalized, parsed)) {
            return null;
        }
        return parsed;
    }

    private boolean isCanonicalDate(HttpServletRequest request, String rawInput, LocalDate parsed) {
        Set<String> formats = new LinkedHashSet<String>();
        formats.add(currentTenantDateFormat(request));
        formats.addAll(supportedDateFormats());
        for (String format : formats) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                LocalDate reparsed = LocalDate.parse(rawInput, formatter);
                if (parsed.equals(reparsed) && rawInput.equals(reparsed.format(formatter))) {
                    return true;
                }
            } catch (Exception ignore) {
                // continue
            }
        }
        return false;
    }

    private String normalizeRequired(String value) {
        if (isBlank(value)) {
            return null;
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        return normalizeRequired(value);
    }
}
