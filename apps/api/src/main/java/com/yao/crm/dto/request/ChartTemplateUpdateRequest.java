package com.yao.crm.dto.request;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 更新图表模板请求DTO
 */
public class ChartTemplateUpdateRequest {

    @Size(max = 100, message = "模板名称长度不能超过100")
    private String name;

    @Size(max = 500, message = "描述长度不能超过500")
    private String description;

    @Size(max = 2000, message = "配置JSON长度不能超过2000")
    private String configJson;

    @Size(max = 1000, message = "布局配置长度不能超过1000")
    private String layoutConfig;

    @Pattern(regexp = "PRIVATE|PUBLIC|DEPARTMENT", message = "可见性必须是PRIVATE、PUBLIC或DEPARTMENT")
    private String visibility;

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
}
