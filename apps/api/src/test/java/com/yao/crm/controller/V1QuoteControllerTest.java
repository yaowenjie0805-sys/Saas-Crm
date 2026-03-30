package com.yao.crm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yao.crm.dto.request.QuoteCreateRequest;
import com.yao.crm.dto.request.QuotePatchRequest;
import com.yao.crm.entity.Customer;
import com.yao.crm.entity.Opportunity;
import com.yao.crm.entity.Quote;
import com.yao.crm.entity.QuoteItem;
import com.yao.crm.repository.*;
import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.CommerceFacadeService;
import com.yao.crm.service.I18nService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class V1QuoteControllerTest {

    @Mock
    private QuoteRepository quoteRepository;
    @Mock
    private QuoteItemRepository quoteItemRepository;
    @Mock
    private QuoteVersionRepository quoteVersionRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private OpportunityRepository opportunityRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private OrderRecordRepository orderRecordRepository;
    @Mock
    private ApprovalTemplateRepository approvalTemplateRepository;
    @Mock
    private ApprovalInstanceRepository approvalInstanceRepository;
    @Mock
    private ApprovalTaskRepository approvalTaskRepository;
    @Mock
    private ApprovalEventRepository approvalEventRepository;
    @Mock
    private CommerceFacadeService commerceFacadeService;
    @Mock
    private AuditLogService auditLogService;

    private V1QuoteController controller;

    @BeforeEach
    void setUp() {
        controller = new V1QuoteController(
                quoteRepository,
                quoteItemRepository,
                quoteVersionRepository,
                customerRepository,
                opportunityRepository,
                productRepository,
                orderRecordRepository,
                approvalTemplateRepository,
                approvalInstanceRepository,
                approvalTaskRepository,
                approvalEventRepository,
                commerceFacadeService,
                auditLogService,
                new ObjectMapper(),
                new I18nService()
        );
    }

    @Test
    void listQuotesShouldNormalizeOpportunityIdBeforeDelegating() {
        MockHttpServletRequest request = authedRequest("ANALYST");
        when(commerceFacadeService.normalizeStatusOrBlank(eq(" draft "), anySet())).thenReturn("DRAFT");
        when(commerceFacadeService.normalizePageSize(10)).thenReturn(10);
        Page<Quote> page = new PageImpl<Quote>(Collections.emptyList());
        when(commerceFacadeService.findQuotes(eq("tenant-1"), eq("DRAFT"), eq("opp-1"), eq(null), any(Pageable.class)))
                .thenReturn(page);

        ResponseEntity<?> response = controller.listQuotes(request, " draft ", "  opp-1  ", 1, 10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(commerceFacadeService).findQuotes(eq("tenant-1"), eq("DRAFT"), eq("opp-1"), eq(null), any(Pageable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void patchQuoteShouldNormalizePayloadBeforePersistence() {
        MockHttpServletRequest request = authedRequest("MANAGER");
        Quote existing = new Quote();
        existing.setId("qt-1");
        existing.setTenantId("tenant-1");
        existing.setStatus("DRAFT");
        existing.setOwner("legacy-owner");
        when(quoteRepository.findByIdAndTenantId("qt-1", "tenant-1")).thenReturn(Optional.of(existing));

        Customer customer = new Customer();
        customer.setId("c-1");
        customer.setTenantId("tenant-1");
        when(customerRepository.findById("c-1")).thenReturn(Optional.of(customer));

        Opportunity opportunity = new Opportunity();
        opportunity.setId("o-1");
        opportunity.setTenantId("tenant-1");
        when(opportunityRepository.findById("o-1")).thenReturn(Optional.of(opportunity));

        when(quoteItemRepository.findByTenantIdAndQuoteIdOrderByCreatedAtAsc("tenant-1", "qt-1"))
                .thenReturn(Collections.<QuoteItem>emptyList());
        when(quoteRepository.save(any(Quote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QuotePatchRequest payload = new QuotePatchRequest();
        payload.setId("  qt-1  ");
        payload.setCustomerId("  c-1  ");
        payload.setOpportunityId("  o-1  ");
        payload.setOwner("  seller  ");
        payload.setStatus("  approved  ");

        ResponseEntity<?> response = controller.patchQuote(request, payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(quoteRepository).findByIdAndTenantId("qt-1", "tenant-1");
        verify(customerRepository).findById("c-1");
        verify(opportunityRepository).findById("o-1");

        ArgumentCaptor<Quote> savedCaptor = ArgumentCaptor.forClass(Quote.class);
        verify(quoteRepository, atLeastOnce()).save(savedCaptor.capture());
        Quote saved = savedCaptor.getAllValues().get(0);
        assertEquals("c-1", saved.getCustomerId());
        assertEquals("o-1", saved.getOpportunityId());
        assertEquals("seller", saved.getOwner());
        assertEquals("APPROVED", saved.getStatus());

        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("quote_updated", body.get("code"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void createQuoteShouldReturnForbiddenWhenNormalizedOwnerOutsideScope() {
        MockHttpServletRequest request = authedRequest("SALES");
        request.setAttribute("authUsername", "alice");
        request.setAttribute("authOwnerScope", "alice");

        Customer customer = new Customer();
        customer.setId("c-1");
        customer.setTenantId("tenant-1");
        when(customerRepository.findById("c-1")).thenReturn(Optional.of(customer));

        QuoteCreateRequest payload = new QuoteCreateRequest();
        payload.setCustomerId("  c-1  ");
        payload.setOwner("  bob  ");

        ResponseEntity<?> response = controller.createQuote(request, payload);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("scope_forbidden", body.get("code"));
        verify(quoteRepository, never()).save(any(Quote.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void listQuoteItemsShouldNormalizeIdForLookupAndResponseContract() {
        MockHttpServletRequest request = authedRequest("ANALYST");
        Quote quote = new Quote();
        quote.setId("qt-1");
        quote.setTenantId("tenant-1");
        quote.setOwner("owner-1");
        when(quoteRepository.findByIdAndTenantId("qt-1", "tenant-1")).thenReturn(Optional.of(quote));
        when(quoteItemRepository.findByTenantIdAndQuoteIdOrderByCreatedAtAsc("tenant-1", "qt-1"))
                .thenReturn(Collections.<QuoteItem>emptyList());

        ResponseEntity<?> response = controller.listQuoteItems(request, "  qt-1  ");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(quoteRepository).findByIdAndTenantId("qt-1", "tenant-1");
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("qt-1", body.get("quoteId"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void listQuoteItemsShouldReturnBadRequestWhenIdBlankAfterNormalization() {
        MockHttpServletRequest request = authedRequest("ANALYST");

        ResponseEntity<?> response = controller.listQuoteItems(request, "   ");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("id_required", body.get("code"));
        verify(quoteRepository, never()).findByIdAndTenantId(any(), any());
    }

    private MockHttpServletRequest authedRequest(String role) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authRole", role);
        request.setAttribute("authUsername", "tester");
        request.setAttribute("authTenantId", "tenant-1");
        return request;
    }
}
