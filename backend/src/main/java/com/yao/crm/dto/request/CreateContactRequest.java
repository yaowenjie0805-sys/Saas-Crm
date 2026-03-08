package com.yao.crm.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

public class CreateContactRequest {

    @NotBlank(message = "contact_customer_name_required")
    private String customerId;

    @NotBlank(message = "contact_customer_name_required")
    private String name;

    private String title;

    @Pattern(regexp = "^$|^\\+?[0-9 ()-]{6,20}$", message = "contact_phone_invalid")
    private String phone;

    @Pattern(regexp = "^$|^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$", message = "contact_email_invalid")
    private String email;
    private String owner;

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
}
