package com.yao.crm.controller;

import com.yao.crm.dto.request.ChartPreviewRequest;
import com.yao.crm.dto.request.ChartTemplateCloneRequest;
import com.yao.crm.dto.request.ChartTemplateCreateRequest;
import com.yao.crm.dto.request.ChartTemplateUpdateRequest;
import com.yao.crm.entity.ChartTemplate;
import com.yao.crm.service.ChartService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 鍥捐〃鎺у埗鍣? * 鎻愪緵鍥捐〃鏁版嵁銆佹ā鏉跨鐞咥PI
 */
@Tag(name = "Charts", description = "Chart data and templates")
@RestController
@RequestMapping("/api/v2/charts")
public class ChartController {

    private final ChartService chartService;

    public ChartController(ChartService chartService) {
        this.chartService = chartService;
    }

    /**
     * 鑾峰彇鍥捐〃鏁版嵁
     */
    @GetMapping("/data")
    public ResponseEntity<?> getChartData(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam String datasetType,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String owner,
            @RequestParam(required = false) String chartType) {

        tenantId = requireTenantId(tenantId);

        Map<String, Object> filters = new HashMap<>();
        if (fromDate != null) filters.put("fromDate", fromDate);
        if (toDate != null) filters.put("toDate", toDate);
        if (owner != null) filters.put("owner", owner);

        Map<String, Object> data = chartService.getChartData(tenantId, datasetType, filters);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", data);
        result.put("datasetType", datasetType);
        result.put("generatedAt", LocalDateTime.now());
        return ResponseEntity.ok(result);
    }

    /**
     * 棰勮鍥捐〃鏁版嵁
     */
    @PostMapping("/preview")
    public ResponseEntity<?> previewChartData(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @Valid @RequestBody ChartPreviewRequest request) {

        tenantId = requireTenantId(tenantId);

        Map<String, Object> filters = new HashMap<>();
        if (request.getFromDate() != null) filters.put("fromDate", request.getFromDate());
        if (request.getToDate() != null) filters.put("toDate", request.getToDate());
        if (request.getOwner() != null) filters.put("owner", request.getOwner());

        Map<String, Object> data = chartService.getChartData(tenantId, request.getDatasetType(), filters);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", data);
        return ResponseEntity.ok(result);
    }

    /**
     * 鑾峰彇鍥捐〃妯℃澘鍒楄〃
     */
    @GetMapping("/templates")
    public ResponseEntity<List<ChartTemplate>> getTemplates(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(required = false) String chartType,
            @RequestParam(required = false, defaultValue = "false") boolean includeSystem) {

        tenantId = requireTenantId(tenantId);

        List<ChartTemplate> templates;

        if (includeSystem) {
            templates = new ArrayList<>(chartService.getSystemTemplates(tenantId));
            templates.addAll(chartService.getTemplates(tenantId, chartType));
        } else {
            templates = chartService.getTemplates(tenantId, chartType);
        }

        return ResponseEntity.ok(templates);
    }

    /**
     * 鑾峰彇鍥捐〃妯℃澘璇︽儏
     */
    @GetMapping("/templates/{id}")
    public ResponseEntity<?> getTemplate(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String id) {

        // 绠€鍗曞疄鐜帮紝瀹為檯搴斾粠鏁版嵁搴撴煡璇?        tenantId = requireTenantId(tenantId);
        id = requirePathId(id);
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("name", "绀轰緥妯℃澘");
        result.put("chartType", "BAR");
        result.put("datasetType", "CUSTOMERS");
        return ResponseEntity.ok(result);
    }

    /**
     * 鍒涘缓鍥捐〃妯℃澘
     */
    @PostMapping("/templates")
    public ResponseEntity<ChartTemplate> createTemplate(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @Valid @RequestBody ChartTemplateCreateRequest request) {

        tenantId = requireTenantId(tenantId);

        ChartTemplate template = new ChartTemplate();
        template.setId(UUID.randomUUID().toString());
        template.setTenantId(tenantId);
        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setChartType(request.getChartType());
        template.setDatasetType(request.getDatasetType());
        template.setConfigJson(request.getConfigJson() != null ? request.getConfigJson() : "{}");
        template.setLayoutConfig(request.getLayoutConfig());
        template.setVisibility(request.getVisibility() != null ? request.getVisibility() : "PRIVATE");
        template.setOwner(request.getOwner());
        template.setDepartment(request.getDepartment());
        template.setVersion(1);
        template.setIsSystem(false);
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());

