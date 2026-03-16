package com.yao.crm.controller;

import com.yao.crm.entity.ContractRecord;
import com.yao.crm.entity.Customer;
import com.yao.crm.entity.Opportunity;
import com.yao.crm.entity.OrderRecord;
import com.yao.crm.entity.ApprovalEvent;
import com.yao.crm.entity.ApprovalInstance;
import com.yao.crm.entity.ApprovalTask;
import com.yao.crm.entity.ApprovalTemplate;
import com.yao.crm.entity.PriceBook;
import com.yao.crm.entity.PriceBookItem;
import com.yao.crm.entity.Product;
import com.yao.crm.entity.Quote;
import com.yao.crm.entity.QuoteItem;
import com.yao.crm.entity.QuoteVersion;
import com.yao.crm.entity.Tenant;
import com.yao.crm.repository.ContractRecordRepository;
import com.yao.crm.repository.CustomerRepository;
import com.yao.crm.repository.OpportunityRepository;
import com.yao.crm.repository.ApprovalEventRepository;
import com.yao.crm.repository.ApprovalInstanceRepository;
import com.yao.crm.repository.ApprovalTaskRepository;
import com.yao.crm.repository.ApprovalTemplateRepository;
import com.yao.crm.repository.OrderRecordRepository;
import com.yao.crm.repository.PriceBookItemRepository;
import com.yao.crm.repository.PriceBookRepository;
import com.yao.crm.repository.ProductRepository;
import com.yao.crm.repository.QuoteItemRepository;
import com.yao.crm.repository.QuoteRepository;
import com.yao.crm.repository.QuoteVersionRepository;
import com.yao.crm.repository.TenantRepository;
import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.I18nService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1")
public class V1CommerceController extends BaseApiController {

    private static final Set<String> PRODUCT_STATUSES = new HashSet<String>(Arrays.asList("ACTIVE", "INACTIVE"));
    private static final Set<String> PRICE_BOOK_STATUSES = new HashSet<String>(Arrays.asList("ACTIVE", "INACTIVE"));
    private static final Set<String> QUOTE_STATUSES = new HashSet<String>(Arrays.asList(
            "DRAFT", "SUBMITTED", "APPROVED", "REJECTED", "ACCEPTED", "EXPIRED", "CANCELED"
    ));
    private static final Set<String> ORDER_STATUSES = new HashSet<String>(Arrays.asList(
            "DRAFT", "CONFIRMED", "FULFILLING", "COMPLETED", "CANCELED"
    ));

    private final ProductRepository productRepository;
    private final PriceBookRepository priceBookRepository;
    private final PriceBookItemRepository priceBookItemRepository;
    private final QuoteRepository quoteRepository;
    private final QuoteItemRepository quoteItemRepository;
    private final QuoteVersionRepository quoteVersionRepository;
    private final OrderRecordRepository orderRecordRepository;
    private final CustomerRepository customerRepository;
    private final OpportunityRepository opportunityRepository;
    private final ContractRecordRepository contractRecordRepository;
    private final ApprovalTemplateRepository approvalTemplateRepository;
    private final ApprovalInstanceRepository approvalInstanceRepository;
    private final ApprovalTaskRepository approvalTaskRepository;
    private final ApprovalEventRepository approvalEventRepository;
    private final TenantRepository tenantRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public V1CommerceController(ProductRepository productRepository,
                                PriceBookRepository priceBookRepository,
                                PriceBookItemRepository priceBookItemRepository,
                                QuoteRepository quoteRepository,
                                QuoteItemRepository quoteItemRepository,
                                QuoteVersionRepository quoteVersionRepository,
                                OrderRecordRepository orderRecordRepository,
                                CustomerRepository customerRepository,
                                OpportunityRepository opportunityRepository,
                                ContractRecordRepository contractRecordRepository,
                                ApprovalTemplateRepository approvalTemplateRepository,
                                ApprovalInstanceRepository approvalInstanceRepository,
                                ApprovalTaskRepository approvalTaskRepository,
                                ApprovalEventRepository approvalEventRepository,
                                TenantRepository tenantRepository,
                                AuditLogService auditLogService,
                                I18nService i18nService) {
        super(i18nService);
        this.productRepository = productRepository;
        this.priceBookRepository = priceBookRepository;
        this.priceBookItemRepository = priceBookItemRepository;
        this.quoteRepository = quoteRepository;
        this.quoteItemRepository = quoteItemRepository;
        this.quoteVersionRepository = quoteVersionRepository;
        this.orderRecordRepository = orderRecordRepository;
        this.customerRepository = customerRepository;
        this.opportunityRepository = opportunityRepository;
        this.contractRecordRepository = contractRecordRepository;
        this.approvalTemplateRepository = approvalTemplateRepository;
        this.approvalInstanceRepository = approvalInstanceRepository;
        this.approvalTaskRepository = approvalTaskRepository;
        this.approvalEventRepository = approvalEventRepository;
        this.tenantRepository = tenantRepository;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/products")
    public ResponseEntity<?> listProducts(HttpServletRequest request,
                                          @RequestParam(defaultValue = "") String status,
                                          @RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int size) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        Pageable pageable = buildPageable(page, Math.min(100, size), "updatedAt", "desc",
                new HashSet<String>(Arrays.asList("createdAt", "updatedAt", "name", "code")), "updatedAt");
        Page<Product> result = isBlank(status)
                ? productRepository.findByTenantId(tenantId, pageable)
                : productRepository.findByTenantIdAndStatus(tenantId, status.trim().toUpperCase(Locale.ROOT), pageable);
        return ResponseEntity.ok(successWithFields(request, "products_listed", pageBody(result, page, size)));
    }

    @PostMapping("/products")
    public ResponseEntity<?> createProduct(HttpServletRequest request, @RequestBody Map<String, Object> payload) {
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
    public ResponseEntity<?> patchProduct(HttpServletRequest request, @RequestBody Map<String, Object> payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String id = str(payload.get("id"));
        if (isBlank(id)) {
            return ResponseEntity.badRequest().body(errorBody(request, "id_required", msg(request, "id_required"), null));
        }
        String tenantId = currentTenant(request);
        Optional<Product> optional = productRepository.findByIdAndTenantId(id, tenantId);
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
            if (conflict.isPresent() && !id.equals(conflict.get().getId())) {
                return ResponseEntity.status(409).body(errorBody(request, "product_code_exists", msg(request, "product_code_exists"), null));
            }
        }
        Product saved = productRepository.save(product);
        auditLogService.record(currentUser(request), currentRole(request), "UPDATE", "PRODUCT", saved.getId(), "Update product", tenantId);
        return ResponseEntity.ok(successWithFields(request, "product_updated", toProductView(saved)));
    }

