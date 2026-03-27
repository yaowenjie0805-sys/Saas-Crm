package com.yao.crm.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 批量操作请求DTO
 * 用于客户、商机、产品、报价、订单、合同、付款等的批量操作
 */
public class BatchActionRequest {

    @NotBlank(message = "操作类型不能为空")
    @Pattern(regexp = "DELETE|UPDATE_STATUS|ASSIGN_OWNER", message = "操作类型必须是DELETE、UPDATE_STATUS或ASSIGN_OWNER")
    private String action;

    @NotEmpty(message = "ID列表不能为空")
    @Size(max = 100, message = "ID列表不能超过100个")
    private List<String> ids;

    @Pattern(regexp = "ACTIVE|INACTIVE|DRAFT|SUBMITTED|APPROVED|REJECTED|ACCEPTED|EXPIRED|CANCELED|CONFIRMED|FULFILLING|COMPLETED|PENDING|PAID|REFUNDED", message = "状态值无效")
    private String status;

    @Size(max = 100, message = "负责人长度不能超过100")
    private String owner;

    @Size(max = 50, message = "阶段长度不能超过50")
    private String stage;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public List<String> getIds() {
        return ids;
    }

    public void setIds(List<String> ids) {
        this.ids = ids;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }
}
