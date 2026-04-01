package com.yao.crm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yao.crm.dto.request.V1ReportDesignerRunRequest;
import com.yao.crm.entity.ReportDesignerTemplate;
import com.yao.crm.repository.ContractRecordRepository;
import com.yao.crm.repository.CustomerRepository;
import com.yao.crm.repository.LeadRepository;
import com.yao.crm.repository.OpportunityRepository;
import com.yao.crm.repository.PaymentRecordRepository;
import com.yao.crm.repository.ReportDesignerTemplateRepository;
import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.I18nService;
import com.yao.crm.util.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;
import java.util.Optional;

import static com.yao.crm.support.TestTenant.TENANT_TEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class V1ReportDesignerControllerTest {

    private ReportDesignerTemplateRepository templateRepository;
    private CustomerRepository customerRepository;
    private OpportunityRepository opportunityRepository;
    private ContractRecordRepository contractRecordRepository;
    private PaymentRecordRepository paymentRecordRepository;
    private LeadRepository leadRepository;
    private IdGenerator idGenerator;
    private V1ReportDesignerController controller;

    @BeforeEach
    void setUp() {
        templateRepository = mock(ReportDesignerTemplateRepository.class);
        customerRepository = mock(CustomerRepository.class);
        opportunityRepository = mock(OpportunityRepository.class);
        contractRecordRepository = mock(ContractRecordRepository.class);
        paymentRecordRepository = mock(PaymentRecordRepository.class);
        leadRepository = mock(LeadRepository.class);
        idGenerator = mock(IdGenerator.class);
        AuditLogService auditLogService = mock(AuditLogService.class);
        controller = new V1ReportDesignerController(
                templateRepository,
                customerRepository,
                opportunityRepository,
                contractRecordRepository,
                paymentRecordRepository,
                leadRepository,
                auditLogService,
                new ObjectMapper(),
                new I18nService(),
                idGenerator
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void runTemplateShouldRejectInvalidDatasetOverride() {
        MockHttpServletRequest request = authedRequest("ANALYST");
        ReportDesignerTemplate template = new ReportDesignerTemplate();
        template.setId("rpt_1");
        template.setTenantId(TENANT_TEST);
        template.setOwner("alice");
        template.setVisibility("TENANT");
        template.setDataset("CUSTOMERS");
        template.setVersion(2);
        template.setConfigJson("{}");
        when(templateRepository.findByIdAndTenantId("rpt_1", TENANT_TEST)).thenReturn(Optional.of(template));

        V1ReportDesignerRunRequest payload = new V1ReportDesignerRunRequest();
        payload.setDataset("  invalid_dataset  ");

        ResponseEntity<?> response = controller.runTemplate(request, "  rpt_1  ", payload);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("report_dataset_invalid", body.get("code"));
        verify(templateRepository).findByIdAndTenantId("rpt_1", TENANT_TEST);
        verifyNoInteractions(customerRepository, opportunityRepository, contractRecordRepository, paymentRecordRepository, leadRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getTemplateShouldRejectBlankId() {
        MockHttpServletRequest request = authedRequest("ANALYST");

        ResponseEntity<?> response = controller.getTemplate(request, "   ");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("bad_request", body.get("code"));
        verifyNoInteractions(templateRepository);
    }

    private MockHttpServletRequest authedRequest(String role) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authRole", role);
        request.setAttribute("authUsername", "alice");
        request.setAttribute("authTenantId", TENANT_TEST);
        return request;
    }
}
