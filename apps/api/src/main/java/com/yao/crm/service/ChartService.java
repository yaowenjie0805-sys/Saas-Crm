package com.yao.crm.service;

import com.yao.crm.entity.ChartTemplate;
import com.yao.crm.repository.ChartTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 图表服务
 * 提供图表数据计算和配置管理
 */
@Service
public class ChartService {

    private final ChartTemplateRepository chartTemplateRepository;
    private final ReportService reportService;

    public ChartService(ChartTemplateRepository chartTemplateRepository, ReportService reportService) {
        this.chartTemplateRepository = chartTemplateRepository;
        this.reportService = reportService;
    }

    /**
     * 获取图表数据
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getChartData(String tenantId, String datasetType, Map<String, Object> filters) {
        LocalDate fromDate = filters.containsKey("fromDate")
                ? LocalDate.parse((String) filters.get("fromDate"))
                : LocalDate.now().minusMonths(1);
        LocalDate toDate = filters.containsKey("toDate")
                ? LocalDate.parse((String) filters.get("toDate"))
                : LocalDate.now();
        String owner = (String) filters.getOrDefault("owner", "");

        switch (datasetType.toUpperCase()) {
            case "CUSTOMERS":
                return getCustomerChartData(tenantId, fromDate, toDate, owner);
            case "OPPORTUNITIES":
                return getOpportunityChartData(tenantId, fromDate, toDate, owner);
            case "REVENUE":
                return getRevenueChartData(tenantId, fromDate, toDate, owner);
            case "QUOTES":
                return getQuoteChartData(tenantId, fromDate, toDate, owner);
            case "ORDERS":
                return getOrderChartData(tenantId, fromDate, toDate, owner);
            case "TASKS":
                return getTaskChartData(tenantId, fromDate, toDate, owner);
            case "LEADS":
                return getLeadChartData(tenantId, fromDate, toDate, owner);
            case "FUNNEL":
                return getFunnelChartData(tenantId, fromDate, toDate, owner);
            default:
                return getDefaultChartData();
        }
    }

    /**
     * 客户图表数据
     */
    private Map<String, Object> getCustomerChartData(String tenantId, LocalDate fromDate, LocalDate toDate, String owner) {
        Map<String, Object> data = reportService.overviewByTenant(tenantId, fromDate, toDate, null, owner, "");

        Map<String, Object> chartData = new HashMap<>();
        chartData.put("type", "BAR");

        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();

        // 按负责人分布
        Map<String, Integer> byOwner = (Map<String, Integer>) data.get("customerByOwner");
        if (byOwner != null) {
            byOwner.forEach((key, value) -> {
                labels.add(key);
                values.add(value.longValue());
            });
        }

        chartData.put("labels", labels);
        Map<String, Object> dataset = new HashMap<>();
        dataset.put("label", "客户数量");
        dataset.put("data", values);
        List<String> colors = new ArrayList<>();
        colors.add("#3B82F6");
        colors.add("#10B981");
        colors.add("#F59E0B");
        colors.add("#EF4444");
        colors.add("#8B5CF6");
        dataset.put("backgroundColor", colors);
        List<Map<String, Object>> datasets = new ArrayList<>();
        datasets.add(dataset);
        chartData.put("datasets", datasets);

        return chartData;
    }

    /**
     * 商机图表数据
     */
    private Map<String, Object> getOpportunityChartData(String tenantId, LocalDate fromDate, LocalDate toDate, String owner) {
        Map<String, Object> data = reportService.overviewByTenant(tenantId, fromDate, toDate, null, owner, "");

        Map<String, Object> chartData = new HashMap<>();
        chartData.put("type", "LINE");

        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();

        // 按阶段分布
        Map<String, Integer> byStage = (Map<String, Integer>) data.get("opportunityByStage");
        if (byStage != null) {
            byStage.forEach((key, value) -> {
                labels.add(key);
                values.add(value.longValue());
            });
        }

        chartData.put("labels", labels);
        Map<String, Object> dataset = new HashMap<>();
        dataset.put("label", "商机数量");
        dataset.put("data", values);
        dataset.put("borderColor", "#3B82F6");
        dataset.put("fill", false);
        List<Map<String, Object>> datasets = new ArrayList<>();
        datasets.add(dataset);
        chartData.put("datasets", datasets);

        return chartData;
    }

    /**
     * 收入图表数据
     */
    private Map<String, Object> getRevenueChartData(String tenantId, LocalDate fromDate, LocalDate toDate, String owner) {
        Map<String, Object> data = reportService.overviewByTenant(tenantId, fromDate, toDate, null, owner, "");

        Map<String, Object> chartData = new HashMap<>();
        chartData.put("type", "LINE");

        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();

        // 按状态营收分布
        Map<String, Long> byStatus = (Map<String, Long>) data.get("revenueByStatus");
        if (byStatus != null) {
            byStatus.forEach((key, value) -> {
                labels.add(key);
                values.add(value);
            });
        }

        chartData.put("labels", labels);
        Map<String, Object> dataset = new HashMap<>();
        dataset.put("label", "营收金额");
        dataset.put("data", values);
        dataset.put("borderColor", "#10B981");
        dataset.put("fill", true);
        dataset.put("backgroundColor", "rgba(16, 185, 129, 0.1)");
        List<Map<String, Object>> datasets = new ArrayList<>();
        datasets.add(dataset);
        chartData.put("datasets", datasets);

        return chartData;
    }

