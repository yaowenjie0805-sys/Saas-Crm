package com.yao.crm.service;

import static com.yao.crm.support.TestTenant.TENANT_TEST;

import com.yao.crm.entity.WorkflowConnection;
import com.yao.crm.entity.WorkflowDefinition;
import com.yao.crm.entity.WorkflowExecution;
import com.yao.crm.entity.WorkflowNode;
import com.yao.crm.enums.WorkflowStatus;
import com.yao.crm.repository.WorkflowConnectionRepository;
import com.yao.crm.repository.WorkflowDefinitionRepository;
import com.yao.crm.repository.WorkflowExecutionRepository;
import com.yao.crm.repository.WorkflowNodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WorkflowService
 */
@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

    @Mock
    private WorkflowDefinitionRepository workflowRepository;

    @Mock
    private WorkflowNodeRepository nodeRepository;

    @Mock
    private WorkflowConnectionRepository connectionRepository;

    @Mock
    private WorkflowExecutionRepository executionRepository;

    private WorkflowService service;

    @BeforeEach
    void setUp() {
        service = new WorkflowService(workflowRepository, nodeRepository, connectionRepository, executionRepository);
    }

    @Test
    @DisplayName("shouldCreateWorkflow_whenCreateWorkflow")
    void shouldCreateWorkflow_whenCreateWorkflow() {
        WorkflowDefinition savedWorkflow = new WorkflowDefinition();
        savedWorkflow.setId("wf-1");
        when(workflowRepository.save(any(WorkflowDefinition.class))).thenReturn(savedWorkflow);

        WorkflowDefinition result = service.createWorkflow(
                TENANT_TEST, "Test Workflow", "Description", "approval", "owner-1"
        );

        assertNotNull(result);
        verify(workflowRepository).save(any(WorkflowDefinition.class));
    }

    @Test
    @DisplayName("shouldSetDraftStatus_whenCreateWorkflow")
    void shouldSetDraftStatus_whenCreateWorkflow() {
        ArgumentCaptor<WorkflowDefinition> captor = ArgumentCaptor.forClass(WorkflowDefinition.class);
        when(workflowRepository.save(any(WorkflowDefinition.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createWorkflow(TENANT_TEST, "Test", "Desc", "approval", "owner-1");

        verify(workflowRepository).save(captor.capture());
        assertEquals(WorkflowStatus.DRAFT.name(), captor.getValue().getStatus());
        assertEquals(1, captor.getValue().getVersion());
    }

    @Test
    @DisplayName("shouldReturnNull_whenWorkflowNotFound")
    void shouldReturnNull_whenWorkflowNotFound() {
        when(workflowRepository.findByIdAndTenantId("wf-1", TENANT_TEST)).thenReturn(Optional.empty());

        Map<String, Object> result = service.getWorkflowDetail(TENANT_TEST, "wf-1");

        assertNull(result);
    }

    @Test
    @DisplayName("shouldReturnWorkflowDetail_whenWorkflowFound")
    void shouldReturnWorkflowDetail_whenWorkflowFound() {
        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId("wf-1");
        when(workflowRepository.findByIdAndTenantId("wf-1", TENANT_TEST)).thenReturn(Optional.of(workflow));
        when(nodeRepository.findByWorkflowIdOrderByPositionXAscPositionYAsc("wf-1")).thenReturn(new ArrayList<>());
        when(connectionRepository.findByWorkflowId("wf-1")).thenReturn(new ArrayList<>());

        Map<String, Object> result = service.getWorkflowDetail(TENANT_TEST, "wf-1");

        assertNotNull(result);
        assertEquals(workflow, result.get("workflow"));
        assertNotNull(result.get("nodes"));
        assertNotNull(result.get("connections"));
    }

    @Test
    @DisplayName("shouldAddNode_whenAddNode")
    void shouldAddNode_whenAddNode() {
        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId("wf-1");
        when(workflowRepository.findByIdAndTenantId("wf-1", TENANT_TEST)).thenReturn(Optional.of(workflow));
        when(nodeRepository.save(any(WorkflowNode.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkflowNode result = service.addNode(TENANT_TEST, "wf-1", "TRIGGER", "RECORD_CREATED", "Start", 0, 0, "{}");

        assertNotNull(result);
        assertEquals("TRIGGER", result.getNodeType());
        verify(nodeRepository).save(any(WorkflowNode.class));
    }

    @Test
    @DisplayName("shouldNormalizeNodeTypeAndSubtype_whenAddNode")
    void shouldNormalizeNodeTypeAndSubtype_whenAddNode() {
        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId("wf-1");
        when(workflowRepository.findByIdAndTenantId("wf-1", TENANT_TEST)).thenReturn(Optional.of(workflow));
        when(nodeRepository.save(any(WorkflowNode.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkflowNode result = service.addNode(TENANT_TEST, "wf-1", " trigger ", " manual ", "Start", 0, 0, "{}");

        assertEquals("TRIGGER", result.getNodeType());
        assertEquals("MANUAL", result.getNodeSubtype());
    }

    @Test
    @DisplayName("shouldThrowException_whenAddNodeToNonExistentWorkflow")
    void shouldThrowException_whenAddNodeToNonExistentWorkflow() {
        when(workflowRepository.findByIdAndTenantId("wf-1", TENANT_TEST)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            service.addNode(TENANT_TEST, "wf-1", "TRIGGER", "RECORD_CREATED", "Start", 0, 0, "{}");
        });
    }

    @Test
    @DisplayName("shouldAddConnection_whenAddConnection")
    void shouldAddConnection_whenAddConnection() {
        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId("wf-1");
        when(workflowRepository.findByIdAndTenantId("wf-1", TENANT_TEST)).thenReturn(Optional.of(workflow));
        when(connectionRepository.save(any(WorkflowConnection.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkflowConnection result = service.addConnection(TENANT_TEST, "wf-1", "node-1", "node-2", "DEFAULT", "Label");

        assertNotNull(result);
        assertEquals("node-1", result.getSourceNodeId());
        assertEquals("node-2", result.getTargetNodeId());
    }

    @Test
    @DisplayName("shouldNormalizeConnectionTypeAndFallbackToDefault_whenAddConnection")
    void shouldNormalizeConnectionTypeAndFallbackToDefault_whenAddConnection() {
        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId("wf-1");
        when(workflowRepository.findByIdAndTenantId("wf-1", TENANT_TEST)).thenReturn(Optional.of(workflow));
        when(connectionRepository.save(any(WorkflowConnection.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkflowConnection normalized = service.addConnection(TENANT_TEST, "wf-1", "node-1", "node-2", " branch ", "Label");
        WorkflowConnection defaulted = service.addConnection(TENANT_TEST, "wf-1", "node-1", "node-2", "   ", "Label");

        assertEquals("BRANCH", normalized.getConnectionType());
        assertEquals("DEFAULT", defaulted.getConnectionType());
    }

    @Test
    @DisplayName("shouldFailValidation_whenNoTriggerNode")
    void shouldFailValidation_whenNoTriggerNode() {
        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId("wf-1");
        when(workflowRepository.findByIdAndTenantId("wf-1", TENANT_TEST)).thenReturn(Optional.of(workflow));
        
        WorkflowNode node = new WorkflowNode();
        node.setId("node-1");
        node.setName("Action");
        node.setNodeType("ACTION");
        node.setConfigValidation("VALID");
        when(nodeRepository.findByWorkflowIdOrderByPositionXAscPositionYAsc("wf-1")).thenReturn(Arrays.asList(node));
        when(connectionRepository.findByWorkflowId("wf-1")).thenReturn(new ArrayList<>());

        Map<String, Object> result = service.validateWorkflow(TENANT_TEST, "wf-1");

        assertFalse((Boolean) result.get("valid"));
        assertFalse(((List<?>) result.get("errors")).isEmpty());
    }

    @Test
    @DisplayName("shouldPassValidation_whenWorkflowIsValid")
    void shouldPassValidation_whenWorkflowIsValid() {
        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId("wf-1");
        when(workflowRepository.findByIdAndTenantId("wf-1", TENANT_TEST)).thenReturn(Optional.of(workflow));
        
        WorkflowNode triggerNode = new WorkflowNode();
        triggerNode.setId("node-1");
        triggerNode.setName("Trigger");
        triggerNode.setNodeType("TRIGGER");
        triggerNode.setConfigValidation("VALID");
        
        WorkflowNode endNode = new WorkflowNode();
        endNode.setId("node-2");
        endNode.setName("End");
        endNode.setNodeType("END");
        endNode.setConfigValidation("VALID");
        
        when(nodeRepository.findByWorkflowIdOrderByPositionXAscPositionYAsc("wf-1")).thenReturn(Arrays.asList(triggerNode, endNode));
        when(connectionRepository.findByWorkflowId("wf-1")).thenReturn(new ArrayList<>());

        Map<String, Object> result = service.validateWorkflow(TENANT_TEST, "wf-1");

        assertTrue((Boolean) result.get("valid"));
    }

    @Test
    @DisplayName("shouldTreatNodeTypeCaseInsensitively_whenValidateWorkflow")
    void shouldTreatNodeTypeCaseInsensitively_whenValidateWorkflow() {
        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId("wf-1");
        when(workflowRepository.findByIdAndTenantId("wf-1", TENANT_TEST)).thenReturn(Optional.of(workflow));

        WorkflowNode triggerNode = new WorkflowNode();
        triggerNode.setId("node-1");
        triggerNode.setName("Trigger");
        triggerNode.setNodeType("trigger");
        triggerNode.setConfigValidation("valid");

        WorkflowNode endNode = new WorkflowNode();
        endNode.setId("node-2");
        endNode.setName("End");
        endNode.setNodeType("end");
        endNode.setConfigValidation("VALID");

        when(nodeRepository.findByWorkflowIdOrderByPositionXAscPositionYAsc("wf-1")).thenReturn(Arrays.asList(triggerNode, endNode));
        when(connectionRepository.findByWorkflowId("wf-1")).thenReturn(new ArrayList<>());

        Map<String, Object> result = service.validateWorkflow(TENANT_TEST, "wf-1");

        assertTrue((Boolean) result.get("valid"));
    }

    @Test
    @DisplayName("shouldActivateWorkflow_whenValidationPasses")
    void shouldActivateWorkflow_whenValidationPasses() {
        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId("wf-1");
        workflow.setStatus(WorkflowStatus.DRAFT.name());
        when(workflowRepository.findByIdAndTenantId("wf-1", TENANT_TEST)).thenReturn(Optional.of(workflow));
        when(nodeRepository.findByWorkflowIdOrderByPositionXAscPositionYAsc("wf-1")).thenReturn(new ArrayList<>());
        when(connectionRepository.findByWorkflowId("wf-1")).thenReturn(new ArrayList<>());
        when(workflowRepository.save(any(WorkflowDefinition.class))).thenAnswer(inv -> inv.getArgument(0));

        // First add a trigger node
        WorkflowNode triggerNode = new WorkflowNode();
        triggerNode.setId("node-1");
        triggerNode.setNodeType("TRIGGER");
        triggerNode.setName("Trigger");
        triggerNode.setConfigValidation("VALID");
        when(nodeRepository.findByWorkflowIdOrderByPositionXAscPositionYAsc("wf-1")).thenReturn(Arrays.asList(triggerNode));

        WorkflowDefinition result = service.activateWorkflow(TENANT_TEST, "wf-1", "admin");

        assertEquals(WorkflowStatus.ACTIVE.name(), result.getStatus());
        assertNotNull(result.getActivatedAt());
    }

    @Test
    @DisplayName("shouldThrowException_whenActivateInvalidWorkflow")
    void shouldThrowException_whenActivateInvalidWorkflow() {
        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId("wf-1");
        workflow.setStatus(WorkflowStatus.DRAFT.name());
        when(workflowRepository.findByIdAndTenantId("wf-1", TENANT_TEST)).thenReturn(Optional.of(workflow));
        when(nodeRepository.findByWorkflowIdOrderByPositionXAscPositionYAsc("wf-1")).thenReturn(new ArrayList<>());
        when(connectionRepository.findByWorkflowId("wf-1")).thenReturn(new ArrayList<>());

        assertThrows(IllegalStateException.class, () -> {
            service.activateWorkflow(TENANT_TEST, "wf-1", "admin");
        });
    }

    @Test
    @DisplayName("shouldDeactivateWorkflow_whenNoRunningExecutions")
    void shouldDeactivateWorkflow_whenNoRunningExecutions() {
        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId("wf-1");
        workflow.setStatus(WorkflowStatus.ACTIVE.name());
        when(workflowRepository.findByIdAndTenantId("wf-1", TENANT_TEST)).thenReturn(Optional.of(workflow));
        when(executionRepository.findRunningByWorkflowId("wf-1")).thenReturn(new ArrayList<>());
        when(workflowRepository.save(any(WorkflowDefinition.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkflowDefinition result = service.deactivateWorkflow(TENANT_TEST, "wf-1");

        assertEquals(WorkflowStatus.PAUSED.name(), result.getStatus());
    }

    @Test
    @DisplayName("shouldThrowException_whenDeactivateWithRunningExecutions")
    void shouldThrowException_whenDeactivateWithRunningExecutions() {
        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId("wf-1");
        workflow.setStatus(WorkflowStatus.ACTIVE.name());
        when(workflowRepository.findByIdAndTenantId("wf-1", TENANT_TEST)).thenReturn(Optional.of(workflow));
        
        WorkflowExecution execution = new WorkflowExecution();
        when(executionRepository.findRunningByWorkflowId("wf-1")).thenReturn(Arrays.asList(execution));

        assertThrows(IllegalStateException.class, () -> {
            service.deactivateWorkflow(TENANT_TEST, "wf-1");
        });
    }

    @Test
    @DisplayName("shouldDeleteWorkflowAndRelatedData_whenDeleteWorkflow")
    void shouldDeleteWorkflowAndRelatedData_whenDeleteWorkflow() {
        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId("wf-1");
        when(workflowRepository.findByIdAndTenantId("wf-1", TENANT_TEST)).thenReturn(Optional.of(workflow));

        service.deleteWorkflow(TENANT_TEST, "wf-1");

        verify(connectionRepository).deleteByWorkflowId("wf-1");
        verify(nodeRepository).deleteByWorkflowId("wf-1");
        verify(workflowRepository).deleteByIdAndTenantId("wf-1", TENANT_TEST);
    }

    @Test
    @DisplayName("shouldReturnWorkflows_whenGetWorkflows")
    void shouldReturnWorkflows_whenGetWorkflows() {
        List<WorkflowDefinition> workflows = new ArrayList<>();
        when(workflowRepository.findByTenantId(TENANT_TEST)).thenReturn(workflows);

        List<WorkflowDefinition> result = service.getWorkflows(TENANT_TEST, null, null);

        assertEquals(workflows, result);
    }

    @Test
    @DisplayName("shouldReturnWorkflowsByStatus_whenGetWorkflowsWithStatus")
    void shouldReturnWorkflowsByStatus_whenGetWorkflowsWithStatus() {
        List<WorkflowDefinition> workflows = new ArrayList<>();
        when(workflowRepository.findByTenantIdAndStatus(TENANT_TEST, "ACTIVE")).thenReturn(workflows);

        List<WorkflowDefinition> result = service.getWorkflows(TENANT_TEST, "ACTIVE", null);

        assertEquals(workflows, result);
        verify(workflowRepository, never()).findByTenantId(anyString());
    }

    @Test
    @DisplayName("shouldReturnNodeTypes_whenGetNodeTypes")
    void shouldReturnNodeTypes_whenGetNodeTypes() {
        Map<String, List<Map<String, String>>> nodeTypes = service.getNodeTypes();

        assertNotNull(nodeTypes);
        assertTrue(nodeTypes.containsKey("TRIGGER"));
        assertTrue(nodeTypes.containsKey("ACTION"));
        assertTrue(nodeTypes.containsKey("CONDITION"));
        assertTrue(nodeTypes.containsKey("APPROVAL"));
    }
}

