package com.yao.crm.controller;

import com.yao.crm.dto.request.PriceBookCreateRequest;
import com.yao.crm.dto.request.PriceBookItemRequest;
import com.yao.crm.dto.request.PriceBookPatchRequest;
import com.yao.crm.dto.request.ProductCreateRequest;
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
import com.yao.crm.util.IdGenerator;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.*;

@Tag(name = "Products", description = "Products and price books")
@RestController
@RequestMapping("/api/v1")
@Validated
public class V1ProductController extends CommerceControllerSupport {

    private static final Set<String> PRODUCT_STATUSES = Set.of("ACTIVE", "INACTIVE");
    private static final Set<String> PRICE_BOOK_STATUSES = Set.of("ACTIVE", "INACTIVE");

    private final ProductRepository productRepository;
    private final PriceBookRepository priceBookRepository;
    private final PriceBookItemRepository priceBookItemRepository;
    private final CommerceFacadeService commerceFacadeService;
    private final AuditLogService auditLogService;
    private final I18nService i18nService;
    private final IdGenerator idGenerator;

    public V1ProductController(ProductRepository productRepository,
                                PriceBookRepository priceBookRepository,
                                PriceBookItemRepository priceBookItemRepository,
                                CommerceFacadeService commerceFacadeService,
                                AuditLogService auditLogService,
                                I18nService i18nService,
                                IdGenerator idGenerator) {
        super(i18nService);
        this.productRepository = productRepository;
        this.priceBookRepository = priceBookRepository;
        this.priceBookItemRepository = priceBookItemRepository;
        this.commerceFacadeService = commerceFacadeService;
        this.auditLogService = auditLogService;
        this.i18nService = i18nService;
        this.idGenerator = idGenerator;
    }

    // ========== Products ==========

