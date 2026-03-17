package com.yao.crm.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class V1LeadUpsertRequest {

    @NotBlank(message = "bad_request")
    @Size(max = 255, message = "bad_request")
    private String name;

    @Size(max = 120, message = "bad_request")
    private String company;

    @Size(max = 120, message = "bad_request")
    private String phone;

    @Size(max = 120, message = "bad_request")
    private String email;

    @Size(max = 40, message = "bad_request")
    private String status;

    @Size(max = 120, message = "bad_request")
    private String owner;

    @Size(max = 255, message = "bad_request")
    private String source;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
