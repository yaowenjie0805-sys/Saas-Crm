package com.yao.crm.controller;

import com.yao.crm.dto.request.CreateOpportunityRequest;
import com.yao.crm.dto.request.UpdateOpportunityRequest;
import com.yao.crm.entity.Opportunity;
import com.yao.crm.repository.OpportunityRepository;
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
public class OpportunityController extends BaseApiController {

    private final OpportunityRepository opportunityRepository;
    private final AuditLogService auditLogService;
    private final ValueNormalizerService valueNormalizerService;

    public OpportunityController(OpportunityRepository opportunityRepository,
                                 AuditLogService auditLogService,
                                 ValueNormalizerService valueNormalizerService,
                                 I18nService i18nService) {
        super(i18nService);
        this.opportunityRepository = opportunityRepository;
        this.auditLogService = auditLogService;
        this.valueNormalizerService = valueNormalizerService;
    }

    @GetMapping("/opportunities")
    public ResponseEntity<?> opportunities(HttpServletRequest request,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "50") int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(50, size));
        Pageable pageable = buildPageable(safePage, safeSize, "updatedAt", "desc",
                new HashSet<String>(Arrays.asList("title", "stage", "owner", "amount", "progress", "createdAt", "updatedAt")),
                "updatedAt");
        org.springframework.data.domain.Page<Opportunity> result = opportunityRepository.findByTenantId(currentTenant(request), pageable);
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("items", result.getContent());
        body.put("total", result.getTotalElements());
        body.put("page", safePage);
        body.put("size", safeSize);
        body.put("totalPages", result.getTotalPages());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/opportunities/search")
    public ResponseEntity<?> searchOpportunities(
            HttpServletRequest request,
            @RequestParam(defaultValue = "") String stage,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "6") int size,
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
                new HashSet<String>(Arrays.asList("stage", "count", "amount", "progress", "owner", "createdAt", "updatedAt")),
                "updatedAt"
        );

        final String ownerScope = currentOwnerScope(request);
        final boolean salesScoped = isSalesScoped(request);
        final String tenantId = currentTenant(request);
        Specification<Opportunity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<Predicate>();
            predicates.add(cb.equal(root.get("tenantId"), tenantId));
            if (!isBlank(stage)) {
                String normalizedStage = valueNormalizerService.normalizeOpportunityStage(stage);
                predicates.add(cb.equal(cb.lower(root.get("stage")), normalizedStage.toLowerCase(Locale.ROOT)));
            }
            if (salesScoped) {
                predicates.add(cb.equal(root.get("owner"), ownerScope));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Opportunity> result = opportunityRepository.findAll(spec, pageable);
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("items", result.getContent());
        body.put("total", result.getTotalElements());
        body.put("page", safePage);
        body.put("size", safeSize);
        body.put("totalPages", result.getTotalPages());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/opportunities")
    public ResponseEntity<?> createOpportunity(HttpServletRequest request, @Valid @RequestBody CreateOpportunityRequest payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }

        Opportunity opportunity = new Opportunity();
        opportunity.setId(newId("o"));
        opportunity.setTenantId(currentTenant(request));
        if (!valueNormalizerService.isValidOpportunityStage(payload.getStage())) {
            return ResponseEntity.badRequest().body(legacyErrorByKey(request, "invalid_opportunity_stage", "BAD_REQUEST", null));
        }
        opportunity.setStage(valueNormalizerService.normalizeOpportunityStage(payload.getStage()));
        opportunity.setCount(payload.getCount() == null ? 0 : payload.getCount());
        opportunity.setAmount(payload.getAmount() == null ? 0L : payload.getAmount());
        opportunity.setProgress(payload.getProgress() == null ? 0 : payload.getProgress());
        if (isSalesScoped(request)) {
            opportunity.setOwner(currentOwnerScope(request));
        } else {
            opportunity.setOwner(isBlank(payload.getOwner()) ? currentUser(request) : payload.getOwner());
        }

        if (!hasAnyRole(request, "ADMIN", "MANAGER") && opportunity.getAmount() > 0) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "only_admin_set_amount", "FORBIDDEN", null));
        }

        Opportunity saved = opportunityRepository.save(opportunity);
        auditLogService.record(currentUser(request), currentRole(request), "CREATE", "OPPORTUNITY", saved.getId(), saved.getStage());
        return ResponseEntity.status(201).body(saved);
    }

    @PatchMapping("/opportunities/{id}")
    public ResponseEntity<?> updateOpportunity(HttpServletRequest request, @PathVariable String id, @Valid @RequestBody UpdateOpportunityRequest patch) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }
        String tenantId = currentTenant(request);
        Optional<Opportunity> optional = opportunityRepository.findByIdAndTenantId(id, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(legacyErrorByKey(request, "opportunity_not_found", "NOT_FOUND", null));
        }
        Opportunity opportunity = optional.get();
        if (isSalesScoped(request) && !ownerMatchesScope(request, opportunity.getOwner())) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "scope_forbidden", "FORBIDDEN", null));
        }

        if (patch.getStage() != null) {
            if (!valueNormalizerService.isValidOpportunityStage(patch.getStage())) {
                return ResponseEntity.badRequest().body(legacyErrorByKey(request, "invalid_opportunity_stage", "BAD_REQUEST", null));
            }
            opportunity.setStage(valueNormalizerService.normalizeOpportunityStage(patch.getStage()));
        }
        if (patch.getCount() != null) opportunity.setCount(patch.getCount());
        if (patch.getAmount() != null) {
            if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
                return ResponseEntity.status(403).body(legacyErrorByKey(request, "only_admin_update_amount", "FORBIDDEN", null));
            }
            opportunity.setAmount(patch.getAmount());
        }
        if (patch.getProgress() != null) opportunity.setProgress(patch.getProgress());
        if (patch.getOwner() != null && !isSalesScoped(request)) {
            opportunity.setOwner(patch.getOwner());
        }

        Opportunity saved = opportunityRepository.save(opportunity);
        auditLogService.record(currentUser(request), currentRole(request), "UPDATE", "OPPORTUNITY", saved.getId(), "Updated opportunity");
        return ResponseEntity.ok(saved);
    }


    @DeleteMapping("/opportunities/{id}")
    public ResponseEntity<?> deleteOpportunity(HttpServletRequest request, @PathVariable String id) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }

        String tenantId = currentTenant(request);
        if (!opportunityRepository.existsByIdAndTenantId(id, tenantId)) {
            return ResponseEntity.status(404).body(legacyErrorByKey(request, "opportunity_not_found", "NOT_FOUND", null));
        }

        opportunityRepository.deleteById(id);
        auditLogService.record(currentUser(request), currentRole(request), "DELETE", "OPPORTUNITY", id, "Deleted opportunity");
        return ResponseEntity.noContent().build();
    }
    private String newId(String prefix) {
        return prefix + "_" + Long.toString(System.currentTimeMillis(), 36) + String.format("%03d", (int) (Math.random() * 1000));
    }
}


