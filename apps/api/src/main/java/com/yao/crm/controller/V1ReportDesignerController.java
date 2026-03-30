package com.yao.crm.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yao.crm.dto.request.V1ReportDesignerRunRequest;
import com.yao.crm.dto.request.V1ReportDesignerTemplateRequest;
import com.yao.crm.entity.*;
import com.yao.crm.repository.*;
import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.I18nService;
import com.yao.crm.util.CollectionsUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.*;

@RestController
@RequestMapping("/api/v1/reports/designer")
public class V1ReportDesignerController extends BaseApiController {

    private static final Set<String> DATASETS = CollectionsUtil.setOf("CUSTOMERS", "OPPORTUNITIES", "CONTRACTS", "PAYMENTS", "LEADS");
    private static final Set<String> VISIBILITY = CollectionsUtil.setOf("PRIVATE", "DEPARTMENT", "TENANT");

    private final ReportDesignerTemplateRepository templateRepository;
    private final CustomerRepository customerRepository;
    private final OpportunityRepository opportunityRepository;
    private final ContractRecordRepository contractRecordRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final LeadRepository leadRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public V1ReportDesignerController(ReportDesignerTemplateRepository templateRepository,
                                      CustomerRepository customerRepository,
                                      OpportunityRepository opportunityRepository,
                                      ContractRecordRepository contractRecordRepository,
                                      PaymentRecordRepository paymentRecordRepository,
                                      LeadRepository leadRepository,
                                      AuditLogService auditLogService,
                                      ObjectMapper objectMapper,
                                      I18nService i18nService) {
        super(i18nService);
        this.templateRepository = templateRepository;
        this.customerRepository = customerRepository;
        this.opportunityRepository = opportunityRepository;
        this.contractRecordRepository = contractRecordRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.leadRepository = leadRepository;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/datasets/query")
    public ResponseEntity<?> queryDataset(HttpServletRequest request, @RequestBody(required = false) V1ReportDesignerRunRequest payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        String dataset = normalizeDataset(payload == null ? null : payload.getDataset());
        if (!DATASETS.contains(dataset)) {
            return ResponseEntity.badRequest().body(errorBody(request, "report_dataset_invalid", msg(request, "report_dataset_invalid"), null));
        }
        Map<String, Object> body = buildDatasetResult(tenantId, dataset, payload == null ? null : payload.getConfig());
        return ResponseEntity.ok(successWithFields(request, "report_dataset_queried", body));
    }

    @PostMapping("/templates")
    public ResponseEntity<?> createTemplate(HttpServletRequest request, @Valid @RequestBody V1ReportDesignerTemplateRequest payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        String dataset = normalizeDataset(payload.getDataset());
        if (!DATASETS.contains(dataset)) {
            return ResponseEntity.badRequest().body(errorBody(request, "report_dataset_invalid", msg(request, "report_dataset_invalid"), null));
        }
        String visibility = normalizeVisibility(payload.getVisibility());
        if (!VISIBILITY.contains(visibility)) {
            return ResponseEntity.badRequest().body(errorBody(request, "report_visibility_invalid", msg(request, "report_visibility_invalid"), null));
        }
        String name = payload.getName().trim();
        if (templateRepository.existsByTenantIdAndName(tenantId, name)) {
            return ResponseEntity.status(409).body(errorBody(request, "report_template_name_exists", msg(request, "report_template_name_exists"), null));
        }
        ReportDesignerTemplate template = new ReportDesignerTemplate();
        template.setId(newId("rpt"));
        template.setTenantId(tenantId);
        template.setName(name);
        template.setDataset(dataset);
        template.setVisibility(visibility);
        template.setOwner(currentUser(request));
        template.setDepartment(isBlank(payload.getDepartment()) ? null : payload.getDepartment().trim());
        template.setConfigJson(writeJson(payload.getConfig()));
        template.setVersion(1);
        template = templateRepository.save(template);
        auditLogService.record(currentUser(request), currentRole(request), "CREATE", "REPORT_TEMPLATE", template.getId(), "Created report template", tenantId);
        return ResponseEntity.status(201).body(successWithFields(request, "report_template_created", toTemplateView(template)));
    }

    @GetMapping("/templates")
    public ResponseEntity<?> listTemplates(HttpServletRequest request) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "ANALYST", "SALES")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (ReportDesignerTemplate row : templateRepository.findByTenantIdOrderByUpdatedAtDesc(tenantId)) {
            if (canSeeTemplate(request, row)) {
                items.add(toTemplateView(row));
            }
        }
        return ResponseEntity.ok(successWithFields(request, "report_templates_listed", Collections.<String, Object>singletonMap("items", items)));
    }

    @GetMapping("/templates/{id}")
    public ResponseEntity<?> getTemplate(HttpServletRequest request, @PathVariable String id) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "ANALYST", "SALES")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String templateId = normalizeTemplateId(id);
        if (isBlank(templateId)) {
            return ResponseEntity.badRequest().body(errorBody(request, "bad_request", msg(request, "bad_request"), null));
        }
        String tenantId = currentTenant(request);
        Optional<ReportDesignerTemplate> optional = templateRepository.findByIdAndTenantId(templateId, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "report_template_not_found", msg(request, "report_template_not_found"), null));
        }
        ReportDesignerTemplate template = optional.get();
        if (!canSeeTemplate(request, template)) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        return ResponseEntity.ok(successWithFields(request, "report_template_loaded", toTemplateView(template)));
    }

    @PatchMapping("/templates/{id}")
    public ResponseEntity<?> patchTemplate(HttpServletRequest request, @PathVariable String id, @RequestBody V1ReportDesignerTemplateRequest payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String templateId = normalizeTemplateId(id);
        if (isBlank(templateId)) {
            return ResponseEntity.badRequest().body(errorBody(request, "bad_request", msg(request, "bad_request"), null));
        }
        String tenantId = currentTenant(request);
        Optional<ReportDesignerTemplate> optional = templateRepository.findByIdAndTenantId(templateId, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "report_template_not_found", msg(request, "report_template_not_found"), null));
        }
        ReportDesignerTemplate template = optional.get();
        if (!canManageTemplate(request, template)) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        if (!isBlank(payload.getName())) template.setName(payload.getName().trim());
        if (templateRepository.existsByTenantIdAndNameAndIdNot(tenantId, template.getName(), template.getId())) {
            return ResponseEntity.status(409).body(errorBody(request, "report_template_name_exists", msg(request, "report_template_name_exists"), null));
        }
        if (!isBlank(payload.getDataset())) {
            String dataset = normalizeDataset(payload.getDataset());
            if (!DATASETS.contains(dataset)) {
                return ResponseEntity.badRequest().body(errorBody(request, "report_dataset_invalid", msg(request, "report_dataset_invalid"), null));
            }
            template.setDataset(dataset);
        }
        if (!isBlank(payload.getVisibility())) {
            String visibility = normalizeVisibility(payload.getVisibility());
            if (!VISIBILITY.contains(visibility)) {
                return ResponseEntity.badRequest().body(errorBody(request, "report_visibility_invalid", msg(request, "report_visibility_invalid"), null));
            }
            template.setVisibility(visibility);
        }
        if (payload.getDepartment() != null) template.setDepartment(isBlank(payload.getDepartment()) ? null : payload.getDepartment().trim());
        if (payload.getConfig() != null) template.setConfigJson(writeJson(payload.getConfig()));
        template.setVersion((template.getVersion() == null ? 0 : template.getVersion()) + 1);
        template = templateRepository.save(template);
        auditLogService.record(currentUser(request), currentRole(request), "UPDATE", "REPORT_TEMPLATE", template.getId(), "Updated report template", tenantId);
        return ResponseEntity.ok(successWithFields(request, "report_template_updated", toTemplateView(template)));
    }

    @PostMapping("/templates/{id}/run")
    public ResponseEntity<?> runTemplate(HttpServletRequest request, @PathVariable String id, @RequestBody(required = false) V1ReportDesignerRunRequest payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "ANALYST", "SALES")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String templateId = normalizeTemplateId(id);
        if (isBlank(templateId)) {
            return ResponseEntity.badRequest().body(errorBody(request, "bad_request", msg(request, "bad_request"), null));
        }
        String tenantId = currentTenant(request);
        Optional<ReportDesignerTemplate> optional = templateRepository.findByIdAndTenantId(templateId, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "report_template_not_found", msg(request, "report_template_not_found"), null));
        }
        ReportDesignerTemplate template = optional.get();
        if (!canSeeTemplate(request, template)) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String dataset = normalizeDataset(isBlank(payload == null ? null : payload.getDataset()) ? template.getDataset() : payload.getDataset());
        if (!DATASETS.contains(dataset)) {
            return ResponseEntity.badRequest().body(errorBody(request, "report_dataset_invalid", msg(request, "report_dataset_invalid"), null));
        }
        JsonNode config = payload == null || payload.getConfig() == null ? readConfig(template.getConfigJson()) : payload.getConfig();
        Map<String, Object> result = buildDatasetResult(tenantId, dataset, config);
        result.put("templateId", template.getId());
        result.put("templateVersion", template.getVersion());
        return ResponseEntity.ok(successWithFields(request, "report_template_run_completed", result));
    }

    private Map<String, Object> buildDatasetResult(String tenantId, String dataset, JsonNode config) {
        int limit = 100;
        if (config != null && config.has("limit")) {
            try {
                limit = Math.max(1, Math.min(200, config.get("limit").asInt(100)));
            } catch (Exception ignore) {
                limit = 100;
            }
        }
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        if ("CUSTOMERS".equals(dataset)) {
            for (Customer row : customerRepository.findByTenantId(tenantId)) rows.add(objectMapper.convertValue(row, new TypeReference<Map<String, Object>>() {}));
        } else if ("OPPORTUNITIES".equals(dataset)) {
            for (Opportunity row : opportunityRepository.findByTenantId(tenantId)) rows.add(objectMapper.convertValue(row, new TypeReference<Map<String, Object>>() {}));
        } else if ("CONTRACTS".equals(dataset)) {
            for (ContractRecord row : contractRecordRepository.findByTenantId(tenantId)) rows.add(objectMapper.convertValue(row, new TypeReference<Map<String, Object>>() {}));
        } else if ("PAYMENTS".equals(dataset)) {
            for (PaymentRecord row : paymentRecordRepository.findByTenantId(tenantId)) rows.add(objectMapper.convertValue(row, new TypeReference<Map<String, Object>>() {}));
        } else if ("LEADS".equals(dataset)) {
            for (Lead row : leadRepository.findByTenantId(tenantId)) rows.add(objectMapper.convertValue(row, new TypeReference<Map<String, Object>>() {}));
        }
        if (rows.size() > limit) {
            rows = rows.subList(0, limit);
        }
        Set<String> fields = new LinkedHashSet<String>();
        for (Map<String, Object> row : rows) fields.addAll(row.keySet());
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("dataset", dataset);
        out.put("fields", new ArrayList<String>(fields));
        out.put("rows", rows);
        out.put("count", rows.size());
        return out;
    }

    private boolean canSeeTemplate(HttpServletRequest request, ReportDesignerTemplate row) {
        if ("TENANT".equalsIgnoreCase(row.getVisibility())) return true;
        if ("PRIVATE".equalsIgnoreCase(row.getVisibility())) return currentUser(request).equalsIgnoreCase(row.getOwner());
        if ("DEPARTMENT".equalsIgnoreCase(row.getVisibility())) {
            String currentDepartment = String.valueOf(request.getAttribute("authDepartment") == null ? "" : request.getAttribute("authDepartment"));
            if (!isBlank(currentDepartment) && !isBlank(row.getDepartment())) {
                return currentDepartment.equalsIgnoreCase(row.getDepartment());
            }
            return currentUser(request).equalsIgnoreCase(row.getOwner());
        }
        return currentUser(request).equalsIgnoreCase(row.getOwner());
    }

    private boolean canManageTemplate(HttpServletRequest request, ReportDesignerTemplate row) {
        if (hasAnyRole(request, "ADMIN", "MANAGER")) return true;
        return currentUser(request).equalsIgnoreCase(row.getOwner());
    }

    private Map<String, Object> toTemplateView(ReportDesignerTemplate row) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("id", row.getId());
        out.put("tenantId", row.getTenantId());
        out.put("name", row.getName());
        out.put("dataset", row.getDataset());
        out.put("visibility", row.getVisibility());
        out.put("owner", row.getOwner());
        out.put("department", row.getDepartment());
        out.put("version", row.getVersion());
        out.put("config", readConfig(row.getConfigJson()));
        out.put("createdAt", row.getCreatedAt());
        out.put("updatedAt", row.getUpdatedAt());
        return out;
    }

    private String normalizeDataset(String dataset) {
        return isBlank(dataset) ? "" : dataset.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeVisibility(String visibility) {
        return isBlank(visibility) ? "PRIVATE" : visibility.trim().toUpperCase(Locale.ROOT);
    }

    private String writeJson(JsonNode node) {
        try {
            return node == null ? "{}" : objectMapper.writeValueAsString(node);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private JsonNode readConfig(String raw) {
        try {
            return objectMapper.readTree(isBlank(raw) ? "{}" : raw);
        } catch (Exception ex) {
            return objectMapper.createObjectNode();
        }
    }

    private String newId(String prefix) {
        return prefix + "_" + Long.toString(System.currentTimeMillis(), 36) + String.format("%03d", (int) (Math.random() * 1000));
    }

    private String normalizeTemplateId(String value) {
        return isBlank(value) ? "" : value.trim();
    }
}
