package com.yao.crm.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class V1AcceptInvitationRequest {

    @NotBlank(message = "invitation_token_required")
    private String token;

    @NotBlank(message = "register_password_required")
    @Size(min = 6, max = 64, message = "register_password_length")
    private String password;

    @NotBlank(message = "register_password_required")
    @Size(min = 6, max = 64, message = "register_password_length")
    private String confirmPassword;

    @Size(max = 80, message = "display_name_too_long")
    private String displayName;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
}
