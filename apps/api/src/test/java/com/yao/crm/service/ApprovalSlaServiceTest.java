package com.yao.crm.service;

import static com.yao.crm.support.TestTenant.TENANT_TEST;

import com.yao.crm.entity.ApprovalTask;
import com.yao.crm.repository.ApprovalEventRepository;
import com.yao.crm.repository.ApprovalInstanceRepository;
import com.yao.crm.repository.ApprovalTaskRepository;
import com.yao.crm.util.IdGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalSlaServiceTest {

    @Mock
    private ApprovalTaskRepository taskRepository;

    @Mock
    private ApprovalInstanceRepository instanceRepository;

    @Mock
    private ApprovalEventRepository eventRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private NotificationJobService notificationJobService;

    @Mock
    private IdGenerator idGenerator;

    @InjectMocks
    private ApprovalSlaService service;

    @Test
    @DisplayName("shouldQueryExistingEscalationsByTenant_whenScanningOverdueTasks")
    void shouldQueryExistingEscalationsByTenant_whenScanningOverdueTasks() {
        ApprovalTask overdue = new ApprovalTask();
        overdue.setId("task-1");
        overdue.setTenantId(TENANT_TEST);
        overdue.setInstanceId("inst-1");
        overdue.setStatus("PENDING");
        overdue.setApproverRole("MANAGER");
        overdue.setSeq(1);
        overdue.setNodeKey("n1");
        overdue.setSlaMinutes(60);
        overdue.setDeadlineAt(LocalDateTime.now().minusMinutes(5));

        when(taskRepository.findByStatusAndDeadlineAtBefore(anyString(), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(overdue));
        when(taskRepository.findByTenantIdAndEscalationSourceTaskIdIn(anyString(), anyList()))
                .thenReturn(Collections.emptyList());
        when(idGenerator.generate(anyString())).thenReturn("gen-1", "gen-2", "gen-3");
        when(taskRepository.save(any(ApprovalTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApprovalSlaService.ScanResult result = service.scanOverdueAndEscalate();

        assertEquals(1, result.getAffected());
        ArgumentCaptor<List<String>> sourceIdsCaptor = ArgumentCaptor.forClass(List.class);
        verify(taskRepository).findByTenantIdAndEscalationSourceTaskIdIn(
                org.mockito.ArgumentMatchers.eq(TENANT_TEST),
                sourceIdsCaptor.capture()
        );
        assertEquals(Collections.singletonList("task-1"), sourceIdsCaptor.getValue());
        verify(taskRepository, never()).findByEscalationSourceTaskIdIn(anyList());
    }
}

