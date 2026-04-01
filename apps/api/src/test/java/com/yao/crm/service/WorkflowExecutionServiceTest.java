package com.yao.crm.service;

import static com.yao.crm.support.TestTenant.TENANT_TEST;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yao.crm.entity.WorkflowConnection;
import com.yao.crm.entity.WorkflowDefinition;
import com.yao.crm.entity.WorkflowExecution;
import com.yao.crm.entity.WorkflowNode;
import com.yao.crm.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 鐎规悶鍎扮紞鏂棵规担鐟扳挃閻炴稑鑻槐鈺呭箼鎼粹€崇闁稿繐鍟粊瀵告嫚?
 */
@ExtendWith(MockitoExtension.class)
class WorkflowExecutionServiceTest {
    private static final String TENANT_ID = TENANT_TEST;

    @Mock
    private WorkflowDefinitionRepository workflowRepository;

    @Mock
    private WorkflowNodeRepository nodeRepository;

    @Mock
    private WorkflowConnectionRepository connectionRepository;

    @Mock
    private WorkflowExecutionRepository executionRepository;

    @Mock
    private ApprovalNodeRepository approvalNodeRepository;

    private WorkflowExecutionService executionService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        executionService = spy(new WorkflowExecutionService(
                workflowRepository,
                nodeRepository,
                connectionRepository,
                executionRepository,
                approvalNodeRepository,
                objectMapper
        ));
    }

    @Test
    void testStartExecution_Success() {
        // Given
        String workflowId = "wf_test_123";
        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId(workflowId);
        workflow.setTenantId(TENANT_ID);
        workflow.setStatus("ACTIVE");
        workflow.setVersion(1);
        workflow.setExecutionCount(0);

        when(workflowRepository.findByIdAndTenantId(workflowId, TENANT_ID)).thenReturn(Optional.of(workflow));
        when(workflowRepository.save(any())).thenReturn(workflow);
        when(executionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(executionService).executeAsync(anyString(), anyString());

        // When
        WorkflowExecution execution = executionService.startExecution(
                TENANT_ID, workflowId, "MANUAL", "user123", null, null
        );

        // Then
        assertNotNull(execution);
        assertEquals(TENANT_ID, execution.getTenantId());
        assertEquals(workflowId, execution.getWorkflowId());
        assertEquals("RUNNING", execution.getStatus());
        verify(workflowRepository).save(any());
    }

    @Test
    void testStartExecution_WorkflowNotFound() {
        // Given
        String workflowId = "wf_nonexistent";
        when(workflowRepository.findByIdAndTenantId(workflowId, TENANT_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
                executionService.startExecution(TENANT_ID, workflowId, "MANUAL", null, null, null)
        );
    }

    @Test
    void testStartExecution_WorkflowNotActive() {
        // Given
        String workflowId = "wf_draft";

        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId(workflowId);
        workflow.setTenantId(TENANT_ID);
        workflow.setStatus("DRAFT"); // Not ACTIVE

        when(workflowRepository.findByIdAndTenantId(workflowId, TENANT_ID)).thenReturn(Optional.of(workflow));

        // When & Then
        assertThrows(IllegalStateException.class, () ->
                executionService.startExecution(TENANT_ID, workflowId, "MANUAL", null, null, null)
        );
    }

    @Test
    void testExecuteNextNodes_WithTriggerNode() {
        // Given
        String executionId = "exec_123";
        String workflowId = "wf_123";
        String triggerNodeId = "node_trigger";

        WorkflowExecution execution = new WorkflowExecution();
        execution.setId(executionId);
        execution.setWorkflowId(workflowId);
        execution.setStatus("RUNNING");

        WorkflowNode triggerNode = new WorkflowNode();
        triggerNode.setId(triggerNodeId);
        triggerNode.setWorkflowId(workflowId);
        triggerNode.setNodeType("TRIGGER");
        triggerNode.setNodeSubtype("MANUAL");
        triggerNode.setConfigJson("{}");

        when(executionRepository.findById(executionId)).thenReturn(Optional.of(execution));
        when(executionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(nodeRepository.findByWorkflowIdAndNodeType(workflowId, "TRIGGER"))
                .thenReturn(Collections.singletonList(triggerNode));
        when(nodeRepository.findAllById(Collections.singletonList(triggerNodeId)))
                .thenReturn(Collections.singletonList(triggerNode));
        when(connectionRepository.findBySourceNodeId(triggerNodeId))
                .thenReturn(Collections.emptyList());

        // When
        executionService.executeNextNodes(executionId);

        // Then
        verify(executionRepository, atLeastOnce()).save(any());
        verify(nodeRepository).findAllById(Collections.singletonList(triggerNodeId));
    }

    @Test
    void testCancelExecution_Success() {
        // Given
        String executionId = "exec_123";

        WorkflowExecution execution = new WorkflowExecution();
        execution.setId(executionId);
        execution.setTenantId(TENANT_ID);
        execution.setStatus("RUNNING");

        when(executionRepository.findByIdAndTenantId(executionId, TENANT_ID)).thenReturn(Optional.of(execution));
        when(executionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        executionService.cancelExecution(TENANT_ID, executionId, "user123");

        // Then
        verify(executionRepository).save(argThat(exec ->
                "CANCELLED".equals(exec.getStatus())
        ));
    }

    @Test
    void testCancelExecution_NotRunning() {
        // Given
        String executionId = "exec_123";

        WorkflowExecution execution = new WorkflowExecution();
        execution.setId(executionId);
        execution.setTenantId(TENANT_ID);
        execution.setStatus("COMPLETED");

        when(executionRepository.findByIdAndTenantId(executionId, TENANT_ID)).thenReturn(Optional.of(execution));

        // When & Then
        assertThrows(IllegalStateException.class, () ->
                executionService.cancelExecution(TENANT_ID, executionId, "user123")
        );
    }

    @Test
    void testRetryExecution_Success() {
        // Given
        String oldExecutionId = "exec_old";
        String workflowId = "wf_123";

        WorkflowExecution oldExecution = new WorkflowExecution();
        oldExecution.setId(oldExecutionId);
        oldExecution.setTenantId(TENANT_ID);
        oldExecution.setWorkflowId(workflowId);
        oldExecution.setStatus("FAILED");
        oldExecution.setTriggerType("MANUAL");

        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId(workflowId);
        workflow.setTenantId(TENANT_ID);
        workflow.setStatus("ACTIVE");
        workflow.setVersion(1);
        workflow.setExecutionCount(0);

        when(executionRepository.findByIdAndTenantId(oldExecutionId, TENANT_ID)).thenReturn(Optional.of(oldExecution));
        when(workflowRepository.findByIdAndTenantId(workflowId, TENANT_ID)).thenReturn(Optional.of(workflow));
        when(workflowRepository.save(any())).thenReturn(workflow);
        when(executionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(executionService).executeAsync(anyString(), anyString());

        // When
        WorkflowExecution newExecution = executionService.retryExecution(TENANT_ID, oldExecutionId);

        // Then
        assertNotNull(newExecution);
        assertEquals(workflowId, newExecution.getWorkflowId());
        assertEquals("RUNNING", newExecution.getStatus());
    }

    @Test
    void testRetryExecution_NotFailed() {
        // Given
        String executionId = "exec_123";

        WorkflowExecution execution = new WorkflowExecution();
        execution.setId(executionId);
        execution.setTenantId(TENANT_ID);
        execution.setStatus("RUNNING");

        when(executionRepository.findByIdAndTenantId(executionId, TENANT_ID)).thenReturn(Optional.of(execution));

        // When & Then
        assertThrows(IllegalStateException.class, () ->
                executionService.retryExecution(TENANT_ID, executionId)
        );
    }

    @Test
    void testGetExecutionDetail() {
        // Given
        String executionId = "exec_123";
        String workflowId = "wf_123";

        WorkflowExecution execution = new WorkflowExecution();
        execution.setId(executionId);
        execution.setTenantId(TENANT_ID);
        execution.setWorkflowId(workflowId);
        execution.setStatus("RUNNING");
        execution.setExecutionContext("{}");

        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId(workflowId);
        workflow.setTenantId(TENANT_ID);
        workflow.setName("Test Workflow");

        when(executionRepository.findByIdAndTenantId(executionId, TENANT_ID)).thenReturn(Optional.of(execution));
        when(workflowRepository.findByIdAndTenantId(workflowId, TENANT_ID)).thenReturn(Optional.of(workflow));

        // When
        Map<String, Object> detail = executionService.getExecutionDetail(TENANT_ID, executionId);

        // Then
        assertNotNull(detail);
        assertNotNull(detail.get("execution"));
        assertNotNull(detail.get("workflow"));
    }

    @Test
    void testGetExecutionHistory() {
        // Given
        String workflowId = "wf_123";

        List<WorkflowExecution> executions = Arrays.asList(
                createExecution("exec_1", "COMPLETED"),
                createExecution("exec_2", "RUNNING"),
                createExecution("exec_3", "FAILED")
        );

        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId(workflowId);
        workflow.setTenantId(TENANT_ID);

        when(workflowRepository.findByIdAndTenantId(workflowId, TENANT_ID)).thenReturn(Optional.of(workflow));
        when(executionRepository.findByWorkflowIdOrderByStartedAtDesc(eq(workflowId), any(Pageable.class)))
                .thenReturn(executions);

        // When
        List<WorkflowExecution> result = executionService.getExecutionHistory(TENANT_ID, workflowId, null, 0, 20);

        // Then
        assertEquals(3, result.size());
    }

    @Test
    void testGetExecutionHistory_WithStatus() {
        // Given
        String workflowId = "wf_123";
        String status = "FAILED";

        List<WorkflowExecution> executions = Collections.singletonList(
                createExecution("exec_1", status)
        );

        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId(workflowId);
        workflow.setTenantId(TENANT_ID);

        when(workflowRepository.findByIdAndTenantId(workflowId, TENANT_ID)).thenReturn(Optional.of(workflow));
        when(executionRepository.findByWorkflowIdAndStatusOrderByStartedAtDesc(eq(workflowId), eq(status), any(Pageable.class)))
                .thenReturn(executions);

        // When
        List<WorkflowExecution> result = executionService.getExecutionHistory(TENANT_ID, workflowId, status, 0, 20);

        // Then
        assertEquals(1, result.size());
        assertEquals(status, result.get(0).getStatus());
    }

    @Test
    void testGetExecutionDetail_WrongTenantRejected() {
        String executionId = "exec_123";
        when(executionRepository.findByIdAndTenantId(executionId, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                executionService.getExecutionDetail(TENANT_ID, executionId)
        );
    }

    @Test
    void testGetExecutionHistory_WrongTenantRejected() {
        String workflowId = "wf_123";
        when(workflowRepository.findByIdAndTenantId(workflowId, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                executionService.getExecutionHistory(TENANT_ID, workflowId, null, 0, 20)
        );
    }

    private WorkflowExecution createExecution(String id, String status) {
        WorkflowExecution execution = new WorkflowExecution();
        execution.setId(id);
        execution.setTenantId(TENANT_ID);
        execution.setStatus(status);
        execution.setWorkflowId("wf_123");
        return execution;
    }
}

