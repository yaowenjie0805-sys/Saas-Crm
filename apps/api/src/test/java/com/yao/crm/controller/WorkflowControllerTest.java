package com.yao.crm.controller;

import com.yao.crm.entity.WorkflowExecution;
import com.yao.crm.service.WorkflowExecutionService;
import com.yao.crm.service.WorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import static com.yao.crm.support.TestTenant.TENANT_TEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowControllerTest {

    @Mock
    private WorkflowService workflowService;

    @Mock
    private WorkflowExecutionService executionService;

    private WorkflowController controller;

    @BeforeEach
    void setUp() {
        controller = new WorkflowController(workflowService, executionService);
        setAuthRole("ADMIN");
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void getWorkflowsShouldRejectBlankTenantHeader() {
        ResponseEntity<?> response = controller.getWorkflows("   ", null, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(workflowService, executionService);
    }

    @Test
    void getWorkflowsShouldTrimTenantAndNormalizeBlankQueryParams() {
        when(workflowService.getWorkflows("tenant-1", null, null)).thenReturn(Collections.emptyList());

        ResponseEntity<?> response = controller.getWorkflows(" tenant-1 ", "   ", "");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(workflowService).getWorkflows("tenant-1", null, null);
        verifyNoInteractions(executionService);
    }

    @Test
    void createWorkflowShouldReturnForbiddenForSalesRoleAndNotCallService() {
        setAuthRole("SALES");
        WorkflowController.CreateWorkflowRequest request = new WorkflowController.CreateWorkflowRequest();
        request.name = "wf";

        ResponseEntity<?> response = controller.createWorkflow(TENANT_TEST, request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        Map<?, ?> body = assertInstanceOf(Map.class, response.getBody());
        assertEquals("forbidden", body.get("error"));
        verifyNoInteractions(workflowService, executionService);
    }

    @Test
    void getWorkflowsShouldAllowSalesRole() {
        setAuthRole("SALES");
        when(workflowService.getWorkflows(TENANT_TEST, null, null)).thenReturn(List.of());

        ResponseEntity<?> response = controller.getWorkflows(TENANT_TEST, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(workflowService).getWorkflows(TENANT_TEST, null, null);
        verifyNoInteractions(executionService);
    }

    @Test
    void deleteWorkflowShouldReturnNoContentWhenWorkflowExists() {
        ResponseEntity<?> response = controller.deleteWorkflow(" tenant-1 ", " wf-1 ");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(workflowService).deleteWorkflow("tenant-1", "wf-1");
        verifyNoInteractions(executionService);
    }

    @Test
    void deleteWorkflowShouldReturnNotFoundWhenWorkflowDoesNotExist() {
        doThrow(new IllegalArgumentException("Workflow not found"))
                .when(workflowService).deleteWorkflow(TENANT_TEST, "wf-1");

        ResponseEntity<?> response = controller.deleteWorkflow(TENANT_TEST, "wf-1");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(workflowService).deleteWorkflow(TENANT_TEST, "wf-1");
        verifyNoInteractions(executionService);
    }

    @Test
    void deleteWorkflowShouldReturnBadRequestForBlankId() {
        ResponseEntity<?> response = controller.deleteWorkflow(TENANT_TEST, "   ");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(workflowService, executionService);
    }

    @Test
    void deleteNodeShouldReturnNoContentWhenPathIsValid() {
        ResponseEntity<?> response = controller.deleteNode(" wf-1 ", " node-1 ");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verifyNoInteractions(workflowService, executionService);
    }

    @Test
    void deleteNodeShouldReturnBadRequestForBlankPathParam() {
        ResponseEntity<?> response = controller.deleteNode("wf-1", "   ");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(workflowService, executionService);
    }

    @Test
    void updateWorkflowShouldReturnBadRequestForBlankId() {
        WorkflowController.UpdateWorkflowRequest request = new WorkflowController.UpdateWorkflowRequest();

        ResponseEntity<?> response = controller.updateWorkflow("   ", request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(workflowService, executionService);
    }

    @Test
    void updateNodeShouldReturnBadRequestForBlankWorkflowId() {
        WorkflowController.UpdateNodeRequest request = new WorkflowController.UpdateNodeRequest();

        ResponseEntity<?> response = controller.updateNode("   ", "node-1", request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(workflowService, executionService);
    }

    @Test
    void updateNodeShouldReturnBadRequestForBlankNodeId() {
        WorkflowController.UpdateNodeRequest request = new WorkflowController.UpdateNodeRequest();

        ResponseEntity<?> response = controller.updateNode("wf-1", "   ", request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(workflowService, executionService);
    }

    @Test
    void deleteConnectionShouldReturnNoContentWhenPathIsValid() {
        ResponseEntity<?> response = controller.deleteConnection(" wf-1 ", " conn-1 ");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verifyNoInteractions(workflowService, executionService);
    }

    @Test
    void deleteConnectionShouldReturnBadRequestForBlankPathParam() {
        ResponseEntity<?> response = controller.deleteConnection("wf-1", "   ");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(workflowService, executionService);
    }

    @Test
    void getWorkflowDetailShouldReturnBadRequestForBlankId() {
        ResponseEntity<?> response = controller.getWorkflowDetail(TENANT_TEST, "   ");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(workflowService, executionService);
    }

    @Test
    void activateWorkflowShouldReturnBadRequestForBlankId() {
        WorkflowController.ActivateRequest request = new WorkflowController.ActivateRequest();
        request.activatedBy = "user-1";

        ResponseEntity<?> response = controller.activateWorkflow(TENANT_TEST, "   ", request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(workflowService, executionService);
    }

    @Test
    void activateWorkflowShouldReturnConflictWhenServiceThrowsIllegalState() {
        WorkflowController.ActivateRequest request = new WorkflowController.ActivateRequest();
        request.activatedBy = "user-1";
        doThrow(new IllegalStateException("Workflow validation failed"))
                .when(workflowService).activateWorkflow(TENANT_TEST, "wf-1", "user-1");

        ResponseEntity<?> response = controller.activateWorkflow(TENANT_TEST, "wf-1", request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        verify(workflowService).activateWorkflow(TENANT_TEST, "wf-1", "user-1");
        verifyNoInteractions(executionService);
    }

    @Test
    void deactivateWorkflowShouldReturnBadRequestForBlankId() {
        ResponseEntity<?> response = controller.deactivateWorkflow(TENANT_TEST, "   ");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(workflowService, executionService);
    }

    @Test
    void validateWorkflowShouldReturnBadRequestForBlankId() {
        ResponseEntity<?> response = controller.validateWorkflow(TENANT_TEST, "   ");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(workflowService, executionService);
    }

    @Test
    void getWorkflowStatsShouldReturnBadRequestForBlankId() {
        ResponseEntity<?> response = controller.getWorkflowStats(TENANT_TEST, "   ");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(workflowService, executionService);
    }

    @Test
    void addNodeShouldReturnBadRequestForBlankId() {
        WorkflowController.AddNodeRequest request = new WorkflowController.AddNodeRequest();

        ResponseEntity<?> response = controller.addNode(TENANT_TEST, "   ", request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(workflowService, executionService);
    }

    @Test
    void testWorkflowShouldReturnBadRequestForBlankId() {
        WorkflowController.TestWorkflowRequest request = new WorkflowController.TestWorkflowRequest();

        ResponseEntity<?> response = controller.testWorkflow(TENANT_TEST, "   ", request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(workflowService, executionService);
    }

    @Test
    void executeWorkflowShouldReturnBadRequestForBlankId() {
        WorkflowController.ExecuteWorkflowRequest request = new WorkflowController.ExecuteWorkflowRequest();

        ResponseEntity<?> response = controller.executeWorkflow(TENANT_TEST, "   ", request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(workflowService, executionService);
    }

    @Test
    void executeWorkflowShouldTrimPathIdBeforeCallingService() throws Exception {
        WorkflowController.ExecuteWorkflowRequest request = new WorkflowController.ExecuteWorkflowRequest();
        request.payload = new HashMap<>();
        request.payload.put("key", "value");

        WorkflowExecution execution = mock(WorkflowExecution.class);
        when(execution.getId()).thenReturn("exec-1");
        when(execution.getStatus()).thenReturn("RUNNING");
        when(execution.getStartedAt()).thenReturn(java.time.LocalDateTime.parse("2026-03-28T10:15:30"));
        when(executionService.startExecution(
                eq("tenant-1"),
                eq("wf-1"),
                eq("MANUAL"),
                eq(null),
                eq("{\"key\":\"value\"}"),
                eq(request.payload)))
                .thenReturn(execution);

        ResponseEntity<?> response = controller.executeWorkflow(" tenant-1 ", " wf-1 ", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(executionService).startExecution("tenant-1", "wf-1", "MANUAL", null, "{\"key\":\"value\"}", request.payload);
        verifyNoInteractions(workflowService);
    }

    @Test
    void executeWorkflowShouldReturnInternalServerErrorWhenServiceThrowsUnexpectedException() throws Exception {
        WorkflowController.ExecuteWorkflowRequest request = new WorkflowController.ExecuteWorkflowRequest();
        request.payload = new HashMap<>();
        request.payload.put("key", "value");
        doThrow(new RuntimeException("database down"))
                .when(executionService).startExecution(
                        eq(TENANT_TEST),
                        eq("wf-1"),
                        eq("MANUAL"),
                        eq(null),
                        eq("{\"key\":\"value\"}"),
                        eq(request.payload));

        ResponseEntity<?> response = controller.executeWorkflow(TENANT_TEST, "wf-1", request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(executionService).startExecution(TENANT_TEST, "wf-1", "MANUAL", null, "{\"key\":\"value\"}", request.payload);
        verifyNoInteractions(workflowService);
    }

    @Test
    void getExecutionDetailShouldReturnBadRequestForBlankExecutionId() {
        ResponseEntity<?> response = controller.getExecutionDetail(TENANT_TEST, "   ");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(workflowService, executionService);
    }

    @Test
    void getExecutionDetailShouldTrimExecutionIdBeforeCallingService() {
        Map<String, Object> detail = Collections.singletonMap("id", "exec-1");
        when(executionService.getExecutionDetail("tenant-1", "exec-1")).thenReturn(detail);

        ResponseEntity<?> response = controller.getExecutionDetail(" tenant-1 ", " exec-1 ");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(detail, response.getBody());
        verify(executionService).getExecutionDetail("tenant-1", "exec-1");
        verifyNoInteractions(workflowService);
    }

    @Test
    void getExecutionDetailShouldReturnNotFoundWhenExecutionDoesNotExist() {
        doThrow(new IllegalArgumentException("Execution not found"))
                .when(executionService).getExecutionDetail(TENANT_TEST, "exec-404");

        ResponseEntity<?> response = controller.getExecutionDetail(TENANT_TEST, "exec-404");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(executionService).getExecutionDetail(TENANT_TEST, "exec-404");
        verifyNoInteractions(workflowService);
    }

    @Test
    void getExecutionHistoryShouldReturnBadRequestForBlankWorkflowId() {
        ResponseEntity<?> response = controller.getExecutionHistory(TENANT_TEST, "   ", null, 0, 20);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(workflowService, executionService);
    }

    @Test
    void getExecutionHistoryShouldTrimWorkflowIdBeforeCallingService() {
        when(executionService.getExecutionHistory("tenant-1", "wf-1", null, 1, 10))
                .thenReturn(Collections.emptyList());

        ResponseEntity<?> response = controller.getExecutionHistory(" tenant-1 ", " wf-1 ", "   ", 1, 10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(executionService).getExecutionHistory("tenant-1", "wf-1", null, 1, 10);
        verifyNoInteractions(workflowService);
    }

    @Test
    void cancelExecutionShouldReturnBadRequestForBlankExecutionId() {
        WorkflowController.CancelExecutionRequest request = new WorkflowController.CancelExecutionRequest();
        request.cancelledBy = "user-1";

        ResponseEntity<?> response = controller.cancelExecution(TENANT_TEST, "   ", request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(workflowService, executionService);
    }

    @Test
    void cancelExecutionShouldTrimExecutionIdBeforeCallingService() {
        WorkflowController.CancelExecutionRequest request = new WorkflowController.CancelExecutionRequest();
        request.cancelledBy = "user-1";

        ResponseEntity<?> response = controller.cancelExecution(" tenant-1 ", " exec-1 ", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(executionService).cancelExecution("tenant-1", "exec-1", "user-1");
        verifyNoInteractions(workflowService);
    }

    @Test
    void cancelExecutionShouldReturnConflictWhenExecutionStatusDisallowsCancel() {
        WorkflowController.CancelExecutionRequest request = new WorkflowController.CancelExecutionRequest();
        request.cancelledBy = "user-1";
        doThrow(new IllegalStateException("Cannot cancel execution with status: COMPLETED"))
                .when(executionService).cancelExecution(TENANT_TEST, "exec-1", "user-1");

        ResponseEntity<?> response = controller.cancelExecution(TENANT_TEST, "exec-1", request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        verify(executionService).cancelExecution(TENANT_TEST, "exec-1", "user-1");
        verifyNoInteractions(workflowService);
    }

    @Test
    void retryExecutionShouldReturnBadRequestForBlankExecutionId() {
        ResponseEntity<?> response = controller.retryExecution(TENANT_TEST, "   ");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(workflowService, executionService);
    }

    @Test
    void retryExecutionShouldTrimExecutionIdBeforeCallingService() {
        WorkflowExecution execution = mock(WorkflowExecution.class);
        when(execution.getId()).thenReturn("exec-2");
        when(execution.getStatus()).thenReturn("RUNNING");
        when(executionService.retryExecution("tenant-1", "exec-1")).thenReturn(execution);

        ResponseEntity<?> response = controller.retryExecution(" tenant-1 ", " exec-1 ");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(executionService).retryExecution("tenant-1", "exec-1");
        verifyNoInteractions(workflowService);
    }

    @Test
    void retryExecutionShouldReturnNotFoundWhenExecutionDoesNotExist() {
        doThrow(new IllegalArgumentException("Execution not found"))
                .when(executionService).retryExecution(TENANT_TEST, "exec-missing");

        ResponseEntity<?> response = controller.retryExecution(TENANT_TEST, "exec-missing");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(executionService).retryExecution(TENANT_TEST, "exec-missing");
        verifyNoInteractions(workflowService);
    }

    @Test
    void handleApprovalCallbackShouldReturnBadRequestForBlankExecutionId() {
        WorkflowController.ApprovalCallbackRequest request = new WorkflowController.ApprovalCallbackRequest();
        request.nodeId = "node-1";
        request.action = "APPROVE";
        request.approverId = "user-1";

        ResponseEntity<?> response = controller.handleApprovalCallback(TENANT_TEST, "   ", request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(workflowService, executionService);
    }

    @Test
    void handleApprovalCallbackShouldTrimExecutionIdBeforeCallingService() {
        WorkflowController.ApprovalCallbackRequest request = new WorkflowController.ApprovalCallbackRequest();
        request.nodeId = "node-1";
        request.action = "APPROVE";
        request.approverId = "user-1";
        request.comments = "ok";

        ResponseEntity<?> response = controller.handleApprovalCallback(" tenant-1 ", " exec-1 ", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(executionService).handleApprovalCallback("tenant-1", "exec-1", "node-1", "APPROVE", "user-1", "ok");
        verifyNoInteractions(workflowService);
    }

    @Test
    void handleApprovalCallbackShouldReturnInternalServerErrorWhenUnexpectedExceptionThrown() {
        WorkflowController.ApprovalCallbackRequest request = new WorkflowController.ApprovalCallbackRequest();
        request.nodeId = "node-1";
        request.action = "APPROVE";
        request.approverId = "user-1";
        request.comments = "ok";
        doThrow(new RuntimeException("service unavailable"))
                .when(executionService).handleApprovalCallback(TENANT_TEST, "exec-1", "node-1", "APPROVE", "user-1", "ok");

        ResponseEntity<?> response = controller.handleApprovalCallback(TENANT_TEST, "exec-1", request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(executionService).handleApprovalCallback(TENANT_TEST, "exec-1", "node-1", "APPROVE", "user-1", "ok");
        verifyNoInteractions(workflowService);
    }

    @Test
    void handleApprovalCallbackShouldReturnForbiddenForSalesWithoutCallbackToken() {
        setAuthRole("SALES");
        WorkflowController.ApprovalCallbackRequest request = new WorkflowController.ApprovalCallbackRequest();
        request.nodeId = "node-1";
        request.action = "APPROVE";
        request.approverId = "user-1";
        request.comments = "ok";

        ResponseEntity<?> response = controller.handleApprovalCallback(TENANT_TEST, "exec-1", request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        Map<?, ?> body = assertInstanceOf(Map.class, response.getBody());
        assertEquals("forbidden", body.get("error"));
        verifyNoInteractions(workflowService, executionService);
    }

    @Test
    void handleApprovalCallbackShouldAllowValidCallbackTokenWithoutWriteRole() {
        controller = new WorkflowController(workflowService, executionService, "test-callback-token");
        setRequestContext("SALES", "test-callback-token");
        WorkflowController.ApprovalCallbackRequest request = new WorkflowController.ApprovalCallbackRequest();
        request.nodeId = "node-1";
        request.action = "APPROVE";
        request.approverId = "user-1";
        request.comments = "ok";

        ResponseEntity<?> response = controller.handleApprovalCallback(TENANT_TEST, "exec-1", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(executionService).handleApprovalCallback(TENANT_TEST, "exec-1", "node-1", "APPROVE", "user-1", "ok");
        verifyNoInteractions(workflowService);
    }

    @Test
    void handleApprovalCallbackShouldAllowSecondTokenWhenMultipleTokensConfigured() {
        controller = new WorkflowController(workflowService, executionService, "fallback-token", "first-token,second-token");
        setRequestContext("SALES", "second-token");
        WorkflowController.ApprovalCallbackRequest request = new WorkflowController.ApprovalCallbackRequest();
        request.nodeId = "node-1";
        request.action = "APPROVE";
        request.approverId = "user-1";
        request.comments = "ok";

        ResponseEntity<?> response = controller.handleApprovalCallback(TENANT_TEST, "exec-1", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(executionService).handleApprovalCallback(TENANT_TEST, "exec-1", "node-1", "APPROVE", "user-1", "ok");
        verifyNoInteractions(workflowService);
    }

    @Test
    void handleApprovalCallbackShouldRejectInvalidTokenWhenMultipleTokensConfigured() {
        controller = new WorkflowController(workflowService, executionService, "fallback-token", "first-token,second-token");
        setRequestContext("SALES", "wrong-token");
        WorkflowController.ApprovalCallbackRequest request = new WorkflowController.ApprovalCallbackRequest();
        request.nodeId = "node-1";
        request.action = "APPROVE";
        request.approverId = "user-1";
        request.comments = "ok";

        ResponseEntity<?> response = controller.handleApprovalCallback(TENANT_TEST, "exec-1", request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        Map<?, ?> body = assertInstanceOf(Map.class, response.getBody());
        assertEquals("forbidden", body.get("error"));
        verifyNoInteractions(workflowService, executionService);
    }

    @Test
    void deleteWorkflowShouldReturnInternalServerErrorWhenServiceThrowsUnexpectedException() {
        doThrow(new RuntimeException("db failure"))
                .when(workflowService).deleteWorkflow(TENANT_TEST, "wf-1");

        ResponseEntity<?> response = controller.deleteWorkflow(TENANT_TEST, "wf-1");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(workflowService).deleteWorkflow(TENANT_TEST, "wf-1");
        verifyNoInteractions(executionService);
    }

    private void setAuthRole(String role) {
        setRequestContext(role, null);
    }

    private void setRequestContext(String role, String callbackTokenHeader) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authRole", role);
        if (callbackTokenHeader != null) {
            request.addHeader("X-Workflow-Callback-Token", callbackTokenHeader);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
