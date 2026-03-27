package com.yao.crm.dto.request;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 租户配置更新请求DTO
 */
public class TenantConfigPatchRequest {

    @Pattern(regexp = "CN|GLOBAL", message = "市场配置必须是CN或GLOBAL")
    private String marketProfile;

    @Size(max = 50, message = "税务规则长度不能超过50")
    private String taxRule;

    @Pattern(regexp = "STRICT|STAGE_GATE", message = "审批模式必须是STRICT或STAGE_GATE")
    private String approvalMode;

    private List<String> channels;

    @Size(max = 20, message = "数据驻留长度不能超过20")
    private String dataResidency;

    @Size(max = 20, message = "脱敏级别长度不能超过20")
    private String maskLevel;

    public String getMarketProfile() {
        return marketProfile;
    }

    public void setMarketProfile(String marketProfile) {
        this.marketProfile = marketProfile;
    }

    public String getTaxRule() {
        return taxRule;
    }

    public void setTaxRule(String taxRule) {
        this.taxRule = taxRule;
    }

    public String getApprovalMode() {
        return approvalMode;
    }

    public void setApprovalMode(String approvalMode) {
        this.approvalMode = approvalMode;
    }

    public List<String> getChannels() {
        return channels;
    }

    public void setChannels(List<String> channels) {
        this.channels = channels;
    }

    public String getDataResidency() {
        return dataResidency;
    }

    public void setDataResidency(String dataResidency) {
        this.dataResidency = dataResidency;
    }

    public String getMaskLevel() {
        return maskLevel;
    }

    public void setMaskLevel(String maskLevel) {
        this.maskLevel = maskLevel;
    }
}
