package com.yao.crm.controller;

import com.yao.crm.dto.request.CreateTaskRequest;
import com.yao.crm.dto.request.UpdateTaskRequest;
import com.yao.crm.entity.TaskItem;
import com.yao.crm.repository.TaskRepository;
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
public class TaskController extends BaseApiController {

    private final TaskRepository taskRepository;
    private final AuditLogService auditLogService;

    public TaskController(TaskRepository taskRepository, AuditLogService auditLogService, I18nService i18nService) {
        super(i18nService);
        this.taskRepository = taskRepository;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/tasks")
    public List<TaskItem> tasks(HttpServletRequest request) {
        return taskRepository.findByTenantId(currentTenant(request));
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
        Specification<TaskItem> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<Predicate>();
            predicates.add(cb.equal(root.get("tenantId"), tenantId));
            if (!isBlank(q)) {
                String pattern = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("time")), pattern),
                        cb.like(cb.lower(root.get("level")), pattern),
                        cb.like(cb.lower(root.get("owner")), pattern)
                ));
            }
            if (!isBlank(done)) {
                if (!"true".equalsIgnoreCase(done) && !"false".equalsIgnoreCase(done)) {
                    return cb.and(cb.equal(cb.literal(1), 0));
                }
                predicates.add(cb.equal(root.get("done"), Boolean.parseBoolean(done)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<TaskItem> result = taskRepository.findAll(spec, pageable);
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("items", result.getContent());
        body.put("total", result.getTotalElements());
        body.put("page", safePage);
        body.put("size", safeSize);
        body.put("totalPages", result.getTotalPages());
        return ResponseEntity.ok(body);
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
}


