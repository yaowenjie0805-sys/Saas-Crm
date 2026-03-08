package com.yao.crm.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "register_username_required")
    @Size(min = 4, max = 40, message = "register_username_length")
    private String username;

    @NotBlank(message = "register_password_required")
    @Size(min = 6, max = 64, message = "register_password_length")
    private String password;

    @Size(max = 80, message = "display_name_too_long")
    private String displayName;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
