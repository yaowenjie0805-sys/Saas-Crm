package com.yao.crm.controller;

import com.yao.crm.dto.request.CreateContactRequest;
import com.yao.crm.dto.request.UpdateContactRequest;
import com.yao.crm.entity.Contact;
import com.yao.crm.entity.Customer;
import com.yao.crm.repository.ContactRepository;
import com.yao.crm.repository.CustomerRepository;
import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.I18nService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.Predicate;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ContactController extends BaseApiController {

    private final ContactRepository contactRepository;
    private final CustomerRepository customerRepository;
    private final AuditLogService auditLogService;

    public ContactController(ContactRepository contactRepository,
                             CustomerRepository customerRepository,
                             AuditLogService auditLogService,
                             I18nService i18nService) {
        super(i18nService);
        this.contactRepository = contactRepository;
        this.customerRepository = customerRepository;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/contacts/search")
    public ResponseEntity<?> searchContacts(
            HttpServletRequest request,
            @RequestParam(defaultValue = "") String customerId,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "8") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES", "ANALYST")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }

        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(50, size));
        Pageable pageable = buildPageable(
                safePage,
                safeSize,
                sortBy,
                sortDir,
                new HashSet<String>(Arrays.asList("customerId", "name", "title", "phone", "email", "owner", "createdAt", "updatedAt")),
                "updatedAt"
        );

        final boolean salesScoped = isSalesScoped(request);
        final String ownerScope = currentOwnerScope(request);
        Specification<Contact> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<Predicate>();
            if (!isBlank(customerId)) {
                predicates.add(cb.equal(root.get("customerId"), customerId));
            }
            if (!isBlank(q)) {
                String pattern = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("phone")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(cb.lower(root.get("title")), pattern)
                ));
            }
            if (salesScoped) {
                Predicate selfOwner = cb.equal(root.get("owner"), currentUser(request));
                Predicate scopeOwner = cb.equal(root.get("owner"), ownerScope);
                predicates.add(cb.or(selfOwner, scopeOwner));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Contact> result = contactRepository.findAll(spec, pageable);
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("items", result.getContent());
        body.put("total", result.getTotalElements());
        body.put("page", safePage);
        body.put("size", safeSize);
        body.put("totalPages", result.getTotalPages());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/contacts")
    public ResponseEntity<?> createContact(HttpServletRequest request, @Valid @RequestBody CreateContactRequest payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }

        Optional<Customer> customer = customerRepository.findById(payload.getCustomerId());
        if (!customer.isPresent()) {
            return ResponseEntity.badRequest().body(legacyErrorByKey(request, "customer_not_found", "BAD_REQUEST", null));
        }
        if (isSalesScoped(request) && !ownerMatchesScope(request, customer.get().getOwner())) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "scope_forbidden", "FORBIDDEN", null));
        }

        Contact contact = new Contact();
        contact.setId(newId("ct"));
        contact.setCustomerId(payload.getCustomerId());
        contact.setName(payload.getName());
        contact.setTitle(payload.getTitle());
        contact.setPhone(payload.getPhone());
        contact.setEmail(payload.getEmail());
        if (isSalesScoped(request)) {
            contact.setOwner(currentOwnerScope(request));
        } else {
            contact.setOwner(isBlank(payload.getOwner()) ? customer.get().getOwner() : payload.getOwner());
        }

        Contact saved = contactRepository.save(contact);
        auditLogService.record(currentUser(request), currentRole(request), "CREATE", "CONTACT", saved.getId(), saved.getName());
        return ResponseEntity.status(201).body(saved);
    }

    @PatchMapping("/contacts/{id}")
    public ResponseEntity<?> updateContact(HttpServletRequest request, @PathVariable String id, @Valid @RequestBody UpdateContactRequest patch) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }

        Optional<Contact> optional = contactRepository.findById(id);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(legacyErrorByKey(request, "contact_not_found", "NOT_FOUND", null));
        }
        Contact contact = optional.get();
        if (isSalesScoped(request) && !ownerMatchesScope(request, contact.getOwner())) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "scope_forbidden", "FORBIDDEN", null));
        }

        if (patch.getCustomerId() != null) {
            Optional<Customer> customer = customerRepository.findById(patch.getCustomerId());
            if (!customer.isPresent()) {
                return ResponseEntity.badRequest().body(legacyErrorByKey(request, "customer_not_found", "BAD_REQUEST", null));
            }
            if (isSalesScoped(request) && !ownerMatchesScope(request, customer.get().getOwner())) {
                return ResponseEntity.status(403).body(legacyErrorByKey(request, "scope_forbidden", "FORBIDDEN", null));
            }
            contact.setCustomerId(patch.getCustomerId());
            if (isSalesScoped(request)) {
                contact.setOwner(currentOwnerScope(request));
            }
        }

        if (patch.getName() != null) contact.setName(patch.getName());
        if (patch.getTitle() != null) contact.setTitle(patch.getTitle());
        if (patch.getPhone() != null) contact.setPhone(patch.getPhone());
        if (patch.getEmail() != null) contact.setEmail(patch.getEmail());
        if (patch.getOwner() != null && !isSalesScoped(request)) contact.setOwner(patch.getOwner());

        Contact saved = contactRepository.save(contact);
        auditLogService.record(currentUser(request), currentRole(request), "UPDATE", "CONTACT", saved.getId(), "Updated contact");
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/contacts/{id}")
    public ResponseEntity<?> deleteContact(HttpServletRequest request, @PathVariable String id) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }

        Optional<Contact> optional = contactRepository.findById(id);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(legacyErrorByKey(request, "contact_not_found", "NOT_FOUND", null));
        }
        if (isSalesScoped(request) && !ownerMatchesScope(request, optional.get().getOwner())) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "scope_forbidden", "FORBIDDEN", null));
        }

        contactRepository.deleteById(id);
        auditLogService.record(currentUser(request), currentRole(request), "DELETE", "CONTACT", id, "Deleted contact");
        return ResponseEntity.noContent().build();
    }

    private String newId(String prefix) {
        return prefix + "_" + Long.toString(System.currentTimeMillis(), 36) + String.format("%03d", (int) (Math.random() * 1000));
    }
}


