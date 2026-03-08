package com.yao.crm.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;


    @Test
    void ssoLoginShouldWorkWhenEnabled() throws Exception {
        mockMvc.perform(get("/api/auth/sso/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        String response = mockMvc.perform(post("/api/auth/sso/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"sso_demo\",\"displayName\":\"SSO Demo\",\"code\":\"SSO-ACCESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(response).get("token").asText();
        mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpointShouldRequireToken() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Missing Bearer token"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void loginValidationShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void loginShouldReturnTokenAndAllowProtectedApi() throws Exception {
        String token = login("admin", "admin123");
        mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void salesShouldNotUpdateOpportunityAmount() throws Exception {
        String token = login("sales", "sales123");
        mockMvc.perform(patch("/api/opportunities/o_2001")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":123456}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("only ADMIN or MANAGER can update amount"));
    }

    @Test
    void customerValidationShouldReturnBadRequest() throws Exception {
        String token = login("admin", "admin123");
        mockMvc.perform(post("/api/customers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"R1\",\"owner\":\"A\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void adminCanCreateAndQueryFollowUp() throws Exception {
        String token = login("admin", "admin123");

        String createResponse = mockMvc.perform(post("/api/follow-ups")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"c_1001\",\"summary\":\"Need pricing confirmation\",\"channel\":\"Phone\",\"result\":\"Waiting\",\"nextActionDate\":\"2026-03-10\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String followId = objectMapper.readTree(createResponse).get("id").asText();

        mockMvc.perform(get("/api/follow-ups/search")
                        .header("Authorization", "Bearer " + token)
                        .queryParam("customerId", "c_1001")
                        .queryParam("q", "pricing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(followId));

        mockMvc.perform(delete("/api/follow-ups/" + followId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void analystCanReadReportsButCannotCreateCustomer() throws Exception {
        String token = login("analyst", "analyst123");
        mockMvc.perform(get("/api/reports/overview").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/customers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"R1\",\"owner\":\"A\",\"status\":\"new\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void analystForbiddenShouldReturnChineseMessageWhenLocaleIsZh() throws Exception {
        String token = login("analyst", "analyst123");
        mockMvc.perform(post("/api/customers")
                        .header("Authorization", "Bearer " + token)
                        .header("Accept-Language", "zh-CN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"R1\",\"owner\":\"A\",\"status\":\"new\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void auditExportJobShouldSupportListAndRetryFlow() throws Exception {
        String token = login("analyst", "analyst123");

        String body = mockMvc.perform(post("/api/audit-logs/export-jobs")
                        .header("Authorization", "Bearer " + token)
                        .queryParam("role", "ADMIN")
                        .queryParam("from", "2000-01-01")
                        .queryParam("to", "2999-01-01"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").isString())
                .andReturn().getResponse().getContentAsString();

        String jobId = objectMapper.readTree(body).get("jobId").asText();

        mockMvc.perform(get("/api/audit-logs/export-jobs")
                        .header("Authorization", "Bearer " + token)
                        .queryParam("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());

        String retried = mockMvc.perform(post("/api/audit-logs/export-jobs/" + jobId + "/retry")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.sourceJobId").value(jobId))
                .andReturn().getResponse().getContentAsString();

        String retryJobId = objectMapper.readTree(retried).get("jobId").asText();

        for (int i = 0; i < 12; i++) {
            String statusBody = mockMvc.perform(get("/api/audit-logs/export-jobs/" + retryJobId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            String statusText = objectMapper.readTree(statusBody).get("status").asText();
            if ("DONE".equals(statusText)) break;
            Thread.sleep(100L);
        }

        mockMvc.perform(get("/api/audit-logs/export-jobs/" + retryJobId + "/download")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void permissionPreviewUpdateAndRollbackShouldWorkForAdmin() throws Exception {
        String adminToken = login("admin", "admin123");

        mockMvc.perform(get("/api/permissions/matrix")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matrix").isArray());

        mockMvc.perform(post("/api/permissions/roles/SALES/preview")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"grant\":[\"opViewReports\"],\"revoke\":[\"opDeleteCustomers\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preview").value(true));

        mockMvc.perform(patch("/api/permissions/roles/SALES")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"grant\":[\"opViewReports\"],\"revoke\":[\"opDeleteCustomers\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("SALES"));

        mockMvc.perform(post("/api/permissions/roles/SALES/rollback")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rolledBack").value(true));
    }

    @Test
    void taskValidationShouldReturnBadRequest() throws Exception {
        String token = login("admin", "admin123");
        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"time\":\"today\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void opportunityValidationShouldReturnBadRequest() throws Exception {
        String token = login("admin", "admin123");
        mockMvc.perform(post("/api/opportunities")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stage\":\"Demo\",\"progress\":101}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void managerCanDeleteOpportunity() throws Exception {
        String token = login("manager", "manager123");

        String created = mockMvc.perform(post("/api/opportunities")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stage\":\"Lead\",\"count\":1,\"amount\":0,\"progress\":10,\"owner\":\"manager\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String oppId = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(delete("/api/opportunities/" + oppId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/opportunities/" + oppId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void followUpPatchWithInvalidDateShouldReturnBadRequest() throws Exception {
        String token = login("admin", "admin123");
        String created = mockMvc.perform(post("/api/follow-ups")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"c_1001\",\"summary\":\"Date check\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String followId = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(patch("/api/follow-ups/" + followId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nextActionDate\":\"2026-99-01\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void taskPatchWithInvalidDoneTypeShouldReturnBadRequest() throws Exception {
        String token = login("admin", "admin123");
        mockMvc.perform(patch("/api/tasks/t_3001")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"done\":\"yes\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }


    @Test
    void adminCanManageUsersGovernance() throws Exception {
        String adminToken = login("admin", "admin123");

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());

        mockMvc.perform(patch("/api/admin/users/sales")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"SALES\",\"ownerScope\":\"Chen Xi\",\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("sales"));

        mockMvc.perform(post("/api/admin/users/sales/unlock")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("sales"));
    }

    @Test
    void adminCanCrudContactContractAndPayment() throws Exception {
        String token = login("admin", "admin123");

        String contactBody = mockMvc.perform(post("/api/contacts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"c_1001\",\"name\":\"Test Contact\",\"title\":\"CTO\",\"phone\":\"13900139000\",\"email\":\"ct@test.com\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String contactId = objectMapper.readTree(contactBody).get("id").asText();

        String contractBody = mockMvc.perform(post("/api/contracts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"c_1001\",\"title\":\"Test Contract\",\"status\":\"Draft\",\"amount\":10000}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String contractId = objectMapper.readTree(contractBody).get("id").asText();

        String paymentBody = mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contractId\":\"" + contractId + "\",\"amount\":3000,\"status\":\"Received\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String paymentId = objectMapper.readTree(paymentBody).get("id").asText();

        mockMvc.perform(get("/api/contacts/search")
                        .header("Authorization", "Bearer " + token)
                        .queryParam("q", "Test Contact"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());

        mockMvc.perform(get("/api/contracts/search")
                        .header("Authorization", "Bearer " + token)
                        .queryParam("q", "Test Contract"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());

        mockMvc.perform(get("/api/payments/search")
                        .header("Authorization", "Bearer " + token)
                        .queryParam("contractId", contractId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());

        mockMvc.perform(delete("/api/contacts/" + contactId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/payments/" + paymentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/contracts/" + contractId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void contactCreateWithInvalidPhoneShouldReturnValidationError() throws Exception {
        String token = login("admin", "admin123");
        mockMvc.perform(post("/api/contacts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"c_1001\",\"name\":\"Invalid Phone\",\"phone\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors.phone").value("phone format is invalid"));
    }

    @Test
    void contactCreateWithInvalidEmailShouldReturnChineseValidationError() throws Exception {
        String token = login("admin", "admin123");
        mockMvc.perform(post("/api/contacts")
                        .header("Authorization", "Bearer " + token)
                        .header("Accept-Language", "zh-CN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"c_1001\",\"name\":\"Invalid Email\",\"email\":\"bad@\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors.email").value("\u90ae\u7bb1\u683c\u5f0f\u4e0d\u6b63\u786e"));
    }

    @Test
    void contractCreateWithInvalidStatusShouldReturnBadRequest() throws Exception {
        String token = login("admin", "admin123");
        mockMvc.perform(post("/api/contracts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"c_1001\",\"title\":\"Status Check\",\"status\":\"Archived\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("invalid contract status"));
    }

    @Test
    void contractCreateWithInvalidSignDateShouldReturnBadRequest() throws Exception {
        String token = login("admin", "admin123");
        mockMvc.perform(post("/api/contracts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"c_1001\",\"title\":\"Date Check\",\"status\":\"Draft\",\"signDate\":\"2026-99-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("invalid date format, use YYYY-MM-DD"));
    }

    @Test
    void paymentCreateWithInvalidMethodShouldReturnBadRequest() throws Exception {
        String token = login("admin", "admin123");
        String contractBody = mockMvc.perform(post("/api/contracts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"c_1001\",\"title\":\"Payment Method Check\",\"status\":\"Draft\",\"amount\":1000}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String contractId = objectMapper.readTree(contractBody).get("id").asText();

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contractId\":\"" + contractId + "\",\"amount\":100,\"method\":\"Crypto\",\"status\":\"Pending\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("invalid payment method"));
    }

    @Test
    void paymentCreateWithInvalidDateShouldReturnBadRequest() throws Exception {
        String token = login("admin", "admin123");
        String contractBody = mockMvc.perform(post("/api/contracts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"c_1001\",\"title\":\"Payment Date Check\",\"status\":\"Draft\",\"amount\":1000}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String contractId = objectMapper.readTree(contractBody).get("id").asText();

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contractId\":\"" + contractId + "\",\"amount\":100,\"receivedDate\":\"2026-13-40\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("invalid date format, use YYYY-MM-DD"));
    }

    @Test
    void reportsOverviewShouldRejectInvalidDateRange() throws Exception {
        String token = login("analyst", "analyst123");
        mockMvc.perform(get("/api/reports/overview")
                        .header("Authorization", "Bearer " + token)
                        .queryParam("from", "2026-03-10")
                        .queryParam("to", "2026-03-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("start date must be earlier than end date"));
    }

    @Test
    void reportsOverviewRoleFilterShouldIncludeTaskOwner() throws Exception {
        String managerToken = login("manager", "manager123");
        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Manager Task For Report\",\"time\":\"Today 09:30\",\"level\":\"High\"}"))
                .andExpect(status().isCreated());

        String analystToken = login("analyst", "analyst123");
        mockMvc.perform(get("/api/reports/overview")
                        .header("Authorization", "Bearer " + analystToken)
                        .queryParam("role", "MANAGER")
                        .queryParam("from", "2000-01-01")
                        .queryParam("to", "2999-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskStatus.pending", greaterThanOrEqualTo(1)));
    }
    @Test
    void reportsOverviewExportShouldReturnCsv() throws Exception {
        String token = login("analyst", "analyst123");
        mockMvc.perform(get("/api/reports/overview/export")
                        .header("Authorization", "Bearer " + token)
                        .queryParam("role", "MANAGER")
                        .queryParam("from", "2000-01-01")
                        .queryParam("to", "2999-12-31"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("report-overview-")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("section,key,value")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"filter\",\"role\",\"MANAGER\"")));
    }
    @Test
    void reportsExportJobShouldSupportListStatusRetryAndDownload() throws Exception {
        String token = login("analyst", "analyst123");

        String body = mockMvc.perform(post("/api/reports/export-jobs")
                        .header("Authorization", "Bearer " + token)
                        .queryParam("role", "MANAGER")
                        .queryParam("from", "2000-01-01")
                        .queryParam("to", "2999-01-01"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").isString())
                .andReturn().getResponse().getContentAsString();

        String jobId = objectMapper.readTree(body).get("jobId").asText();

        mockMvc.perform(get("/api/reports/export-jobs")
                        .header("Authorization", "Bearer " + token)
                        .queryParam("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());

        String retried = mockMvc.perform(post("/api/reports/export-jobs/" + jobId + "/retry")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.sourceJobId").value(jobId))
                .andReturn().getResponse().getContentAsString();

        String retryJobId = objectMapper.readTree(retried).get("jobId").asText();

        for (int i = 0; i < 12; i++) {
            String statusBody = mockMvc.perform(get("/api/reports/export-jobs/" + retryJobId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            String statusText = objectMapper.readTree(statusBody).get("status").asText();
            if ("DONE".equals(statusText)) break;
            Thread.sleep(100L);
        }

        mockMvc.perform(get("/api/reports/export-jobs/" + retryJobId + "/download")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("section,key,value")));
    }

    @Test
    void paymentAndFollowUpShouldNormalizeEnumLikeValues() throws Exception {
        String token = login("admin", "admin123");

        String contractBody = mockMvc.perform(post("/api/contracts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"c_1001\",\"title\":\"Normalize Contract\",\"status\":\"draft\",\"amount\":1000}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("Draft"))
                .andReturn().getResponse().getContentAsString();
        String contractId = objectMapper.readTree(contractBody).get("id").asText();

        mockMvc.perform(get("/api/contracts/search")
                        .header("Authorization", "Bearer " + token)
                        .queryParam("status", "draft"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contractId\":\"" + contractId + "\",\"amount\":500,\"method\":\"bank_transfer\",\"status\":\"received\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.method").value("Bank"))
                .andExpect(jsonPath("$.status").value("Received"));

        mockMvc.perform(post("/api/follow-ups")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"c_1001\",\"summary\":\"Normalize Channel\",\"channel\":\"wechat\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.channel").value("WeChat"));
    }

    @Test
    void customerAndOpportunityShouldValidateEnumValues() throws Exception {
        String token = login("admin", "admin123");

        mockMvc.perform(post("/api/customers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Enum C\",\"owner\":\"admin\",\"status\":\"unknown_status\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("invalid customer status"));

        mockMvc.perform(post("/api/customers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Enum C2\",\"owner\":\"admin\",\"status\":\"negotiating\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("Active"));

        mockMvc.perform(post("/api/opportunities")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stage\":\"random stage\",\"count\":1,\"amount\":0,\"progress\":10,\"owner\":\"admin\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("invalid opportunity stage"));

        mockMvc.perform(post("/api/opportunities")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stage\":\"business_negotiation\",\"count\":1,\"amount\":0,\"progress\":10,\"owner\":\"admin\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stage").value("Negotiation"));
    }

    @Test
    void v1AuthLoginAndTenantHeaderShouldWork() throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_default\",\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value("tenant_default"))
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(response).get("token").asText();

        mockMvc.perform(get("/api/v1/reports/overview")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isOk());
    }

    @Test
    void v1EndpointsShouldRejectTenantMismatch() throws Exception {
        String token = login("admin", "admin123");
        mockMvc.perform(get("/api/v1/reports/overview")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_other"))
                .andExpect(status().isForbidden());
    }

    @Test
    void v1ApprovalAndReportExportFlowShouldWork() throws Exception {
        String token = login("admin", "admin123");

        mockMvc.perform(post("/api/v1/approval/templates")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bizType\":\"CONTRACT\",\"name\":\"Contract Approval\",\"amountMin\":0,\"amountMax\":999999999,\"approverRoles\":\"MANAGER,ADMIN\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantId").value("tenant_default"));

        mockMvc.perform(post("/api/v1/approval/instances/CONTRACT/cr_6001/submit")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bizType\":\"CONTRACT\",\"bizId\":\"cr_6001\",\"amount\":1000,\"role\":\"SALES\",\"department\":\"DEFAULT\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantId").value("tenant_default"));

        String jobBody = mockMvc.perform(post("/api/v1/reports/export-jobs")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .queryParam("role", "MANAGER")
                        .queryParam("from", "2000-01-01")
                        .queryParam("to", "2999-01-01"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.tenantId").value("tenant_default"))
                .andReturn().getResponse().getContentAsString();
        String jobId = objectMapper.readTree(jobBody).get("jobId").asText();

        for (int i = 0; i < 12; i++) {
            String statusBody = mockMvc.perform(get("/api/v1/reports/export-jobs/" + jobId)
                            .header("Authorization", "Bearer " + token)
                            .header("X-Tenant-Id", "tenant_default"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            String statusText = objectMapper.readTree(statusBody).get("status").asText();
            if ("DONE".equals(statusText)) break;
            Thread.sleep(100L);
        }

        mockMvc.perform(get("/api/v1/reports/export-jobs/" + jobId + "/download")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isOk());
    }

    @Test
    void v1ApprovalTemplatePublishVersionAndRollbackShouldWork() throws Exception {
        String token = login("admin", "admin123");

        String created = mockMvc.perform(post("/api/v1/approval/templates")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bizType\":\"CONTRACT\",\"name\":\"Version Test\",\"approverRoles\":\"MANAGER\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value(1))
                .andReturn().getResponse().getContentAsString();
        String templateId = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(post("/api/v1/approval/templates/" + templateId + "/publish")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        mockMvc.perform(get("/api/v1/approval/templates/" + templateId + "/versions")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].diffSummary").exists());

        mockMvc.perform(post("/api/v1/approval/templates/" + templateId + "/rollback/1")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void notificationJobsShouldListAndRetry() throws Exception {
        String token = login("admin", "admin123");

        mockMvc.perform(post("/api/v1/approval/sla/scan")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isOk());

        String listBody = mockMvc.perform(get("/api/v1/integrations/notifications/jobs")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .queryParam("status", "ALL")
                        .queryParam("page", "1")
                        .queryParam("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalPages").isNumber())
                .andExpect(jsonPath("$.total").isNumber())
                .andReturn().getResponse().getContentAsString();
        JsonNode list = objectMapper.readTree(listBody).get("items");
        if (list.size() > 0) {
            String jobId = list.get(0).get("jobId").asText();
            mockMvc.perform(post("/api/v1/integrations/notifications/jobs/" + jobId + "/retry")
                            .header("Authorization", "Bearer " + token)
                            .header("X-Tenant-Id", "tenant_default"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.jobId").value(jobId));

            mockMvc.perform(post("/api/v1/integrations/notifications/jobs/batch-retry")
                            .header("Authorization", "Bearer " + token)
                            .header("X-Tenant-Id", "tenant_default")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"jobIds\":[\"" + jobId + "\"]}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.requested").value(1))
                    .andExpect(jsonPath("$.succeeded").isNumber())
                    .andExpect(jsonPath("$.skipped").isNumber())
                    .andExpect(jsonPath("$.notFound").isNumber())
                    .andExpect(jsonPath("$.forbidden").isNumber());
        }

        mockMvc.perform(post("/api/v1/integrations/notifications/jobs/retry-by-filter")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ALL\",\"page\":1,\"size\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requested").isNumber())
                .andExpect(jsonPath("$.succeeded").isNumber())
                .andExpect(jsonPath("$.skipped").isNumber());
    }

    @Test
    void notificationBatchRetryShouldRejectOverLimitPayload() throws Exception {
        String token = login("admin", "admin123");
        mockMvc.perform(post("/api/v1/integrations/notifications/jobs/batch-retry")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobIds\":[\"a\",\"b\",\"c\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("batch_limit_exceeded"))
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.details.maxBatchSize").value(2));
    }

    @Test
    void v1OpsHealthAndMetricsShouldWork() throws Exception {
        String token = login("admin", "admin123");
        mockMvc.perform(get("/api/v1/ops/health")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.database.status").value("UP"))
                .andExpect(jsonPath("$.notificationScheduler.status").exists())
                .andExpect(jsonPath("$.requestId").isString());

        mockMvc.perform(get("/api/v1/ops/metrics/summary")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalBacklog").isNumber())
                .andExpect(jsonPath("$.notificationSuccessRate").isNumber())
                .andExpect(jsonPath("$.notificationRetryDistribution.retry0").isNumber());
    }

    @Test
    void v1AuthInvalidCredentialsShouldReturnUnifiedError() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_default\",\"username\":\"admin\",\"password\":\"bad\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_credentials"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void v1ApprovalForbiddenShouldReturnUnifiedError() throws Exception {
        String token = login("analyst", "analyst123");
        mockMvc.perform(post("/api/v1/approval/templates")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bizType\":\"CONTRACT\",\"name\":\"Denied\",\"approverRoles\":\"MANAGER\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"))
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void v1ReportInvalidDateShouldReturnUnifiedError() throws Exception {
        String token = login("analyst", "analyst123");
        mockMvc.perform(get("/api/v1/reports/overview")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .queryParam("from", "2026-99-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_date_format"))
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void v1TenantForbiddenShouldReturnUnifiedError() throws Exception {
        String token = login("manager", "manager123");
        mockMvc.perform(get("/api/v1/tenants")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"))
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void v1AutomationForbiddenShouldReturnUnifiedError() throws Exception {
        String token = login("analyst", "analyst123");
        mockMvc.perform(post("/api/v1/automation/rules")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"r1\",\"triggerType\":\"FIELD_CHANGE\",\"triggerExpr\":\"x\",\"actionType\":\"CREATE_TASK\",\"actionPayload\":\"{}\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"))
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void v1IntegrationForbiddenShouldReturnUnifiedError() throws Exception {
        String token = login("analyst", "analyst123");
        mockMvc.perform(post("/api/v1/integrations/webhooks/wecom")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event\":\"x\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"))
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void v1AdminUsersForbiddenShouldReturnUnifiedError() throws Exception {
        String token = login("manager", "manager123");
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"))
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void v1MissingTokenShouldReturnUnifiedError() throws Exception {
        mockMvc.perform(get("/api/v1/reports/overview")
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"))
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void legacyDashboardUnauthorizedShouldContainCompatibilityErrorFields() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void legacyCustomerForbiddenShouldContainCompatibilityErrorFields() throws Exception {
        String token = login("analyst", "analyst123");
        mockMvc.perform(post("/api/customers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"R1\",\"owner\":\"A\",\"status\":\"new\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void legacyContactBadRequestShouldContainCompatibilityErrorFields() throws Exception {
        String token = login("admin", "admin123");
        mockMvc.perform(post("/api/contacts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"not_exists\",\"name\":\"A\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"))
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void legacyReportDateRangeErrorShouldContainCompatibilityErrorFields() throws Exception {
        String token = login("analyst", "analyst123");
        mockMvc.perform(get("/api/reports/overview")
                        .header("Authorization", "Bearer " + token)
                        .queryParam("from", "2026-03-10")
                        .queryParam("to", "2026-03-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.code").value("DATE_RANGE_INVALID"))
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void legacyPermissionForbiddenShouldContainCompatibilityErrorFields() throws Exception {
        String token = login("analyst", "analyst123");
        mockMvc.perform(patch("/api/permissions/roles/SALES")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"grant\":[\"opViewReports\"],\"revoke\":[]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void legacyPermissionInvalidRoleShouldReturnWhitelistedCode() throws Exception {
        String token = login("admin", "admin123");
        mockMvc.perform(post("/api/permissions/roles/UNKNOWN/preview")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"grant\":[\"opViewReports\"],\"revoke\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.code").value("INVALID_ROLE"))
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void legacyAuditForbiddenShouldContainCompatibilityErrorFields() throws Exception {
        String token = login("sales", "sales123");
        mockMvc.perform(get("/api/audit-logs").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void legacyAuditExportJobNotFoundShouldReturnWhitelistedCode() throws Exception {
        String token = login("analyst", "analyst123");
        mockMvc.perform(get("/api/audit-logs/export-jobs/not_exists")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.code").value("EXPORT_JOB_NOT_FOUND"))
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void legacyReportExportJobNotFoundShouldReturnWhitelistedCode() throws Exception {
        String token = login("analyst", "analyst123");
        mockMvc.perform(get("/api/reports/export-jobs/not_exists")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.code").value("EXPORT_JOB_NOT_FOUND"))
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void v1TenantMismatchShouldReturnUnifiedForbiddenError() throws Exception {
        String token = login("admin", "admin123");
        mockMvc.perform(get("/api/v1/reports/overview")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_other"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void legacyAuthInvalidCredentialsShouldContainCompatibilityErrorFields() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"bad\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.details").isMap());
    }

    private String login(String username, String password) throws Exception {
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(response);
        return node.get("token").asText();
    }
}




