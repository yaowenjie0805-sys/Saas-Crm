package com.yao.crm.controller;

import com.yao.crm.dto.request.V1LeadConvertRequest;
import com.yao.crm.dto.request.V1LeadAssignRequest;
import com.yao.crm.dto.request.V1LeadUpsertRequest;
import com.yao.crm.entity.Contact;
import com.yao.crm.entity.Customer;
import com.yao.crm.entity.Lead;
import com.yao.crm.entity.LeadImportJob;
import com.yao.crm.entity.Opportunity;
import com.yao.crm.repository.ContactRepository;
import com.yao.crm.repository.CustomerRepository;
import com.yao.crm.repository.LeadImportJobRepository;
import com.yao.crm.repository.LeadRepository;
import com.yao.crm.repository.OpportunityRepository;
import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.I18nService;
import com.yao.crm.service.LeadAssignmentService;
import com.yao.crm.service.LeadAutomationService;
import com.yao.crm.service.LeadImportFailedRowsExportJobService;
import com.yao.crm.service.LeadImportService;
import com.yao.crm.service.ValueNormalizerService;
import com.yao.crm.enums.LeadStatusEnum;
import com.yao.crm.util.CollectionsUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.criteria.Predicate;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/v1/leads")
public class V1LeadController extends BaseApiController {

    private static final Set<String> CONVERT_ALLOWED_STATUSES = CollectionsUtil.setOf("NEW", "QUALIFIED", "NURTURING");
    private static final Set<String> IMPORT_CANCEL_ALLOWED_STATUSES = new LinkedHashSet<String>(Arrays.asList("PENDING", "RUNNING"));
    private static final Set<String> IMPORT_RETRY_ALLOWED_STATUSES = new LinkedHashSet<String>(Arrays.asList("FAILED", "CANCELED"));

    private final LeadRepository leadRepository;
    private final CustomerRepository customerRepository;
    private final ContactRepository contactRepository;
    private final OpportunityRepository opportunityRepository;
    private final LeadImportJobRepository leadImportJobRepository;
    private final AuditLogService auditLogService;
    private final ValueNormalizerService valueNormalizerService;
    private final LeadAssignmentService leadAssignmentService;
    private final LeadAutomationService leadAutomationService;
    private final LeadImportService leadImportService;
    private final LeadImportFailedRowsExportJobService leadImportFailedRowsExportJobService;

    public V1LeadController(LeadRepository leadRepository,
                            CustomerRepository customerRepository,
                            ContactRepository contactRepository,
                            OpportunityRepository opportunityRepository,
                            LeadImportJobRepository leadImportJobRepository,
                            AuditLogService auditLogService,
                            ValueNormalizerService valueNormalizerService,
                            LeadAssignmentService leadAssignmentService,
                            LeadAutomationService leadAutomationService,
                            LeadImportService leadImportService,
                            LeadImportFailedRowsExportJobService leadImportFailedRowsExportJobService,
                            I18nService i18nService) {
        super(i18nService);
        this.leadRepository = leadRepository;
        this.customerRepository = customerRepository;
        this.contactRepository = contactRepository;
        this.opportunityRepository = opportunityRepository;
        this.leadImportJobRepository = leadImportJobRepository;
        this.auditLogService = auditLogService;
        this.valueNormalizerService = valueNormalizerService;
        this.leadAssignmentService = leadAssignmentService;
        this.leadAutomationService = leadAutomationService;
        this.leadImportService = leadImportService;
        this.leadImportFailedRowsExportJobService = leadImportFailedRowsExportJobService;
    }

