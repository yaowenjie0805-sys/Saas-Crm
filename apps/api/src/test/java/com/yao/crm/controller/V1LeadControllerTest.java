package com.yao.crm.controller;

import com.yao.crm.dto.request.V1LeadAssignRequest;
import com.yao.crm.dto.request.V1LeadUpsertRequest;
import com.yao.crm.entity.Lead;
import com.yao.crm.repository.ContactRepository;
import com.yao.crm.repository.CustomerRepository;
import com.yao.crm.repository.LeadImportJobRepository;
import com.yao.crm.repository.LeadRepository;
import com.yao.crm.repository.OpportunityRepository;
import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.I18nService;
import com.yao.crm.service.LeadAssignmentService;
import com.yao.crm.service.LeadAutomationService;
import com.yao.crm.service.LeadImportFailedRowsExportJobService;
import com.yao.crm.service.LeadImportService;
import com.yao.crm.service.ValueNormalizerService;
import com.yao.crm.util.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Collections;
import java.util.Optional;

import static com.yao.crm.support.TestTenant.TENANT_TEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class V1LeadControllerTest {

    @Mock
    private LeadRepository leadRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private ContactRepository contactRepository;
    @Mock
    private OpportunityRepository opportunityRepository;
    @Mock
    private LeadImportJobRepository leadImportJobRepository;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private ValueNormalizerService valueNormalizerService;
    @Mock
    private LeadAssignmentService leadAssignmentService;
    @Mock
    private LeadAutomationService leadAutomationService;
    @Mock
    private LeadImportService leadImportService;
    @Mock
    private LeadImportFailedRowsExportJobService leadImportFailedRowsExportJobService;
    @Mock
    private I18nService i18nService;
    @Mock
    private IdGenerator idGenerator;

    private V1LeadController controller;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        controller = new V1LeadController(
                leadRepository,
                customerRepository,
                contactRepository,
                opportunityRepository,
                leadImportJobRepository,
                auditLogService,
                valueNormalizerService,
                leadAssignmentService,
                leadAutomationService,
                leadImportService,
                leadImportFailedRowsExportJobService,
                i18nService,
                idGenerator
        );
        when(i18nService.msg(any(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
        request = new MockHttpServletRequest();
        request.setAttribute("authRole", "MANAGER");
        request.setAttribute("authUsername", "manager-1");
        request.setAttribute("authTenantId", " tenant-1 ");
    }

    @Test
    void patchLeadShouldReturnBadRequestWhenIdIsBlank() {
        V1LeadUpsertRequest payload = new V1LeadUpsertRequest();
        payload.setName("Lead A");

        ResponseEntity<?> response = controller.patchLead(request, "  ", payload);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(leadRepository);
    }

    @Test
    void assignLeadShouldTrimIdAndOwnerBeforeCallingService() {
        Lead lead = new Lead();
        lead.setId("ld-1");
        lead.setOwner("old-owner");
        when(leadRepository.findByIdAndTenantId("ld-1", TENANT_TEST)).thenReturn(Optional.of(lead));
        when(leadAssignmentService.assignLeadOwner(TENANT_TEST, "manager-1", lead, "alice", false)).thenReturn("alice");
        V1LeadAssignRequest payload = new V1LeadAssignRequest();
        payload.setOwner("  alice  ");
        payload.setUseRule(false);

        ResponseEntity<?> response = controller.assignLead(request, "  ld-1  ", payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(leadRepository).findByIdAndTenantId("ld-1", TENANT_TEST);
        verify(leadAssignmentService).assignLeadOwner(TENANT_TEST, "manager-1", lead, "alice", false);
        verify(leadAutomationService).onLeadEvent(TENANT_TEST, "LEAD_ASSIGNED", lead, "manager-1");
    }

    @Test
    void listImportJobsShouldNormalizeStatusAndPagingBeforeServiceCall() {
        when(leadImportService.listJobs(TENANT_TEST, "RUNNING", 1, 1)).thenReturn(Collections.emptyMap());

        ResponseEntity<?> response = controller.listImportJobs(request, " running ", 0, 0);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(leadImportService).listJobs(TENANT_TEST, "RUNNING", 1, 1);
        verify(leadImportService, never()).listJobs(" tenant-1 ", " running ", 0, 0);
    }
}

