package com.yao.crm.service;

import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class I18nService {

    private final Map<String, String> en = new HashMap<String, String>();
    private final Map<String, String> zh = new HashMap<String, String>();

    public I18nService() {
        en.put("missing_bearer", "Missing Bearer token");
        en.put("invalid_or_expired", "Invalid or expired token");
        en.put("forbidden", "forbidden");
        en.put("scope_forbidden", "forbidden for current data scope");
        en.put("username_password_required", "username and password are required");
        en.put("invalid_credentials", "invalid credentials");
        en.put("login_locked", "too many failed attempts, account temporarily locked");
        en.put("account_disabled", "account is disabled");
        en.put("mfa_required", "MFA code is required");
        en.put("mfa_invalid", "invalid MFA code");
        en.put("name_owner_status_required", "name, owner, status are required");
        en.put("value_number", "value must be a number");
        en.put("value_gte_0", "value must be >= 0");
        en.put("customer_not_found", "customer not found");
        en.put("title_time_required", "title and time are required");
        en.put("task_not_found", "task not found");
        en.put("done_bool", "done must be true or false");
        en.put("stage_required", "stage is required");
        en.put("count_number", "count must be a number");
        en.put("count_gte_0", "count must be >= 0");
        en.put("amount_number", "amount must be a number");
        en.put("amount_gte_0", "amount must be >= 0");
        en.put("progress_number", "progress must be a number");
        en.put("progress_range", "progress must be 0..100");
        en.put("only_admin_set_amount", "only ADMIN or MANAGER can set amount");
        en.put("only_admin_update_amount", "only ADMIN or MANAGER can update amount");
        en.put("opportunity_not_found", "opportunity not found");
        en.put("invalid_date_format", "invalid date format, use YYYY-MM-DD");
        en.put("date_range_invalid", "start date must be earlier than end date");
        en.put("customer_id_summary_required", "customerId and summary are required");
        en.put("follow_up_not_found", "follow-up not found");
        en.put("next_action_date_format", "nextActionDate must be YYYY-MM-DD");
        en.put("bad_request", "invalid request payload");
        en.put("internal_error", "internal server error");
        en.put("too_many_requests", "too many requests, please try again later");
        en.put("invalid_role", "invalid role");
        en.put("invalid_permission", "invalid permission key");
        en.put("delete_requires_manage_customers", "delete customer permission requires manage customer permission");
        en.put("edit_amount_requires_create_opportunity", "edit amount permission requires create opportunity permission");
        en.put("analyst_should_be_read_only", "analyst should stay read-only");
        en.put("sales_should_not_have_high_risk_write", "sales should not have high-risk write permissions");
        en.put("export_job_not_found", "export job not found");
        en.put("export_job_not_ready", "export job is not ready yet");
        en.put("permission_rollback_empty", "no permission history to rollback");
        en.put("sso_disabled", "sso login is disabled");
        en.put("sso_invalid_code", "invalid sso code");
        en.put("sso_code_required", "sso code is required");
        en.put("admin_update_payload_empty", "at least one field must be provided");
        en.put("owner_scope_too_long", "ownerScope length must be <= 64");
        en.put("sso_username_required", "sso username is required");
        en.put("sso_oidc_exchange_failed", "oidc exchange or token verification failed");
        en.put("user_not_found", "user not found");
        en.put("register_username_required", "username is required");
        en.put("register_username_length", "username length must be 4-40");
        en.put("register_password_required", "password is required");
        en.put("register_password_length", "password length must be 6-64");
        en.put("display_name_too_long", "displayName length must be <= 80");
        en.put("username_exists", "username already exists");
        en.put("contact_customer_name_required", "customerId and name are required");
        en.put("contact_phone_invalid", "phone format is invalid");
        en.put("contact_email_invalid", "email format is invalid");
        en.put("contact_not_found", "contact not found");
        en.put("contract_customer_title_required", "customerId and title are required");
        en.put("contract_status_required", "contract status is required");
        en.put("contract_not_found", "contract not found");
        en.put("payment_contract_required", "contractId is required");
        en.put("payment_not_found", "payment not found");
        en.put("invalid_customer_status", "invalid customer status");
        en.put("invalid_opportunity_stage", "invalid opportunity stage");
        en.put("invalid_contract_status", "invalid contract status");
        en.put("invalid_payment_status", "invalid payment status");
        en.put("invalid_payment_method", "invalid payment method");
        en.put("tenant_header_required", "X-Tenant-Id is required");
        en.put("tenant_mismatch", "tenant in header does not match token");
        en.put("tenant_not_found", "tenant not found");
        en.put("tenant_name_required", "tenant name is required");
        en.put("tenant_quota_required", "tenant quotaUsers is required");
        en.put("mfa_challenge_required", "mfa challengeId is required");
        en.put("mfa_challenge_invalid", "mfa challenge is invalid or expired");
        en.put("approval_biz_type_required", "approval bizType is required");
        en.put("approval_name_required", "approval name is required");
        en.put("approval_approver_required", "approval approver roles are required");
        en.put("approval_biz_id_required", "approval bizId is required");
        en.put("approval_template_not_found", "approval template not found");
        en.put("approval_task_not_found", "approval task not found");
        en.put("approval_task_closed", "approval task is already closed");
        en.put("approval_transfer_required", "transfer target is required");
        en.put("approval_flow_invalid", "approval flow definition is invalid");
        en.put("approval_flow_no_path", "no matched node in approval flow");
        en.put("approval_sla_invalid", "approval SLA minutes are invalid");
        en.put("notification_job_not_found", "notification job not found");
        en.put("batch_limit_exceeded", "batch request exceeds max allowed size");
        en.put("automation_name_required", "automation name is required");
        en.put("automation_trigger_required", "automation trigger is required");
        en.put("automation_action_required", "automation action is required");
        en.put("invitation_token_required", "invitation token is required");
        en.put("password_not_match", "passwords do not match");
        en.put("invitation_not_found", "invitation is invalid");
        en.put("invitation_expired", "invitation has expired");
        en.put("invitation_accepted", "invitation accepted");

        zh.put("missing_bearer", "\u7f3a\u5c11Bearer Token");
        zh.put("invalid_or_expired", "Token\u65e0\u6548\u6216\u5df2\u8fc7\u671f");
        zh.put("forbidden", "\u65e0\u6743\u9650\u8bbf\u95ee");
        zh.put("scope_forbidden", "\u5f53\u524d\u8d26\u53f7\u65e0\u6743\u8bbf\u95ee\u8be5\u6570\u636e");
        zh.put("username_password_required", "\u7528\u6237\u540d\u548c\u5bc6\u7801\u4e0d\u80fd\u4e3a\u7a7a");
        zh.put("invalid_credentials", "\u7528\u6237\u540d\u6216\u5bc6\u7801\u9519\u8bef");
        zh.put("login_locked", "\u767b\u5f55\u5931\u8d25\u6b21\u6570\u8fc7\u591a\uff0c\u8d26\u53f7\u5df2\u6682\u65f6\u9501\u5b9a");
        zh.put("account_disabled", "\u8d26\u53f7\u5df2\u88ab\u505c\u7528");
        zh.put("mfa_required", "\u8bf7\u8f93\u5165MFA\u9a8c\u8bc1\u7801");
        zh.put("mfa_invalid", "MFA\u9a8c\u8bc1\u7801\u9519\u8bef");
        zh.put("name_owner_status_required", "name\u3001owner\u3001status\u4e0d\u80fd\u4e3a\u7a7a");
        zh.put("value_number", "value\u5fc5\u987b\u662f\u6570\u5b57");
        zh.put("value_gte_0", "value\u5fc5\u987b\u5927\u4e8e\u7b49\u4e8e0");
        zh.put("customer_not_found", "\u5ba2\u6237\u4e0d\u5b58\u5728");
        zh.put("title_time_required", "title\u548ctime\u4e0d\u80fd\u4e3a\u7a7a");
        zh.put("task_not_found", "\u4efb\u52a1\u4e0d\u5b58\u5728");
        zh.put("done_bool", "done\u53ea\u80fd\u662ftrue\u6216false");
        zh.put("stage_required", "stage\u4e0d\u80fd\u4e3a\u7a7a");
        zh.put("count_number", "count\u5fc5\u987b\u662f\u6570\u5b57");
        zh.put("count_gte_0", "count\u5fc5\u987b\u5927\u4e8e\u7b49\u4e8e0");
        zh.put("amount_number", "amount\u5fc5\u987b\u662f\u6570\u5b57");
        zh.put("amount_gte_0", "amount\u5fc5\u987b\u5927\u4e8e\u7b49\u4e8e0");
        zh.put("progress_number", "progress\u5fc5\u987b\u662f\u6570\u5b57");
        zh.put("progress_range", "progress\u8303\u56f4\u5fc5\u987b\u662f0..100");
        zh.put("only_admin_set_amount", "\u53ea\u6709ADMIN\u6216MANAGER\u53ef\u8bbe\u7f6eamount");
        zh.put("only_admin_update_amount", "\u53ea\u6709ADMIN\u6216MANAGER\u53ef\u4fee\u6539amount");
        zh.put("opportunity_not_found", "\u5546\u673a\u4e0d\u5b58\u5728");
        zh.put("invalid_date_format", "\u65e5\u671f\u683c\u5f0f\u9519\u8bef\uff0c\u8bf7\u4f7f\u7528YYYY-MM-DD");
        zh.put("date_range_invalid", "\u5f00\u59cb\u65e5\u671f\u4e0d\u80fd\u665a\u4e8e\u7ed3\u675f\u65e5\u671f");
        zh.put("customer_id_summary_required", "customerId\u548csummary\u4e0d\u80fd\u4e3a\u7a7a");
        zh.put("follow_up_not_found", "\u8ddf\u8fdb\u8bb0\u5f55\u4e0d\u5b58\u5728");
        zh.put("next_action_date_format", "nextActionDate\u5fc5\u987b\u662fYYYY-MM-DD");
        zh.put("bad_request", "\u8bf7\u6c42\u53c2\u6570\u683c\u5f0f\u9519\u8bef");
        zh.put("internal_error", "\u670d\u52a1\u5668\u5185\u90e8\u9519\u8bef");
        zh.put("too_many_requests", "\u8bf7\u6c42\u8fc7\u4e8e\u9891\u7e41\uff0c\u8bf7\u7a0d\u540e\u518d\u8bd5");
        zh.put("invalid_role", "\u89d2\u8272\u4e0d\u5408\u6cd5");
        zh.put("invalid_permission", "\u6743\u9650\u952e\u4e0d\u5408\u6cd5");
        zh.put("delete_requires_manage_customers", "\u5220\u9664\u5ba2\u6237\u6743\u9650\u4f9d\u8d56\u5ba2\u6237\u7ba1\u7406\u6743\u9650");
        zh.put("edit_amount_requires_create_opportunity", "\u5546\u673a\u91d1\u989d\u4fee\u6539\u6743\u9650\u4f9d\u8d56\u5546\u673a\u521b\u5efa\u6743\u9650");
        zh.put("analyst_should_be_read_only", "\u5206\u6790\u5e08\u5e94\u4fdd\u6301\u53ea\u8bfb\u6743\u9650");
        zh.put("sales_should_not_have_high_risk_write", "\u9500\u552e\u89d2\u8272\u4e0d\u5e94\u62e5\u6709\u9ad8\u98ce\u9669\u5199\u6743\u9650");
        zh.put("export_job_not_found", "\u5bfc\u51fa\u4efb\u52a1\u4e0d\u5b58\u5728");
        zh.put("export_job_not_ready", "\u5bfc\u51fa\u4efb\u52a1\u5c1a\u672a\u5b8c\u6210");
        zh.put("permission_rollback_empty", "\u6ca1\u6709\u53ef\u56de\u6eda\u7684\u6743\u9650\u53d8\u66f4");
        zh.put("sso_disabled", "\u5355\u70b9\u767b\u5f55\u672a\u542f\u7528");
        zh.put("sso_invalid_code", "SSO\u6388\u6743\u7801\u65e0\u6548");
        zh.put("sso_code_required", "SSO\u6388\u6743\u7801\u4e0d\u80fd\u4e3a\u7a7a");
        zh.put("admin_update_payload_empty", "\u81f3\u5c11\u9700\u4f20\u5165\u4e00\u4e2a\u53ef\u4fee\u6539\u5b57\u6bb5");
        zh.put("owner_scope_too_long", "ownerScope\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc764");
        zh.put("sso_username_required", "SSO\u7528\u6237\u540d\u4e0d\u80fd\u4e3a\u7a7a");
        zh.put("sso_oidc_exchange_failed", "OIDC\u6388\u6743\u7801\u4ea4\u6362\u6216Token\u6821\u9a8c\u5931\u8d25");
        zh.put("user_not_found", "\u7528\u6237\u4e0d\u5b58\u5728");
        zh.put("register_username_required", "\u7528\u6237\u540d\u4e0d\u80fd\u4e3a\u7a7a");
        zh.put("register_username_length", "\u7528\u6237\u540d\u957f\u5ea6\u9700\u4e3a 4-40");
        zh.put("register_password_required", "\u5bc6\u7801\u4e0d\u80fd\u4e3a\u7a7a");
        zh.put("register_password_length", "\u5bc6\u7801\u957f\u5ea6\u9700\u4e3a 6-64");
        zh.put("display_name_too_long", "displayName\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 80");
        zh.put("username_exists", "\u7528\u6237\u540d\u5df2\u5b58\u5728");
        zh.put("contact_phone_invalid", "\u7535\u8bdd\u683c\u5f0f\u4e0d\u6b63\u786e");
        zh.put("contact_email_invalid", "\u90ae\u7bb1\u683c\u5f0f\u4e0d\u6b63\u786e");
        zh.put("invalid_customer_status", "\u5ba2\u6237\u72b6\u6001\u4e0d\u5408\u6cd5");
        zh.put("invalid_opportunity_stage", "\u5546\u673a\u9636\u6bb5\u4e0d\u5408\u6cd5");
        zh.put("invalid_contract_status", "\u5408\u540c\u72b6\u6001\u4e0d\u5408\u6cd5");
        zh.put("invalid_payment_status", "\u56de\u6b3e\u72b6\u6001\u4e0d\u5408\u6cd5");
        zh.put("invalid_payment_method", "\u56de\u6b3e\u65b9\u5f0f\u4e0d\u5408\u6cd5");
        zh.put("tenant_header_required", "\u8bf7\u6c42\u5934\u7f3a\u5c11 X-Tenant-Id");
        zh.put("tenant_mismatch", "\u8bf7\u6c42\u5934\u79df\u6237\u4e0e Token \u4e0d\u5339\u914d");
        zh.put("tenant_not_found", "\u79df\u6237\u4e0d\u5b58\u5728");
        zh.put("tenant_name_required", "\u79df\u6237\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a");
        zh.put("tenant_quota_required", "\u79df\u6237\u914d\u989d\u4e0d\u80fd\u4e3a\u7a7a");
        zh.put("mfa_challenge_required", "MFA \u6311\u6218ID\u4e0d\u80fd\u4e3a\u7a7a");
        zh.put("mfa_challenge_invalid", "MFA \u6311\u6218\u65e0\u6548\u6216\u5df2\u8fc7\u671f");
        zh.put("approval_biz_type_required", "\u5ba1\u6279\u4e1a\u52a1\u7c7b\u578b\u4e0d\u80fd\u4e3a\u7a7a");
        zh.put("approval_name_required", "\u5ba1\u6279\u6a21\u677f\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a");
        zh.put("approval_approver_required", "\u5ba1\u6279\u4eba\u89d2\u8272\u4e0d\u80fd\u4e3a\u7a7a");
        zh.put("approval_biz_id_required", "\u5ba1\u6279\u4e1a\u52a1ID\u4e0d\u80fd\u4e3a\u7a7a");
        zh.put("approval_template_not_found", "\u672a\u627e\u5230\u5339\u914d\u7684\u5ba1\u6279\u6a21\u677f");
        zh.put("approval_task_not_found", "\u5ba1\u6279\u4efb\u52a1\u4e0d\u5b58\u5728");
        zh.put("approval_task_closed", "\u5ba1\u6279\u4efb\u52a1\u5df2\u5173\u95ed");
        zh.put("approval_transfer_required", "\u8f6c\u5ba1\u76ee\u6807\u4e0d\u80fd\u4e3a\u7a7a");
        zh.put("approval_flow_invalid", "\u5ba1\u6279\u6d41\u7a0b\u5b9a\u4e49\u4e0d\u5408\u6cd5");
        zh.put("approval_flow_no_path", "\u5f53\u524d\u6761\u4ef6\u4e0b\u6ca1\u6709\u53ef\u6267\u884c\u7684\u5ba1\u6279\u8282\u70b9");
        zh.put("approval_sla_invalid", "SLA \u65f6\u957f\u914d\u7f6e\u4e0d\u5408\u6cd5");
        zh.put("notification_job_not_found", "\u901a\u77e5\u4efb\u52a1\u4e0d\u5b58\u5728");
        zh.put("batch_limit_exceeded", "\u6279\u91cf\u8bf7\u6c42\u6570\u91cf\u8d85\u8fc7\u4e0a\u9650");
        zh.put("automation_name_required", "\u81ea\u52a8\u5316\u89c4\u5219\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a");
        zh.put("automation_trigger_required", "\u81ea\u52a8\u5316\u89e6\u53d1\u6761\u4ef6\u4e0d\u80fd\u4e3a\u7a7a");
        zh.put("automation_action_required", "\u81ea\u52a8\u5316\u6267\u884c\u52a8\u4f5c\u4e0d\u80fd\u4e3a\u7a7a");
        zh.put("invitation_token_required", "\u9080\u8bf7\u7801\u4e0d\u80fd\u4e3a\u7a7a");
        zh.put("password_not_match", "\u4e24\u6b21\u5bc6\u7801\u8f93\u5165\u4e0d\u4e00\u81f4");
        zh.put("invitation_not_found", "\u9080\u8bf7\u94fe\u63a5\u65e0\u6548");
        zh.put("invitation_expired", "\u9080\u8bf7\u5df2\u8fc7\u671f");
        zh.put("invitation_accepted", "\u9080\u8bf7\u5df2\u6fc0\u6d3b");
    }

    public String msg(HttpServletRequest request, String key) {
        if (request != null && isZh(request.getHeader("Accept-Language"))) {
            String text = zh.get(key);
            if (text != null) {
                return text;
            }
        }
        String fallback = en.get(key);
        return fallback == null ? key : fallback;
    }

    private boolean isZh(String acceptLanguage) {
        if (acceptLanguage == null) {
            return false;
        }
        return acceptLanguage.toLowerCase(Locale.ROOT).startsWith("zh");
    }
}





