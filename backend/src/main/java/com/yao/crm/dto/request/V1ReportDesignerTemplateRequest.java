package com.yao.crm.dto.request;

import com.fasterxml.jackson.databind.JsonNode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class V1ReportDesignerTemplateRequest {

    @NotBlank(message = "bad_request")
    @Size(max = 120, message = "bad_request")
    private String name;

    @NotBlank(message = "bad_request")
    @Size(max = 40, message = "bad_request")
    private String dataset;

    @Size(max = 32, message = "bad_request")
    private String visibility;

    @Size(max = 80, message = "bad_request")
    private String department;

    private JsonNode config;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDataset() { return dataset; }
    public void setDataset(String dataset) { this.dataset = dataset; }
    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public JsonNode getConfig() { return config; }
    public void setConfig(JsonNode config) { this.config = config; }
}
