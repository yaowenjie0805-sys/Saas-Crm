package com.yao.crm.controller;

import com.yao.crm.dto.request.CreateTaskRequest;
import com.yao.crm.dto.request.UpdateTaskRequest;
import com.yao.crm.entity.TaskItem;
import com.yao.crm.repository.TaskRepository;
import com.yao.crm.service.AuditLogService;
import com.yao.crm.service.I18nService;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.Predicate;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class TaskController extends BaseApiController {

    private final TaskRepository taskRepository;
    private final AuditLogService auditLogService;

    public TaskController(TaskRepository taskRepository, AuditLogService auditLogService, I18nService i18nService) {
        super(i18nService);
        this.taskRepository = taskRepository;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/tasks")
    public ResponseEntity<?> tasks(HttpServletRequest request,
                                   @RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "50") int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(50, size));
        Pageable pageable = buildPageable(safePage, safeSize, "updatedAt", "desc",
                new HashSet<>(Set.of("title", "owner", "done", "createdAt", "updatedAt")),
                "updatedAt");
        org.springframework.data.domain.Page<TaskItem> result = taskRepository.findByTenantId(currentTenant(request), pageable);
        Map<String, Object> body = new HashMap<>();
        body.put("items", result.getContent());
        body.put("total", result.getTotalElements());
        body.put("page", safePage);
        body.put("size", safeSize);
        body.put("totalPages", result.getTotalPages());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/tasks/search")
    public ResponseEntity<?> searchTasks(
            HttpServletRequest request,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String done,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
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
                new HashSet<String>(Arrays.asList("title", "time", "level", "done", "owner", "createdAt", "updatedAt")),
                "updatedAt"
        );

        final String tenantId = currentTenant(request);
        final Set<String> ownerScope = buildOwnerScope(request);
        if (ownerScope != null && ownerScope.isEmpty()) {
            return ResponseEntity.ok(pageBody(new PageImpl<TaskItem>(Collections.emptyList(), pageable, 0), safePage, safeSize));
        }

        Boolean doneFilter = parseDoneFilter(done);
        boolean hasKeyword = !isBlank(q);
        boolean updatedDesc = "updatedAt".equalsIgnoreCase(sortBy) && !"asc".equalsIgnoreCase(sortDir);
        if (updatedDesc && !hasKeyword && doneFilter != null) {
            Page<TaskItem> indexedResult = ownerScope == null
                    ? taskRepository.findByTenantIdAndDoneOrderByUpdatedAtDesc(tenantId, doneFilter, pageable)
                    : taskRepository.findByTenantIdAndOwnerInAndDoneOrderByUpdatedAtDesc(tenantId, ownerScope, doneFilter, pageable);
            return ResponseEntity.ok(pageBody(indexedResult, safePage, safeSize));
        }

        Specification<TaskItem> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<Predicate>();
            predicates.add(cb.equal(root.get("tenantId"), tenantId));
            if (ownerScope != null) {
                predicates.add(root.get("owner").in(ownerScope));
            }
            if (!isBlank(q)) {
                String pattern = "%" + escapeLike(q.trim().toLowerCase(Locale.ROOT)) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern, '\\'),
                        cb.like(cb.lower(root.get("time")), pattern, '\\'),
                        cb.like(cb.lower(root.get("level")), pattern, '\\'),
                        cb.like(cb.lower(root.get("owner")), pattern, '\\')
                ));
            }
            if (!isBlank(done)) {
                if (doneFilter == null) {
                    return cb.and(cb.equal(cb.literal(1), 0));
                }
                predicates.add(cb.equal(root.get("done"), doneFilter));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };

        Page<TaskItem> result = taskRepository.findAll(spec, pageable);
        return ResponseEntity.ok(pageBody(result, safePage, safeSize));
    }

    @PostMapping("/tasks")
    public ResponseEntity<?> createTask(HttpServletRequest request, @Valid @RequestBody CreateTaskRequest payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }

        TaskItem task = new TaskItem();
        task.setId(newId("t"));
        task.setTenantId(currentTenant(request));
        task.setTitle(payload.getTitle());
        task.setTime(payload.getTime());
        task.setLevel(isBlank(payload.getLevel()) ? "Medium" : payload.getLevel());
        task.setDone(payload.getDone() == null ? false : payload.getDone());
        task.setOwner(currentUser(request));

        TaskItem saved = taskRepository.save(task);
        auditLogService.record(currentUser(request), currentRole(request), "CREATE", "TASK", saved.getId(), saved.getTitle());
        return ResponseEntity.status(201).body(saved);
    }

    @PatchMapping("/tasks/{id}")
    public ResponseEntity<?> updateTask(HttpServletRequest request, @PathVariable String id, @Valid @RequestBody UpdateTaskRequest patch) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "SALES")) {
            return ResponseEntity.status(403).body(legacyErrorByKey(request, "forbidden", "FORBIDDEN", null));
        }
        String tenantId = currentTenant(request);
        Optional<TaskItem> optional = taskRepository.findByIdAndTenantId(id, tenantId);
        if (!optional.isPresent()) {
            return ResponseEntity.status(404).body(legacyErrorByKey(request, "task_not_found", "NOT_FOUND", null));
        }
        TaskItem task = optional.get();
        if (patch.getTitle() != null) task.setTitle(patch.getTitle());
        if (patch.getTime() != null) task.setTime(patch.getTime());
        if (patch.getLevel() != null) task.setLevel(patch.getLevel());
        if (patch.getDone() != null) task.setDone(patch.getDone());

        TaskItem saved = taskRepository.save(task);
        auditLogService.record(currentUser(request), currentRole(request), "UPDATE", "TASK", saved.getId(), "Updated task");
        return ResponseEntity.ok(saved);
    }

    private String newId(String prefix) {
        return prefix + "_" + Long.toString(System.currentTimeMillis(), 36) + String.format("%03d", (int) (Math.random() * 1000));
    }

    private Boolean parseDoneFilter(String done) {
        if (isBlank(done)) return null;
        if (!"true".equalsIgnoreCase(done) && !"false".equalsIgnoreCase(done)) return null;
        return Boolean.parseBoolean(done);
    }

    private Set<String> buildOwnerScope(HttpServletRequest request) {
        if (!isSalesScoped(request)) return null;
        Set<String> scoped = new LinkedHashSet<String>();
        String current = currentUser(request);
        String currentScope = currentOwnerScope(request);
        if (!isBlank(current)) scoped.add(current.trim());
        if (!isBlank(currentScope)) scoped.add(currentScope.trim());
        return scoped.stream()
                .filter(item -> !isBlank(item))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Map<String, Object> pageBody(Page<TaskItem> page, int safePage, int safeSize) {
        Map<String, Object> body = new HashMap<>();
        body.put("items", page.getContent());
        body.put("total", page.getTotalElements());
        body.put("page", safePage);
        body.put("size", safeSize);
        body.put("totalPages", page.getTotalPages());
        return body;
    }
}