    @GetMapping("/price-books")
    public ResponseEntity<?> listPriceBooks(HttpServletRequest request,
                                            @RequestParam(defaultValue = "") String status,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        Pageable pageable = buildPageable(page, Math.min(100, size), "updatedAt", "desc",
                new HashSet<String>(Arrays.asList("createdAt", "updatedAt", "name", "status")), "updatedAt");
        Page<PriceBook> result = priceBookRepository.findByTenantId(tenantId, pageable);
        if (!isBlank(status)) {
            String expected = status.trim().toUpperCase(Locale.ROOT);
            List<PriceBook> filtered = new ArrayList<PriceBook>();
            for (PriceBook row : result.getContent()) {
                if (expected.equalsIgnoreCase(row.getStatus())) filtered.add(row);
            }
            Map<String, Object> body = new LinkedHashMap<String, Object>();
            body.put("items", filtered);
            body.put("total", filtered.size());
            body.put("page", Math.max(1, page));
            body.put("size", Math.max(1, size));
            body.put("totalPages", 1);
            return ResponseEntity.ok(successWithFields(request, "price_books_listed", body));
        }
        return ResponseEntity.ok(successWithFields(request, "price_books_listed", pageBody(result, page, size)));
    }

    @PostMapping("/price-books")
    public ResponseEntity<?> createPriceBook(HttpServletRequest request, @RequestBody Map<String, Object> payload) {
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
    public ResponseEntity<?> patchPriceBook(HttpServletRequest request, @RequestBody Map<String, Object> payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String id = str(payload.get("id"));
        if (isBlank(id)) {
            return ResponseEntity.badRequest().body(errorBody(request, "id_required", msg(request, "id_required"), null));
        }
        String tenantId = currentTenant(request);
        Optional<PriceBook> optional = priceBookRepository.findByIdAndTenantId(id, tenantId);
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
    public ResponseEntity<?> listPriceBookItems(HttpServletRequest request, @PathVariable String id) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        if (!priceBookRepository.findByIdAndTenantId(id, tenantId).isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "price_book_not_found", msg(request, "price_book_not_found"), null));
        }
        List<PriceBookItem> items = priceBookItemRepository.findByTenantIdAndPriceBookIdOrderByUpdatedAtDesc(tenantId, id);
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (PriceBookItem item : items) out.add(toPriceBookItemView(item));
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("items", out);
        body.put("total", out.size());
        return ResponseEntity.ok(successWithFields(request, "price_book_items_listed", body));
    }

    @PostMapping("/price-books/{id}/items")
    public ResponseEntity<?> upsertPriceBookItem(HttpServletRequest request, @PathVariable String id, @RequestBody Map<String, Object> payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        if (!priceBookRepository.findByIdAndTenantId(id, tenantId).isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "price_book_not_found", msg(request, "price_book_not_found"), null));
        }
        String productId = str(payload.get("productId"));
        if (isBlank(productId)) {
            return ResponseEntity.badRequest().body(errorBody(request, "product_id_required", msg(request, "product_id_required"), null));
        }
        if (!productRepository.findByIdAndTenantId(productId, tenantId).isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "product_not_found", msg(request, "product_not_found"), null));
        }
        PriceBookItem item = priceBookItemRepository.findByTenantIdAndPriceBookIdAndProductId(tenantId, id, productId).orElseGet(PriceBookItem::new);
        if (isBlank(item.getId())) item.setId(newId("pbi"));
        item.setTenantId(tenantId);
        item.setPriceBookId(id);
        item.setProductId(productId);
        item.setPrice(readLong(payload.get("price"), 0L));
        item.setTaxRate(readDouble(payload.get("taxRate"), 0.0));
        item.setCurrency(upperOrDefault(str(payload.get("currency")), "CNY"));
        PriceBookItem saved = priceBookItemRepository.save(item);
        auditLogService.record(currentUser(request), currentRole(request), "UPSERT", "PRICE_BOOK_ITEM", saved.getId(), "Upsert price book item", tenantId);
        return ResponseEntity.ok(successWithFields(request, "price_book_item_saved", toPriceBookItemView(saved)));
    }

    @GetMapping("/quotes")
    public ResponseEntity<?> listQuotes(HttpServletRequest request,
                                        @RequestParam(defaultValue = "") String status,
                                        @RequestParam(defaultValue = "") String opportunityId,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        String normalizedStatus = isBlank(status) ? "" : status.trim().toUpperCase(Locale.ROOT);
        Pageable pageable = buildPageable(page, Math.min(100, size), "updatedAt", "desc",
                new HashSet<String>(Arrays.asList("createdAt", "updatedAt", "quoteNo", "status", "totalAmount")), "updatedAt");
        if (isSalesScoped(request)) {
            Set<String> ownerSet = new LinkedHashSet<String>();
            ownerSet.add(currentUser(request));
            ownerSet.add(currentOwnerScope(request));
            List<String> owners = new ArrayList<String>(ownerSet);
            Page<Quote> scopedSource;
            if (isBlank(opportunityId)) {
                scopedSource = isBlank(normalizedStatus)
                        ? quoteRepository.findByTenantIdAndOwnerIn(tenantId, owners, pageable)
                        : quoteRepository.findByTenantIdAndStatusAndOwnerIn(tenantId, normalizedStatus, owners, pageable);
            } else {
                scopedSource = isBlank(normalizedStatus)
                        ? quoteRepository.findByTenantIdAndOpportunityIdAndOwnerIn(tenantId, opportunityId, owners, pageable)
                        : quoteRepository.findByTenantIdAndStatusAndOpportunityIdAndOwnerIn(tenantId, normalizedStatus, opportunityId, owners, pageable);
            }
            List<Map<String, Object>> scopedItems = new ArrayList<Map<String, Object>>();
            for (Quote row : scopedSource.getContent()) {
                scopedItems.add(toQuoteView(row, false));
            }
            Map<String, Object> body = pageBody(scopedSource, page, size);
            body.put("items", scopedItems);
            return ResponseEntity.ok(successWithFields(request, "quotes_listed", body));
        }
        Page<Quote> result;
        if (isBlank(opportunityId)) {
            result = isBlank(normalizedStatus)
                    ? quoteRepository.findByTenantId(tenantId, pageable)
                    : quoteRepository.findByTenantIdAndStatus(tenantId, normalizedStatus, pageable);
        } else {
            result = isBlank(normalizedStatus)
                    ? quoteRepository.findByTenantIdAndOpportunityId(tenantId, opportunityId, pageable)
                    : quoteRepository.findByTenantIdAndStatusAndOpportunityId(tenantId, normalizedStatus, opportunityId, pageable);
        }
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (Quote row : result.getContent()) {
            items.add(toQuoteView(row, false));
        }
        Map<String, Object> body = pageBody(result, page, size);
        body.put("items", items);
        return ResponseEntity.ok(successWithFields(request, "quotes_listed", body));
    }

    @PostMapping("/quotes")
    public ResponseEntity<?> createQuote(HttpServletRequest request, @RequestBody Map<String, Object> payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        Quote row = new Quote();
        row.setId(newId("qt"));
        row.setTenantId(tenantId);
        row.setQuoteNo(generateNo("QUO"));
        String validate = applyQuotePayload(request, row, payload, true);
        if (!isBlank(validate)) {
            return ResponseEntity.badRequest().body(errorBody(request, validate, msg(request, validate), null));
        }
        Quote saved = quoteRepository.save(row);
        recomputeQuoteAmounts(saved);
        auditLogService.record(currentUser(request), currentRole(request), "CREATE", "QUOTE", saved.getId(), "Create quote", tenantId);
        return ResponseEntity.status(201).body(successWithFields(request, "quote_created", toQuoteView(saved, true)));
    }

    @PatchMapping("/quotes")
    public ResponseEntity<?> patchQuote(HttpServletRequest request, @RequestBody Map<String, Object> payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String id = str(payload.get("id"));
        if (isBlank(id)) {
            return ResponseEntity.badRequest().body(errorBody(request, "id_required", msg(request, "id_required"), null));
        }
        String tenantId = currentTenant(request);
        Optional<Quote> optional = quoteRepository.findByIdAndTenantId(id, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "quote_not_found", msg(request, "quote_not_found"), null));
        }
        Quote row = optional.get();
        if (isSalesScoped(request) && !ownerMatchesScope(request, row.getOwner())) {
            return ResponseEntity.status(403).body(errorBody(request, "scope_forbidden", msg(request, "scope_forbidden"), null));
        }
        if (!"DRAFT".equalsIgnoreCase(row.getStatus())) {
            return ResponseEntity.status(409).body(errorBody(request, "quote_only_draft_editable", msg(request, "quote_only_draft_editable"), null));
        }
        String validate = applyQuotePayload(request, row, payload, false);
        if (!isBlank(validate)) {
            return ResponseEntity.badRequest().body(errorBody(request, validate, msg(request, validate), null));
        }
        Quote saved = quoteRepository.save(row);
        recomputeQuoteAmounts(saved);
        auditLogService.record(currentUser(request), currentRole(request), "UPDATE", "QUOTE", saved.getId(), "Update quote", tenantId);
        return ResponseEntity.ok(successWithFields(request, "quote_updated", toQuoteView(saved, true)));
    }

    @GetMapping("/quotes/{id}/items")
    public ResponseEntity<?> listQuoteItems(HttpServletRequest request, @PathVariable String id) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        Optional<Quote> quoteOptional = quoteRepository.findByIdAndTenantId(id, tenantId);
        if (!quoteOptional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "quote_not_found", msg(request, "quote_not_found"), null));
        }
        Quote quote = quoteOptional.get();
        if (isSalesScoped(request) && !ownerMatchesScope(request, quote.getOwner())) {
            return ResponseEntity.status(403).body(errorBody(request, "scope_forbidden", msg(request, "scope_forbidden"), null));
        }
        List<QuoteItem> items = quoteItemRepository.findByTenantIdAndQuoteIdOrderByCreatedAtAsc(tenantId, id);
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (QuoteItem item : items) out.add(toQuoteItemView(item));
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("quoteId", id);
        body.put("items", out);
        body.put("total", out.size());
        return ResponseEntity.ok(successWithFields(request, "quote_items_listed", body));
    }

    @PostMapping("/quotes/{id}/items")
    public ResponseEntity<?> replaceQuoteItems(HttpServletRequest request, @PathVariable String id, @RequestBody List<Map<String, Object>> payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        Optional<Quote> quoteOptional = quoteRepository.findByIdAndTenantId(id, tenantId);
        if (!quoteOptional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "quote_not_found", msg(request, "quote_not_found"), null));
        }
        Quote quote = quoteOptional.get();
        if (isSalesScoped(request) && !ownerMatchesScope(request, quote.getOwner())) {
            return ResponseEntity.status(403).body(errorBody(request, "scope_forbidden", msg(request, "scope_forbidden"), null));
        }
        if (!"DRAFT".equalsIgnoreCase(quote.getStatus())) {
            return ResponseEntity.status(409).body(errorBody(request, "quote_only_draft_editable", msg(request, "quote_only_draft_editable"), null));
        }
        quoteItemRepository.deleteByTenantIdAndQuoteId(tenantId, id);
        List<Map<String, Object>> savedItems = new ArrayList<Map<String, Object>>();
        if (payload != null) {
            for (Map<String, Object> itemPayload : payload) {
                String productId = str(itemPayload.get("productId"));
                if (isBlank(productId)) continue;
                Optional<Product> productOptional = productRepository.findByIdAndTenantId(productId, tenantId);
                if (!productOptional.isPresent()) continue;
                Product product = productOptional.get();
                QuoteItem item = new QuoteItem();
                item.setId(newId("qti"));
                item.setTenantId(tenantId);
                item.setQuoteId(id);
                item.setProductId(productId);
                item.setProductName(isBlank(str(itemPayload.get("productName"))) ? product.getName() : str(itemPayload.get("productName")).trim());
                item.setQuantity(Math.max(1, readInt(itemPayload.get("quantity"), 1)));
                item.setUnitPrice(Math.max(0L, readLong(itemPayload.get("unitPrice"), product.getStandardPrice() == null ? 0L : product.getStandardPrice())));
                item.setDiscountRate(Math.max(0.0, readDouble(itemPayload.get("discountRate"), 0.0)));
                item.setTaxRate(Math.max(0.0, readDouble(itemPayload.get("taxRate"), product.getTaxRate() == null ? 0.0 : product.getTaxRate())));
                calculateLine(item);
                QuoteItem savedItem = quoteItemRepository.save(item);
                savedItems.add(toQuoteItemView(savedItem));
            }
        }
        Quote updated = recomputeQuoteAmounts(quote);
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("quote", toQuoteView(updated, false));
        body.put("items", savedItems);
        auditLogService.record(currentUser(request), currentRole(request), "UPDATE", "QUOTE_ITEM", id, "Replace quote items", tenantId);
        return ResponseEntity.ok(successWithFields(request, "quote_items_saved", body));
    }

    @PostMapping("/quotes/{id}/submit")
    public ResponseEntity<?> submitQuote(HttpServletRequest request, @PathVariable String id) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        Optional<Quote> optional = quoteRepository.findByIdAndTenantId(id, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "quote_not_found", msg(request, "quote_not_found"), null));
        }
        Quote quote = optional.get();
        if (isSalesScoped(request) && !ownerMatchesScope(request, quote.getOwner())) {
            return ResponseEntity.status(403).body(errorBody(request, "scope_forbidden", msg(request, "scope_forbidden"), null));
        }
        if (!"DRAFT".equalsIgnoreCase(quote.getStatus())) {
            return ResponseEntity.status(409).body(errorBody(request, "quote_status_transition_invalid", msg(request, "quote_status_transition_invalid"), null));
        }
        String approvalMode = resolveApprovalMode(tenantId);
        List<QuoteItem> rows = quoteItemRepository.findByTenantIdAndQuoteIdOrderByCreatedAtAsc(tenantId, quote.getId());
        boolean triggerByAmount = (quote.getTotalAmount() == null ? 0L : quote.getTotalAmount()) >= 500000L;
        boolean triggerByDiscount = false;
        for (QuoteItem row : rows) {
            if (row.getDiscountRate() != null && row.getDiscountRate() >= 0.20d) {
                triggerByDiscount = true;
                break;
            }
        }
        boolean approvalTriggered = "STAGE_GATE".equals(approvalMode) || triggerByAmount || triggerByDiscount;
        String approvalInstanceId = "";
        if (approvalTriggered) {
            ApprovalInstance instance = createQuoteApprovalInstance(request, quote);
            if (instance == null) {
                return ResponseEntity.status(409).body(errorBody(request, "approval_template_not_found", msg(request, "approval_template_not_found"), null));
            }
            approvalInstanceId = instance.getId();
        }
        quote.setStatus("SUBMITTED");
        quoteRepository.save(quote);
        Map<String, Object> body = toQuoteView(quote, false);
        body.put("approvalTriggered", approvalTriggered);
        body.put("approvalInstanceId", approvalInstanceId);
        body.put("approvalReason", approvalTriggered ? ("STAGE_GATE".equals(approvalMode) ? "STAGE_GATE" : (triggerByAmount ? "AMOUNT" : "DISCOUNT")) : "NONE");
        body.put("approvalMode", approvalMode);
        auditLogService.record(currentUser(request), currentRole(request), "SUBMIT", "QUOTE", quote.getId(), approvalTriggered ? ("submit with approval trigger (" + approvalMode + ")") : "submit without approval trigger", tenantId);
        return ResponseEntity.ok(successWithFields(request, "quote_submitted", body));
    }

    @PostMapping("/quotes/{id}/accept")
    public ResponseEntity<?> acceptQuote(HttpServletRequest request, @PathVariable String id) {
        return transitionQuote(request, id, Collections.singletonList("APPROVED"), "ACCEPTED", "quote_accepted");
    }

    @PostMapping("/quotes/{id}/to-order")
    public ResponseEntity<?> quoteToOrder(HttpServletRequest request, @PathVariable String id) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        Optional<Quote> optional = quoteRepository.findByIdAndTenantId(id, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "quote_not_found", msg(request, "quote_not_found"), null));
        }
        Quote quote = optional.get();
        if (isSalesScoped(request) && !ownerMatchesScope(request, quote.getOwner())) {
            return ResponseEntity.status(403).body(errorBody(request, "scope_forbidden", msg(request, "scope_forbidden"), null));
        }
        String approvalMode = resolveApprovalMode(tenantId);
        if ("STAGE_GATE".equals(approvalMode) && !"APPROVED".equalsIgnoreCase(quote.getStatus())) {
            Map<String, Object> details = new LinkedHashMap<String, Object>();
            details.put("requiredStatus", "APPROVED");
            details.put("currentStatus", quote.getStatus());
            details.put("approvalMode", approvalMode);
            return ResponseEntity.status(409).body(errorBody(request, "quote_stage_gate_requires_approval", msg(request, "quote_stage_gate_requires_approval"), details));
        }
        if (!"STAGE_GATE".equals(approvalMode) && !"ACCEPTED".equalsIgnoreCase(quote.getStatus()) && !"APPROVED".equalsIgnoreCase(quote.getStatus())) {
            return ResponseEntity.status(409).body(errorBody(request, "quote_not_accepted", msg(request, "quote_not_accepted"), null));
        }
        OrderRecord order = new OrderRecord();
        order.setId(newId("ord"));
        order.setTenantId(tenantId);
        order.setOrderNo(generateNo("ORD"));
        order.setCustomerId(quote.getCustomerId());
        order.setOpportunityId(quote.getOpportunityId());
        order.setQuoteId(quote.getId());
        order.setOwner(quote.getOwner());
        order.setStatus("DRAFT");
        order.setAmount(quote.getTotalAmount());
        order.setNotes("Created from quote " + quote.getQuoteNo());
        OrderRecord saved = orderRecordRepository.save(order);
        quote.setStatus("ACCEPTED");
        quoteRepository.save(quote);
        auditLogService.record(currentUser(request), currentRole(request), "CONVERT", "QUOTE", quote.getId(), "Quote to order", tenantId);
        Map<String, Object> body = toOrderView(saved);
        body.put("orderId", saved.getId());
        body.put("orderStatus", saved.getStatus());
        body.put("sourceQuoteId", quote.getId());
        body.put("sourceQuoteStatus", quote.getStatus());
        body.put("approvalMode", approvalMode);
        return ResponseEntity.status(201).body(successWithFields(request, "quote_order_created", body));
    }

    private String resolveApprovalMode(String tenantId) {
        Optional<Tenant> tenantOpt = tenantRepository.findById(tenantId);
        if (!tenantOpt.isPresent()) return "STRICT";
        String mode = tenantOpt.get().getApprovalMode();
        if (isBlank(mode)) return "STRICT";
        String normalized = mode.trim().toUpperCase(Locale.ROOT);
        return "STAGE_GATE".equals(normalized) ? "STAGE_GATE" : "STRICT";
    }

    @GetMapping("/orders")
    public ResponseEntity<?> listOrders(HttpServletRequest request,
                                        @RequestParam(defaultValue = "") String status,
                                        @RequestParam(defaultValue = "") String opportunityId,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        String normalizedStatus = isBlank(status) ? "" : status.trim().toUpperCase(Locale.ROOT);
        Pageable pageable = buildPageable(page, Math.min(100, size), "updatedAt", "desc",
                new HashSet<String>(Arrays.asList("createdAt", "updatedAt", "orderNo", "status", "amount")), "updatedAt");
        if (isSalesScoped(request)) {
            Set<String> ownerSet = new LinkedHashSet<String>();
            ownerSet.add(currentUser(request));
            ownerSet.add(currentOwnerScope(request));
            List<String> owners = new ArrayList<String>(ownerSet);
            Page<OrderRecord> scopedSource;
            if (isBlank(opportunityId)) {
                scopedSource = isBlank(normalizedStatus)
                        ? orderRecordRepository.findByTenantIdAndOwnerIn(tenantId, owners, pageable)
                        : orderRecordRepository.findByTenantIdAndStatusAndOwnerIn(tenantId, normalizedStatus, owners, pageable);
            } else {
                scopedSource = isBlank(normalizedStatus)
                        ? orderRecordRepository.findByTenantIdAndOpportunityIdAndOwnerIn(tenantId, opportunityId, owners, pageable)
                        : orderRecordRepository.findByTenantIdAndStatusAndOpportunityIdAndOwnerIn(tenantId, normalizedStatus, opportunityId, owners, pageable);
            }
            List<Map<String, Object>> scopedItems = new ArrayList<Map<String, Object>>();
            for (OrderRecord row : scopedSource.getContent()) {
                scopedItems.add(toOrderView(row));
            }
            Map<String, Object> body = pageBody(scopedSource, page, size);
            body.put("items", scopedItems);
            return ResponseEntity.ok(successWithFields(request, "orders_listed", body));
        }
        Page<OrderRecord> result;
        if (isBlank(opportunityId)) {
            result = isBlank(normalizedStatus)
                    ? orderRecordRepository.findByTenantId(tenantId, pageable)
                    : orderRecordRepository.findByTenantIdAndStatus(tenantId, normalizedStatus, pageable);
        } else {
            result = isBlank(normalizedStatus)
                    ? orderRecordRepository.findByTenantIdAndOpportunityId(tenantId, opportunityId, pageable)
                    : orderRecordRepository.findByTenantIdAndStatusAndOpportunityId(tenantId, normalizedStatus, opportunityId, pageable);
        }
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (OrderRecord row : result.getContent()) {
            items.add(toOrderView(row));
        }
        Map<String, Object> body = pageBody(result, page, size);
        body.put("items", items);
        return ResponseEntity.ok(successWithFields(request, "orders_listed", body));
    }

    @PostMapping("/orders")
    public ResponseEntity<?> createOrder(HttpServletRequest request, @RequestBody Map<String, Object> payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        OrderRecord row = new OrderRecord();
        row.setId(newId("ord"));
        row.setTenantId(tenantId);
        row.setOrderNo(generateNo("ORD"));
        String validate = applyOrderPayload(request, row, payload, true);
        if (!isBlank(validate)) {
            return ResponseEntity.badRequest().body(errorBody(request, validate, msg(request, validate), null));
        }
        OrderRecord saved = orderRecordRepository.save(row);
        auditLogService.record(currentUser(request), currentRole(request), "CREATE", "ORDER", saved.getId(), "Create order", tenantId);
        return ResponseEntity.status(201).body(successWithFields(request, "order_created", toOrderView(saved)));
    }

    @GetMapping("/quotes/{id}/versions")
    public ResponseEntity<?> listQuoteVersions(HttpServletRequest request, @PathVariable String id) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        Optional<Quote> optional = quoteRepository.findByIdAndTenantId(id, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "quote_not_found", msg(request, "quote_not_found"), null));
        }
        Quote quote = optional.get();
        if (isSalesScoped(request) && !ownerMatchesScope(request, quote.getOwner())) {
            return ResponseEntity.status(403).body(errorBody(request, "scope_forbidden", msg(request, "scope_forbidden"), null));
        }
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (QuoteVersion row : quoteVersionRepository.findByTenantIdAndQuoteIdOrderByVersionNoDesc(tenantId, id)) {
            Map<String, Object> out = new LinkedHashMap<String, Object>();
            out.put("id", row.getId());
            out.put("quoteId", row.getQuoteId());
            out.put("versionNo", row.getVersionNo());
            out.put("status", row.getStatus());
            out.put("totalAmount", row.getTotalAmount());
            out.put("createdBy", row.getCreatedBy());
            out.put("createdAt", row.getCreatedAt());
            out.put("snapshotJson", row.getSnapshotJson());
            items.add(out);
        }
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("quoteId", id);
        body.put("items", items);
        body.put("total", items.size());
        return ResponseEntity.ok(successWithFields(request, "quote_versions_listed", body));
    }

    @PostMapping("/quotes/{id}/versions")
    public ResponseEntity<?> createQuoteVersion(HttpServletRequest request,
                                                @PathVariable String id,
                                                @RequestBody(required = false) Map<String, Object> payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        Optional<Quote> optional = quoteRepository.findByIdAndTenantId(id, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "quote_not_found", msg(request, "quote_not_found"), null));
        }
        Quote quote = optional.get();
        if (isSalesScoped(request) && !ownerMatchesScope(request, quote.getOwner())) {
            return ResponseEntity.status(403).body(errorBody(request, "scope_forbidden", msg(request, "scope_forbidden"), null));
        }
        int nextVersion = quoteVersionRepository.findTopByTenantIdAndQuoteIdOrderByVersionNoDesc(tenantId, id)
                .map(v -> (v.getVersionNo() == null ? 0 : v.getVersionNo()) + 1)
                .orElse(1);
        List<Map<String, Object>> lineItems = new ArrayList<Map<String, Object>>();
        for (QuoteItem item : quoteItemRepository.findByTenantIdAndQuoteIdOrderByCreatedAtAsc(tenantId, id)) {
            lineItems.add(toQuoteItemView(item));
        }
        Map<String, Object> snapshot = new LinkedHashMap<String, Object>();
        snapshot.put("quote", toQuoteView(quote, false));
        snapshot.put("items", lineItems);
        snapshot.put("remark", payload == null ? "" : str(payload.get("remark")));
        snapshot.put("requestId", traceId(request));
        String snapshotJson;
        try {
            snapshotJson = objectMapper.writeValueAsString(snapshot);
        } catch (Exception ex) {
            snapshotJson = "{\"quoteId\":\"" + id + "\"}";
        }
        QuoteVersion row = new QuoteVersion();
        row.setId(newId("qtv"));
        row.setTenantId(tenantId);
        row.setQuoteId(id);
        row.setVersionNo(nextVersion);
        row.setStatus(quote.getStatus());
        row.setTotalAmount(quote.getTotalAmount() == null ? 0L : quote.getTotalAmount());
        row.setCreatedBy(currentUser(request));
        row.setSnapshotJson(snapshotJson);
        QuoteVersion saved = quoteVersionRepository.save(row);
        auditLogService.record(currentUser(request), currentRole(request), "VERSION", "QUOTE", quote.getId(), "Create quote version " + nextVersion, tenantId);
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("id", saved.getId());
        body.put("quoteId", saved.getQuoteId());
        body.put("versionNo", saved.getVersionNo());
        body.put("status", saved.getStatus());
        body.put("totalAmount", saved.getTotalAmount());
        body.put("createdBy", saved.getCreatedBy());
        body.put("createdAt", saved.getCreatedAt());
        return ResponseEntity.status(201).body(successWithFields(request, "quote_version_created", body));
    }

    @PatchMapping("/orders")
    public ResponseEntity<?> patchOrder(HttpServletRequest request, @RequestBody Map<String, Object> payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String id = str(payload.get("id"));
        if (isBlank(id)) {
            return ResponseEntity.badRequest().body(errorBody(request, "id_required", msg(request, "id_required"), null));
        }
        String tenantId = currentTenant(request);
        Optional<OrderRecord> optional = orderRecordRepository.findByIdAndTenantId(id, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "order_not_found", msg(request, "order_not_found"), null));
        }
        OrderRecord row = optional.get();
        if (isSalesScoped(request) && !ownerMatchesScope(request, row.getOwner())) {
            return ResponseEntity.status(403).body(errorBody(request, "scope_forbidden", msg(request, "scope_forbidden"), null));
        }
        String validate = applyOrderPayload(request, row, payload, false);
        if (!isBlank(validate)) {
            return ResponseEntity.badRequest().body(errorBody(request, validate, msg(request, validate), null));
        }
        OrderRecord saved = orderRecordRepository.save(row);
        auditLogService.record(currentUser(request), currentRole(request), "UPDATE", "ORDER", saved.getId(), "Update order", tenantId);
        return ResponseEntity.ok(successWithFields(request, "order_updated", toOrderView(saved)));
    }

    @PostMapping("/orders/{id}/confirm")
    public ResponseEntity<?> confirmOrder(HttpServletRequest request, @PathVariable String id) {
        return transitionOrder(request, id, "DRAFT", "CONFIRMED", "order_confirmed");
    }

    @PostMapping("/orders/{id}/fulfill")
    public ResponseEntity<?> fulfillOrder(HttpServletRequest request, @PathVariable String id) {
        return transitionOrder(request, id, "CONFIRMED", "FULFILLING", "order_fulfilling");
    }

    @PostMapping("/orders/{id}/complete")
    public ResponseEntity<?> completeOrder(HttpServletRequest request, @PathVariable String id) {
        return transitionOrder(request, id, "FULFILLING", "COMPLETED", "order_completed");
    }

    @PostMapping("/orders/{id}/cancel")
    public ResponseEntity<?> cancelOrder(HttpServletRequest request, @PathVariable String id) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        Optional<OrderRecord> optional = orderRecordRepository.findByIdAndTenantId(id, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "order_not_found", msg(request, "order_not_found"), null));
        }
        OrderRecord row = optional.get();
        if (!Arrays.asList("DRAFT", "CONFIRMED", "FULFILLING").contains(row.getStatus().toUpperCase(Locale.ROOT))) {
            return ResponseEntity.status(409).body(errorBody(request, "order_status_transition_invalid", msg(request, "order_status_transition_invalid"), null));
        }
        row.setStatus("CANCELED");
        row = orderRecordRepository.save(row);
        auditLogService.record(currentUser(request), currentRole(request), "STATUS", "ORDER", row.getId(), "Order CANCELED", tenantId);
        return ResponseEntity.ok(successWithFields(request, "order_canceled", toOrderView(row)));
    }

    @PostMapping("/orders/{id}/to-contract")
    public ResponseEntity<?> orderToContract(HttpServletRequest request, @PathVariable String id) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        Optional<OrderRecord> optional = orderRecordRepository.findByIdAndTenantId(id, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "order_not_found", msg(request, "order_not_found"), null));
        }
        OrderRecord order = optional.get();
        if (!"CONFIRMED".equalsIgnoreCase(order.getStatus()) && !"FULFILLING".equalsIgnoreCase(order.getStatus())) {
            return ResponseEntity.status(409).body(errorBody(request, "order_not_confirmed", msg(request, "order_not_confirmed"), null));
        }
        ContractRecord contract = new ContractRecord();
        contract.setId(newId("ctr"));
        contract.setTenantId(tenantId);
        contract.setCustomerId(order.getCustomerId());
        contract.setContractNo(generateNo("CTR"));
        contract.setTitle("Contract from " + order.getOrderNo());
        contract.setAmount(order.getAmount());
        contract.setStatus("Draft");
        contract.setOwner(order.getOwner());
        contract.setSignDate(order.getSignDate());
        ContractRecord saved = contractRecordRepository.save(contract);
        auditLogService.record(currentUser(request), currentRole(request), "CONVERT", "ORDER", order.getId(), "Order to contract", tenantId);
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("orderId", order.getId());
        body.put("contractId", saved.getId());
        body.put("contractNo", saved.getContractNo());
        return ResponseEntity.status(201).body(successWithFields(request, "order_contract_created", body));
    }

    private ResponseEntity<?> transitionQuote(HttpServletRequest request, String id, List<String> fromStatuses, String to, String successCode) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        Optional<Quote> optional = quoteRepository.findByIdAndTenantId(id, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "quote_not_found", msg(request, "quote_not_found"), null));
        }
        Quote row = optional.get();
        if (isSalesScoped(request) && !ownerMatchesScope(request, row.getOwner())) {
            return ResponseEntity.status(403).body(errorBody(request, "scope_forbidden", msg(request, "scope_forbidden"), null));
        }
        boolean validFrom = false;
        for (String from : fromStatuses) {
            if (from.equalsIgnoreCase(row.getStatus())) {
                validFrom = true;
                break;
            }
        }
        if (!validFrom) {
            return ResponseEntity.status(409).body(errorBody(request, "quote_status_transition_invalid", msg(request, "quote_status_transition_invalid"), null));
        }
        row.setStatus(to);
        Quote saved = quoteRepository.save(row);
        auditLogService.record(currentUser(request), currentRole(request), "STATUS", "QUOTE", saved.getId(), "Quote " + to, tenantId);
        return ResponseEntity.ok(successWithFields(request, successCode, toQuoteView(saved, false)));
    }

    private ResponseEntity<?> transitionOrder(HttpServletRequest request, String id, String from, String to, String successCode) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        Optional<OrderRecord> optional = orderRecordRepository.findByIdAndTenantId(id, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(errorBody(request, "order_not_found", msg(request, "order_not_found"), null));
        }
        OrderRecord row = optional.get();
        if (!from.equalsIgnoreCase(row.getStatus())) {
            return ResponseEntity.status(409).body(errorBody(request, "order_status_transition_invalid", msg(request, "order_status_transition_invalid"), null));
        }
        row.setStatus(to);
        OrderRecord saved = orderRecordRepository.save(row);
        auditLogService.record(currentUser(request), currentRole(request), "STATUS", "ORDER", saved.getId(), "Order " + to, tenantId);
        return ResponseEntity.ok(successWithFields(request, successCode, toOrderView(saved)));
    }

    private void resetDefaultPriceBook(String tenantId) {
        List<PriceBook> all = priceBookRepository.findByTenantIdOrderByUpdatedAtDesc(tenantId);
        for (PriceBook row : all) {
            if (Boolean.TRUE.equals(row.getDefault())) {
                row.setDefault(false);
                priceBookRepository.save(row);
            }
        }
    }

    private Quote recomputeQuoteAmounts(Quote quote) {
        List<QuoteItem> items = quoteItemRepository.findByTenantIdAndQuoteIdOrderByCreatedAtAsc(quote.getTenantId(), quote.getId());
        long subtotal = 0L;
        long tax = 0L;
        long total = 0L;
        for (QuoteItem item : items) {
            subtotal += item.getSubtotalAmount() == null ? 0L : item.getSubtotalAmount();
            tax += item.getTaxAmount() == null ? 0L : item.getTaxAmount();
            total += item.getTotalAmount() == null ? 0L : item.getTotalAmount();
        }
        quote.setSubtotalAmount(subtotal);
        quote.setTaxAmount(tax);
        quote.setTotalAmount(total);
        return quoteRepository.save(quote);
    }

    private void calculateLine(QuoteItem item) {
        int qty = item.getQuantity() == null ? 1 : Math.max(1, item.getQuantity());
        long unitPrice = item.getUnitPrice() == null ? 0L : Math.max(0L, item.getUnitPrice());
        double discountRate = item.getDiscountRate() == null ? 0.0 : Math.max(0.0, item.getDiscountRate());
        if (discountRate > 1) discountRate = 1;
        double taxRate = item.getTaxRate() == null ? 0.0 : Math.max(0.0, item.getTaxRate());
        long subtotal = Math.max(0L, Math.round(qty * unitPrice * (1 - discountRate)));
        long tax = Math.max(0L, Math.round(subtotal * taxRate));
        item.setQuantity(qty);
        item.setUnitPrice(unitPrice);
        item.setDiscountRate(discountRate);
        item.setTaxRate(taxRate);
        item.setSubtotalAmount(subtotal);
        item.setTaxAmount(tax);
        item.setTotalAmount(subtotal + tax);
    }

    private String applyProductPayload(Product row, Map<String, Object> payload, boolean creating) {
        String code = str(payload.get("code"));
        String name = str(payload.get("name"));
        if (creating && (isBlank(code) || isBlank(name))) return "product_code_name_required";
        if (!isBlank(code)) row.setCode(code.trim().toUpperCase(Locale.ROOT));
        if (!isBlank(name)) row.setName(name.trim());
        if (payload.containsKey("category")) row.setCategory(nullable(payload.get("category")));
        String status = upperOrDefault(str(payload.get("status")), row.getStatus());
        if (!isBlank(status)) {
            if (!PRODUCT_STATUSES.contains(status)) return "product_status_invalid";
            row.setStatus(status);
        }
        if (payload.containsKey("standardPrice")) row.setStandardPrice(Math.max(0L, readLong(payload.get("standardPrice"), 0L)));
        if (payload.containsKey("taxRate")) row.setTaxRate(Math.max(0.0, readDouble(payload.get("taxRate"), 0.0)));
        if (payload.containsKey("currency")) row.setCurrency(upperOrDefault(str(payload.get("currency")), "CNY"));
        if (payload.containsKey("unit")) row.setUnit(nullable(payload.get("unit")));
        if (payload.containsKey("saleRegion")) row.setSaleRegion(nullable(payload.get("saleRegion")));
        if (payload.containsKey("validFrom")) row.setValidFrom(parseLocalDate(payload.get("validFrom")));
        if (payload.containsKey("validTo")) row.setValidTo(parseLocalDate(payload.get("validTo")));
        if (row.getValidFrom() != null && row.getValidTo() != null && row.getValidFrom().isAfter(row.getValidTo())) return "date_range_invalid";
        return "";
    }

    private String applyPriceBookPayload(PriceBook row, Map<String, Object> payload, boolean creating) {
        String name = str(payload.get("name"));
        if (creating && isBlank(name)) return "price_book_name_required";
        if (!isBlank(name)) row.setName(name.trim());
        if (payload.containsKey("status")) {
            String status = upperOrDefault(str(payload.get("status")), "ACTIVE");
            if (!PRICE_BOOK_STATUSES.contains(status)) return "price_book_status_invalid";
            row.setStatus(status);
        }
        if (payload.containsKey("isDefault")) row.setDefault(Boolean.TRUE.equals(payload.get("isDefault")));
        if (payload.containsKey("department")) row.setDepartment(nullable(payload.get("department")));
        if (payload.containsKey("currency")) row.setCurrency(upperOrDefault(str(payload.get("currency")), "CNY"));
        if (payload.containsKey("validFrom")) row.setValidFrom(parseLocalDate(payload.get("validFrom")));
        if (payload.containsKey("validTo")) row.setValidTo(parseLocalDate(payload.get("validTo")));
        if (row.getValidFrom() != null && row.getValidTo() != null && row.getValidFrom().isAfter(row.getValidTo())) return "date_range_invalid";
        return "";
    }

    private String applyQuotePayload(HttpServletRequest request, Quote row, Map<String, Object> payload, boolean creating) {
        String customerId = str(payload.get("customerId"));
        if (creating && isBlank(customerId)) return "quote_customer_required";
        if (!isBlank(customerId)) {
            if (!belongsToTenant(customerRepository.findById(customerId).orElse(null), row.getTenantId())) return "customer_not_found";
            row.setCustomerId(customerId);
        }
        String opportunityId = str(payload.get("opportunityId"));
        if (!isBlank(opportunityId)) {
            if (!belongsToTenant(opportunityRepository.findById(opportunityId).orElse(null), row.getTenantId())) return "opportunity_not_found";
            row.setOpportunityId(opportunityId);
        }
        if (payload.containsKey("priceBookId")) row.setPriceBookId(nullable(payload.get("priceBookId")));
        if (payload.containsKey("owner")) {
            String owner = str(payload.get("owner"));
            if (!isBlank(owner)) {
                if (isSalesScoped(request) && !ownerMatchesScope(request, owner)) return "scope_forbidden";
                row.setOwner(owner.trim());
            }
        } else if (creating) {
            row.setOwner(currentUser(request));
        }
        if (payload.containsKey("status")) {
            String status = upperOrDefault(str(payload.get("status")), row.getStatus());
            if (!QUOTE_STATUSES.contains(status)) return "quote_status_invalid";
            row.setStatus(status);
        }
        if (payload.containsKey("validUntil")) row.setValidUntil(parseLocalDate(payload.get("validUntil")));
        if (payload.containsKey("notes")) row.setNotes(nullable(payload.get("notes")));
        if (payload.containsKey("version")) row.setVersion(Math.max(1, readInt(payload.get("version"), row.getVersion() == null ? 1 : row.getVersion())));
        return "";
    }

    private String applyOrderPayload(HttpServletRequest request, OrderRecord row, Map<String, Object> payload, boolean creating) {
        String customerId = str(payload.get("customerId"));
        if (creating && isBlank(customerId)) return "order_customer_required";
        if (!isBlank(customerId)) {
            if (!belongsToTenant(customerRepository.findById(customerId).orElse(null), row.getTenantId())) return "customer_not_found";
            row.setCustomerId(customerId);
        }
        if (payload.containsKey("opportunityId")) {
            String opportunityId = str(payload.get("opportunityId"));
            if (!isBlank(opportunityId) && !belongsToTenant(opportunityRepository.findById(opportunityId).orElse(null), row.getTenantId())) {
                return "opportunity_not_found";
            }
            row.setOpportunityId(nullable(payload.get("opportunityId")));
        }
        if (payload.containsKey("quoteId")) row.setQuoteId(nullable(payload.get("quoteId")));
        if (payload.containsKey("owner")) {
            String owner = str(payload.get("owner"));
            if (!isBlank(owner)) {
                if (isSalesScoped(request) && !ownerMatchesScope(request, owner)) return "scope_forbidden";
                row.setOwner(owner.trim());
            }
        } else if (creating) {
            row.setOwner(currentUser(request));
        }
        if (payload.containsKey("status")) {
            String status = upperOrDefault(str(payload.get("status")), row.getStatus());
            if (!ORDER_STATUSES.contains(status)) return "order_status_invalid";
            row.setStatus(status);
        }
        if (payload.containsKey("amount")) row.setAmount(Math.max(0L, readLong(payload.get("amount"), row.getAmount() == null ? 0L : row.getAmount())));
        if (payload.containsKey("signDate")) row.setSignDate(parseLocalDate(payload.get("signDate")));
        if (payload.containsKey("notes")) row.setNotes(nullable(payload.get("notes")));
        return "";
    }

    private ApprovalInstance createQuoteApprovalInstance(HttpServletRequest request, Quote quote) {
        String tenantId = quote.getTenantId();
        String role = currentRole(request);
        List<ApprovalTemplate> templates = approvalTemplateRepository.findByTenantIdAndBizTypeAndEnabledTrueOrderByCreatedAtAsc(tenantId, "QUOTE");
        ApprovalTemplate selected = null;
        for (ApprovalTemplate t : templates) {
            long amount = quote.getTotalAmount() == null ? 0L : quote.getTotalAmount();
            if (t.getAmountMin() != null && amount < t.getAmountMin()) continue;
            if (t.getAmountMax() != null && amount > t.getAmountMax()) continue;
            if (!isBlank(t.getRole()) && !t.getRole().equalsIgnoreCase(role)) continue;
            if ("INACTIVE".equalsIgnoreCase(t.getStatus())) continue;
            selected = t;
            break;
        }
        if (selected == null) {
            return null;
        }
        ApprovalInstance instance = new ApprovalInstance();
        instance.setId(newId("api"));
        instance.setTenantId(tenantId);
        instance.setTemplateId(selected.getId());
        instance.setTemplateVersion(selected.getVersion());
        instance.setBizType("QUOTE");
        instance.setBizId(quote.getId());
        instance.setSubmitter(currentUser(request));
        instance.setComment("Auto submitted by quote threshold");
        instance.setStatus("PENDING");
        instance.setCurrentSeq(1);
        instance = approvalInstanceRepository.save(instance);

        String[] roles = (selected.getApproverRoles() == null ? "" : selected.getApproverRoles()).split(",");
        int seq = 1;
        for (String r : roles) {
            String roleItem = isBlank(r) ? "" : r.trim().toUpperCase(Locale.ROOT);
            if (roleItem.isEmpty()) continue;
            ApprovalTask task = new ApprovalTask();
            task.setId(newId("aptk"));
            task.setTenantId(tenantId);
            task.setInstanceId(instance.getId());
            task.setApproverRole(roleItem);
            task.setSeq(seq);
            task.setStatus(seq == 1 ? "PENDING" : "WAITING");
            task.setSlaMinutes(240);
            task.setDeadlineAt(LocalDateTime.now().plusMinutes(240));
            approvalTaskRepository.save(task);

            ApprovalEvent event = new ApprovalEvent();
            event.setId(newId("apev"));
            event.setTenantId(tenantId);
            event.setInstanceId(instance.getId());
            event.setTaskId(task.getId());
            event.setEventType("TASK_CREATED");
            event.setOperatorUser(currentUser(request));
            event.setDetail("quote_auto_submit");
            event.setRequestId(traceId(request));
            approvalEventRepository.save(event);
            seq++;
        }

        ApprovalEvent submitEvent = new ApprovalEvent();
        submitEvent.setId(newId("apev"));
        submitEvent.setTenantId(tenantId);
        submitEvent.setInstanceId(instance.getId());
        submitEvent.setTaskId(null);
        submitEvent.setEventType("SUBMITTED");
        submitEvent.setOperatorUser(currentUser(request));
        submitEvent.setDetail("quote_auto_submit");
        submitEvent.setRequestId(traceId(request));
        approvalEventRepository.save(submitEvent);
        return instance;
    }

    private Map<String, Object> toProductView(Product row) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
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
        Map<String, Object> out = new LinkedHashMap<String, Object>();
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
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("id", row.getId());
        out.put("priceBookId", row.getPriceBookId());
        out.put("productId", row.getProductId());
        out.put("price", row.getPrice());
        out.put("taxRate", row.getTaxRate());
        out.put("currency", row.getCurrency());
        out.put("updatedAt", row.getUpdatedAt());
        return out;
    }

    private Map<String, Object> toQuoteView(Quote row, boolean withItems) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("id", row.getId());
        out.put("quoteNo", row.getQuoteNo());
        out.put("customerId", row.getCustomerId());
        out.put("opportunityId", row.getOpportunityId());
        out.put("priceBookId", row.getPriceBookId());
        out.put("owner", row.getOwner());
        out.put("status", row.getStatus());
        out.put("subtotalAmount", row.getSubtotalAmount());
        out.put("taxAmount", row.getTaxAmount());
        out.put("totalAmount", row.getTotalAmount());
        out.put("version", row.getVersion());
        out.put("validUntil", row.getValidUntil());
        out.put("notes", row.getNotes());
        out.put("tenantId", row.getTenantId());
        out.put("updatedAt", row.getUpdatedAt());
        if (withItems) {
            List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
            for (QuoteItem item : quoteItemRepository.findByTenantIdAndQuoteIdOrderByCreatedAtAsc(row.getTenantId(), row.getId())) {
                items.add(toQuoteItemView(item));
            }
            out.put("items", items);
        }
        return out;
    }

    private Map<String, Object> toQuoteItemView(QuoteItem row) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("id", row.getId());
        out.put("quoteId", row.getQuoteId());
        out.put("productId", row.getProductId());
        out.put("productName", row.getProductName());
        out.put("quantity", row.getQuantity());
        out.put("unitPrice", row.getUnitPrice());
        out.put("discountRate", row.getDiscountRate());
        out.put("taxRate", row.getTaxRate());
        out.put("subtotalAmount", row.getSubtotalAmount());
        out.put("taxAmount", row.getTaxAmount());
        out.put("totalAmount", row.getTotalAmount());
        return out;
    }

    private Map<String, Object> toOrderView(OrderRecord row) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("id", row.getId());
        out.put("orderNo", row.getOrderNo());
        out.put("customerId", row.getCustomerId());
        out.put("opportunityId", row.getOpportunityId());
        out.put("quoteId", row.getQuoteId());
        out.put("owner", row.getOwner());
        out.put("status", row.getStatus());
        out.put("amount", row.getAmount());
        out.put("signDate", row.getSignDate());
        out.put("notes", row.getNotes());
        out.put("tenantId", row.getTenantId());
        out.put("updatedAt", row.getUpdatedAt());
        return out;
    }

    private Map<String, Object> pageBody(Page<?> pageResult, int page, int size) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("items", pageResult.getContent());
        body.put("total", pageResult.getTotalElements());
        body.put("page", Math.max(1, page));
        body.put("size", Math.max(1, size));
        body.put("totalPages", pageResult.getTotalPages());
        return body;
    }

    private String generateNo(String prefix) {
        return prefix + "-" + LocalDate.now().toString().replace("-", "") + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    }

    private String newId(String prefix) {
        return prefix + "_" + Long.toString(System.currentTimeMillis(), 36) + String.format("%03d", (int) (Math.random() * 1000));
    }

    private boolean belongsToTenant(Customer customer, String tenantId) {
        return customer != null && tenantId.equals(customer.getTenantId());
    }

    private boolean belongsToTenant(Opportunity row, String tenantId) {
        return row != null && tenantId.equals(row.getTenantId());
    }

    private String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String nullable(Object value) {
        if (value == null) return null;
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private String upperOrDefault(String value, String fallback) {
        if (isBlank(value)) return fallback;
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private long readLong(Object value, long fallback) {
        try { return Long.parseLong(String.valueOf(value)); } catch (Exception ex) { return fallback; }
    }

    private int readInt(Object value, int fallback) {
        try { return Integer.parseInt(String.valueOf(value)); } catch (Exception ex) { return fallback; }
    }

    private double readDouble(Object value, double fallback) {
        try { return Double.parseDouble(String.valueOf(value)); } catch (Exception ex) { return fallback; }
    }
}
