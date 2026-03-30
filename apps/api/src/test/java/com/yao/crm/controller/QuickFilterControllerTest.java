package com.yao.crm.controller;

import com.yao.crm.repository.QuickFilterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class QuickFilterControllerTest {

    @Mock
    private QuickFilterRepository quickFilterRepository;

    private QuickFilterController controller;

    @BeforeEach
    void setUp() {
        controller = new QuickFilterController(quickFilterRepository);
    }

    @Test
    void createQuickFilterShouldRejectBlankTenantHeader() {
        QuickFilterController.CreateQuickFilterRequest request = new QuickFilterController.CreateQuickFilterRequest();

        ResponseEntity<?> response = controller.createQuickFilter("   ", request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(quickFilterRepository);
    }

    @Test
    void createQuickFilterShouldRejectBlankName() {
        QuickFilterController.CreateQuickFilterRequest request = new QuickFilterController.CreateQuickFilterRequest();
        request.name = "   ";
        request.entityType = "lead";

        ResponseEntity<?> response = controller.createQuickFilter("tenant-1", request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(quickFilterRepository);
    }

    @Test
    void createQuickFilterShouldRejectBlankEntityType() {
        QuickFilterController.CreateQuickFilterRequest request = new QuickFilterController.CreateQuickFilterRequest();
        request.name = "My filter";
        request.entityType = "   ";

        ResponseEntity<?> response = controller.createQuickFilter("tenant-1", request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(quickFilterRepository);
    }

    @Test
    void createQuickFilterShouldTrimFieldsBeforeSaving() {
        QuickFilterController.CreateQuickFilterRequest request = new QuickFilterController.CreateQuickFilterRequest();
        request.name = "  My filter  ";
        request.owner = "  alice  ";
        request.entityType = "  lead  ";

        ResponseEntity<?> response = controller.createQuickFilter("  tenant-1  ", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ArgumentCaptor<com.yao.crm.entity.QuickFilter> captor =
                ArgumentCaptor.forClass(com.yao.crm.entity.QuickFilter.class);
        verify(quickFilterRepository).save(captor.capture());
        com.yao.crm.entity.QuickFilter saved = captor.getValue();
        assertEquals("tenant-1", saved.getTenantId());
        assertEquals("My filter", saved.getName());
        assertEquals("alice", saved.getOwner());
        assertEquals("lead", saved.getEntityType());
        assertNotNull(saved.getId());
    }

    @Test
    void getQuickFiltersShouldRejectBlankEntityType() {
        ResponseEntity<?> response = controller.getQuickFilters("tenant-1", "   ", null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(quickFilterRepository);
    }

    @Test
    void deleteQuickFilterShouldReturnNoContentWhenDeleted() {
        when(quickFilterRepository.deleteByTenantIdAndId("tenant-1", "qf-1")).thenReturn(1);

        ResponseEntity<?> response = controller.deleteQuickFilter("tenant-1", "qf-1");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void deleteQuickFilterShouldRejectBlankId() {
        ResponseEntity<?> response = controller.deleteQuickFilter("tenant-1", "   ");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(quickFilterRepository, never()).deleteByTenantIdAndId(anyString(), anyString());
        verifyNoInteractions(quickFilterRepository);
    }

    @Test
    void deleteQuickFilterShouldTrimTenantAndIdBeforeDeleting() {
        when(quickFilterRepository.deleteByTenantIdAndId("tenant-1", "qf-1")).thenReturn(1);

        ResponseEntity<?> response = controller.deleteQuickFilter("  tenant-1  ", "  qf-1  ");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(quickFilterRepository).deleteByTenantIdAndId("tenant-1", "qf-1");
    }

    @Test
    void deleteQuickFilterShouldReturnNotFoundWhenMissing() {
        when(quickFilterRepository.deleteByTenantIdAndId("tenant-1", "qf-404")).thenReturn(0);

        ResponseEntity<?> response = controller.deleteQuickFilter("tenant-1", "qf-404");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void updateQuickFilterShouldRejectBlankId() {
        QuickFilterController.UpdateQuickFilterRequest request = new QuickFilterController.UpdateQuickFilterRequest();

        ResponseEntity<?> response = controller.updateQuickFilter("tenant-1", "   ", request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(quickFilterRepository, never()).findByTenantIdAndId(anyString(), anyString());
        verify(quickFilterRepository, never()).save(any(com.yao.crm.entity.QuickFilter.class));
        verifyNoInteractions(quickFilterRepository);
    }

    @Test
    void updateQuickFilterShouldUseTenantScopedLookup() {
        com.yao.crm.entity.QuickFilter filter = new com.yao.crm.entity.QuickFilter();
        filter.setId("qf-1");
        filter.setTenantId("tenant-1");
        filter.setName("Before");
        when(quickFilterRepository.findByTenantIdAndId("tenant-1", "qf-1")).thenReturn(Optional.of(filter));

        QuickFilterController.UpdateQuickFilterRequest request = new QuickFilterController.UpdateQuickFilterRequest();
        request.name = "After";

        ResponseEntity<?> response = controller.updateQuickFilter("tenant-1", "qf-1", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(quickFilterRepository).findByTenantIdAndId("tenant-1", "qf-1");
        verify(quickFilterRepository, never()).findById(anyString());
        assertNotNull(response.getBody());
    }

    @Test
    void updateQuickFilterShouldTrimFieldsBeforeSaving() {
        com.yao.crm.entity.QuickFilter filter = new com.yao.crm.entity.QuickFilter();
        filter.setId("qf-1");
        filter.setTenantId("tenant-1");
        filter.setName("Before");
        filter.setOwner("before-owner");
        filter.setEntityType("before-type");
        when(quickFilterRepository.findByTenantIdAndId("tenant-1", "qf-1")).thenReturn(Optional.of(filter));

        QuickFilterController.UpdateQuickFilterRequest request = new QuickFilterController.UpdateQuickFilterRequest();
        request.name = "  After  ";
        request.owner = "  bob  ";
        request.entityType = "  opportunity  ";

        ResponseEntity<?> response = controller.updateQuickFilter("tenant-1", "qf-1", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ArgumentCaptor<com.yao.crm.entity.QuickFilter> captor =
                ArgumentCaptor.forClass(com.yao.crm.entity.QuickFilter.class);
        verify(quickFilterRepository).save(captor.capture());
        com.yao.crm.entity.QuickFilter saved = captor.getValue();
        assertEquals("After", saved.getName());
        assertEquals("bob", saved.getOwner());
        assertEquals("opportunity", saved.getEntityType());
        verify(quickFilterRepository, times(1)).findByTenantIdAndId("tenant-1", "qf-1");
        assertNotNull(response.getBody());
    }
}
