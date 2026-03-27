package com.yao.crm.controller;

import com.yao.crm.entity.SavedSearch;
import com.yao.crm.repository.SavedSearchRepository;
import com.yao.crm.service.GlobalSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchControllerTest {

    @Mock
    private GlobalSearchService globalSearchService;

    @Mock
    private SavedSearchRepository savedSearchRepository;

    private SearchController controller;

    @BeforeEach
    void setUp() {
        controller = new SearchController(globalSearchService, savedSearchRepository);
    }

    @Test
    void saveSearchShouldPreferAuthUsernameAndKeepTenantScope() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");

        SearchController.SaveSearchRequest body = new SearchController.SaveSearchRequest();
        body.owner = "legacy-owner";
        body.name = "Customer hot list";
        body.searchType = null;
        body.queryJson = "{\"q\":\"vip\"}";
        body.isShared = true;
        body.shareWithRoles = "ADMIN";

        when(savedSearchRepository.save(any(SavedSearch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = controller.saveSearch(request, "tenant-1", body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        SavedSearch saved = (SavedSearch) response.getBody();
        assertNotNull(saved);
        assertEquals("tenant-1", saved.getTenantId());
        assertEquals("alice", saved.getOwner());
        assertEquals("Customer hot list", saved.getName());
        assertEquals("GLOBAL", saved.getSearchType());
        assertEquals("{\"q\":\"vip\"}", saved.getQueryJson());
        assertTrue(saved.getIsShared());
        verify(savedSearchRepository).save(any(SavedSearch.class));
    }

    @Test
    void saveSearchShouldReturnBadRequestWhenUserContextMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        SearchController.SaveSearchRequest body = new SearchController.SaveSearchRequest();
        body.owner = "";
        body.name = "Customer hot list";
        body.searchType = "GLOBAL";
        body.queryJson = "{\"q\":\"vip\"}";
        body.isShared = true;
        body.shareWithRoles = "ADMIN";

        ResponseEntity<?> response = controller.saveSearch(request, "tenant-1", body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(savedSearchRepository);
    }

    @Test
    void getSavedSearchesShouldPreferAuthUsernameOverLegacyOwner() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");

        when(savedSearchRepository.findByTenantIdAndOwner("tenant-1", "alice")).thenReturn(Collections.emptyList());

        ResponseEntity<?> response = controller.getSavedSearches(request, "tenant-1", "legacy-owner");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(savedSearchRepository).findByTenantIdAndOwner("tenant-1", "alice");
    }

    @Test
    void getSavedSearchesShouldReturnBadRequestWhenUserContextMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        ResponseEntity<?> response = controller.getSavedSearches(request, "tenant-1", null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(savedSearchRepository);
    }

    @Test
    void getSearchHistoryShouldPreferAuthUsernameAndClampToTenantScopedPageSize() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");

        when(savedSearchRepository.findByTenantIdAndOwnerOrderByLastUsedAtDesc(eq("tenant-1"), eq("alice"), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        ResponseEntity<?> response = controller.getSearchHistory(request, "tenant-1", "legacy-owner", 999);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(savedSearchRepository).findByTenantIdAndOwnerOrderByLastUsedAtDesc(eq("tenant-1"), eq("alice"), pageableCaptor.capture());
        assertEquals(100, pageableCaptor.getValue().getPageSize());
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
    }

    @Test
    void getSearchHistoryShouldReturnBadRequestWhenUserContextMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        ResponseEntity<?> response = controller.getSearchHistory(request, "tenant-1", null, 10);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(savedSearchRepository);
    }

    @Test
    void deleteSavedSearchShouldScopeToTenantAndAuthenticatedOwner() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");

        when(savedSearchRepository.deleteByIdAndTenantIdAndOwner("saved-1", "tenant-1", "alice")).thenReturn(1L);

        ResponseEntity<?> response = controller.deleteSavedSearch(request, "tenant-1", "legacy-owner", "saved-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(savedSearchRepository).deleteByIdAndTenantIdAndOwner("saved-1", "tenant-1", "alice");
    }

    @Test
    void deleteSavedSearchShouldReturnBadRequestWhenUserContextMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        ResponseEntity<?> response = controller.deleteSavedSearch(request, "tenant-1", null, "saved-1");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(savedSearchRepository);
    }

    @Test
    void deleteSavedSearchShouldReturnNotFoundWhenTenantOwnerAndIdDoNotMatch() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");

        when(savedSearchRepository.deleteByIdAndTenantIdAndOwner("saved-1", "tenant-1", "alice")).thenReturn(0L);

        ResponseEntity<?> response = controller.deleteSavedSearch(request, "tenant-1", "legacy-owner", "saved-1");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(savedSearchRepository).deleteByIdAndTenantIdAndOwner("saved-1", "tenant-1", "alice");
    }

    @Test
    void incrementUsageShouldPreferAuthUsernameWhenResolvingSavedSearch() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");

        SavedSearch saved = new SavedSearch();
        saved.setId("saved-1");
        saved.setTenantId("tenant-1");
        saved.setOwner("alice");
        saved.setUsageCount(2);
        saved.setCreatedAt(LocalDateTime.now().minusDays(1));
        saved.setUpdatedAt(LocalDateTime.now().minusDays(1));

        when(savedSearchRepository.findByIdAndTenantIdAndOwner("saved-1", "tenant-1", "alice"))
                .thenReturn(Optional.of(saved));
        when(savedSearchRepository.save(any(SavedSearch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = controller.incrementUsage(request, "tenant-1", "legacy-owner", "saved-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(savedSearchRepository).findByIdAndTenantIdAndOwner("saved-1", "tenant-1", "alice");
        verify(savedSearchRepository).save(saved);
        assertEquals(Integer.valueOf(3), saved.getUsageCount());
        assertNotNull(saved.getLastUsedAt());
    }

    @Test
    void incrementUsageShouldReturnBadRequestWhenUserContextMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        ResponseEntity<?> response = controller.incrementUsage(request, "tenant-1", null, "saved-1");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(savedSearchRepository);
    }

    @Test
    void incrementUsageShouldReturnNotFoundWhenTenantOwnerAndIdDoNotMatch() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");

        when(savedSearchRepository.findByIdAndTenantIdAndOwner("saved-1", "tenant-1", "alice"))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.incrementUsage(request, "tenant-1", "legacy-owner", "saved-1");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(savedSearchRepository).findByIdAndTenantIdAndOwner("saved-1", "tenant-1", "alice");
    }

    @Test
    void getSavedSearchesShouldFallbackToLegacyOwnerWhenAuthUsernameMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        when(savedSearchRepository.findByTenantIdAndOwner("tenant-1", "legacy-owner")).thenReturn(Collections.emptyList());

        ResponseEntity<?> response = controller.getSavedSearches(request, "tenant-1", "legacy-owner");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(savedSearchRepository).findByTenantIdAndOwner("tenant-1", "legacy-owner");
    }
}
