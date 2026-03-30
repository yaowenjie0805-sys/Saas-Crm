package com.yao.crm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yao.crm.dto.request.V1ApprovalSubmitRequest;
import com.yao.crm.dto.request.V1ApprovalTemplatePatchRequest;
import com.yao.crm.entity.ApprovalInstance;
import com.yao.crm.repository.ApprovalEventRepository;
import com.yao.crm.repository.ApprovalInstanceRepository;
import com.yao.crm.repository.ApprovalTaskRepository;
import com.yao.crm.repository.ApprovalTemplateRepository;
import com.yao.crm.repository.ContractRecordRepository;
import com.yao.crm.repository.PaymentRecordRepository;
import com.yao.crm.repository.QuoteRepository;
import com.yao.crm.service.ApprovalSlaService;
import com.yao.crm.service.ApprovalTemplateVersionService;
import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.I18nService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class V1ApprovalControllerTest {

    @Mock
    private ApprovalTemplateRepository templateRepository;
    @Mock
    private ApprovalInstanceRepository instanceRepository;
    @Mock
    private ApprovalTaskRepository taskRepository;
    @Mock
    private ApprovalEventRepository eventRepository;
    @Mock
    private ContractRecordRepository contractRepository;
    @Mock
    private PaymentRecordRepository paymentRepository;
    @Mock
    private QuoteRepository quoteRepository;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private ApprovalSlaService approvalSlaService;
    @Mock
    private ApprovalTemplateVersionService templateVersionService;
    @Mock
    private I18nService i18nService;

    private V1ApprovalController controller;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        controller = new V1ApprovalController(
                templateRepository,
                instanceRepository,
                taskRepository,
                eventRepository,
                contractRepository,
                paymentRepository,
                quoteRepository,
                auditLogService,
                approvalSlaService,
                templateVersionService,
                new ObjectMapper(),
                i18nService
        );
        when(i18nService.msg(any(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
        request = new MockHttpServletRequest();
        request.setAttribute("authRole", "MANAGER");
        request.setAttribute("authUsername", "manager-1");
        request.setAttribute("authTenantId", "  tenant-1  ");
    }

    @Test
    @SuppressWarnings("unchecked")
    void patchTemplateShouldReturnBadRequestWhenIdIsBlank() {
        ResponseEntity<?> response = controller.patchTemplate(request, "   ", new V1ApprovalTemplatePatchRequest());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("bad_request", body.get("code"));
        verifyNoInteractions(templateRepository);
    }

    @Test
    void patchTemplateShouldTrimIdAndTenantBeforeLookup() {
        when(templateRepository.findByIdAndTenantId("tpl-1", "tenant-1")).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.patchTemplate(request, "  tpl-1  ", new V1ApprovalTemplatePatchRequest());

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(templateRepository).findByIdAndTenantId("tpl-1", "tenant-1");
    }

    @Test
    @SuppressWarnings("unchecked")
    void approveTaskShouldReturnBadRequestWhenTaskIdIsBlank() {
        ResponseEntity<?> response = controller.approveTask(request, "   ", null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("bad_request", body.get("code"));
        verifyNoInteractions(taskRepository);
    }

    @Test
    void approveTaskShouldTrimTaskIdAndTenantBeforeLookup() {
        when(taskRepository.findByIdAndTenantId("task-1", "tenant-1")).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.approveTask(request, "  task-1  ", null);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(taskRepository).findByIdAndTenantId("task-1", "tenant-1");
    }

    @Test
    void submitInstanceShouldTrimBizTypeBizIdAndTenantBeforeConflictCheck() {
        ApprovalInstance active = new ApprovalInstance();
        active.setId("api-1");
        active.setStatus("PENDING");
        active.setBizType("QUOTE");
        active.setBizId("biz-1");
        when(instanceRepository.findTopByTenantIdAndBizTypeAndBizIdAndStatusInOrderByCreatedAtDesc(
                eq("tenant-1"),
                eq("QUOTE"),
                eq("biz-1"),
                eq(Arrays.asList("PENDING", "WAITING"))
        )).thenReturn(Optional.of(active));

        V1ApprovalSubmitRequest payload = new V1ApprovalSubmitRequest();
        ResponseEntity<?> response = controller.submitInstance(request, "  quote  ", "  biz-1  ", payload);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        verify(instanceRepository).findTopByTenantIdAndBizTypeAndBizIdAndStatusInOrderByCreatedAtDesc(
                "tenant-1",
                "QUOTE",
                "biz-1",
                Arrays.asList("PENDING", "WAITING")
        );
        verifyNoInteractions(templateRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void instanceDetailShouldReturnInstanceNotFoundCode() {
        when(instanceRepository.findByIdAndTenantId("ins-1", "tenant-1")).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.instanceDetail(request, "  ins-1  ");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(instanceRepository).findByIdAndTenantId("ins-1", "tenant-1");
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("approval_instance_not_found", body.get("code"));
    }
}