    @GetMapping
    public ResponseEntity<?> listLeads(HttpServletRequest request,
                                       @RequestParam(defaultValue = "") String q,
                                       @RequestParam(defaultValue = "") String status,
                                       @RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int size,
                                       @RequestParam(defaultValue = "updatedAt") String sortBy,
                                       @RequestParam(defaultValue = "desc") String sortDir) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = normalize(currentTenant(request));
        String normalizedQ = normalize(q);
        String normalizedStatus = normalizeStatusValue(status);
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(100, size));
        Pageable pageable = buildPageable(
                safePage,
                safeSize,
                sortBy,
                sortDir,
                CollectionsUtil.setOf("name", "company", "owner", "status", "createdAt", "updatedAt"),
                "updatedAt"
        );
        final boolean salesScoped = isSalesScoped(request);
        final String ownerScope = currentOwnerScope(request);
        Specification<Lead> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<Predicate>();
            predicates.add(cb.equal(root.get("tenantId"), tenantId));
            if (!isBlank(normalizedQ)) {
                String pattern = "%" + escapeLike(normalizedQ.toLowerCase(Locale.ROOT)) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern, '\\'),
                        cb.like(cb.lower(root.get("company")), pattern, '\\'),
                        cb.like(cb.lower(root.get("email")), pattern, '\\'),
                        cb.like(cb.lower(root.get("phone")), pattern, '\\')
                ));
            }
            if (!isBlank(normalizedStatus)) {
                predicates.add(cb.equal(cb.upper(root.get("status")), normalizedStatus));
            }
            if (salesScoped) {
                predicates.add(cb.equal(cb.lower(root.get("owner")), ownerScope.toLowerCase(Locale.ROOT)));
            }
            return cb.and(predicates.toArray(new Predicate[predicates.size()]));

        };
        Page<Lead> result = leadRepository.findAll(spec, pageable);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", result.getContent());
        body.put("total", result.getTotalElements());
        body.put("page", safePage);
        body.put("size", safeSize);
        body.put("totalPages", result.getTotalPages());
        return ResponseEntity.ok(successWithFields(request, "leads_listed", body));
    }

    @PostMapping
    public ResponseEntity<?> createLead(HttpServletRequest request, @Valid @RequestBody V1LeadUpsertRequest payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        if (payload == null) {
            return ResponseEntity.badRequest().body(errorBody(request, "bad_request", msg(request, "bad_request"), null));
        }
        String tenantId = normalize(currentTenant(request));
        Lead lead = new Lead();
        lead.setId(newId("ld"));
        try {
            applyPayload(request, lead, payload, true);
        } catch (IllegalArgumentException ex) {
            String code = normalizeCode(ex.getMessage(), "bad_request");
            return ResponseEntity.badRequest().body(errorBody(request, code, msg(request, code), null));
        } catch (IllegalStateException ex) {
            String targetStatus = normalizeStatusValue(payload == null ? null : payload.getStatus());
            if (isBlank(targetStatus)) targetStatus = normalizeStatusValue(lead.getStatus());
            return leadStatusTransitionConflict(request, lead.getStatus(), targetStatus);
        }
        lead.setTenantId(tenantId);
        lead = leadRepository.save(lead);
        String ownerBeforeAssign = lead.getOwner();
        if (isBlank(payload.getOwner())) {
            String assigned = leadAssignmentService.assignLeadOwner(tenantId, currentUser(request), lead, "", true);
            if (!isBlank(assigned)) {
                lead.setOwner(assigned);
            }
        }
        auditLogService.record(currentUser(request), currentRole(request), "CREATE", "LEAD", lead.getId(), "Created lead", tenantId);
        leadAutomationService.onLeadEvent(tenantId, "LEAD_CREATED", lead, currentUser(request));
        if (!Objects.equals(ownerBeforeAssign, lead.getOwner())) {
            leadAutomationService.onLeadEvent(tenantId, "LEAD_ASSIGNED", lead, currentUser(request));
        }
        return ResponseEntity.status(201).body(successWithFields(request, "lead_created", toLeadView(lead)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> patchLead(HttpServletRequest request, @PathVariable String id, @Valid @RequestBody V1LeadUpsertRequest payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        if (payload == null) {
            return ResponseEntity.badRequest().body(errorBody(request, "bad_request", msg(request, "bad_request"), null));
        }
        String leadId = normalize(id);
        if (isBlank(leadId)) {
            return ResponseEntity.badRequest().body(errorBody(request, "bad_request", msg(request, "bad_request"), null));
        }
        String tenantId = normalize(currentTenant(request));
        Optional<Lead> optional = leadRepository.findByIdAndTenantId(leadId, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "lead_not_found", msg(request, "lead_not_found"), null));
        }
        Lead lead = optional.get();
        String beforeStatus = lead.getStatus();
        String beforeOwner = lead.getOwner();
        if (isSalesScoped(request) && !ownerMatchesScope(request, lead.getOwner())) {
            return ResponseEntity.status(403).body(errorBody(request, "scope_forbidden", msg(request, "scope_forbidden"), null));
        }
        String targetStatus = normalizeStatusValue(payload == null ? null : payload.getStatus());
        if (isBlank(targetStatus)) targetStatus = normalizeStatusValue(lead.getStatus());
        try {
            applyPayload(request, lead, payload, false);
        } catch (IllegalArgumentException ex) {
            String code = normalizeCode(ex.getMessage(), "bad_request");
            return ResponseEntity.badRequest().body(errorBody(request, code, msg(request, code), null));
        } catch (IllegalStateException ex) {
            return leadStatusTransitionConflict(request, lead.getStatus(), targetStatus);
        }
        lead = leadRepository.save(lead);
        auditLogService.record(currentUser(request), currentRole(request), "UPDATE", "LEAD", lead.getId(), "Updated lead", tenantId);
        if (!Objects.equals(beforeStatus, lead.getStatus())) {
            leadAutomationService.onLeadEvent(tenantId, "LEAD_STATUS_CHANGED", lead, currentUser(request));
        }
        if (!Objects.equals(beforeOwner, lead.getOwner())) {
            leadAutomationService.onLeadEvent(tenantId, "LEAD_ASSIGNED", lead, currentUser(request));
        }
        return ResponseEntity.ok(successWithFields(request, "lead_updated", toLeadView(lead)));
    }

    @PostMapping("/{id}/convert")
    public ResponseEntity<?> convertLead(HttpServletRequest request, @PathVariable String id, @RequestBody(required = false) V1LeadConvertRequest payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String leadId = normalize(id);
        if (isBlank(leadId)) {
            return ResponseEntity.badRequest().body(errorBody(request, "bad_request", msg(request, "bad_request"), null));
        }
        String tenantId = normalize(currentTenant(request));
        Optional<Lead> optional = leadRepository.findByIdAndTenantId(leadId, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "lead_not_found", msg(request, "lead_not_found"), null));
        }
        Lead lead = optional.get();
        if (isSalesScoped(request) && !ownerMatchesScope(request, lead.getOwner())) {
            return ResponseEntity.status(403).body(errorBody(request, "scope_forbidden", msg(request, "scope_forbidden"), null));
        }
        if ("CONVERTED".equalsIgnoreCase(lead.getStatus())) {
            return ResponseEntity.status(409).body(errorBody(request, "lead_already_converted", msg(request, "lead_already_converted"), null));
        }
        if (!CONVERT_ALLOWED_STATUSES.contains(normalizeStatusValue(lead.getStatus()))) {
            return leadStatusTransitionConflict(request, lead.getStatus(), "CONVERTED");
        }

        String owner = isBlank(payload == null ? null : payload.getCustomerOwner()) ? lead.getOwner() : payload.getCustomerOwner().trim();
        if (isBlank(owner)) owner = currentUser(request);

        Customer customer = new Customer();
        customer.setId(newId("c"));
        customer.setName(isBlank(lead.getCompany()) ? lead.getName() : lead.getCompany());
        customer.setOwner(owner);
        customer.setTag("Lead Converted");
        customer.setValue(0L);
        customer.setStatus("Active");
        customer.setTenantId(tenantId);
        customer = customerRepository.save(customer);

        Contact contact = new Contact();
        contact.setId(newId("ct"));
        contact.setCustomerId(customer.getId());
        contact.setName(isBlank(payload == null ? null : payload.getContactName()) ? lead.getName() : payload.getContactName().trim());
        contact.setTitle(isBlank(payload == null ? null : payload.getContactTitle()) ? "Decision Maker" : payload.getContactTitle().trim());
        contact.setPhone(lead.getPhone());
        contact.setEmail(lead.getEmail());
        contact.setOwner(owner);
        contact.setTenantId(tenantId);
        contact = contactRepository.save(contact);

        Opportunity opportunity = new Opportunity();
        opportunity.setId(newId("o"));
        opportunity.setStage(valueNormalizerService.normalizeOpportunityStage(isBlank(payload == null ? null : payload.getOpportunityStage()) ? "Lead" : payload.getOpportunityStage().trim()));
        opportunity.setCount(1);
        opportunity.setAmount(0L);
        opportunity.setOwner(owner);
        opportunity.setProgress(10);
        opportunity.setTenantId(tenantId);
        opportunity = opportunityRepository.save(opportunity);

        lead.setStatus("CONVERTED");
        lead = leadRepository.save(lead);
        auditLogService.record(currentUser(request), currentRole(request), "CONVERT", "LEAD", lead.getId(), "Converted lead", tenantId);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("lead", toLeadView(lead));
        body.put("customerId", customer.getId());
        body.put("contactId", contact.getId());
        body.put("opportunityId", opportunity.getId());
        return ResponseEntity.ok(successWithFields(request, "lead_converted", body));
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<?> assignLead(HttpServletRequest request, @PathVariable String id, @RequestBody(required = false) V1LeadAssignRequest payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String leadId = normalize(id);
        if (isBlank(leadId)) {
            return ResponseEntity.badRequest().body(errorBody(request, "bad_request", msg(request, "bad_request"), null));
        }
        String tenantId = normalize(currentTenant(request));
        Optional<Lead> optional = leadRepository.findByIdAndTenantId(leadId, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "lead_not_found", msg(request, "lead_not_found"), null));
        }
        Lead lead = optional.get();
        String before = lead.getOwner();
        String requestedOwner = payload == null ? "" : normalize(payload.getOwner());
        boolean useRule = payload == null || payload.getUseRule() == null || payload.getUseRule();
        String assigned = leadAssignmentService.assignLeadOwner(tenantId, currentUser(request), lead, requestedOwner, useRule);
        if (!Objects.equals(before, assigned)) {
            leadAutomationService.onLeadEvent(tenantId, "LEAD_ASSIGNED", lead, currentUser(request));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("leadId", lead.getId());
        body.put("owner", assigned);
        return ResponseEntity.ok(successWithFields(request, "lead_assigned", body));
    }

    @GetMapping("/import-template")
    public ResponseEntity<?> importTemplate(HttpServletRequest request) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        boolean zh = request.getHeader("Accept-Language") != null && request.getHeader("Accept-Language").toLowerCase(Locale.ROOT).startsWith("zh");
        String csv = "\uFEFFname,company,phone,email,source,owner,status\n";
        String filename = zh ? "\u7ebf\u7d22\u5bfc\u5165\u6a21\u677f.csv" : "lead_import_template.csv";
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(csv);
    }

    @PostMapping(value = "/import-jobs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createImportJob(HttpServletRequest request, @RequestPart("file") MultipartFile file) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(errorBody(request, "lead_import_file_required", msg(request, "lead_import_file_required"), null));
        }
        String tenantId = currentTenant(request);
        try {
            LeadImportJob job = leadImportService.createAsyncImportJob(tenantId, currentUser(request), traceId(request), file);
            return ResponseEntity.status(201).body(successWithFields(request, "lead_import_job_created", leadImportService.toView(job)));
        } catch (IllegalStateException ex) {
            String code = isBlank(ex.getMessage()) ? "lead_import_concurrent_limit_exceeded" : ex.getMessage().trim();
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("action", "create");
            details.put("tenantId", tenantId);
            details.put("allowedConcurrentStatuses", new ArrayList<String>() { { add("PENDING"); add("RUNNING"); } });
            return ResponseEntity.status(409).body(errorBody(request, code, msg(request, code), details));
        }
    }

    @GetMapping("/import-jobs")
    public ResponseEntity<?> listImportJobs(HttpServletRequest request,
                                            @RequestParam(defaultValue = "ALL") String status,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = normalize(currentTenant(request));
        String normalizedStatus = normalizeToUpper(status, "ALL");
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, size);
        Map<String, Object> body = leadImportService.listJobs(tenantId, normalizedStatus, safePage, safeSize);
        return ResponseEntity.ok(successWithFields(request, "lead_import_jobs_listed", body));
    }

    @GetMapping("/import-jobs/{jobId}")
    public ResponseEntity<?> getImportJob(HttpServletRequest request, @PathVariable String jobId) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String normalizedJobId = normalize(jobId);
        if (isBlank(normalizedJobId)) {
            return ResponseEntity.badRequest().body(errorBody(request, "bad_request", msg(request, "bad_request"), null));
        }
        String tenantId = normalize(currentTenant(request));
        Optional<LeadImportJob> optional = leadImportJobRepository.findByIdAndTenantId(normalizedJobId, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "lead_import_job_not_found", msg(request, "lead_import_job_not_found"), null));
        }
        return ResponseEntity.ok(successWithFields(request, "lead_import_job_loaded", leadImportService.toView(optional.get())));
    }

    @PostMapping("/import-jobs/{jobId}/cancel")
    public ResponseEntity<?> cancelImportJob(HttpServletRequest request, @PathVariable String jobId) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String normalizedJobId = normalize(jobId);
        if (isBlank(normalizedJobId)) {
            return ResponseEntity.badRequest().body(errorBody(request, "bad_request", msg(request, "bad_request"), null));
        }
        String tenantId = normalize(currentTenant(request));
        try {
            LeadImportJob job = leadImportService.cancelJob(tenantId, normalizedJobId, currentUser(request), traceId(request));
            if (job == null) {
                return ResponseEntity.status(404).body(errorBody(request, "lead_import_job_not_found", msg(request, "lead_import_job_not_found"), null));
            }
            return ResponseEntity.ok(successWithFields(request, "lead_import_job_canceled", leadImportService.toView(job)));
        } catch (IllegalStateException ex) {
            String code = isBlank(ex.getMessage()) ? "lead_import_status_transition_invalid" : ex.getMessage().trim();
            Map<String, Object> details = buildImportConflictDetails(tenantId, normalizedJobId, "cancel", code);
            return ResponseEntity.status(409).body(errorBody(request, code, msg(request, code), details));
        }
    }

    @PostMapping("/import-jobs/{jobId}/retry")
    public ResponseEntity<?> retryImportJob(HttpServletRequest request, @PathVariable String jobId) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String normalizedJobId = normalize(jobId);
        if (isBlank(normalizedJobId)) {
            return ResponseEntity.badRequest().body(errorBody(request, "bad_request", msg(request, "bad_request"), null));
        }
        String tenantId = normalize(currentTenant(request));
        try {
            LeadImportJob job = leadImportService.retryJob(tenantId, normalizedJobId, currentUser(request), traceId(request));
            if (job == null) {
                return ResponseEntity.status(404).body(errorBody(request, "lead_import_job_not_found", msg(request, "lead_import_job_not_found"), null));
            }
            return ResponseEntity.ok(successWithFields(request, "lead_import_job_retried", leadImportService.toView(job)));
        } catch (IllegalStateException ex) {
            String code = isBlank(ex.getMessage()) ? "lead_import_status_transition_invalid" : ex.getMessage().trim();
            Map<String, Object> details = buildImportConflictDetails(tenantId, normalizedJobId, "retry", code);
            return ResponseEntity.status(409).body(errorBody(request, code, msg(request, code), details));
        }
    }

    @GetMapping("/import-jobs/{jobId}/failed-rows")
    public ResponseEntity<?> listImportFailedRows(HttpServletRequest request,
                                                  @PathVariable String jobId,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "50") int size) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String normalizedJobId = normalize(jobId);
        if (isBlank(normalizedJobId)) {
            return ResponseEntity.badRequest().body(errorBody(request, "bad_request", msg(request, "bad_request"), null));
        }
        String tenantId = normalize(currentTenant(request));
        Optional<LeadImportJob> optional = leadImportJobRepository.findByIdAndTenantId(normalizedJobId, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "lead_import_job_not_found", msg(request, "lead_import_job_not_found"), null));
        }
        Map<String, Object> body = leadImportService.listFailedRows(tenantId, normalizedJobId, Math.max(1, page), Math.max(1, size));
        body.put("jobId", normalizedJobId);
        return ResponseEntity.ok(successWithFields(request, "lead_import_failed_rows_loaded", body));
    }

    @PostMapping("/import-jobs/{jobId}/failed-rows/export-jobs")
    public ResponseEntity<?> createFailedRowsExportJob(HttpServletRequest request,
                                                       @PathVariable String jobId,
                                                       @RequestParam(defaultValue = "") String errorCode,
                                                       @RequestParam(defaultValue = "") String from,
                                                       @RequestParam(defaultValue = "") String to) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String normalizedJobId = normalize(jobId);
        if (isBlank(normalizedJobId)) {
            return ResponseEntity.badRequest().body(errorBody(request, "bad_request", msg(request, "bad_request"), null));
        }
        String tenantId = normalize(currentTenant(request));
        String normalizedErrorCode = normalize(errorCode);
        String normalizedFrom = normalize(from);
        String normalizedTo = normalize(to);
        LocalDate fromDate = parseLocalDate(request, normalizedFrom);
        LocalDate toDate = parseLocalDate(request, normalizedTo);
        if ((fromDate == null && !isBlank(normalizedFrom)) || (toDate == null && !isBlank(normalizedTo))) {
            return ResponseEntity.badRequest().body(errorBody(request, "invalid_date_format", msg(request, "invalid_date_format"), null));
        }
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            return ResponseEntity.badRequest().body(errorBody(request, "date_range_invalid", msg(request, "date_range_invalid"), null));
        }
        try {
            Map<String, Object> job = leadImportFailedRowsExportJobService.submitByTenant(
                    tenantId,
                    normalizedJobId,
                    currentUser(request),
                    currentRole(request),
                    hasAnyRole(request, "ADMIN", "MANAGER"),
                    traceId(request),
                    request.getHeader("Accept-Language"),
                    normalizedErrorCode,
                    fromDate,
                    toDate
            );
            return ResponseEntity.accepted().body(successWithFields(request, "lead_import_export_submitted", job));
        } catch (IllegalArgumentException ex) {
            String code = normalizeCode(ex.getMessage(), "bad_request");
            if ("lead_import_job_not_found".equals(code) || "lead_import_export_job_not_found".equals(code)) {
                return ResponseEntity.status(404).body(errorBody(request, code, msg(request, code), null));
            }
            if ("lead_import_export_forbidden".equals(code) || "forbidden".equals(code)) {
                return ResponseEntity.status(403).body(errorBody(request, "lead_import_export_forbidden", msg(request, "lead_import_export_forbidden"), null));
            }
            if ("lead_import_export_limit_exceeded".equals(code)) {
                return ResponseEntity.status(409).body(errorBody(request, "lead_import_export_limit_exceeded", msg(request, "lead_import_export_limit_exceeded"), null));
            }
            return ResponseEntity.badRequest().body(errorBody(request, code, msg(request, code), null));
        }
    }

    @GetMapping("/import-jobs/{jobId}/failed-rows/export-jobs")
    public ResponseEntity<?> listFailedRowsExportJobs(HttpServletRequest request,
                                                      @PathVariable String jobId,
                                                      @RequestParam(defaultValue = "ALL") String status,
                                                      @RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "10") int size) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String normalizedJobId = normalize(jobId);
        if (isBlank(normalizedJobId)) {
            return ResponseEntity.badRequest().body(errorBody(request, "bad_request", msg(request, "bad_request"), null));
        }
        String tenantId = normalize(currentTenant(request));
        if (!leadImportJobRepository.findByIdAndTenantId(normalizedJobId, tenantId).isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "lead_import_job_not_found", msg(request, "lead_import_job_not_found"), null));
        }
        String normalizedStatus = normalizeToUpper(status, "ALL");
        Map<String, Object> body = leadImportFailedRowsExportJobService.listByTenant(
                tenantId,
                normalizedJobId,
                currentUser(request),
                hasAnyRole(request, "ADMIN", "MANAGER"),
                Math.max(1, page),
                Math.max(1, size),
                normalizedStatus
        );
        body.put("jobId", normalizedJobId);
        return ResponseEntity.ok(successWithFields(request, "lead_import_export_jobs_listed", body));
    }

    @GetMapping("/import-jobs/{jobId}/failed-rows/export-jobs/{exportJobId}/download")
    public ResponseEntity<?> downloadFailedRowsExportJob(HttpServletRequest request,
                                                         @PathVariable String jobId,
                                                         @PathVariable String exportJobId) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String normalizedJobId = normalize(jobId);
        String normalizedExportJobId = normalize(exportJobId);
        if (isBlank(normalizedJobId) || isBlank(normalizedExportJobId)) {
            return ResponseEntity.badRequest().body(errorBody(request, "bad_request", msg(request, "bad_request"), null));
        }
        try {
            String csv = leadImportFailedRowsExportJobService.downloadByTenant(
                    normalize(currentTenant(request)),
                    normalizedJobId,
                    normalizedExportJobId,
                    currentUser(request),
                    currentRole(request),
                    hasAnyRole(request, "ADMIN", "MANAGER"),
                    traceId(request)
            );
            boolean zh = request.getHeader("Accept-Language") != null && request.getHeader("Accept-Language").toLowerCase(Locale.ROOT).startsWith("zh");
            String filename = (zh ? "\u7ebf\u7d22\u5bfc\u5165\u5931\u8d25\u660e\u7ec6-" : "lead-import-failed-rows-") + normalizedExportJobId + ".csv";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(csv);
        } catch (IllegalArgumentException ex) {
            String code = normalizeCode(ex.getMessage(), "bad_request");
            if ("lead_import_export_job_not_found".equals(code) || "lead_import_job_not_found".equals(code)) {
                return ResponseEntity.status(404).body(errorBody(request, "lead_import_export_job_not_found", msg(request, "lead_import_export_job_not_found"), null));
            }
            if ("lead_import_export_forbidden".equals(code) || "forbidden".equals(code)) {
                return ResponseEntity.status(403).body(errorBody(request, "lead_import_export_forbidden", msg(request, "lead_import_export_forbidden"), null));
            }
            return ResponseEntity.badRequest().body(errorBody(request, code, msg(request, code), null));
        } catch (IllegalStateException ex) {
            String code = normalizeCode(ex.getMessage(), "lead_import_export_job_not_ready");
            return ResponseEntity.status(409).body(errorBody(request, code, msg(request, code), null));
        }
    }

    private void applyPayload(HttpServletRequest request, Lead lead, V1LeadUpsertRequest payload, boolean creating) {
        lead.setName(payload.getName().trim());
        lead.setCompany(isBlank(payload.getCompany()) ? null : payload.getCompany().trim());
        lead.setPhone(isBlank(payload.getPhone()) ? null : payload.getPhone().trim());
        lead.setEmail(isBlank(payload.getEmail()) ? null : payload.getEmail().trim());
        lead.setSource(isBlank(payload.getSource()) ? null : payload.getSource().trim());

        String status = isBlank(payload.getStatus()) ? (creating ? "NEW" : lead.getStatus()) : payload.getStatus().trim().toUpperCase(Locale.ROOT);
        if (!LeadStatusEnum.isValid(status)) {
            throw new IllegalArgumentException("invalid_lead_status");
        }
        if (!creating && !canTransitLeadStatus(lead.getStatus(), status)) {
            throw new IllegalStateException("lead_status_transition_invalid");
        }
        lead.setStatus(status);

        String owner;
        if (isSalesScoped(request)) {
            owner = currentOwnerScope(request);
        } else {
            owner = isBlank(payload.getOwner()) ? (creating ? currentUser(request) : lead.getOwner()) : payload.getOwner().trim();
        }
        lead.setOwner(isBlank(owner) ? currentUser(request) : owner);
    }

    private ResponseEntity<?> leadStatusTransitionConflict(HttpServletRequest request, String currentStatus, String targetStatus) {
        String current = normalizeStatusValue(currentStatus);
        String target = normalizeStatusValue(targetStatus);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("from", current);
        details.put("to", target);
        details.put("allowed", new ArrayList<String>(allowedLeadTransitions(current)));
        return ResponseEntity.status(409).body(errorBody(request, "lead_status_transition_invalid", msg(request, "lead_status_transition_invalid"), details));
    }

    private Map<String, Object> buildImportConflictDetails(String tenantId, String jobId, String action, String code) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("jobId", jobId);
        details.put("action", action);
        details.put("tenantId", tenantId);
        if ("cancel".equals(action)) {
            details.put("allowedFrom", new ArrayList<String>(IMPORT_CANCEL_ALLOWED_STATUSES));
        } else if ("retry".equals(action)) {
            details.put("allowedFrom", new ArrayList<String>(IMPORT_RETRY_ALLOWED_STATUSES));
        }
        if ("lead_import_retry_no_pending_chunks".equals(normalizeCode(code, ""))) {
            details.put("reason", "no_pending_chunks");
        }
        leadImportJobRepository.findByIdAndTenantId(jobId, tenantId).ifPresent(job -> {
            details.put("currentStatus", normalizeStatusValue(job.getStatus()));
            details.put("cancelRequested", Boolean.TRUE.equals(job.getCancelRequested()));
            Map<String, Object> view = leadImportService.toView(job);
            details.put("taskStats", view.get("taskStats"));
            details.put("failureSummary", view.get("failureSummary"));
        });
        return details;
    }

    private boolean canTransitLeadStatus(String currentStatus, String targetStatus) {
        String current = normalizeStatusValue(currentStatus);
        String target = normalizeStatusValue(targetStatus);
        if (isBlank(current) || isBlank(target)) return false;
        if (current.equals(target)) return true;
        return allowedLeadTransitions(current).contains(target);
    }

    private Set<String> allowedLeadTransitions(String currentStatus) {
        String current = normalizeStatusValue(currentStatus);
        if ("NEW".equals(current)) {
            return new LinkedHashSet<String>(Arrays.asList("QUALIFIED", "NURTURING", "DISQUALIFIED"));
        }
        if ("QUALIFIED".equals(current)) {
            return new LinkedHashSet<String>(Arrays.asList("NURTURING", "DISQUALIFIED"));
        }
        if ("NURTURING".equals(current)) {
            return new LinkedHashSet<String>(Arrays.asList("QUALIFIED", "DISQUALIFIED"));
        }
        if ("DISQUALIFIED".equals(current)) {
            return new LinkedHashSet<String>(Arrays.asList("NURTURING", "QUALIFIED"));
        }
        return Collections.emptySet();
    }

    private String normalizeStatusValue(String value) {
        return isBlank(value) ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeToUpper(String value, String fallback) {
        String normalized = normalize(value);
        if (normalized.isEmpty()) {
            normalized = normalize(fallback);
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private Map<String, Object> toLeadView(Lead lead) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", lead.getId());
        out.put("name", lead.getName());
        out.put("company", lead.getCompany());
        out.put("phone", lead.getPhone());
        out.put("email", lead.getEmail());
        out.put("status", lead.getStatus());
        out.put("owner", lead.getOwner());
        out.put("source", lead.getSource());
        out.put("tenantId", lead.getTenantId());
        out.put("createdAt", lead.getCreatedAt());
        out.put("updatedAt", lead.getUpdatedAt());
        return out;
    }

    private String newId(String prefix) {
        return prefix + "_" + Long.toString(System.currentTimeMillis(), 36) + String.format("%03d", (int) (Math.random() * 1000));
    }
}