    @GetMapping("/products")
    public ResponseEntity<?> listProducts(HttpServletRequest request,
                                          @RequestParam(defaultValue = "") String status,
                                          @RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int size) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        int normalizedSize = commerceFacadeService.normalizePageSize(size);
        String normalizedStatus = commerceFacadeService.normalizeStatusOrBlank(status, PRODUCT_STATUSES);
        if (normalizedStatus == null) {
            return ResponseEntity.badRequest().body(errorBody(request, "product_status_invalid", msg(request, "product_status_invalid"), null));
        }
        Pageable pageable = buildPageable(page, normalizedSize, "updatedAt", "desc",
                Set.of("createdAt", "updatedAt", "name", "code"), "updatedAt");
        Page<Product> result = isBlank(normalizedStatus)
                ? productRepository.findByTenantId(tenantId, pageable)
                : productRepository.findByTenantIdAndStatus(tenantId, normalizedStatus, pageable);
        return ResponseEntity.ok(successWithFields(request, "products_listed", pageBody(result, page, normalizedSize)));
    }

    @PostMapping("/products")
    public ResponseEntity<?> createProduct(HttpServletRequest request, @Valid @RequestBody ProductCreateRequest payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        Product product = new Product();
        product.setId(newId("prd"));
        product.setTenantId(tenantId);
        String validate = applyProductPayload(product, payload, true);
        if (!isBlank(validate)) {
            return ResponseEntity.badRequest().body(errorBody(request, validate, msg(request, validate), null));
        }
        if (productRepository.findByTenantIdAndCode(tenantId, product.getCode()).isPresent()) {
            return ResponseEntity.status(409).body(errorBody(request, "product_code_exists", msg(request, "product_code_exists"), null));
        }
        Product saved = productRepository.save(product);
        auditLogService.record(currentUser(request), currentRole(request), "CREATE", "PRODUCT", saved.getId(), "Create product", tenantId);
        return ResponseEntity.status(201).body(successWithFields(request, "product_created", toProductView(saved)));
    }

    @PatchMapping("/products")
    public ResponseEntity<?> patchProduct(HttpServletRequest request, @Valid @RequestBody ProductPatchRequest payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String normalizedId = normalizeId(payload.getId());
        if (isBlank(normalizedId)) {
            return ResponseEntity.badRequest().body(errorBody(request, "id_required", msg(request, "id_required"), null));
        }
        String tenantId = currentTenant(request);
        Optional<Product> optional = productRepository.findByIdAndTenantId(normalizedId, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "product_not_found", msg(request, "product_not_found"), null));
        }
        Product product = optional.get();
        String beforeCode = product.getCode();
        String validate = applyProductPayload(product, payload, false);
        if (!isBlank(validate)) {
            return ResponseEntity.badRequest().body(errorBody(request, validate, msg(request, validate), null));
        }
        if (!beforeCode.equalsIgnoreCase(product.getCode())) {
            Optional<Product> conflict = productRepository.findByTenantIdAndCode(tenantId, product.getCode());
            if (conflict.isPresent() && !normalizedId.equals(conflict.get().getId())) {
                return ResponseEntity.status(409).body(errorBody(request, "product_code_exists", msg(request, "product_code_exists"), null));
            }
        }
        Product saved = productRepository.save(product);
        auditLogService.record(currentUser(request), currentRole(request), "UPDATE", "PRODUCT", saved.getId(), "Update product", tenantId);
        return ResponseEntity.ok(successWithFields(request, "product_updated", toProductView(saved)));
    }

    // ========== Price Books ==========

    @GetMapping("/price-books")
    public ResponseEntity<?> listPriceBooks(HttpServletRequest request,
                                            @RequestParam(defaultValue = "") String status,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        int normalizedSize = commerceFacadeService.normalizePageSize(size);
        String normalizedStatus = commerceFacadeService.normalizeStatusOrBlank(status, PRICE_BOOK_STATUSES);
        if (normalizedStatus == null) {
            return ResponseEntity.badRequest().body(errorBody(request, "price_book_status_invalid", msg(request, "price_book_status_invalid"), null));
        }
        Pageable pageable = buildPageable(page, normalizedSize, "updatedAt", "desc",
                Set.of("createdAt", "updatedAt", "name", "status"), "updatedAt");
        Page<PriceBook> result = commerceFacadeService.findPriceBooks(tenantId, normalizedStatus, pageable);
        return ResponseEntity.ok(successWithFields(request, "price_books_listed", pageBody(result, page, normalizedSize)));
    }

    @PostMapping("/price-books")
    public ResponseEntity<?> createPriceBook(HttpServletRequest request, @Valid @RequestBody PriceBookCreateRequest payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        PriceBook row = new PriceBook();
        row.setId(newId("pb"));
        row.setTenantId(tenantId);
        String validate = applyPriceBookPayload(row, payload, true);
        if (!isBlank(validate)) {
            return ResponseEntity.badRequest().body(errorBody(request, validate, msg(request, validate), null));
        }
        if (Boolean.TRUE.equals(row.getDefault())) {
            resetDefaultPriceBook(tenantId);
        }
        PriceBook saved = priceBookRepository.save(row);
        auditLogService.record(currentUser(request), currentRole(request), "CREATE", "PRICE_BOOK", saved.getId(), "Create price book", tenantId);
        return ResponseEntity.status(201).body(successWithFields(request, "price_book_created", toPriceBookView(saved)));
    }

    @PatchMapping("/price-books")
    public ResponseEntity<?> patchPriceBook(HttpServletRequest request, @Valid @RequestBody PriceBookPatchRequest payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String normalizedId = normalizeId(payload.getId());
        if (isBlank(normalizedId)) {
            return ResponseEntity.badRequest().body(errorBody(request, "id_required", msg(request, "id_required"), null));
        }
        String tenantId = currentTenant(request);
        Optional<PriceBook> optional = priceBookRepository.findByIdAndTenantId(normalizedId, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "price_book_not_found", msg(request, "price_book_not_found"), null));
        }
        PriceBook row = optional.get();
        String validate = applyPriceBookPayload(row, payload, false);
        if (!isBlank(validate)) {
            return ResponseEntity.badRequest().body(errorBody(request, validate, msg(request, validate), null));
        }
        if (Boolean.TRUE.equals(row.getDefault())) {
            resetDefaultPriceBook(tenantId);
        }
        PriceBook saved = priceBookRepository.save(row);
        auditLogService.record(currentUser(request), currentRole(request), "UPDATE", "PRICE_BOOK", saved.getId(), "Update price book", tenantId);
        return ResponseEntity.ok(successWithFields(request, "price_book_updated", toPriceBookView(saved)));
    }

    @GetMapping("/price-books/{id}/items")
    public ResponseEntity<?> listPriceBookItems(HttpServletRequest request, @PathVariable @NotBlank String id) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String normalizedId = normalizeId(id);
        if (isBlank(normalizedId)) {
            return ResponseEntity.badRequest().body(errorBody(request, "id_required", msg(request, "id_required"), null));
        }
        String tenantId = currentTenant(request);
        if (!priceBookRepository.findByIdAndTenantId(normalizedId, tenantId).isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "price_book_not_found", msg(request, "price_book_not_found"), null));
        }
        List<PriceBookItem> items = priceBookItemRepository.findByTenantIdAndPriceBookIdOrderByUpdatedAtDesc(tenantId, normalizedId);
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (PriceBookItem item : items) out.add(toPriceBookItemView(item));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", out);
        body.put("total", out.size());
        return ResponseEntity.ok(successWithFields(request, "price_book_items_listed", body));
    }

    @PostMapping("/price-books/{id}/items")
    public ResponseEntity<?> upsertPriceBookItem(HttpServletRequest request, @PathVariable @NotBlank String id, @Valid @RequestBody PriceBookItemRequest payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String normalizedPriceBookId = normalizeId(id);
        if (isBlank(normalizedPriceBookId)) {
            return ResponseEntity.badRequest().body(errorBody(request, "id_required", msg(request, "id_required"), null));
        }
        String tenantId = currentTenant(request);
        if (!priceBookRepository.findByIdAndTenantId(normalizedPriceBookId, tenantId).isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "price_book_not_found", msg(request, "price_book_not_found"), null));
        }
        String productId = normalizeId(payload.getProductId());
        if (isBlank(productId)) {
            return ResponseEntity.badRequest().body(errorBody(request, "product_id_required", msg(request, "product_id_required"), null));
        }
        if (!productRepository.findByIdAndTenantId(productId, tenantId).isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "product_not_found", msg(request, "product_not_found"), null));
        }
        PriceBookItem item = priceBookItemRepository.findByTenantIdAndPriceBookIdAndProductId(tenantId, normalizedPriceBookId, productId).orElseGet(PriceBookItem::new);
        if (isBlank(item.getId())) item.setId(newId("pbi"));
        item.setTenantId(tenantId);
        item.setPriceBookId(normalizedPriceBookId);
        item.setProductId(productId);
        item.setPrice(payload.getPrice() != null ? payload.getPrice() : 0L);
        item.setTaxRate(payload.getTaxRate() != null ? payload.getTaxRate() : 0.0);
        item.setCurrency(upperOrDefault(payload.getCurrency(), "CNY"));
        PriceBookItem saved = priceBookItemRepository.save(item);
        auditLogService.record(currentUser(request), currentRole(request), "UPSERT", "PRICE_BOOK_ITEM", saved.getId(), "Upsert price book item", tenantId);
        return ResponseEntity.ok(successWithFields(request, "price_book_item_saved", toPriceBookItemView(saved)));
    }

    // ========== Private Helper Methods ==========

    private void resetDefaultPriceBook(String tenantId) {
        List<PriceBook> all = priceBookRepository.findByTenantIdOrderByUpdatedAtDesc(tenantId);
        List<PriceBook> toUpdate = new ArrayList<>();
        for (PriceBook row : all) {
            if (Boolean.TRUE.equals(row.getDefault())) {
                row.setDefault(false);
                toUpdate.add(row);
            }
        }
        if (!toUpdate.isEmpty()) {
            priceBookRepository.saveAll(toUpdate);
        }
    }

    private String applyProductPayload(Product row, ProductCreateRequest payload, boolean creating) {
        String code = payload.getCode();
        String name = payload.getName();
        if (creating && (isBlank(code) || isBlank(name))) return "product_code_name_required";
        if (!isBlank(code)) row.setCode(code.trim().toUpperCase(Locale.ROOT));
        if (!isBlank(name)) row.setName(name.trim());
        if (payload.getCategory() != null) row.setCategory(payload.getCategory());
        String status = upperOrDefault(payload.getStatus(), row.getStatus());
        if (!isBlank(status)) {
            if (!PRODUCT_STATUSES.contains(status)) return "product_status_invalid";
            row.setStatus(status);
        }
        if (payload.getStandardPrice() != null) row.setStandardPrice(Math.max(0L, payload.getStandardPrice()));
        if (payload.getTaxRate() != null) row.setTaxRate(Math.max(0.0, payload.getTaxRate()));
        if (payload.getCurrency() != null) row.setCurrency(upperOrDefault(payload.getCurrency(), "CNY"));
        if (payload.getUnit() != null) row.setUnit(payload.getUnit());
        return "";
    }

    private String applyProductPayload(Product row, ProductPatchRequest payload, boolean creating) {
        String code = payload.getCode();
        String name = payload.getName();
        if (creating && (isBlank(code) || isBlank(name))) return "product_code_name_required";
        if (!isBlank(code)) row.setCode(code.trim().toUpperCase(Locale.ROOT));
        if (!isBlank(name)) row.setName(name.trim());
        if (payload.getCategory() != null) row.setCategory(payload.getCategory());
        String status = upperOrDefault(payload.getStatus(), row.getStatus());
        if (!isBlank(status)) {
            if (!PRODUCT_STATUSES.contains(status)) return "product_status_invalid";
            row.setStatus(status);
        }
        if (payload.getStandardPrice() != null) row.setStandardPrice(Math.max(0L, payload.getStandardPrice()));
        if (payload.getTaxRate() != null) row.setTaxRate(Math.max(0.0, payload.getTaxRate()));
        if (payload.getCurrency() != null) row.setCurrency(upperOrDefault(payload.getCurrency(), "CNY"));
        if (payload.getUnit() != null) row.setUnit(payload.getUnit());
        return "";
    }

    private String applyPriceBookPayload(PriceBook row, PriceBookCreateRequest payload, boolean creating) {
        String name = payload.getName();
        if (creating && isBlank(name)) return "price_book_name_required";
        if (!isBlank(name)) row.setName(name.trim());
        if (payload.getStatus() != null) {
            String status = upperOrDefault(payload.getStatus(), "ACTIVE");
            if (!PRICE_BOOK_STATUSES.contains(status)) return "price_book_status_invalid";
            row.setStatus(status);
        }
        if (payload.getIsDefault() != null) row.setDefault(payload.getIsDefault());
        if (payload.getCurrency() != null) row.setCurrency(upperOrDefault(payload.getCurrency(), "CNY"));
        return "";
    }

    private String applyPriceBookPayload(PriceBook row, PriceBookPatchRequest payload, boolean creating) {
        String name = payload.getName();
        if (creating && isBlank(name)) return "price_book_name_required";
        if (!isBlank(name)) row.setName(name.trim());
        if (payload.getStatus() != null) {
            String status = upperOrDefault(payload.getStatus(), "ACTIVE");
            if (!PRICE_BOOK_STATUSES.contains(status)) return "price_book_status_invalid";
            row.setStatus(status);
        }
        if (payload.getIsDefault() != null) row.setDefault(payload.getIsDefault());
        if (payload.getCurrency() != null) row.setCurrency(upperOrDefault(payload.getCurrency(), "CNY"));
        return "";
    }

    private Map<String, Object> toProductView(Product row) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", row.getId());
        out.put("code", row.getCode());
        out.put("name", row.getName());
        out.put("category", row.getCategory());
        out.put("status", row.getStatus());
        out.put("standardPrice", row.getStandardPrice());
        out.put("taxRate", row.getTaxRate());
        out.put("currency", row.getCurrency());
        out.put("unit", row.getUnit());
        out.put("saleRegion", row.getSaleRegion());
        out.put("validFrom", row.getValidFrom());
        out.put("validTo", row.getValidTo());
        out.put("tenantId", row.getTenantId());
        out.put("updatedAt", row.getUpdatedAt());
        return out;
    }

    private Map<String, Object> toPriceBookView(PriceBook row) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", row.getId());
        out.put("name", row.getName());
        out.put("status", row.getStatus());
        out.put("isDefault", row.getDefault());
        out.put("department", row.getDepartment());
        out.put("currency", row.getCurrency());
        out.put("validFrom", row.getValidFrom());
        out.put("validTo", row.getValidTo());
        out.put("tenantId", row.getTenantId());
        out.put("updatedAt", row.getUpdatedAt());
        return out;
    }

    private Map<String, Object> toPriceBookItemView(PriceBookItem row) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", row.getId());
        out.put("priceBookId", row.getPriceBookId());
        out.put("productId", row.getProductId());
        out.put("price", row.getPrice());
        out.put("taxRate", row.getTaxRate());
        out.put("currency", row.getCurrency());
        out.put("updatedAt", row.getUpdatedAt());
        return out;
    }

    private String normalizeId(String value) {
        return isBlank(value) ? "" : value.trim();
    }
}
