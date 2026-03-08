package com.yao.crm.dto.request;

import javax.validation.constraints.NotBlank;

public class V1MfaVerifyRequest {
    @NotBlank(message = "mfa_challenge_required")
    private String challengeId;
    @NotBlank(message = "mfa_required")
    private String code;

    public String getChallengeId() { return challengeId; }
    public void setChallengeId(String challengeId) { this.challengeId = challengeId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}
