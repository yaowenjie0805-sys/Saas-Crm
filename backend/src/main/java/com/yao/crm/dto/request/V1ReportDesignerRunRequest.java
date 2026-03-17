package com.yao.crm.dto.request;

import com.fasterxml.jackson.databind.JsonNode;

import javax.validation.constraints.Size;

public class V1ReportDesignerRunRequest {

    @Size(max = 40, message = "bad_request")
    private String dataset;

    private JsonNode config;

    public String getDataset() { return dataset; }
    public void setDataset(String dataset) { this.dataset = dataset; }
    public JsonNode getConfig() { return config; }
    public void setConfig(JsonNode config) { this.config = config; }
}
