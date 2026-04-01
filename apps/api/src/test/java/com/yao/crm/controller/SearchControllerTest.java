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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.yao.crm.support.TestTenant.TENANT_TEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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

        ResponseEntity<?> response = controller.saveSearch(request, TENANT_TEST, body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        SavedSearch saved = (SavedSearch) response.getBody();
        assertNotNull(saved);
        assertEquals(TENANT_TEST, saved.getTenantId());
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

        ResponseEntity<?> response = controller.saveSearch(request, TENANT_TEST, body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(savedSearchRepository);
    }

    @Test
    void saveSearchShouldReturnBadRequestWhenTenantIdIsBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");

        SearchController.SaveSearchRequest body = new SearchController.SaveSearchRequest();
        body.owner = "legacy-owner";
        body.name = "Customer hot list";
        body.searchType = "GLOBAL";
        body.queryJson = "{\"q\":\"vip\"}";
        body.isShared = true;
        body.shareWithRoles = "ADMIN";

        ResponseEntity<?> response = controller.saveSearch(request, "   ", body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(savedSearchRepository);
    }

    @Test
    void getSavedSearchesShouldPreferAuthUsernameOverLegacyOwner() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");

        when(savedSearchRepository.findByTenantIdAndOwner(TENANT_TEST, "alice")).thenReturn(Collections.emptyList());

        ResponseEntity<?> response = controller.getSavedSearches(request, TENANT_TEST, "legacy-owner");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(savedSearchRepository).findByTenantIdAndOwner(TENANT_TEST, "alice");
    }

    @Test
    void getSavedSearchesShouldReturnBadRequestWhenUserContextMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        ResponseEntity<?> response = controller.getSavedSearches(request, TENANT_TEST, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(savedSearchRepository);
    }

    @Test
    void getSearchHistoryShouldPreferAuthUsernameAndClampToTenantScopedPageSize() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");

        when(savedSearchRepository.findByTenantIdAndOwnerOrderByLastUsedAtDesc(eq(TENANT_TEST), eq("alice"), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        ResponseEntity<?> response = controller.getSearchHistory(request, TENANT_TEST, "legacy-owner", 999);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(savedSearchRepository).findByTenantIdAndOwnerOrderByLastUsedAtDesc(eq(TENANT_TEST), eq("alice"), pageableCaptor.capture());
        assertEquals(100, pageableCaptor.getValue().getPageSize());
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
    }

    @Test
    void getSearchHistoryShouldReturnBadRequestWhenUserContextMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        ResponseEntity<?> response = controller.getSearchHistory(request, TENANT_TEST, null, 10);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(savedSearchRepository);
    }

    @Test
    void getSearchHistoryShouldReturnBadRequestWhenTenantIdIsBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");

        ResponseEntity<?> response = controller.getSearchHistory(request, " ", "legacy-owner", 10);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(savedSearchRepository);
    }

    @Test
    void deleteSavedSearchShouldScopeToTenantAndAuthenticatedOwner() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");

        when(savedSearchRepository.deleteByIdAndTenantIdAndOwner("saved-1", TENANT_TEST, "alice")).thenReturn(1L);

        ResponseEntity<?> response = controller.deleteSavedSearch(request, TENANT_TEST, "legacy-owner", "saved-1");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(savedSearchRepository).deleteByIdAndTenantIdAndOwner("saved-1", TENANT_TEST, "alice");
    }

    @Test
    void deleteSavedSearchShouldReturnBadRequestWhenUserContextMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        ResponseEntity<?> response = controller.deleteSavedSearch(request, TENANT_TEST, null, "saved-1");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(savedSearchRepository);
    }

    @Test
    void deleteSavedSearchShouldReturnBadRequestWhenTenantIdIsBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");

        ResponseEntity<?> response = controller.deleteSavedSearch(request, "", "legacy-owner", "saved-1");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(savedSearchRepository);
    }

    @Test
    void deleteSavedSearchShouldReturnBadRequestWhenIdIsBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");

        ResponseEntity<?> response = controller.deleteSavedSearch(request, TENANT_TEST, "legacy-owner", "  ");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(savedSearchRepository, never()).deleteByIdAndTenantIdAndOwner(anyString(), anyString(), anyString());
        verifyNoInteractions(savedSearchRepository);
    }

    @Test
    void deleteSavedSearchShouldTrimTenantAndAuthenticatedOwnerBeforeDeleting() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "  alice  ");

        when(savedSearchRepository.deleteByIdAndTenantIdAndOwner("saved-1", TENANT_TEST, "alice")).thenReturn(1L);

        ResponseEntity<?> response = controller.deleteSavedSearch(request, "  tenant-1  ", "legacy-owner", "saved-1");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(savedSearchRepository).deleteByIdAndTenantIdAndOwner("saved-1", TENANT_TEST, "alice");
    }

    @Test
    void deleteSavedSearchShouldReturnNotFoundWhenTenantOwnerAndIdDoNotMatch() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");

        when(savedSearchRepository.deleteByIdAndTenantIdAndOwner("saved-1", TENANT_TEST, "alice")).thenReturn(0L);

        ResponseEntity<?> response = controller.deleteSavedSearch(request, TENANT_TEST, "legacy-owner", "saved-1");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(savedSearchRepository).deleteByIdAndTenantIdAndOwner("saved-1", TENANT_TEST, "alice");
    }

    @Test
    void deleteSavedSearchShouldReturnNotFoundWhenOnlyLegacyOwnerIsProvidedAndRecordIsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        when(savedSearchRepository.deleteByIdAndTenantIdAndOwner("saved-1", TENANT_TEST, "legacy-owner")).thenReturn(0L);

        ResponseEntity<?> response = controller.deleteSavedSearch(request, TENANT_TEST, "legacy-owner", "saved-1");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(savedSearchRepository).deleteByIdAndTenantIdAndOwner("saved-1", TENANT_TEST, "legacy-owner");
    }

    @Test
    void incrementUsageShouldPreferAuthUsernameWhenResolvingSavedSearch() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");

        SavedSearch saved = new SavedSearch();
        saved.setId("saved-1");
        saved.setTenantId(TENANT_TEST);
        saved.setOwner("alice");
        saved.setUsageCount(2);
        saved.setCreatedAt(LocalDateTime.now().minusDays(1));
        saved.setUpdatedAt(LocalDateTime.now().minusDays(1));

        when(savedSearchRepository.findByIdAndTenantIdAndOwner("saved-1", TENANT_TEST, "alice"))
                .thenReturn(Optional.of(saved));
        when(savedSearchRepository.save(any(SavedSearch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = controller.incrementUsage(request, TENANT_TEST, "legacy-owner", "saved-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(savedSearchRepository).findByIdAndTenantIdAndOwner("saved-1", TENANT_TEST, "alice");
        verify(savedSearchRepository).save(saved);
        assertEquals(Integer.valueOf(3), saved.getUsageCount());
        assertNotNull(saved.getLastUsedAt());
    }

    @Test
    void incrementUsageShouldReturnBadRequestWhenUserContextMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        ResponseEntity<?> response = controller.incrementUsage(request, TENANT_TEST, null, "saved-1");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(savedSearchRepository);
    }

    @Test
    void incrementUsageShouldReturnNotFoundWhenTenantOwnerAndIdDoNotMatch() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");

        when(savedSearchRepository.findByIdAndTenantIdAndOwner("saved-1", TENANT_TEST, "alice"))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.incrementUsage(request, TENANT_TEST, "legacy-owner", "saved-1");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(savedSearchRepository).findByIdAndTenantIdAndOwner("saved-1", TENANT_TEST, "alice");
    }

    @Test
    void incrementUsageShouldReturnNotFoundWhenOnlyLegacyOwnerIsProvidedAndRecordIsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        when(savedSearchRepository.findByIdAndTenantIdAndOwner("saved-1", TENANT_TEST, "legacy-owner"))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.incrementUsage(request, TENANT_TEST, "legacy-owner", "saved-1");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(savedSearchRepository).findByIdAndTenantIdAndOwner("saved-1", TENANT_TEST, "legacy-owner");
    }

    @Test
    void getSavedSearchesShouldFallbackToLegacyOwnerWhenAuthUsernameMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        when(savedSearchRepository.findByTenantIdAndOwner(TENANT_TEST, "legacy-owner")).thenReturn(Collections.emptyList());

        ResponseEntity<?> response = controller.getSavedSearches(request, TENANT_TEST, "legacy-owner");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(savedSearchRepository).findByTenantIdAndOwner(TENANT_TEST, "legacy-owner");
    }

    @Test
    void getSavedSearchesShouldReturnBadRequestWhenTenantIdIsBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");

        ResponseEntity<?> response = controller.getSavedSearches(request, "  ", "legacy-owner");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(savedSearchRepository);
    }

    @Test
    void incrementUsageShouldReturnBadRequestWhenTenantIdIsBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");

        ResponseEntity<?> response = controller.incrementUsage(request, "\t", "legacy-owner", "saved-1");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(savedSearchRepository);
    }

    @Test
    void incrementUsageShouldReturnBadRequestWhenIdIsBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");

        ResponseEntity<?> response = controller.incrementUsage(request, TENANT_TEST, "legacy-owner", "\n");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(savedSearchRepository, never()).findByIdAndTenantIdAndOwner(anyString(), anyString(), anyString());
        verify(savedSearchRepository, never()).save(any(SavedSearch.class));
        verifyNoInteractions(savedSearchRepository);
    }

    @Test
    void searchShouldReturnBadRequestWhenTenantIdIsBlank() {
        ResponseEntity<?> response = controller.search("  ", "vip", 20, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(globalSearchService);
    }

    @Test
    void searchShouldTrimTenantBeforeCallingGlobalSearchService() {
        GlobalSearchService.SearchResult searchResult =
                new GlobalSearchService.SearchResult(Collections.emptyMap(), 0);
        when(globalSearchService.search(TENANT_TEST, "vip", 20)).thenReturn(searchResult);

        ResponseEntity<?> response = controller.search(" tenant-1 ", " vip ", 20, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(globalSearchService).search(TENANT_TEST, "vip", 20);
    }

    @Test
    void suggestionsShouldReturnBadRequestWhenTenantIdIsBlank() {
        ResponseEntity<List<String>> response = controller.suggestions("  ", "vip", 5);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(globalSearchService, savedSearchRepository);
    }

    @Test
    void suggestionsShouldReturnPrefixMatchesBeforeContainsMatchesAndDeduplicate() {
        when(savedSearchRepository.findDistinctNamesByTenantIdAndNameStartingWithIgnoreCase(
                eq(TENANT_TEST), eq("vip"), any(Pageable.class)))
                .thenReturn(Arrays.asList("vip alpha", "vip beta"));
        when(savedSearchRepository.findDistinctNamesByTenantIdAndNameContainingIgnoreCase(
                eq(TENANT_TEST), eq("vip"), any(Pageable.class)))
                .thenReturn(Arrays.asList("alpha vip", "vip beta", "vip gamma"));

        ResponseEntity<List<String>> response = controller.suggestions(TENANT_TEST, " vip ", 5);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Arrays.asList("vip alpha", "vip beta", "alpha vip", "vip gamma"), response.getBody());
        verify(savedSearchRepository).findDistinctNamesByTenantIdAndNameStartingWithIgnoreCase(
                eq(TENANT_TEST), eq("vip"), any(Pageable.class));
        verify(savedSearchRepository).findDistinctNamesByTenantIdAndNameContainingIgnoreCase(
                eq(TENANT_TEST), eq("vip"), any(Pageable.class));
    }

    @Test
    void suggestionsShouldClampLimitToMaximumPageSize() {
        List<String> names = Arrays.asList(
                "Suggestion 1", "Suggestion 2", "Suggestion 3", "Suggestion 4", "Suggestion 5",
                "Suggestion 6", "Suggestion 7", "Suggestion 8", "Suggestion 9", "Suggestion 10",
                "Suggestion 11", "Suggestion 12", "Suggestion 13", "Suggestion 14", "Suggestion 15",
                "Suggestion 16", "Suggestion 17", "Suggestion 18", "Suggestion 19", "Suggestion 20",
                "Suggestion 21", "Suggestion 22", "Suggestion 23", "Suggestion 24", "Suggestion 25");

        when(savedSearchRepository.findDistinctNamesByTenantIdAndNameStartingWithIgnoreCase(
                eq(TENANT_TEST), eq("vip"), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        when(savedSearchRepository.findDistinctNamesByTenantIdAndNameContainingIgnoreCase(
                eq(TENANT_TEST), eq("vip"), any(Pageable.class)))
                .thenReturn(names);

        ResponseEntity<List<String>> response = controller.suggestions(TENANT_TEST, "vip", 999);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(20, response.getBody().size());

        ArgumentCaptor<Pageable> prefixPageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(savedSearchRepository).findDistinctNamesByTenantIdAndNameStartingWithIgnoreCase(
                eq(TENANT_TEST), eq("vip"), prefixPageableCaptor.capture());
        assertEquals(20, prefixPageableCaptor.getValue().getPageSize());
        assertEquals(0, prefixPageableCaptor.getValue().getPageNumber());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(savedSearchRepository).findDistinctNamesByTenantIdAndNameContainingIgnoreCase(
                eq(TENANT_TEST), eq("vip"), pageableCaptor.capture());
        assertEquals(20, pageableCaptor.getValue().getPageSize());
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
    }

    @Test
    void suggestionsShouldSkipContainsQueryWhenPrefixResultsReachSafeLimit() {
        List<String> prefixNames = Arrays.asList(
                "vip alpha", "vip beta", "vip gamma", "vip delta", "vip epsilon",
                "vip zeta", "vip eta", "vip theta", "vip iota", "vip kappa",
                "vip lambda", "vip mu", "vip nu", "vip xi", "vip omicron",
                "vip pi", "vip rho", "vip sigma", "vip tau", "vip upsilon");

        when(savedSearchRepository.findDistinctNamesByTenantIdAndNameStartingWithIgnoreCase(
                eq(TENANT_TEST), eq("vip"), any(Pageable.class)))
                .thenReturn(prefixNames);

        ResponseEntity<List<String>> response = controller.suggestions(TENANT_TEST, "vip", 20);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(prefixNames, response.getBody());
        verify(savedSearchRepository).findDistinctNamesByTenantIdAndNameStartingWithIgnoreCase(
                eq(TENANT_TEST), eq("vip"), any(Pageable.class));
        verify(savedSearchRepository, never()).findDistinctNamesByTenantIdAndNameContainingIgnoreCase(
                eq(TENANT_TEST), eq("vip"), any(Pageable.class));
    }

    @Test
    void deleteSavedSearchShouldStillReturnNoContentOnNormalPath() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authUsername", "alice");

        when(savedSearchRepository.deleteByIdAndTenantIdAndOwner("saved-1", TENANT_TEST, "alice")).thenReturn(1L);

        ResponseEntity<?> response = controller.deleteSavedSearch(request, TENANT_TEST, "legacy-owner", "saved-1");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(savedSearchRepository).deleteByIdAndTenantIdAndOwner("saved-1", TENANT_TEST, "alice");
    }
}