    /**
     * 报价图表数据
     */
    private Map<String, Object> getQuoteChartData(String tenantId, LocalDate fromDate, LocalDate toDate, String owner) {
        Map<String, Object> data = reportService.overviewByTenant(tenantId, fromDate, toDate, null, owner, "");

        Map<String, Object> chartData = new HashMap<>();
        chartData.put("type", "PIE");

        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();

        // 按状态分布
        Map<String, Integer> byStatus = (Map<String, Integer>) data.get("quoteByStatus");
        if (byStatus != null) {
            byStatus.forEach((key, value) -> {
                labels.add(key);
                values.add(value.longValue());
            });
        }

        chartData.put("labels", labels);
        Map<String, Object> dataset = new HashMap<>();
        dataset.put("data", values);
        List<String> colors = new ArrayList<>();
        colors.add("#3B82F6");
        colors.add("#10B981");
        colors.add("#F59E0B");
        colors.add("#EF4444");
        dataset.put("backgroundColor", colors);
        List<Map<String, Object>> datasets = new ArrayList<>();
        datasets.add(dataset);
        chartData.put("datasets", datasets);

        return chartData;
    }

    /**
     * 订单图表数据
     */
    private Map<String, Object> getOrderChartData(String tenantId, LocalDate fromDate, LocalDate toDate, String owner) {
        Map<String, Object> data = reportService.overviewByTenant(tenantId, fromDate, toDate, null, owner, "");

        Map<String, Object> chartData = new HashMap<>();
        chartData.put("type", "BAR");

        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();

        // 按状态分布
        Map<String, Integer> byStatus = (Map<String, Integer>) data.get("orderByStatus");
        if (byStatus != null) {
            byStatus.forEach((key, value) -> {
                labels.add(key);
                values.add(value.longValue());
            });
        }

        chartData.put("labels", labels);
        Map<String, Object> dataset = new HashMap<>();
        dataset.put("label", "订单数量");
        dataset.put("data", values);
        dataset.put("backgroundColor", "#8B5CF6");
        List<Map<String, Object>> datasets = new ArrayList<>();
        datasets.add(dataset);
        chartData.put("datasets", datasets);

        return chartData;
    }

    /**
     * 任务图表数据
     */
    private Map<String, Object> getTaskChartData(String tenantId, LocalDate fromDate, LocalDate toDate, String owner) {
        Map<String, Object> data = reportService.overviewByTenant(tenantId, fromDate, toDate, null, owner, "");

        Map<String, Object> chartData = new HashMap<>();
        chartData.put("type", "DOUGHNUT");

        List<String> labels = new ArrayList<>();
        labels.add("已完成");
        labels.add("待完成");
        Map<String, Integer> taskStatus = (Map<String, Integer>) data.get("taskStatus");
        List<Long> values = new ArrayList<>();
        values.add(taskStatus != null && taskStatus.containsKey("done") ? taskStatus.get("done").longValue() : 0L);
        values.add(taskStatus != null && taskStatus.containsKey("pending") ? taskStatus.get("pending").longValue() : 0L);

        chartData.put("labels", labels);
        Map<String, Object> dataset = new HashMap<>();
        dataset.put("data", values);
        List<String> bgColors = new ArrayList<>();
        bgColors.add("#10B981");
        bgColors.add("#F59E0B");
        dataset.put("backgroundColor", bgColors);
        List<Map<String, Object>> datasets = new ArrayList<>();
        datasets.add(dataset);
        chartData.put("datasets", datasets);

        return chartData;
    }

    /**
     * 线索图表数据
     */
    private Map<String, Object> getLeadChartData(String tenantId, LocalDate fromDate, LocalDate toDate, String owner) {
        Map<String, Object> funnelData = reportService.funnelByTenant(tenantId, fromDate, toDate, owner);

        Map<String, Object> chartData = new HashMap<>();
        chartData.put("type", "BAR");

        List<String> labels = new ArrayList<>();
        labels.add("线索");
        labels.add("商机");
        labels.add("报价");
        labels.add("订单");
        Map<String, Object> counts = (Map<String, Object>) funnelData.get("counts");
        List<Long> values = new ArrayList<>();
        values.add(counts != null && counts.containsKey("leads") ? ((Number) counts.get("leads")).longValue() : 0L);
        values.add(counts != null && counts.containsKey("opportunities") ? ((Number) counts.get("opportunities")).longValue() : 0L);
        values.add(counts != null && counts.containsKey("quotes") ? ((Number) counts.get("quotes")).longValue() : 0L);
        values.add(counts != null && counts.containsKey("orders") ? ((Number) counts.get("orders")).longValue() : 0L);

        chartData.put("labels", labels);
        Map<String, Object> dataset = new HashMap<>();
        dataset.put("label", "数量");
        dataset.put("data", values);
        dataset.put("backgroundColor", "#3B82F6");
        List<Map<String, Object>> datasets = new ArrayList<>();
        datasets.add(dataset);
        chartData.put("datasets", datasets);

        return chartData;
    }

