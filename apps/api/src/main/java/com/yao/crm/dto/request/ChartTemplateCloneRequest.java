package com.yao.crm.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 克隆图表模板请求DTO
 */
public class ChartTemplateCloneRequest {

    @NotBlank(message = "新模板名称不能为空")
    @Size(max = 100, message = "新模板名称长度不能超过100")
    private String newName;

    public String getNewName() {
        return newName;
    }

    public void setNewName(String newName) {
        this.newName = newName;
    }
}
