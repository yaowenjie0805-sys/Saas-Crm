package com.yao.crm.controller;

import com.yao.crm.dto.request.V1NotificationBatchRetryRequest;
import com.yao.crm.entity.NotificationJob;
import com.yao.crm.service.I18nService;
import com.yao.crm.service.NotificationJobService;
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
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class V1NotificationJobControllerTest {

    @Mock
    private NotificationJobService notificationJobService;

    @Mock
    private I18nService i18nService;

    private V1NotificationJobController controller;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        controller = new V1NotificationJobController(notificationJobService, 100, i18nService);
        when(i18nService.msg(any(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
        request = new MockHttpServletRequest();
        request.setAttribute("authRole", "MANAGER");
        request.setAttribute("authTenantId", " tenant-1 ");
    }

    @Test
    void retryShouldReturnBadRequestWhenJobIdIsBlank() {
        ResponseEntity<?> response = controller.retry(request, "   ");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(notificationJobService);
    }

    @Test
    void retryShouldReturnConflictWhenStatusTransitionIsInvalid() {
        NotificationJob job = new NotificationJob();
        job.setId("job-1");
        job.setStatus("SUCCESS");
        when(notificationJobService.retry("tenant-1", "job-1")).thenReturn(job);

        ResponseEntity<?> response = controller.retry(request, " job-1 ");

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        verify(notificationJobService).retry("tenant-1", "job-1");
    }

    @Test
    void batchRetryShouldTrimAndFilterJobIdsBeforeCallingService() {
        V1NotificationBatchRetryRequest payload = new V1NotificationBatchRetryRequest();
        payload.setJobIds(Arrays.asList(" job-1 ", " ", null, "\tjob-2\t"));
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("requested", 2);
        summary.put("succeeded", 1);
        summary.put("skipped", 1);
        summary.put("notFound", 0);
        summary.put("forbidden", 0);
        when(notificationJobService.batchRetryByIds("tenant-1", Arrays.asList("job-1", "job-2"))).thenReturn(summary);

        ResponseEntity<?> response = controller.batchRetry(request, payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(notificationJobService).batchRetryByIds("tenant-1", Arrays.asList("job-1", "job-2"));
    }

    @Test
    void listJobsShouldUseAllForBlankStatusAndClampPaging() {
        when(notificationJobService.listJobsPaged("tenant-1", "ALL", 1, 1)).thenReturn(Collections.emptyMap());

        ResponseEntity<?> response = controller.listJobs(request, "   ", 0, 0);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(notificationJobService).listJobsPaged("tenant-1", "ALL", 1, 1);
    }
}
