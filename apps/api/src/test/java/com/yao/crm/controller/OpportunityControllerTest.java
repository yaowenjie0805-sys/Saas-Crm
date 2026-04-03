package com.yao.crm.controller;

import com.yao.crm.repository.OpportunityRepository;
import com.yao.crm.dto.request.UpdateOpportunityRequest;
import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.I18nService;
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

import static com.yao.crm.support.TestTenant.TENANT_TEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpportunityControllerTest {

    @Mock
    private OpportunityRepository opportunityRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ValueNormalizerService valueNormalizerService;
    @Mock
    private IdGenerator idGenerator;

    private MockHttpServletRequest request;

    private OpportunityController controller;

    @BeforeEach
    void setUp() {
        controller = new OpportunityController(opportunityRepository, auditLogService, valueNormalizerService, new I18nService(), idGenerator);
        request = new MockHttpServletRequest();
        request.setAttribute("authRole", "MANAGER");
        request.setAttribute("authUsername", "manager");
        request.setAttribute("authTenantId", TENANT_TEST);
    }

    @Test
    void deleteOpportunityShouldDeleteWithinTenantAndReturnNoContent() {
        when(opportunityRepository.deleteByIdAndTenantId("opp-1", TENANT_TEST)).thenReturn(1L);

        ResponseEntity<?> response = controller.deleteOpportunity(request, "opp-1");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(opportunityRepository).deleteByIdAndTenantId("opp-1", TENANT_TEST);
        verify(opportunityRepository, never()).deleteById(anyString());
        verify(opportunityRepository, never()).existsByIdAndTenantId(anyString(), anyString());
        verify(auditLogService).record(eq("manager"), eq("MANAGER"), eq("DELETE"), eq("OPPORTUNITY"), eq("opp-1"), anyString(), eq(TENANT_TEST));
    }

    @Test
    void deleteOpportunityShouldTrimIdBeforeTenantScopedDelete() {
        when(opportunityRepository.deleteByIdAndTenantId("opp-1", TENANT_TEST)).thenReturn(1L);

        ResponseEntity<?> response = controller.deleteOpportunity(request, "  opp-1  ");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(opportunityRepository).deleteByIdAndTenantId("opp-1", TENANT_TEST);
        verify(opportunityRepository, never()).deleteById(anyString());
        verify(opportunityRepository, never()).existsByIdAndTenantId(anyString(), anyString());
        verify(auditLogService).record(eq("manager"), eq("MANAGER"), eq("DELETE"), eq("OPPORTUNITY"), eq("opp-1"), anyString(), eq(TENANT_TEST));
    }

    @Test
    void deleteOpportunityShouldReturnNotFoundWhenTenantScopedDeleteAffectsZeroRows() {
        when(opportunityRepository.deleteByIdAndTenantId("opp-1", TENANT_TEST)).thenReturn(0L);

        ResponseEntity<?> response = controller.deleteOpportunity(request, "opp-1");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(opportunityRepository).deleteByIdAndTenantId("opp-1", TENANT_TEST);
        verify(opportunityRepository, never()).deleteById(anyString());
        verify(opportunityRepository, never()).existsByIdAndTenantId(anyString(), anyString());
        verify(auditLogService, never()).record(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void deleteOpportunityShouldReturnBadRequestForBlankIdWithoutTouchingRepository() {
        ResponseEntity<?> response = controller.deleteOpportunity(request, " ");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(opportunityRepository, auditLogService);
    }

    @Test
    void updateOpportunityShouldReturnBadRequestForBlankIdWithoutTouchingRepository() {
        ResponseEntity<?> response = controller.updateOpportunity(request, " ", new UpdateOpportunityRequest());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(opportunityRepository, auditLogService);
    }

    @Test
    void deleteOpportunityShouldReturnForbiddenForUnauthorizedRoleWithoutTouchingRepository() {
        request.setAttribute("authRole", "SALES");

        ResponseEntity<?> response = controller.deleteOpportunity(request, "opp-1");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verifyNoInteractions(opportunityRepository, auditLogService);
    }
}
