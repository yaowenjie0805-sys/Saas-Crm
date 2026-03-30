package com.yao.crm.controller;

import com.yao.crm.dto.request.V1LeadAssignmentRuleRequest;
import com.yao.crm.entity.LeadAssignmentRule;
import com.yao.crm.service.I18nService;
import com.yao.crm.service.LeadAssignmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class V1LeadAssignmentControllerTest {

    @Mock
    private LeadAssignmentService leadAssignmentService;

    @Mock
    private I18nService i18nService;

    private V1LeadAssignmentController controller;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        controller = new V1LeadAssignmentController(leadAssignmentService, i18nService);
        when(i18nService.msg(any(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
        request = new MockHttpServletRequest();
        request.setAttribute("authRole", "MANAGER");
        request.setAttribute("authUsername", "manager-1");
        request.setAttribute("authTenantId", "  tenant-1  ");
    }

    @Test
    void patchShouldReturnBadRequestWhenIdIsBlank() {
        ResponseEntity<?> response = controller.patch(request, "   ", validPayload());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(leadAssignmentService);
    }

    @Test
    void patchShouldTrimIdAndTenantBeforeCallingService() {
        V1LeadAssignmentRuleRequest payload = validPayload();
        LeadAssignmentRule row = new LeadAssignmentRule();
        row.setId("rule-1");
        when(leadAssignmentService.patchRule("tenant-1", "rule-1", "manager-1", payload)).thenReturn(row);
        when(leadAssignmentService.toView(row)).thenReturn(Collections.singletonMap("id", "rule-1"));

        ResponseEntity<?> response = controller.patch(request, "  rule-1  ", payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(leadAssignmentService).patchRule("tenant-1", "rule-1", "manager-1", payload);
    }

    private V1LeadAssignmentRuleRequest validPayload() {
        V1LeadAssignmentRuleRequest payload = new V1LeadAssignmentRuleRequest();
        payload.setName("Rule A");
        payload.setEnabled(true);
        V1LeadAssignmentRuleRequest.Member member = new V1LeadAssignmentRuleRequest.Member();
        member.setUsername("alice");
        member.setWeight(1);
        member.setEnabled(true);
        payload.setMembers(Arrays.asList(member));
        return payload;
    }
}