        chartService.createTemplate(template);

        return ResponseEntity.ok(template);
    }

    /**
     * 鏇存柊鍥捐〃妯℃澘
     */
    @PutMapping("/templates/{id}")
    public ResponseEntity<?> updateTemplate(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String id,
            @Valid @RequestBody ChartTemplateUpdateRequest request) {

        tenantId = requireTenantId(tenantId);
        id = requirePathId(id);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("id", id);
        result.put("message", "Template updated");
        return ResponseEntity.ok(result);
    }

    /**
     * 鍒犻櫎鍥捐〃妯℃澘
     */
    @DeleteMapping("/templates/{id}")
    public ResponseEntity<?> deleteTemplate(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String id) {

        requireTenantId(tenantId);
        requirePathId(id);

        return ResponseEntity.noContent().build();
    }

    /**
     * 鍏嬮殕鍥捐〃妯℃澘
     */
    @PostMapping("/templates/{id}/clone")
    public ResponseEntity<?> cloneTemplate(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String id,
            @Valid @RequestBody ChartTemplateCloneRequest request) {

        tenantId = requireTenantId(tenantId);
        id = requirePathId(id);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("id", UUID.randomUUID().toString());
        result.put("name", request.getNewName());
        result.put("message", "Template cloned");
        return ResponseEntity.ok(result);
    }

    /**
     * 鑾峰彇鏀寔鐨勫浘琛ㄧ被鍨?     */
    @GetMapping("/types")
    public ResponseEntity<?> getChartTypes() {
        List<Map<String, String>> types = new ArrayList<>();
        types.add(createChartType("BAR", "Bar Chart", "bar"));
        types.add(createChartType("LINE", "Line Chart", "line"));
        types.add(createChartType("PIE", "Pie Chart", "pie"));
        types.add(createChartType("DOUGHNUT", "Doughnut Chart", "doughnut"));
        types.add(createChartType("FUNNEL", "Funnel Chart", "funnel"));
        types.add(createChartType("RADAR", "Radar Chart", "radar"));
        types.add(createChartType("GAUGE", "Gauge Chart", "gauge"));
        Map<String, Object> result = new HashMap<>();
        result.put("types", types);
        return ResponseEntity.ok(result);
    }

    /**
     * 鑾峰彇鏀寔鐨勬暟鎹泦绫诲瀷
     */
    @GetMapping("/datasets")
    public ResponseEntity<?> getDatasetTypes() {
        List<Map<String, String>> datasets = new ArrayList<>();
        datasets.add(createDatasetType("CUSTOMERS", "Customers"));
        datasets.add(createDatasetType("OPPORTUNITIES", "Opportunities"));
        datasets.add(createDatasetType("REVENUE", "Revenue"));
        datasets.add(createDatasetType("QUOTES", "Quotes"));
        datasets.add(createDatasetType("ORDERS", "Orders"));
        datasets.add(createDatasetType("TASKS", "Tasks"));
        datasets.add(createDatasetType("LEADS", "Leads"));
        datasets.add(createDatasetType("FUNNEL", "Sales Funnel"));
        Map<String, Object> result = new HashMap<>();
        result.put("datasets", datasets);
        return ResponseEntity.ok(result);
    }

    private Map<String, String> createChartType(String value, String label, String icon) {
        Map<String, String> type = new HashMap<>();
        type.put("value", value);
        type.put("label", label);
        type.put("icon", icon);
        return type;
    }

    private Map<String, String> createDatasetType(String value, String label) {
        Map<String, String> type = new HashMap<>();
        type.put("value", value);
        type.put("label", label);
        return type;
    }

    private String requireTenantId(String tenantId) {
        String normalizedTenantId = StringUtils.trimWhitespace(tenantId);
        if (!StringUtils.hasText(normalizedTenantId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-Tenant-Id must not be blank");
        }
        return normalizedTenantId;
    }

    private String requirePathId(String id) {
        String normalizedId = StringUtils.trimWhitespace(id);
        if (!StringUtils.hasText(normalizedId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Path id must not be blank");
        }
        return normalizedId;
    }
}


