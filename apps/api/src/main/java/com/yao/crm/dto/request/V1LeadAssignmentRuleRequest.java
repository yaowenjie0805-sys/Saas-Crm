package com.yao.crm.dto.request;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

public class V1LeadAssignmentRuleRequest {

    @NotBlank(message = "bad_request")
    @Size(max = 120, message = "bad_request")
    private String name;

    @NotNull(message = "bad_request")
    private Boolean enabled;

    @NotEmpty(message = "bad_request")
    @Valid
    private List<Member> members;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public List<Member> getMembers() { return members; }
    public void setMembers(List<Member> members) { this.members = members; }

    public static class Member {
        @NotBlank(message = "bad_request")
        @Size(max = 80, message = "bad_request")
        private String username;

        @NotNull(message = "bad_request")
        private Integer weight;

        private Boolean enabled;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public Integer getWeight() { return weight; }
        public void setWeight(Integer weight) { this.weight = weight; }
        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    }
}
