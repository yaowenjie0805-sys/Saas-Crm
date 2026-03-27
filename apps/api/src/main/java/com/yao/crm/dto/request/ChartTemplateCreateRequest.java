package com.yao.crm.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 创建图表模板请求DTO
 */
public class ChartTemplateCreateRequest {

    @NotBlank(message = "模板名称不能为空")
    @Size(max = 100, message = "模板名称长度不能超过100")
    private String name;

    @Size(max = 500, message = "描述长度不能超过500")
    private String description;

    @NotBlank(message = "图表类型不能为空")
    @Pattern(regexp = "BAR|LINE|PIE|DOUGHNUT|FUNNEL|RADAR|GAUGE", message = "图表类型必须是BAR、LINE、PIE、DOUGHNUT、FUNNEL、RADAR或GAUGE")
    private String chartType;

    @NotBlank(message = "数据集类型不能为空")
    @Size(max = 50, message = "数据集类型长度不能超过50")
    private String datasetType;

    @Size(max = 2000, message = "配置JSON长度不能超过2000")
    private String configJson;

    @Size(max = 1000, message = "布局配置长度不能超过1000")
    private String layoutConfig;

    @Pattern(regexp = "PRIVATE|PUBLIC|DEPARTMENT", message = "可见性必须是PRIVATE、PUBLIC或DEPARTMENT")
    private String visibility;

    @Size(max = 100, message = "负责人长度不能超过100")
    private String owner;

    @Size(max = 100, message = "部门长度不能超过100")
    private String department;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getChartType() {
        return chartType;
    }

    public void setChartType(String chartType) {
        this.chartType = chartType;
    }

    public String getDatasetType() {
        return datasetType;
    }

    public void setDatasetType(String datasetType) {
        this.datasetType = datasetType;
    }

    public String getConfigJson() {
        return configJson;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
    }

    public String getLayoutConfig() {
        return layoutConfig;
    }

    public void setLayoutConfig(String layoutConfig) {
        this.layoutConfig = layoutConfig;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}