    /**
     * 漏斗图数据（国内特色）
     */
    private Map<String, Object> getFunnelChartData(String tenantId, LocalDate fromDate, LocalDate toDate, String owner) {
        Map<String, Object> funnelData = reportService.funnelByTenant(tenantId, fromDate, toDate, owner);

        Map<String, Object> chartData = new HashMap<>();
        chartData.put("type", "FUNNEL");

        List<String> labels = new ArrayList<>();
        labels.add("线索");
        labels.add("商机");
        labels.add("报价");
        labels.add("订单");
        List<Long> values = new ArrayList<>();

        Map<String, Object> counts = (Map<String, Object>) funnelData.get("counts");
        values.add(counts != null && counts.containsKey("leads") ? ((Number) counts.get("leads")).longValue() : 0L);
        values.add(counts != null && counts.containsKey("opportunities") ? ((Number) counts.get("opportunities")).longValue() : 0L);
        values.add(counts != null && counts.containsKey("quotes") ? ((Number) counts.get("quotes")).longValue() : 0L);
        values.add(counts != null && counts.containsKey("orders") ? ((Number) counts.get("orders")).longValue() : 0L);

        // 计算转化率
        List<Double> rates = new ArrayList<>();
        long prev = values.get(0);
        for (Long val : values) {
            double rate = prev > 0 ? (val * 100.0 / prev) : 0;
            rates.add(Math.round(rate * 10) / 10.0);
            prev = val;
        }

        chartData.put("labels", labels);
        chartData.put("values", values);
        chartData.put("rates", rates);

        // 漏斗图专用配置
        Map<String, Object> dataset = new HashMap<>();
        dataset.put("name", "销售漏斗");
        dataset.put("data", values);
        List<Map<String, Object>> datasets = new ArrayList<>();
        datasets.add(dataset);
        chartData.put("datasets", datasets);

        return chartData;
    }

    /**
     * 默认图表数据
     */
    private Map<String, Object> getDefaultChartData() {
        Map<String, Object> chartData = new HashMap<>();
        chartData.put("type", "BAR");
        List<String> labels = new ArrayList<>();
        labels.add("A");
        labels.add("B");
        labels.add("C");
        labels.add("D");
        chartData.put("labels", labels);
        Map<String, Object> dataset = new HashMap<>();
        dataset.put("label", "示例数据");
        List<Integer> data = new ArrayList<>();
        data.add(12);
        data.add(19);
        data.add(3);
        data.add(5);
        dataset.put("data", data);
        dataset.put("backgroundColor", "#3B82F6");
        List<Map<String, Object>> datasets = new ArrayList<>();
        datasets.add(dataset);
        chartData.put("datasets", datasets);
        return chartData;
    }

    /**
     * 创建图表模板
     */
    @Transactional(timeout = 30)
    public ChartTemplate createTemplate(ChartTemplate template) {
        template.setId(UUID.randomUUID().toString());
        template.setVersion(1);
        template.setIsSystem(false);
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());
        return chartTemplateRepository.save(template);
    }

    /**
     * 获取图表模板
     */
    @Transactional(readOnly = true)
    public List<ChartTemplate> getTemplates(String tenantId, String chartType) {
        if (chartType != null && !chartType.isEmpty()) {
            return chartTemplateRepository.findByTenantIdAndChartType(tenantId, chartType);
        }
        return chartTemplateRepository.findByTenantId(tenantId);
    }

    /**
     * 获取系统默认模板
     */
    @Transactional(readOnly = true)
    public List<ChartTemplate> getSystemTemplates(String tenantId) {
        List<ChartTemplate> templates = new ArrayList<>();

        // 添加系统默认模板
        templates.add(createSystemTemplate("客户分布", "BAR", "CUSTOMERS"));
        templates.add(createSystemTemplate("商机阶段", "LINE", "OPPORTUNITIES"));
        templates.add(createSystemTemplate("营收趋势", "LINE", "REVENUE"));
        templates.add(createSystemTemplate("报价状态", "PIE", "QUOTES"));
        templates.add(createSystemTemplate("订单统计", "BAR", "ORDERS"));
        templates.add(createSystemTemplate("任务完成", "DOUGHNUT", "TASKS"));
        templates.add(createSystemTemplate("线索转化", "BAR", "LEADS"));
        templates.add(createSystemTemplate("销售漏斗", "FUNNEL", "FUNNEL")); // 国内特色

        return templates;
    }

    private ChartTemplate createSystemTemplate(String name, String chartType, String datasetType) {
        ChartTemplate template = new ChartTemplate();
        template.setId(UUID.randomUUID().toString());
        template.setName(name);
        template.setChartType(chartType);
        template.setDatasetType(datasetType);
        template.setConfigJson("{}");
        template.setVisibility("SYSTEM");
        template.setOwner("system");
        template.setIsSystem(true);
        template.setVersion(1);
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());
        return template;
    }
}