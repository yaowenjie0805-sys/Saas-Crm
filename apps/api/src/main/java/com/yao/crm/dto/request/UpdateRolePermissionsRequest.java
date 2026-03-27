package com.yao.crm.dto.request;

import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

public class UpdateRolePermissionsRequest {

    @Size(max = 100, message = "grant_list_too_long")
    private List<String> grant = new ArrayList<String>();

    @Size(max = 100, message = "revoke_list_too_long")
    private List<String> revoke = new ArrayList<String>();

    public List<String> getGrant() {
        return grant;
    }

    public void setGrant(List<String> grant) {
        this.grant = grant == null ? new ArrayList<String>() : grant;
    }

    public List<String> getRevoke() {
        return revoke;
    }

    public void setRevoke(List<String> revoke) {
        this.revoke = revoke == null ? new ArrayList<String>() : revoke;
    }
}