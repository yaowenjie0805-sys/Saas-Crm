package com.yao.crm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yao.crm.dto.request.V1ApprovalSubmitRequest;
import com.yao.crm.dto.request.V1ApprovalTemplatePatchRequest;
import com.yao.crm.entity.ApprovalInstance;
import com.yao.crm.entity.ApprovalTask;
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
import com.yao.crm.util.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.yao.crm.support.TestTenant.TENANT_TEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;
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
    @Mock
    private IdGenerator idGenerator;

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
                i18nService,
                idGenerator
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
        when(templateRepository.findByIdAndTenantId("tpl-1", TENANT_TEST)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.patchTemplate(request, "  tpl-1  ", new V1ApprovalTemplatePatchRequest());

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(templateRepository).findByIdAndTenantId("tpl-1", TENANT_TEST);
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
        when(taskRepository.findByIdAndTenantId("task-1", TENANT_TEST)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.approveTask(request, "  task-1  ", null);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(taskRepository).findByIdAndTenantId("task-1", TENANT_TEST);
    }

    @Test
    void submitInstanceShouldTrimBizTypeBizIdAndTenantBeforeConflictCheck() {
        ApprovalInstance active = new ApprovalInstance();
        active.setId("api-1");
        active.setStatus("PENDING");
        active.setBizType("QUOTE");
        active.setBizId("biz-1");
        when(instanceRepository.findTopByTenantIdAndBizTypeAndBizIdAndStatusInOrderByCreatedAtDesc(
                eq(TENANT_TEST),
                eq("QUOTE"),
                eq("biz-1"),
                eq(Arrays.asList("PENDING", "WAITING"))
        )).thenReturn(Optional.of(active));

        V1ApprovalSubmitRequest payload = new V1ApprovalSubmitRequest();
        ResponseEntity<?> response = controller.submitInstance(request, "  quote  ", "  biz-1  ", payload);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        verify(instanceRepository).findTopByTenantIdAndBizTypeAndBizIdAndStatusInOrderByCreatedAtDesc(
                TENANT_TEST,
                "QUOTE",
                "biz-1",
                Arrays.asList("PENDING", "WAITING")
        );
        verifyNoInteractions(templateRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void instanceDetailShouldReturnInstanceNotFoundCode() {
        when(instanceRepository.findByIdAndTenantId("ins-1", TENANT_TEST)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.instanceDetail(request, "  ins-1  ");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(instanceRepository).findByIdAndTenantId("ins-1", TENANT_TEST);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("approval_instance_not_found", body.get("code"));
    }

    @Test
    void listInstancesShouldUsePagedRepositoryQuery() {
        ApprovalInstance row = new ApprovalInstance();
        row.setId("ins-1");
        when(instanceRepository.findByTenantIdOrderByCreatedAtDesc(eq(TENANT_TEST), any(Pageable.class)))
                .thenReturn(Collections.singletonList(row));

        ResponseEntity<?> response = controller.listInstances(request, 5);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(instanceRepository).findByTenantIdOrderByCreatedAtDesc(eq(TENANT_TEST), any(Pageable.class));
    }

    @Test
    void listTasksShouldUsePagedRepositoryQueryWhenNoPostFilters() {
        ApprovalTask task = new ApprovalTask();
        task.setId("task-1");
        task.setStatus("PENDING");
        when(taskRepository.findByTenantIdOrderByCreatedAtDesc(eq(TENANT_TEST), any(Pageable.class)))
                .thenReturn(Collections.singletonList(task));

        ResponseEntity<?> response = controller.listTasks(request, "", false, false, 3);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(taskRepository).findByTenantIdOrderByCreatedAtDesc(eq(TENANT_TEST), any(Pageable.class));
    }

    @Test
    void listTasksShouldScanMultiplePagesWhenOverdueFilterApplied() {
        List<ApprovalTask> firstPage = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            ApprovalTask nonOverdue = new ApprovalTask();
            nonOverdue.setId("task-" + i);
            nonOverdue.setStatus("PENDING");
            nonOverdue.setDeadlineAt(LocalDateTime.now().plusHours(1));
            firstPage.add(nonOverdue);
        }

        ApprovalTask overdue = new ApprovalTask();
        overdue.setId("task-overdue");
        overdue.setStatus("PENDING");
        overdue.setDeadlineAt(LocalDateTime.now().minusHours(1));

        when(taskRepository.findByTenantIdOrderByCreatedAtDesc(eq(TENANT_TEST), any(Pageable.class)))
                .thenReturn(firstPage)
                .thenReturn(Collections.singletonList(overdue))
                .thenReturn(Collections.emptyList());

        ResponseEntity<?> response = controller.listTasks(request, "", true, false, 1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(taskRepository, times(2)).findByTenantIdOrderByCreatedAtDesc(eq(TENANT_TEST), any(Pageable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void statsShouldUseAggregatedRepositoryQueries() {
        when(instanceRepository.countGroupedByStatus(TENANT_TEST))
                .thenReturn(Arrays.<Object[]>asList(new Object[]{"PENDING", 2L}));
        when(instanceRepository.countGroupedByBizType(TENANT_TEST))
                .thenReturn(Arrays.<Object[]>asList(new Object[]{"QUOTE", 3L}));
        when(instanceRepository.countByTenantId(TENANT_TEST)).thenReturn(5L);

        when(taskRepository.countGroupedByStatus(TENANT_TEST))
                .thenReturn(Arrays.<Object[]>asList(new Object[]{"PENDING", 2L}, new Object[]{"APPROVED", 1L}));
        when(taskRepository.countBacklogByRole(TENANT_TEST))
                .thenReturn(Arrays.<Object[]>asList(new Object[]{"MANAGER", 2L}));
        when(taskRepository.countByTenantId(TENANT_TEST)).thenReturn(4L);
        when(taskRepository.countByTenantIdAndStatusIgnoreCase(TENANT_TEST, "PENDING")).thenReturn(2L);
        when(taskRepository.countOverduePendingTasks(eq(TENANT_TEST), any(LocalDateTime.class))).thenReturn(1L);
        when(taskRepository.countEscalatedTasks(TENANT_TEST)).thenReturn(1L);

        ApprovalTask terminal = new ApprovalTask();
        terminal.setCreatedAt(LocalDateTime.now().minusMinutes(30));
        terminal.setUpdatedAt(LocalDateTime.now());
        when(taskRepository.findByTenantIdAndStatusInOrderByCreatedAtDesc(eq(TENANT_TEST), anyCollection(), any(Pageable.class)))
                .thenReturn(Collections.singletonList(terminal));
        when(templateRepository.countByTenantId(TENANT_TEST)).thenReturn(0L);

        ResponseEntity<?> response = controller.stats(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(instanceRepository).countGroupedByStatus(TENANT_TEST);
        verify(instanceRepository).countGroupedByBizType(TENANT_TEST);
        verify(instanceRepository).countByTenantId(TENANT_TEST);
        verify(taskRepository).countGroupedByStatus(TENANT_TEST);
        verify(taskRepository).countBacklogByRole(TENANT_TEST);
        verify(taskRepository).countByTenantId(TENANT_TEST);
        verify(taskRepository).countByTenantIdAndStatusIgnoreCase(TENANT_TEST, "PENDING");
        verify(taskRepository).countOverduePendingTasks(eq(TENANT_TEST), any(LocalDateTime.class));
        verify(taskRepository).countEscalatedTasks(TENANT_TEST);
        verify(taskRepository).findByTenantIdAndStatusInOrderByCreatedAtDesc(eq(TENANT_TEST), anyCollection(), any(Pageable.class));
        verify(templateRepository).countByTenantId(TENANT_TEST);
        verify(instanceRepository, never()).findByTenantIdOrderByCreatedAtDesc(TENANT_TEST);
        verify(taskRepository, never()).findByTenantIdOrderByCreatedAtDesc(TENANT_TEST);
    }
}

