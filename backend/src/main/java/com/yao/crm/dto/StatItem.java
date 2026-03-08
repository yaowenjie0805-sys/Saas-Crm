package com.yao.crm.dto;

public class StatItem {

    private String label;
    private String value;
    private String trend;

    public StatItem() {
    }

    public StatItem(String label, String value, String trend) {
        this.label = label;
        this.value = value;
        this.trend = trend;
    }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getTrend() { return trend; }
    public void setTrend(String trend) { this.trend = trend; }
}
