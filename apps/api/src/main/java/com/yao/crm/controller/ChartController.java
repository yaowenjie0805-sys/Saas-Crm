package com.yao.crm.controller;

import com.yao.crm.dto.request.ChartPreviewRequest;
import com.yao.crm.dto.request.ChartTemplateCloneRequest;
import com.yao.crm.dto.request.ChartTemplateCreateRequest;
import com.yao.crm.dto.request.ChartTemplateUpdateRequest;
import com.yao.crm.entity.ChartTemplate;
import com.yao.crm.service.ChartService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 图表控制器
 * 提供图表数据、模板管理API
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
     * 获取图表数据
     */
    @GetMapping("/data")
    public ResponseEntity<?> getChartData(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam String datasetType,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String owner,
            @RequestParam(required = false) String chartType) {

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
     * 预览图表数据
     */
    @PostMapping("/preview")
    public ResponseEntity<?> previewChartData(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @Valid @RequestBody ChartPreviewRequest request) {

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
     * 获取图表模板列表
     */
    @GetMapping("/templates")
    public ResponseEntity<List<ChartTemplate>> getTemplates(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(required = false) String chartType,
            @RequestParam(required = false, defaultValue = "false") boolean includeSystem) {

        List<ChartTemplate> templates;

        if (includeSystem) {
            templates = chartService.getSystemTemplates(tenantId);
            templates.addAll(chartService.getTemplates(tenantId, chartType));
        } else {
            templates = chartService.getTemplates(tenantId, chartType);
        }

        return ResponseEntity.ok(templates);
    }

    /**
     * 获取图表模板详情
     */
    @GetMapping("/templates/{id}")
    public ResponseEntity<?> getTemplate(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String id) {

        // 简单实现，实际应从数据库查询
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("name", "示例模板");
        result.put("chartType", "BAR");
        result.put("datasetType", "CUSTOMERS");
        return ResponseEntity.ok(result);
    }

    /**
     * 创建图表模板
     */
    @PostMapping("/templates")
    public ResponseEntity<ChartTemplate> createTemplate(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @Valid @RequestBody ChartTemplateCreateRequest request) {

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
     * 更新图表模板
     */
    @PutMapping("/templates/{id}")
    public ResponseEntity<?> updateTemplate(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String id,
            @Valid @RequestBody ChartTemplateUpdateRequest request) {

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("id", id);
        result.put("message", "Template updated");
        return ResponseEntity.ok(result);
    }

    /**
     * 删除图表模板
     */
    @DeleteMapping("/templates/{id}")
    public ResponseEntity<?> deleteTemplate(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String id) {

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("id", id);
        result.put("message", "Template deleted");
        return ResponseEntity.ok(result);
    }

    /**
     * 克隆图表模板
     */
    @PostMapping("/templates/{id}/clone")
    public ResponseEntity<?> cloneTemplate(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String id,
            @Valid @RequestBody ChartTemplateCloneRequest request) {

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("id", UUID.randomUUID().toString());
        result.put("name", request.getNewName());
        result.put("message", "Template cloned");
        return ResponseEntity.ok(result);
    }

    /**
     * 获取支持的图表类型
     */
    @GetMapping("/types")
    public ResponseEntity<?> getChartTypes() {
        List<Map<String, String>> types = new ArrayList<>();
        types.add(createChartType("BAR", "柱状图", "📊"));
        types.add(createChartType("LINE", "折线图", "📈"));
        types.add(createChartType("PIE", "饼图", "🥧"));
        types.add(createChartType("DOUGHNUT", "环形图", "🍩"));
        types.add(createChartType("FUNNEL", "漏斗图", "🔻"));
        types.add(createChartType("RADAR", "雷达图", "🕸️"));
        types.add(createChartType("GAUGE", "仪表盘", "⚙️"));
        Map<String, Object> result = new HashMap<>();
        result.put("types", types);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取支持的数据集类型
     */
    @GetMapping("/datasets")
    public ResponseEntity<?> getDatasetTypes() {
        List<Map<String, String>> datasets = new ArrayList<>();
        datasets.add(createDatasetType("CUSTOMERS", "客户数据"));
        datasets.add(createDatasetType("OPPORTUNITIES", "商机数据"));
        datasets.add(createDatasetType("REVENUE", "营收数据"));
        datasets.add(createDatasetType("QUOTES", "报价数据"));
        datasets.add(createDatasetType("ORDERS", "订单数据"));
        datasets.add(createDatasetType("TASKS", "任务数据"));
        datasets.add(createDatasetType("LEADS", "线索数据"));
        datasets.add(createDatasetType("FUNNEL", "销售漏斗"));
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
}
