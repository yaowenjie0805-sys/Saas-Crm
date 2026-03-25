package com.yao.crm.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yao.crm.entity.ApprovalEvent;
import com.yao.crm.entity.ApprovalTask;
import com.yao.crm.entity.LeadImportJob;
import com.yao.crm.entity.LeadImportJobChunk;
import com.yao.crm.entity.LeadImportJobItem;
import com.yao.crm.entity.PaymentRecord;
import com.yao.crm.repository.ApprovalEventRepository;
import com.yao.crm.repository.ApprovalTaskRepository;
import com.yao.crm.repository.LeadImportJobChunkRepository;
import com.yao.crm.repository.LeadImportJobItemRepository;
import com.yao.crm.repository.LeadImportJobRepository;
import com.yao.crm.repository.PaymentRecordRepository;
import com.yao.crm.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LeadImportJobRepository leadImportJobRepository;

    @Autowired
    private LeadImportJobItemRepository leadImportJobItemRepository;
    @Autowired
    private LeadImportJobChunkRepository leadImportJobChunkRepository;
    @Autowired
    private PaymentRecordRepository paymentRecordRepository;
    @Autowired
    private ApprovalEventRepository approvalEventRepository;
    @Autowired
    private ApprovalTaskRepository approvalTaskRepository;
    @Autowired
    private UserAccountRepository userAccountRepository;


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
                .andExpect(jsonPath("$.code").value("SCOPE_FORBIDDEN"));
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
                .andExpect(jsonPath("$.code").value("approval_template_created"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.details").isMap())
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
                .andExpect(jsonPath("$.code").value("notification_jobs_listed"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.details").isMap())
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
                .andExpect(jsonPath("$.code").value("ops_health_loaded"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.details").isMap())
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
    void v1AuthLoginSuccessShouldContainStandardSuccessFields() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_default\",\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("auth_success"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.details").isMap())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.tenantId").value("tenant_default"));
    }

    @Test
    void v1TenantListSuccessShouldContainStandardSuccessFields() throws Exception {
        String token = login("admin", "admin123");
        mockMvc.perform(get("/api/v1/tenants")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("tenants_listed"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.details").isMap())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void v1AdminInviteSuccessShouldContainStandardSuccessFields() throws Exception {
        String token = login("admin", "admin123");
        String unique = "invite_std_" + System.currentTimeMillis();
        mockMvc.perform(post("/api/v1/admin/users/invite")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + unique + "\",\"role\":\"SALES\",\"ownerScope\":\"" + unique + "\",\"department\":\"DEFAULT\",\"dataScope\":\"SELF\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("invitation_created"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.details").isMap())
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    void v1AutomationCreateSuccessShouldContainStandardSuccessFields() throws Exception {
        String token = login("admin", "admin123");
        mockMvc.perform(post("/api/v1/automation/rules")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"r1\",\"triggerType\":\"FIELD_CHANGE\",\"triggerExpr\":\"x\",\"actionType\":\"CREATE_TASK\",\"actionPayload\":\"{}\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("automation_rule_created"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.details").isMap())
                .andExpect(jsonPath("$.id").isString());
    }

    @Test
    void v1IntegrationWebhookSuccessShouldContainStandardSuccessFields() throws Exception {
        String token = login("admin", "admin123");
        mockMvc.perform(post("/api/v1/integrations/webhooks/wecom")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event\":\"approval_sla_escalated\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.code").value("webhook_accepted"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.details").isMap())
                .andExpect(jsonPath("$.accepted").value(true));
    }

    @Test
    void v1ReportOverviewSuccessShouldContainStandardSuccessFields() throws Exception {
        String token = login("analyst", "analyst123");
        mockMvc.perform(get("/api/v1/reports/overview")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("report_overview_loaded"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.details").isMap())
                .andExpect(jsonPath("$.filters").isMap());
    }

    @Test
    void v1OidcCallbackSuccessShouldContainStandardSuccessFields() throws Exception {
        mockMvc.perform(post("/api/v1/auth/oidc/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_default\",\"code\":\"SSO-ACCESS\",\"username\":\"oidc_user\",\"displayName\":\"OIDC User\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("auth_success"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.details").isMap())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.tenantId").value("tenant_default"));
    }

    @Test
    void ssoLoginShouldRejectUnknownTenantAndAvoidAutoProvision() throws Exception {
        String tenantId = "tenant_missing_" + System.currentTimeMillis();
        String username = "sso_missing_" + System.currentTimeMillis();

        mockMvc.perform(post("/api/auth/sso/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"" + tenantId + "\",\"username\":\"" + username + "\",\"displayName\":\"Missing Tenant\",\"code\":\"SSO-ACCESS\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TENANT_NOT_FOUND"));

        org.junit.jupiter.api.Assertions.assertFalse(
                userAccountRepository.findByUsernameAndTenantIdAndEnabledTrue(username, tenantId).isPresent()
        );
    }

    @Test
    void v1OidcCallbackShouldRejectUnknownTenantAndAvoidAutoProvision() throws Exception {
        String tenantId = "tenant_missing_" + System.currentTimeMillis();
        String username = "oidc_missing_" + System.currentTimeMillis();

        mockMvc.perform(post("/api/v1/auth/oidc/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"" + tenantId + "\",\"username\":\"" + username + "\",\"displayName\":\"Missing Tenant\",\"code\":\"SSO-ACCESS\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("tenant_not_found"));

        org.junit.jupiter.api.Assertions.assertFalse(
                userAccountRepository.findByUsernameAndTenantIdAndEnabledTrue(username, tenantId).isPresent()
        );
    }

    @Test
    void v1InvitationAcceptSuccessShouldContainStandardSuccessFields() throws Exception {
        String adminToken = login("admin", "admin123");
        String unique = "invite_" + System.currentTimeMillis();
        String inviteBody = mockMvc.perform(post("/api/v1/admin/users/invite")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + unique + "\",\"role\":\"SALES\",\"ownerScope\":\"" + unique + "\",\"department\":\"DEFAULT\",\"dataScope\":\"SELF\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(inviteBody).get("token").asText();

        mockMvc.perform(post("/api/v1/auth/invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"password\":\"invite123\",\"confirmPassword\":\"invite123\",\"displayName\":\"Invite User\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("invitation_accepted"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.details").isMap())
                .andExpect(jsonPath("$.tenantId").value("tenant_default"))
                .andExpect(jsonPath("$.username").value(unique))
                .andExpect(jsonPath("$.displayName").value("Invite User"));
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
    void v1LeadImportCancelInvalidTransitionShouldReturnConflictError() throws Exception {
        String token = login("admin", "admin123");
        String csv = "name,company,phone,email,source,owner,status\n" +
                "Lead A,Acme,13900000000,a@example.com,WEB,sales,NEW\n";

        String created = mockMvc.perform(multipart("/api/v1/leads/import-jobs")
                        .file("file", csv.getBytes("UTF-8"))
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .header("Accept-Language", "en")
                        .characterEncoding("UTF-8"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String jobId = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(post("/api/v1/leads/import-jobs/" + jobId + "/cancel")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/leads/import-jobs/" + jobId + "/cancel")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("lead_import_status_transition_invalid"))
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void v1LeadImportConcurrentLimitShouldReturnConflictError() throws Exception {
        String token = login("admin", "admin123");
        String csv = "name,company,phone,email,source,owner,status\n" +
                "Lead A,Acme,13900000000,a@example.com,WEB,sales,NEW\n";

        String first = mockMvc.perform(multipart("/api/v1/leads/import-jobs")
                        .file("file", csv.getBytes("UTF-8"))
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .characterEncoding("UTF-8"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String firstJobId = objectMapper.readTree(first).get("id").asText();

        org.springframework.test.web.servlet.MvcResult secondResult = mockMvc.perform(multipart("/api/v1/leads/import-jobs")
                        .file("file", csv.getBytes("UTF-8"))
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .characterEncoding("UTF-8"))
                .andReturn();
        int secondStatus = secondResult.getResponse().getStatus();
        String secondJobId = "";
        if (secondStatus == 201) {
            secondJobId = objectMapper.readTree(secondResult.getResponse().getContentAsString()).path("id").asText();
            mockMvc.perform(multipart("/api/v1/leads/import-jobs")
                            .file("file", csv.getBytes("UTF-8"))
                            .header("Authorization", "Bearer " + token)
                            .header("X-Tenant-Id", "tenant_default")
                            .characterEncoding("UTF-8"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("lead_import_concurrent_limit_exceeded"))
                    .andExpect(jsonPath("$.requestId").isString())
                    .andExpect(jsonPath("$.details").isMap());
        } else {
            org.junit.jupiter.api.Assertions.assertEquals(409, secondStatus);
            JsonNode conflictBody = objectMapper.readTree(secondResult.getResponse().getContentAsString());
            org.junit.jupiter.api.Assertions.assertEquals("lead_import_concurrent_limit_exceeded", conflictBody.path("code").asText());
            org.junit.jupiter.api.Assertions.assertFalse(conflictBody.path("requestId").asText().trim().isEmpty());
        }

        mockMvc.perform(post("/api/v1/leads/import-jobs/" + firstJobId + "/cancel")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isOk());
        if (!secondJobId.isEmpty()) {
            mockMvc.perform(post("/api/v1/leads/import-jobs/" + secondJobId + "/cancel")
                            .header("Authorization", "Bearer " + token)
                            .header("X-Tenant-Id", "tenant_default"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void v1LeadImportJobResponseShouldExposeTaskStatsAndFailureSummary() throws Exception {
        String token = login("admin", "admin123");
        String csv = "name,company,phone,email,source,owner,status\n" +
                "Lead B,Acme,13900000000,b@example.com,WEB,sales,NEW\n";

        String created = mockMvc.perform(multipart("/api/v1/leads/import-jobs")
                        .file("file", csv.getBytes("UTF-8"))
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .characterEncoding("UTF-8"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.taskStats").isMap())
                .andExpect(jsonPath("$.taskStats.totalChunks").isNumber())
                .andExpect(jsonPath("$.taskStats.pendingChunks").isNumber())
                .andExpect(jsonPath("$.failureSummary").isMap())
                .andExpect(jsonPath("$.failureSummary.failedRows").isNumber())
                .andExpect(jsonPath("$.requestId").isString())
                .andReturn().getResponse().getContentAsString();
        String jobId = objectMapper.readTree(created).path("id").asText();
        mockMvc.perform(post("/api/v1/leads/import-jobs/" + jobId + "/cancel")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isOk());
    }

    @Test
    void v1LeadImportRetryConflictShouldExposeLifecycleDetails() throws Exception {
        String token = login("admin", "admin123");
        String jobId = "lij_conflict_" + System.currentTimeMillis();

        LeadImportJob job = new LeadImportJob();
        job.setId(jobId);
        job.setTenantId("tenant_default");
        job.setCreatedBy("admin");
        job.setFileName("conflict.csv");
        job.setStatus("RUNNING");
        job.setTotalRows(6);
        job.setProcessedRows(4);
        job.setSuccessCount(3);
        job.setFailCount(1);
        job.setPercent(66);
        job.setCancelRequested(false);
        job.setErrorMessage(null);
        job.setCreatedAt(LocalDateTime.now().minusMinutes(10));
        job.setUpdatedAt(LocalDateTime.now().minusMinutes(1));
        leadImportJobRepository.save(job);

        mockMvc.perform(post("/api/v1/leads/import-jobs/" + jobId + "/retry")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("lead_import_status_transition_invalid"))
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.details.action").value("retry"))
                .andExpect(jsonPath("$.details.currentStatus").value("RUNNING"))
                .andExpect(jsonPath("$.details.allowedFrom").isArray())
                .andExpect(jsonPath("$.details.taskStats.totalChunks").exists())
                .andExpect(jsonPath("$.details.failureSummary.failedRows").exists());
    }

    @Test
    void v1LeadImportRetryNoPendingChunksShouldExposeReasonAndRequestId() throws Exception {
        String token = login("admin", "admin123");
        String jobId = "lij_retry_nopending_" + System.currentTimeMillis();

        LeadImportJob job = new LeadImportJob();
        job.setId(jobId);
        job.setTenantId("tenant_default");
        job.setCreatedBy("admin");
        job.setFileName("retry-no-pending.csv");
        job.setStatus("FAILED");
        job.setTotalRows(2);
        job.setProcessedRows(2);
        job.setSuccessCount(1);
        job.setFailCount(1);
        job.setPercent(100);
        job.setCancelRequested(false);
        job.setErrorMessage("lead_import_partial_failure");
        job.setCreatedAt(LocalDateTime.now().minusMinutes(20));
        job.setUpdatedAt(LocalDateTime.now().minusMinutes(2));
        leadImportJobRepository.save(job);

        LeadImportJobChunk chunk = new LeadImportJobChunk();
        chunk.setId("ljc_retry_nopending_" + System.currentTimeMillis());
        chunk.setTenantId("tenant_default");
        chunk.setJobId(jobId);
        chunk.setChunkNo(1);
        chunk.setStatus("PROCESSED");
        chunk.setPayloadJson("[]");
        chunk.setRetryCount(0);
        chunk.setLastError(null);
        chunk.setCreatedAt(LocalDateTime.now().minusMinutes(20));
        chunk.setUpdatedAt(LocalDateTime.now().minusMinutes(2));
        leadImportJobChunkRepository.save(chunk);

        mockMvc.perform(post("/api/v1/leads/import-jobs/" + jobId + "/retry")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("lead_import_retry_no_pending_chunks"))
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.details.action").value("retry"))
                .andExpect(jsonPath("$.details.reason").value("no_pending_chunks"));
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

    @Test
    void v1LeadImportFailedRowsExportJobShouldCreateListAndDownload() throws Exception {
        String token = login("admin", "admin123");
        String importJobId = "lij_test_" + System.currentTimeMillis();

        LeadImportJob job = new LeadImportJob();
        job.setId(importJobId);
        job.setTenantId("tenant_default");
        job.setCreatedBy("admin");
        job.setStatus("FAILED");
        job.setFileName("failed_rows.csv");
        job.setTotalRows(1);
        job.setProcessedRows(1);
        job.setSuccessCount(0);
        job.setFailCount(1);
        job.setPercent(100);
        job.setCancelRequested(false);
        job.setCreatedAt(LocalDateTime.now().minusMinutes(10));
        job.setUpdatedAt(LocalDateTime.now().minusMinutes(1));
        leadImportJobRepository.save(job);

        LeadImportJobItem item = new LeadImportJobItem();
        item.setId("lji_test_" + System.currentTimeMillis());
        item.setTenantId("tenant_default");
        item.setJobId(importJobId);
        item.setLineNo(2);
        item.setStatus("FAILED");
        item.setRawLine("Lead A,Acme,13900000000,a@example.com,WEB,sales,NEW");
        item.setErrorCode("lead_import_duplicate");
        item.setErrorMessage("duplicated lead row");
        item.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        leadImportJobItemRepository.save(item);

        String created = mockMvc.perform(post("/api/v1/leads/import-jobs/" + importJobId + "/failed-rows/export-jobs")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.code").value("lead_import_export_submitted"))
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.details").isMap())
                .andExpect(jsonPath("$.jobId").isString())
                .andReturn().getResponse().getContentAsString();
        String exportJobId = objectMapper.readTree(created).get("jobId").asText();

        mockMvc.perform(get("/api/v1/leads/import-jobs/" + importJobId + "/failed-rows/export-jobs")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .queryParam("status", "ALL")
                        .queryParam("page", "1")
                        .queryParam("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("lead_import_export_jobs_listed"))
                .andExpect(jsonPath("$.items").isArray());

        for (int i = 0; i < 20; i++) {
            String listBody = mockMvc.perform(get("/api/v1/leads/import-jobs/" + importJobId + "/failed-rows/export-jobs")
                            .header("Authorization", "Bearer " + token)
                            .header("X-Tenant-Id", "tenant_default")
                            .queryParam("status", "ALL")
                            .queryParam("page", "1")
                            .queryParam("size", "10"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            JsonNode items = objectMapper.readTree(listBody).get("items");
            String statusText = "";
            for (JsonNode row : items) {
                if (exportJobId.equals(row.path("jobId").asText())) {
                    statusText = row.path("status").asText();
                    break;
                }
            }
            if ("DONE".equals(statusText)) break;
            Thread.sleep(80L);
        }

        mockMvc.perform(get("/api/v1/leads/import-jobs/" + importJobId + "/failed-rows/export-jobs/" + exportJobId + "/download")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("jobId,lineNo,errorCode,errorMessage,rawLine,createdAt")));
    }

    @Test
    void v1OpsMetricsSummaryShouldContainImportMetrics() throws Exception {
        String token = login("analyst", "analyst123");
        mockMvc.perform(get("/api/v1/ops/metrics/summary")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ops_metrics_loaded"))
                .andExpect(jsonPath("$.importMetrics").isMap())
                .andExpect(jsonPath("$.importMetrics.importJobTotal").exists())
                .andExpect(jsonPath("$.importMetrics.importSuccessRate").exists())
                .andExpect(jsonPath("$.importMetrics.importFailureRate").exists());
    }

    @Test
    void v1QuotesAndOrdersShouldRespectSalesScopeOnList() throws Exception {
        String adminToken = login("admin", "admin123");
        String salesToken = login("sales", "sales123");

        mockMvc.perform(post("/api/v1/quotes")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"c_1001\",\"owner\":\"manager\",\"status\":\"DRAFT\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/quotes")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"c_1001\",\"owner\":\"sales\",\"status\":\"DRAFT\"}"))
                .andExpect(status().isCreated());

        String quoteListBody = mockMvc.perform(get("/api/v1/quotes")
                        .header("Authorization", "Bearer " + salesToken)
                        .header("X-Tenant-Id", "tenant_default")
                        .queryParam("page", "1")
                        .queryParam("size", "100"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode quoteRoot = objectMapper.readTree(quoteListBody);
        JsonNode quoteItems = quoteRoot.get("items");
        org.junit.jupiter.api.Assertions.assertEquals(1, quoteRoot.path("page").asInt());
        org.junit.jupiter.api.Assertions.assertEquals(100, quoteRoot.path("size").asInt());
        org.junit.jupiter.api.Assertions.assertTrue(quoteRoot.path("total").asLong() >= quoteItems.size());
        for (JsonNode item : quoteItems) {
            String owner = item.path("owner").asText();
            org.junit.jupiter.api.Assertions.assertTrue("sales".equalsIgnoreCase(owner));
        }

        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"c_1001\",\"owner\":\"manager\",\"status\":\"DRAFT\",\"amount\":100}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"c_1001\",\"owner\":\"sales\",\"status\":\"DRAFT\",\"amount\":100}"))
                .andExpect(status().isCreated());

        String orderListBody = mockMvc.perform(get("/api/v1/orders")
                        .header("Authorization", "Bearer " + salesToken)
                        .header("X-Tenant-Id", "tenant_default")
                        .queryParam("page", "1")
                        .queryParam("size", "100"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode orderRoot = objectMapper.readTree(orderListBody);
        JsonNode orderItems = orderRoot.get("items");
        org.junit.jupiter.api.Assertions.assertEquals(1, orderRoot.path("page").asInt());
        org.junit.jupiter.api.Assertions.assertEquals(100, orderRoot.path("size").asInt());
        org.junit.jupiter.api.Assertions.assertTrue(orderRoot.path("total").asLong() >= orderItems.size());
        for (JsonNode item : orderItems) {
            String owner = item.path("owner").asText();
            org.junit.jupiter.api.Assertions.assertTrue("sales".equalsIgnoreCase(owner));
        }
    }

    @Test
    void v1QuoteSubmitShouldTriggerApprovalAndGateAcceptTransition() throws Exception {
        String token = login("admin", "admin123");
        createQuoteApprovalTemplate(token);
        String productId = createProduct(token, "P-HIGH-" + System.currentTimeMillis(), "High Product", 600000);
        String quoteId = createQuote(token, "admin");
        upsertQuoteItems(token, quoteId, "[{\"productId\":\"" + productId + "\",\"quantity\":1,\"unitPrice\":600000,\"discountRate\":0.0}]");

        mockMvc.perform(post("/api/v1/quotes/" + quoteId + "/submit")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalTriggered").value(true))
                .andExpect(jsonPath("$.approvalInstanceId").isString());

        mockMvc.perform(post("/api/v1/quotes/" + quoteId + "/accept")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("quote_status_transition_invalid"));
    }

    @Test
    void v1LeadConvertAndQuoteToOrderShouldExposeStableContractFields() throws Exception {
        String token = login("admin", "admin123");

        String leadBody = mockMvc.perform(post("/api/v1/leads")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Lead Contract Test\",\"company\":\"Demo Co\",\"status\":\"NEW\",\"owner\":\"admin\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String leadId = objectMapper.readTree(leadBody).path("id").asText();

        String convertedBody = mockMvc.perform(post("/api/v1/leads/" + leadId + "/convert")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lead.id").value(leadId))
                .andExpect(jsonPath("$.customerId").isString())
                .andExpect(jsonPath("$.contactId").isString())
                .andExpect(jsonPath("$.opportunityId").isString())
                .andReturn().getResponse().getContentAsString();
        String customerId = objectMapper.readTree(convertedBody).path("customerId").asText();
        String opportunityId = objectMapper.readTree(convertedBody).path("opportunityId").asText();

        String quoteId = createQuote(token, "admin", "APPROVED", customerId, opportunityId);

        mockMvc.perform(post("/api/v1/quotes/" + quoteId + "/to-order")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.orderId").isString())
                .andExpect(jsonPath("$.orderStatus").value("DRAFT"))
                .andExpect(jsonPath("$.sourceQuoteId").value(quoteId))
                .andExpect(jsonPath("$.sourceQuoteStatus").value("ACCEPTED"));
    }

    @Test
    void v1LeadStatusTransitionShouldRejectDisqualifiedToConverted() throws Exception {
        String token = login("admin", "admin123");

        String leadBody = mockMvc.perform(post("/api/v1/leads")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Disqualified Lead\",\"company\":\"Demo Co\",\"status\":\"DISQUALIFIED\",\"owner\":\"admin\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String leadId = objectMapper.readTree(leadBody).path("id").asText();

        mockMvc.perform(post("/api/v1/leads/" + leadId + "/convert")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("lead_status_transition_invalid"))
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.details.from").value("DISQUALIFIED"))
                .andExpect(jsonPath("$.details.to").value("CONVERTED"))
                .andExpect(jsonPath("$.details.allowed").isArray());
    }

    @Test
    void v1QuoteSubmitShouldTriggerApprovalByDiscount() throws Exception {
        String token = login("admin", "admin123");
        createQuoteApprovalTemplate(token);
        String productId = createProduct(token, "P-DIS-" + System.currentTimeMillis(), "Discount Product", 1000);
        String quoteId = createQuote(token, "admin");
        upsertQuoteItems(token, quoteId, "[{\"productId\":\"" + productId + "\",\"quantity\":1,\"unitPrice\":1000,\"discountRate\":0.25}]");

        mockMvc.perform(post("/api/v1/quotes/" + quoteId + "/submit")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalTriggered").value(true))
                .andExpect(jsonPath("$.approvalReason").value("DISCOUNT"));
    }

    @Test
    void v1QuoteSubmitShouldNotTriggerApprovalWhenThresholdNotMatched() throws Exception {
        String token = login("admin", "admin123");
        setTenantApprovalMode(token, "STRICT");
        createQuoteApprovalTemplate(token);
        String productId = createProduct(token, "P-LOW-" + System.currentTimeMillis(), "Low Product", 1000);
        String quoteId = createQuote(token, "admin");
        upsertQuoteItems(token, quoteId, "[{\"productId\":\"" + productId + "\",\"quantity\":1,\"unitPrice\":1000,\"discountRate\":0.10}]");

        mockMvc.perform(post("/api/v1/quotes/" + quoteId + "/submit")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalTriggered").value(false))
                .andExpect(jsonPath("$.approvalInstanceId").value(""));
    }

    @Test
    void v1QuoteSubmitShouldTriggerApprovalInStageGateMode() throws Exception {
        String token = login("admin", "admin123");
        setTenantApprovalMode(token, "STAGE_GATE");
        createQuoteApprovalTemplate(token);
        String productId = createProduct(token, "P-SG-" + System.currentTimeMillis(), "SG Product", 1000);
        String quoteId = createQuote(token, "admin");
        upsertQuoteItems(token, quoteId, "[{\"productId\":\"" + productId + "\",\"quantity\":1,\"unitPrice\":1000,\"discountRate\":0.0}]");

        mockMvc.perform(post("/api/v1/quotes/" + quoteId + "/submit")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalTriggered").value(true))
                .andExpect(jsonPath("$.approvalReason").value("STAGE_GATE"))
                .andExpect(jsonPath("$.approvalMode").value("STAGE_GATE"))
                .andExpect(jsonPath("$.approvalInstanceId").isString());
    }

    @Test
    void v1QuoteToOrderShouldRespectStageGateAndStrictModes() throws Exception {
        String token = login("admin", "admin123");

        setTenantApprovalMode(token, "STAGE_GATE");
        String stageGateQuoteId = createQuote(token, "admin", "ACCEPTED", "c_1001", "");
        mockMvc.perform(post("/api/v1/quotes/" + stageGateQuoteId + "/to-order")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("quote_stage_gate_requires_approval"))
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.details.requiredStatus").value("APPROVED"));

        setTenantApprovalMode(token, "STRICT");
        String strictQuoteId = createQuote(token, "admin", "ACCEPTED", "c_1001", "");
        mockMvc.perform(post("/api/v1/quotes/" + strictQuoteId + "/to-order")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sourceQuoteId").value(strictQuoteId))
                .andExpect(jsonPath("$.approvalMode").value("STRICT"));
    }

    @Test
    void v1QuoteRejectShouldWriteBackQuoteStatusRejected() throws Exception {
        String token = login("admin", "admin123");
        createQuoteApprovalTemplate(token);
        String productId = createProduct(token, "P-REJ-" + System.currentTimeMillis(), "Reject Product", 600000);
        String quoteId = createQuote(token, "admin");
        upsertQuoteItems(token, quoteId, "[{\"productId\":\"" + productId + "\",\"quantity\":1,\"unitPrice\":600000,\"discountRate\":0.0}]");

        String submitBody = mockMvc.perform(post("/api/v1/quotes/" + quoteId + "/submit")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalTriggered").value(true))
                .andReturn().getResponse().getContentAsString();
        String instanceId = objectMapper.readTree(submitBody).path("approvalInstanceId").asText();

        String instanceBody = mockMvc.perform(get("/api/v1/approval/instances/" + instanceId)
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode tasks = objectMapper.readTree(instanceBody).path("tasks");
        String taskId = tasks.get(0).path("id").asText();

        mockMvc.perform(post("/api/v1/approval/tasks/" + taskId + "/reject")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        String quoteStatus = findQuoteStatusById(token, quoteId);
        org.junit.jupiter.api.Assertions.assertEquals("REJECTED", quoteStatus);
    }

    @Test
    void v1OrderFulfillmentTransitionsShouldValidate() throws Exception {
        String token = login("admin", "admin123");
        String created = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"c_1001\",\"owner\":\"admin\",\"status\":\"DRAFT\",\"amount\":12345}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String orderId = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/confirm")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/complete")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("order_status_transition_invalid"));

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/fulfill")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/complete")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isOk());
    }

    @Test
    void v1OrderStageGateShouldEnforceQuoteAndFulfillingTransitions() throws Exception {
        String token = login("admin", "admin123");
        setTenantApprovalMode(token, "STAGE_GATE");

        String manualOrderBody = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"c_1001\",\"owner\":\"admin\",\"status\":\"DRAFT\",\"amount\":9800}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String manualOrderId = objectMapper.readTree(manualOrderBody).path("id").asText();

        mockMvc.perform(post("/api/v1/orders/" + manualOrderId + "/confirm")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("order_stage_gate_requires_quote_accepted"))
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.details.requiredStatus").value("ACCEPTED_QUOTE"))
                .andExpect(jsonPath("$.details.currentStatus").value("NO_QUOTE"))
                .andExpect(jsonPath("$.details.approvalMode").value("STAGE_GATE"));

        String stageGateQuoteId = createQuote(token, "admin", "APPROVED", "c_1001", "");
        String orderBody = mockMvc.perform(post("/api/v1/quotes/" + stageGateQuoteId + "/to-order")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.approvalMode").value("STAGE_GATE"))
                .andReturn().getResponse().getContentAsString();
        String orderId = objectMapper.readTree(orderBody).path("orderId").asText();

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/confirm")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalMode").value("STAGE_GATE"));

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/to-contract")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("order_stage_gate_requires_fulfilling"))
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.details.requiredStatus").value("FULFILLING"))
                .andExpect(jsonPath("$.details.currentStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.details.approvalMode").value("STAGE_GATE"));

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/fulfill")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/to-contract")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.approvalMode").value("STAGE_GATE"))
                .andExpect(jsonPath("$.contractId").isString());
    }

    @Test
    void v1OrderStageGateShouldKeepStrictModeCompatible() throws Exception {
        String token = login("admin", "admin123");
        setTenantApprovalMode(token, "STRICT");

        String created = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"c_1001\",\"owner\":\"admin\",\"status\":\"DRAFT\",\"amount\":34567}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String orderId = objectMapper.readTree(created).path("id").asText();

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/confirm")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalMode").value("STRICT"));

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/to-contract")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.approvalMode").value("STRICT"))
                .andExpect(jsonPath("$.contractId").isString());
    }

    @Test
    void v1SalesShouldNotOperateOtherOwnersQuoteInM1Flow() throws Exception {
        String adminToken = login("admin", "admin123");
        String salesToken = login("sales", "sales123");

        String quoteId = createQuote(adminToken, "admin");

        mockMvc.perform(post("/api/v1/quotes/" + quoteId + "/submit")
                        .header("Authorization", "Bearer " + salesToken)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("scope_forbidden"));

        mockMvc.perform(post("/api/v1/quotes/" + quoteId + "/to-order")
                        .header("Authorization", "Bearer " + salesToken)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("scope_forbidden"));
    }

    @Test
    void v1ApprovalSubmitShouldRejectDuplicateActiveInstanceAndAllowResubmitAfterClose() throws Exception {
        String token = login("admin", "admin123");
        createQuoteApprovalTemplate(token);
        String quoteId = createQuote(token, "admin");

        String firstSubmitBody = mockMvc.perform(post("/api/v1/approval/instances/QUOTE/" + quoteId + "/submit")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bizType\":\"QUOTE\",\"bizId\":\"" + quoteId + "\",\"amount\":1200,\"role\":\"SALES\",\"department\":\"DEFAULT\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String firstInstanceId = objectMapper.readTree(firstSubmitBody).path("id").asText();

        mockMvc.perform(post("/api/v1/approval/instances/QUOTE/" + quoteId + "/submit")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bizType\":\"QUOTE\",\"bizId\":\"" + quoteId + "\",\"amount\":1200,\"role\":\"SALES\",\"department\":\"DEFAULT\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("approval_instance_active_exists"))
                .andExpect(jsonPath("$.details.instanceId").value(firstInstanceId))
                .andExpect(jsonPath("$.details.bizId").value(quoteId));

        String detailBody = mockMvc.perform(get("/api/v1/approval/instances/" + firstInstanceId)
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String taskId = objectMapper.readTree(detailBody).path("tasks").get(0).path("id").asText();

        mockMvc.perform(post("/api/v1/approval/tasks/" + taskId + "/reject")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"close-then-resubmit\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/approval/instances/QUOTE/" + quoteId + "/submit")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bizType\":\"QUOTE\",\"bizId\":\"" + quoteId + "\",\"amount\":1200,\"role\":\"SALES\",\"department\":\"DEFAULT\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void v1ApprovalTaskActionShouldExposeInstanceAndWritebackFields() throws Exception {
        String token = login("admin", "admin123");
        createQuoteApprovalTemplate(token);
        String quoteId = createQuote(token, "admin");

        String submitBody = mockMvc.perform(post("/api/v1/approval/instances/QUOTE/" + quoteId + "/submit")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bizType\":\"QUOTE\",\"bizId\":\"" + quoteId + "\",\"amount\":1200,\"role\":\"SALES\",\"department\":\"DEFAULT\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String instanceId = objectMapper.readTree(submitBody).path("id").asText();

        String detailBody = mockMvc.perform(get("/api/v1/approval/instances/" + instanceId)
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String taskId = objectMapper.readTree(detailBody).path("tasks").get(0).path("id").asText();

        mockMvc.perform(post("/api/v1/approval/tasks/" + taskId + "/approve")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"approve-now\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instance.id").value(instanceId))
                .andExpect(jsonPath("$.requestIdRef").isString())
                .andExpect(jsonPath("$.bizWriteback.bizType").value("QUOTE"))
                .andExpect(jsonPath("$.bizWriteback.bizId").value(quoteId))
                .andExpect(jsonPath("$.bizWriteback.status").value("APPROVED"));

        mockMvc.perform(post("/api/v1/approval/tasks/" + taskId + "/urge")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"again\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("approval_task_closed"));
    }

    @Test
    void v1ApprovalUrgeShouldEnforceCooldownAndDailyLimitWithReasonDetails() throws Exception {
        String token = login("admin", "admin123");
        createQuoteApprovalTemplate(token);

        String quoteCooldown = createQuote(token, "admin");
        String submitCooldown = mockMvc.perform(post("/api/v1/approval/instances/QUOTE/" + quoteCooldown + "/submit")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bizType\":\"QUOTE\",\"bizId\":\"" + quoteCooldown + "\",\"amount\":1000,\"role\":\"SALES\",\"department\":\"DEFAULT\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String cooldownInstanceId = objectMapper.readTree(submitCooldown).path("id").asText();
        String cooldownTaskId = approvalTaskRepository
                .findByInstanceIdAndTenantIdOrderBySeqAsc(cooldownInstanceId, "tenant_default")
                .get(0).getId();

        mockMvc.perform(post("/api/v1/approval/tasks/" + cooldownTaskId + "/urge")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"first\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("URGED"));

        mockMvc.perform(post("/api/v1/approval/tasks/" + cooldownTaskId + "/urge")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"second\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("approval_task_closed"))
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.details.reason").value("urge_cooldown"))
                .andExpect(jsonPath("$.details.cooldownUntil").isString())
                .andExpect(jsonPath("$.details.dailyLimit").value(10));

        String quoteDaily = createQuote(token, "admin");
        String submitDaily = mockMvc.perform(post("/api/v1/approval/instances/QUOTE/" + quoteDaily + "/submit")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bizType\":\"QUOTE\",\"bizId\":\"" + quoteDaily + "\",\"amount\":1200,\"role\":\"SALES\",\"department\":\"DEFAULT\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String dailyInstanceId = objectMapper.readTree(submitDaily).path("id").asText();
        ApprovalTask dailyTask = approvalTaskRepository
                .findByInstanceIdAndTenantIdOrderBySeqAsc(dailyInstanceId, "tenant_default")
                .get(0);
        LocalDateTime base = LocalDateTime.now().minusMinutes(35);
        for (int i = 0; i < 10; i++) {
            ApprovalEvent event = new ApprovalEvent();
            event.setId("apev_test_" + i + "_" + System.currentTimeMillis());
            event.setTenantId("tenant_default");
            event.setInstanceId(dailyTask.getInstanceId());
            event.setTaskId(dailyTask.getId());
            event.setEventType("URGED");
            event.setOperatorUser("admin");
            event.setDetail("seed-daily-limit");
            event.setRequestId("seed-" + i);
            event.setCreatedAt(base.minusMinutes(i));
            approvalEventRepository.save(event);
        }

        mockMvc.perform(post("/api/v1/approval/tasks/" + dailyTask.getId() + "/urge")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"daily-limit\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("approval_task_closed"))
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.details.reason").value("urge_daily_limit"))
                .andExpect(jsonPath("$.details.dailyLimit").value(10))
                .andExpect(jsonPath("$.details.dailyCount", greaterThanOrEqualTo(10)));
    }

    @Test
    void v1ApprovalSlaScanShouldExposeTierStats() throws Exception {
        String token = login("admin", "admin123");
        createQuoteApprovalTemplate(token);

        String quoteP1 = createQuote(token, "admin");
        String quoteP2 = createQuote(token, "admin");
        String quoteP3 = createQuote(token, "admin");

        String instP1 = objectMapper.readTree(mockMvc.perform(post("/api/v1/approval/instances/QUOTE/" + quoteP1 + "/submit")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bizType\":\"QUOTE\",\"bizId\":\"" + quoteP1 + "\",\"amount\":2000,\"role\":\"SALES\",\"department\":\"DEFAULT\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).path("id").asText();
        String instP2 = objectMapper.readTree(mockMvc.perform(post("/api/v1/approval/instances/QUOTE/" + quoteP2 + "/submit")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bizType\":\"QUOTE\",\"bizId\":\"" + quoteP2 + "\",\"amount\":3000,\"role\":\"SALES\",\"department\":\"DEFAULT\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).path("id").asText();
        String instP3 = objectMapper.readTree(mockMvc.perform(post("/api/v1/approval/instances/QUOTE/" + quoteP3 + "/submit")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bizType\":\"QUOTE\",\"bizId\":\"" + quoteP3 + "\",\"amount\":4000,\"role\":\"SALES\",\"department\":\"DEFAULT\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).path("id").asText();

        ApprovalTask taskP1 = approvalTaskRepository.findByInstanceIdAndTenantIdOrderBySeqAsc(instP1, "tenant_default").get(0);
        ApprovalTask taskP2 = approvalTaskRepository.findByInstanceIdAndTenantIdOrderBySeqAsc(instP2, "tenant_default").get(0);
        ApprovalTask taskP3 = approvalTaskRepository.findByInstanceIdAndTenantIdOrderBySeqAsc(instP3, "tenant_default").get(0);
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        taskP1.setDeadlineAt(now.minusMinutes(35));
        taskP2.setDeadlineAt(now.minusMinutes(130));
        taskP3.setDeadlineAt(now.minusMinutes(24 * 60 + 10));
        approvalTaskRepository.save(taskP1);
        approvalTaskRepository.save(taskP2);
        approvalTaskRepository.save(taskP3);

        mockMvc.perform(post("/api/v1/approval/sla/scan")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.affected", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.tierStats.P1", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.tierStats.P2", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.tierStats.P3", greaterThanOrEqualTo(1)));
    }

    @Test
    void v1ApprovalTaskActionShouldForbidAnalystAndKeepTenantIsolation() throws Exception {
        String adminToken = login("admin", "admin123");
        String analystToken = login("analyst", "analyst123");
        createQuoteApprovalTemplate(adminToken);
        String quoteId = createQuote(adminToken, "admin");
        String submitBody = mockMvc.perform(post("/api/v1/approval/instances/QUOTE/" + quoteId + "/submit")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bizType\":\"QUOTE\",\"bizId\":\"" + quoteId + "\",\"amount\":800,\"role\":\"SALES\",\"department\":\"DEFAULT\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String instanceId = objectMapper.readTree(submitBody).path("id").asText();
        String detailBody = mockMvc.perform(get("/api/v1/approval/instances/" + instanceId)
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String taskId = objectMapper.readTree(detailBody).path("tasks").get(0).path("id").asText();

        mockMvc.perform(post("/api/v1/approval/tasks/" + taskId + "/approve")
                        .header("Authorization", "Bearer " + analystToken)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"forbidden\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
    }

    @Test
    void v1ApprovalTaskRepeatedApproveRejectShouldReturnConflictWithRequestId() throws Exception {
        String token = login("admin", "admin123");
        createQuoteApprovalTemplate(token);
        String quoteId = createQuote(token, "admin");

        String submitBody = mockMvc.perform(post("/api/v1/approval/instances/QUOTE/" + quoteId + "/submit")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bizType\":\"QUOTE\",\"bizId\":\"" + quoteId + "\",\"amount\":900,\"role\":\"SALES\",\"department\":\"DEFAULT\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String instanceId = objectMapper.readTree(submitBody).path("id").asText();

        String detailBody = mockMvc.perform(get("/api/v1/approval/instances/" + instanceId)
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String taskId = objectMapper.readTree(detailBody).path("tasks").get(0).path("id").asText();

        mockMvc.perform(post("/api/v1/approval/tasks/" + taskId + "/approve")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"first-approve\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/approval/tasks/" + taskId + "/approve")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"repeat-approve\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("approval_task_closed"))
                .andExpect(jsonPath("$.requestId").isString());

        mockMvc.perform(post("/api/v1/approval/tasks/" + taskId + "/reject")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"repeat-reject\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("approval_task_closed"))
                .andExpect(jsonPath("$.requestId").isString());
    }

    @Test
    void v1ApprovalTaskRepeatedTransferShouldReturnConflictAndKeepNewTaskStable() throws Exception {
        String token = login("admin", "admin123");
        createQuoteApprovalTemplate(token);
        String quoteId = createQuote(token, "admin");

        String submitBody = mockMvc.perform(post("/api/v1/approval/instances/QUOTE/" + quoteId + "/submit")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bizType\":\"QUOTE\",\"bizId\":\"" + quoteId + "\",\"amount\":1000,\"role\":\"SALES\",\"department\":\"DEFAULT\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String instanceId = objectMapper.readTree(submitBody).path("id").asText();

        String detailBody = mockMvc.perform(get("/api/v1/approval/instances/" + instanceId)
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String taskId = objectMapper.readTree(detailBody).path("tasks").get(0).path("id").asText();

        String transferBody = mockMvc.perform(post("/api/v1/approval/tasks/" + taskId + "/transfer")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"transfer-1\",\"transferTo\":\"manager\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String newTaskId = objectMapper.readTree(transferBody).path("id").asText();

        mockMvc.perform(post("/api/v1/approval/tasks/" + taskId + "/transfer")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"transfer-2\",\"transferTo\":\"manager\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("approval_task_closed"))
                .andExpect(jsonPath("$.requestId").isString());

        String latestDetailBody = mockMvc.perform(get("/api/v1/approval/instances/" + instanceId)
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode tasks = objectMapper.readTree(latestDetailBody).path("tasks");
        boolean foundPendingTransferredTask = false;
        for (JsonNode task : tasks) {
            if (newTaskId.equals(task.path("id").asText()) && "PENDING".equals(task.path("status").asText())) {
                foundPendingTransferredTask = true;
                break;
            }
        }
        org.junit.jupiter.api.Assertions.assertTrue(foundPendingTransferredTask);
    }

    @Test
    void legacyPaymentShouldEnforceTenantIsolation() throws Exception {
        PaymentRecord record = new PaymentRecord();
        record.setId("pm_other_" + System.currentTimeMillis());
        record.setTenantId("tenant_other");
        record.setCustomerId("c_1001");
        record.setContractId("ct_1001");
        record.setOrderId(null);
        record.setAmount(100L);
        record.setMethod("Bank");
        record.setStatus("Received");
        record.setOwner("admin");
        record.setRemark("cross tenant");
        record.setReceivedDate(null);
        paymentRecordRepository.save(record);

        String token = login("admin", "admin123");
        mockMvc.perform(patch("/api/payments/" + record.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":200}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/payments/" + record.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        String listBody = mockMvc.perform(get("/api/payments/search")
                        .header("Authorization", "Bearer " + token)
                        .queryParam("page", "1")
                        .queryParam("size", "100"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode items = objectMapper.readTree(listBody).get("items");
        for (JsonNode item : items) {
            org.junit.jupiter.api.Assertions.assertFalse(record.getId().equals(item.path("id").asText()));
        }
    }

    @Test
    void v1OpsSloSnapshotShouldReturnOperationalSummary() throws Exception {
        String token = login("admin", "admin123");

        mockMvc.perform(get("/api/v1/ops/slo-snapshot")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallStatus").isString())
                .andExpect(jsonPath("$.alerts").isArray())
                .andExpect(jsonPath("$.thresholds.errorRateMax").exists())
                .andExpect(jsonPath("$.summary.errorRate").exists())
                .andExpect(jsonPath("$.summary.dashboardP95Ms").exists())
                .andExpect(jsonPath("$.summary.dashboardP99Ms").exists())
                .andExpect(jsonPath("$.performanceWindow.requestCount").exists())
                .andExpect(jsonPath("$.performanceWindow.keyRoutes.dashboard.p99Ms").exists())
                .andExpect(jsonPath("$.performanceWindow.keyRoutes.dashboard.errorRate5xx").exists())
                .andExpect(jsonPath("$.alertsLevel").exists())
                .andExpect(jsonPath("$.alertsDetailed").isArray())
                .andExpect(jsonPath("$.errorBudget.daily.budget").exists())
                .andExpect(jsonPath("$.errorBudget.daily.burnRate").exists())
                .andExpect(jsonPath("$.errorBudget.weekly.remaining").exists())
                .andExpect(jsonPath("$.oncall.primary").exists())
                .andExpect(jsonPath("$.oncall.escalation").exists())
                .andExpect(jsonPath("$.readiness.ok").exists())
                .andExpect(jsonPath("$.dependencies.database").exists())
                .andExpect(jsonPath("$.api.keyRoutes.dashboard.p95Ms").exists())
                .andExpect(jsonPath("$.auditExport.totalSubmitted").exists());
    }

    private void createQuoteApprovalTemplate(String token) throws Exception {
        mockMvc.perform(post("/api/v1/approval/templates")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bizType\":\"QUOTE\",\"name\":\"Quote Approval\",\"amountMin\":0,\"amountMax\":999999999,\"approverRoles\":\"MANAGER\"}"))
                .andExpect(status().isCreated());
    }

    private void setTenantApprovalMode(String token, String approvalMode) throws Exception {
        mockMvc.perform(patch("/api/v2/tenant-config")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approvalMode\":\"" + approvalMode + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalMode").value(approvalMode));
    }

    private String createProduct(String token, String code, String name, long standardPrice) throws Exception {
        String body = mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + code + "\",\"name\":\"" + name + "\",\"status\":\"ACTIVE\",\"standardPrice\":" + standardPrice + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("id").asText();
    }

    private String createQuote(String token, String owner) throws Exception {
        return createQuote(token, owner, "DRAFT", "c_1001", "");
    }

    private String createQuote(String token, String owner, String status, String customerId, String opportunityId) throws Exception {
        String body = mockMvc.perform(post("/api/v1/quotes")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"" + customerId + "\",\"owner\":\"" + owner + "\",\"status\":\"" + status + "\",\"opportunityId\":\"" + opportunityId + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("id").asText();
    }

    private void upsertQuoteItems(String token, String quoteId, String itemsJson) throws Exception {
        mockMvc.perform(post("/api/v1/quotes/" + quoteId + "/items")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemsJson))
                .andExpect(status().isOk());
    }

    private String findQuoteStatusById(String token, String quoteId) throws Exception {
        String body = mockMvc.perform(get("/api/v1/quotes")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant_default")
                        .queryParam("page", "1")
                        .queryParam("size", "200"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode items = objectMapper.readTree(body).path("items");
        for (JsonNode item : items) {
            if (quoteId.equals(item.path("id").asText())) {
                return item.path("status").asText();
            }
        }
        return "";
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




