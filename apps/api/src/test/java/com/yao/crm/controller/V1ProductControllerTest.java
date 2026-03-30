package com.yao.crm.controller;

import com.yao.crm.dto.request.PriceBookItemRequest;
import com.yao.crm.dto.request.PriceBookPatchRequest;
import com.yao.crm.dto.request.ProductPatchRequest;
import com.yao.crm.entity.PriceBook;
import com.yao.crm.entity.PriceBookItem;
import com.yao.crm.entity.Product;
import com.yao.crm.repository.PriceBookItemRepository;
import com.yao.crm.repository.PriceBookRepository;
import com.yao.crm.repository.ProductRepository;
import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.CommerceFacadeService;
import com.yao.crm.service.I18nService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class V1ProductControllerTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private PriceBookRepository priceBookRepository;
    @Mock
    private PriceBookItemRepository priceBookItemRepository;
    @Mock
    private CommerceFacadeService commerceFacadeService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private I18nService i18nService;

    private V1ProductController controller;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        controller = new V1ProductController(
                productRepository,
                priceBookRepository,
                priceBookItemRepository,
                commerceFacadeService,
                auditLogService,
                i18nService
        );
        when(i18nService.msg(any(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
        request = new MockHttpServletRequest();
        request.setAttribute("authRole", "MANAGER");
        request.setAttribute("authUsername", "manager-1");
        request.setAttribute("authTenantId", "tenant-1");
    }

    @Test
    @SuppressWarnings("unchecked")
    void patchProductShouldTrimIdForLookupAndSelfConflictCheck() {
        Product existing = new Product();
        existing.setId("prd-1");
        existing.setTenantId("tenant-1");
        existing.setCode("OLD-CODE");
        existing.setName("legacy");
        when(productRepository.findByIdAndTenantId("prd-1", "tenant-1")).thenReturn(Optional.of(existing));
        when(productRepository.findByTenantIdAndCode("tenant-1", "NEW-CODE")).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductPatchRequest payload = new ProductPatchRequest();
        payload.setId("  prd-1  ");
        payload.setCode("  new-code  ");
        payload.setName("  New Name  ");

        ResponseEntity<?> response = controller.patchProduct(request, payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(productRepository).findByIdAndTenantId("prd-1", "tenant-1");
        verify(productRepository).findByTenantIdAndCode("tenant-1", "NEW-CODE");
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertEquals("NEW-CODE", captor.getValue().getCode());
        assertEquals("New Name", captor.getValue().getName());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("product_updated", body.get("code"));
    }

    @Test
    void patchPriceBookShouldTrimIdBeforeTenantScopedLookup() {
        PriceBook existing = new PriceBook();
        existing.setId("pb-1");
        existing.setTenantId("tenant-1");
        existing.setName("legacy");
        when(priceBookRepository.findByIdAndTenantId("pb-1", "tenant-1")).thenReturn(Optional.of(existing));
        when(priceBookRepository.save(any(PriceBook.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PriceBookPatchRequest payload = new PriceBookPatchRequest();
        payload.setId("  pb-1  ");
        payload.setName("  Main PB  ");

        ResponseEntity<?> response = controller.patchPriceBook(request, payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(priceBookRepository).findByIdAndTenantId("pb-1", "tenant-1");
        ArgumentCaptor<PriceBook> captor = ArgumentCaptor.forClass(PriceBook.class);
        verify(priceBookRepository).save(captor.capture());
        assertEquals("Main PB", captor.getValue().getName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void listPriceBookItemsShouldTrimIdBeforeLookup() {
        when(priceBookRepository.findByIdAndTenantId("pb-1", "tenant-1")).thenReturn(Optional.of(new PriceBook()));
        when(priceBookItemRepository.findByTenantIdAndPriceBookIdOrderByUpdatedAtDesc("tenant-1", "pb-1"))
                .thenReturn(Collections.<PriceBookItem>emptyList());

        ResponseEntity<?> response = controller.listPriceBookItems(request, "  pb-1  ");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(priceBookRepository).findByIdAndTenantId("pb-1", "tenant-1");
        verify(priceBookItemRepository).findByTenantIdAndPriceBookIdOrderByUpdatedAtDesc("tenant-1", "pb-1");
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("price_book_items_listed", body.get("code"));
        assertEquals(0, body.get("total"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void upsertPriceBookItemShouldNormalizeIdsBeforeLookupAndResponse() {
        when(priceBookRepository.findByIdAndTenantId("pb-1", "tenant-1")).thenReturn(Optional.of(new PriceBook()));
        Product product = new Product();
        product.setId("prd-1");
        product.setTenantId("tenant-1");
        when(productRepository.findByIdAndTenantId("prd-1", "tenant-1")).thenReturn(Optional.of(product));
        when(priceBookItemRepository.findByTenantIdAndPriceBookIdAndProductId("tenant-1", "pb-1", "prd-1"))
                .thenReturn(Optional.empty());
        when(priceBookItemRepository.save(any(PriceBookItem.class))).thenAnswer(invocation -> {
            PriceBookItem saved = invocation.getArgument(0);
            saved.setId("pbi-1");
            return saved;
        });

        PriceBookItemRequest payload = new PriceBookItemRequest();
        payload.setProductId("  prd-1  ");
        payload.setPrice(299L);
        payload.setCurrency(" cny ");

        ResponseEntity<?> response = controller.upsertPriceBookItem(request, "  pb-1  ", payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(priceBookRepository).findByIdAndTenantId("pb-1", "tenant-1");
        verify(productRepository).findByIdAndTenantId("prd-1", "tenant-1");
        verify(priceBookItemRepository).findByTenantIdAndPriceBookIdAndProductId("tenant-1", "pb-1", "prd-1");
        ArgumentCaptor<PriceBookItem> itemCaptor = ArgumentCaptor.forClass(PriceBookItem.class);
        verify(priceBookItemRepository).save(itemCaptor.capture());
        assertEquals("pb-1", itemCaptor.getValue().getPriceBookId());
        assertEquals("prd-1", itemCaptor.getValue().getProductId());
        assertEquals("CNY", itemCaptor.getValue().getCurrency());

        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("price_book_item_saved", body.get("code"));
        assertEquals("pb-1", body.get("priceBookId"));
        assertEquals("prd-1", body.get("productId"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void upsertPriceBookItemShouldReturnBadRequestWhenProductIdBlankAfterNormalization() {
        when(priceBookRepository.findByIdAndTenantId("pb-1", "tenant-1")).thenReturn(Optional.of(new PriceBook()));

        PriceBookItemRequest payload = new PriceBookItemRequest();
        payload.setProductId("   ");

        ResponseEntity<?> response = controller.upsertPriceBookItem(request, "pb-1", payload);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("product_id_required", body.get("code"));
        verify(productRepository, never()).findByIdAndTenantId(anyString(), anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void listPriceBookItemsShouldReturnBadRequestWhenIdBlankAfterNormalization() {
        ResponseEntity<?> response = controller.listPriceBookItems(request, "   ");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("id_required", body.get("code"));
        verifyNoInteractions(priceBookRepository, priceBookItemRepository);
    }
}
