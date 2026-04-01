package com.yao.crm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yao.crm.entity.NotificationJob;
import com.yao.crm.repository.NotificationJobRepository;
import com.yao.crm.util.IdGenerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class NotificationJobService {

    private static final int DEFAULT_PROCESS_QUEUE_BATCH_SIZE = 100;

    private final NotificationJobRepository jobRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    private final IntegrationWebhookService integrationWebhookService;
    private final IdGenerator idGenerator;
    private final int processQueueBatchSize;
    private final int maxRetries;
    private final List<String> slaEscalationTargets;

    public NotificationJobService(NotificationJobRepository jobRepository,
                                   AuditLogService auditLogService,
                                   ObjectMapper objectMapper,
                                   IntegrationWebhookService integrationWebhookService,
                                   IdGenerator idGenerator,
                                   @Value("${integration.notifications.process-queue-batch-size:100}") int processQueueBatchSize,
                                   @Value("${integration.notifications.max-retries:5}") int maxRetries,
                                   @Value("${integration.webhooks.providers:WECOM,DINGTALK}") String providers) {
        this.jobRepository = jobRepository;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
        this.integrationWebhookService = integrationWebhookService;
        this.idGenerator = idGenerator;
        this.processQueueBatchSize = processQueueBatchSize <= 0 ? DEFAULT_PROCESS_QUEUE_BATCH_SIZE : processQueueBatchSize;
        this.maxRetries = Math.max(1, maxRetries);
        this.slaEscalationTargets = parseProviders(providers);
    }

    @Transactional(timeout = 30)
    public void enqueueSlaEscalated(String tenantId, String instanceId, String taskId, String approverRole) {
        if (slaEscalationTargets.isEmpty()) {
            return;
        }
        List<String> targets = new ArrayList<String>();
        List<String> dedupeKeys = new ArrayList<String>();
        for (String target : slaEscalationTargets) {
            String dedupeKey = tenantId + "|" + instanceId + "|" + taskId + "|approval_sla_escalated|" + target;
            targets.add(target);
            dedupeKeys.add(dedupeKey);
        }
        Set<String> existingDedupeKeys = new HashSet<String>();
        if (!dedupeKeys.isEmpty()) {
            for (NotificationJob existing : jobRepository.findByTenantIdAndDedupeKeyIn(tenantId, dedupeKeys)) {
                if (existing.getDedupeKey() != null) existingDedupeKeys.add(existing.getDedupeKey());
            }
        }
        for (String target : targets) {
            String dedupeKey = tenantId + "|" + instanceId + "|" + taskId + "|approval_sla_escalated|" + target;
            if (existingDedupeKeys.contains(dedupeKey)) continue;
            try {
                Map<String, Object> payload = new LinkedHashMap<String, Object>();
                payload.put("event", "approval_sla_escalated");
                payload.put("tenantId", tenantId);
                payload.put("instanceId", instanceId);
                payload.put("taskId", taskId);
                payload.put("approverRole", approverRole);
                NotificationJob job = new NotificationJob();
                job.setId(idGenerator.generate("noj"));
                job.setTenantId(tenantId);
                job.setEventType("approval_sla_escalated");
                job.setTarget(target);
                job.setPayload(objectMapper.writeValueAsString(payload));
                job.setStatus("PENDING");
                job.setRetryCount(0);
                job.setMaxRetries(maxRetries);
                job.setDedupeKey(dedupeKey);
                job.setNextRetryAt(LocalDateTime.now());
                jobRepository.save(job);
                auditLogService.record("system", "SYSTEM", "NOTIFY_ENQUEUED", "NOTIFICATION_JOB", job.getId(), "Queued notification job", tenantId);
            } catch (Exception ex) {
                auditLogService.record("system", "SYSTEM", "NOTIFY_ENQUEUE_FAILED", "NOTIFICATION_JOB", taskId, String.valueOf(ex.getMessage()), tenantId);
            }
        }
    }

    @Transactional(timeout = 30)
    public int processQueue() {
        int processed = 0;
        LocalDateTime now = LocalDateTime.now();
        Pageable pageable = PageRequest.of(
                0,
                processQueueBatchSize,
                Sort.by(Sort.Direction.ASC, "createdAt").and(Sort.by(Sort.Direction.ASC, "id"))
        );
        while (true) {
            Page<NotificationJob> jobs = jobRepository.findByStatusInAndNextRetryAtBefore(
                    Arrays.asList("PENDING", "RETRY"),
                    now,
                    pageable
            );
            List<NotificationJob> batch = jobs.getContent();
            if (batch.isEmpty()) {
                break;
            }
            for (NotificationJob job : batch) {
                processed++;
                try {
                    job.setStatus("RUNNING");
                    jobRepository.save(job);
                    boolean ok = dispatch(job);
                    if (ok) {
                        job.setStatus("SUCCESS");
                        job.setLastError(null);
                        jobRepository.save(job);
                        auditLogService.record("system", "SYSTEM", "NOTIFY_SUCCESS", job.getTarget(), job.getId(), "notification sent", job.getTenantId());
                    } else {
                        failOrRetry(job, "dispatch returned false");
                    }
                } catch (Exception ex) {
                    failOrRetry(job, ex.getMessage());
                }
            }
        }
        return processed;
    }

    public Map<String, Object> listJobsPaged(String tenantId, String status, int page, int size) {
        int finalPage = Math.max(1, page);
        int finalSize = Math.max(1, Math.min(size, 200));
        Pageable pageable = PageRequest.of(finalPage - 1, finalSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<NotificationJob> rows = (status == null || status.trim().isEmpty() || "ALL".equalsIgnoreCase(status))
                ? jobRepository.findByTenantId(tenantId, pageable)
                : jobRepository.findByTenantIdAndStatus(tenantId, status.trim().toUpperCase(Locale.ROOT), pageable);
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (NotificationJob row : rows.getContent()) {
            items.add(toView(row));
        }
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("items", items);
        out.put("page", finalPage);
        out.put("size", finalSize);
        out.put("totalPages", Math.max(1, rows.getTotalPages()));
        out.put("total", rows.getTotalElements());
        return out;
    }

    @Transactional
    public NotificationJob retry(String tenantId, String jobId) {
        Optional<NotificationJob> optional = jobRepository.findByIdAndTenantId(jobId, tenantId);
        if (!optional.isPresent()) return null;
        NotificationJob job = optional.get();
        if (!"FAILED".equalsIgnoreCase(job.getStatus())) return job;
        moveToRetry(job);
        return jobRepository.save(job);
    }

    @Transactional
    public Map<String, Object> batchRetryByIds(String tenantId, List<String> jobIds) {
        int requested = jobIds == null ? 0 : jobIds.size();
        int succeeded = 0;
        int skipped = 0;
        int notFound = 0;
        int forbidden = 0;
        if (jobIds != null) {
            List<String> normalizedIds = new ArrayList<String>();
            LinkedHashSet<String> uniqueIds = new LinkedHashSet<String>();
            for (String rawId : jobIds) {
                String id = rawId == null ? "" : rawId.trim();
                if (id.isEmpty()) {
                    skipped++;
                    continue;
                }
                normalizedIds.add(id);
                uniqueIds.add(id);
            }
            if (!uniqueIds.isEmpty()) {
                List<NotificationJob> jobs = jobRepository.findAllById(uniqueIds);
                Map<String, NotificationJob> jobsById = new HashMap<String, NotificationJob>();
                for (NotificationJob job : jobs) {
                    if (job != null && job.getId() != null) {
                        jobsById.put(job.getId(), job);
                    }
                }
                List<NotificationJob> toRetry = new ArrayList<NotificationJob>();
                for (String id : normalizedIds) {
                    NotificationJob job = jobsById.get(id);
                    if (job == null) {
                        notFound++;
                        continue;
                    }
                    if (!tenantId.equals(job.getTenantId())) {
                        forbidden++;
                        continue;
                    }
                    if (!"FAILED".equalsIgnoreCase(job.getStatus())) {
                        skipped++;
                        continue;
                    }
                    moveToRetry(job);
                    toRetry.add(job);
                    succeeded++;
                }
                if (!toRetry.isEmpty()) {
                    jobRepository.saveAll(toRetry);
                }
            }
        }
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("requested", requested);
        out.put("succeeded", succeeded);
        out.put("skipped", skipped);
        out.put("notFound", notFound);
        out.put("forbidden", forbidden);
        auditLogService.record("system", "SYSTEM", "NOTIFY_BATCH_RETRY", "NOTIFICATION_JOB", tenantId, out.toString(), tenantId);
        return out;
    }

    @Transactional
    public Map<String, Object> retryByFilter(String tenantId, String status, int page, int size) {
        int finalPage = Math.max(1, page);
        int finalSize = Math.max(1, Math.min(size, 200));
        String normalizedStatus = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        String effectiveStatus = (normalizedStatus.isEmpty() || "ALL".equals(normalizedStatus)) ? "FAILED" : normalizedStatus;

        if (!"FAILED".equals(effectiveStatus)) {
            Map<String, Object> out = new LinkedHashMap<String, Object>();
            out.put("requested", 0);
            out.put("succeeded", 0);
            out.put("skipped", 0);
            out.put("status", effectiveStatus);
            out.put("page", finalPage);
            out.put("size", finalSize);
            auditLogService.record("system", "SYSTEM", "NOTIFY_RETRY_BY_FILTER", "NOTIFICATION_JOB", tenantId, out.toString(), tenantId);
            return out;
        }

        Pageable pageable = PageRequest.of(finalPage - 1, finalSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<NotificationJob> rows = jobRepository.findByTenantIdAndStatus(tenantId, effectiveStatus, pageable);
        int requested = rows.getContent().size();
        int succeeded = 0;
        int skipped = 0;
        for (NotificationJob job : rows.getContent()) {
            if (!"FAILED".equalsIgnoreCase(job.getStatus())) {
                skipped++;
                continue;
            }
            moveToRetry(job);
            jobRepository.save(job);
            succeeded++;
        }
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("requested", requested);
        out.put("succeeded", succeeded);
        out.put("skipped", skipped);
        out.put("status", effectiveStatus);
        out.put("page", finalPage);
        out.put("size", finalSize);
        auditLogService.record("system", "SYSTEM", "NOTIFY_RETRY_BY_FILTER", "NOTIFICATION_JOB", tenantId, out.toString(), tenantId);
        return out;
    }

    private void failOrRetry(NotificationJob job, String reason) {
        int nextRetryCount = (job.getRetryCount() == null ? 0 : job.getRetryCount()) + 1;
        job.setRetryCount(nextRetryCount);
        job.setLastError(reason == null ? "unknown" : reason);
        if (nextRetryCount >= (job.getMaxRetries() == null ? maxRetries : job.getMaxRetries())) {
            job.setStatus("FAILED");
            job.setNextRetryAt(null);
            auditLogService.record("system", "SYSTEM", "NOTIFY_FAILED", job.getTarget(), job.getId(), job.getLastError(), job.getTenantId());
        } else {
            long delaySeconds = (long) Math.min(300, Math.pow(2, nextRetryCount) * 5);
            job.setStatus("RETRY");
            job.setNextRetryAt(LocalDateTime.now().plusSeconds(delaySeconds));
            auditLogService.record("system", "SYSTEM", "NOTIFY_RETRY", job.getTarget(), job.getId(), "retry in " + delaySeconds + "s", job.getTenantId());
        }
        jobRepository.save(job);
    }

    private boolean dispatch(NotificationJob job) {
        String payload = job.getPayload() == null ? "{}" : job.getPayload();
        boolean sent = integrationWebhookService.sendEvent(job.getTarget(), job.getTenantId(), job.getEventType(), payload, job.getId());
        auditLogService.record("system", "SYSTEM", "WEBHOOK_DISPATCH", job.getTarget(), job.getId(),
                "sent=" + sent + ", payload=" + payload, job.getTenantId());
        return sent;
    }

    private void moveToRetry(NotificationJob job) {
        job.setStatus("RETRY");
        job.setNextRetryAt(LocalDateTime.now());
        job.setLastError(null);
    }

    private Map<String, Object> toView(NotificationJob row) {
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("jobId", row.getId());
        item.put("tenantId", row.getTenantId());
        item.put("eventType", row.getEventType());
        item.put("target", row.getTarget());
        item.put("status", row.getStatus());
        item.put("retryCount", row.getRetryCount());
        item.put("maxRetries", row.getMaxRetries());
        item.put("nextRetryAt", row.getNextRetryAt());
        item.put("lastError", row.getLastError());
        item.put("createdAt", row.getCreatedAt());
        item.put("updatedAt", row.getUpdatedAt());
        return item;
    }


    private List<String> parseProviders(String providers) {
        if (providers == null || providers.trim().isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> targets = new LinkedHashSet<String>();
        String[] configuredTargets = providers.split(",");
        for (String targetRaw : configuredTargets) {
            String target = targetRaw == null ? "" : targetRaw.trim().toUpperCase(Locale.ROOT);
            if (!target.isEmpty()) {
                targets.add(target);
            }
        }
        if (targets.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<String>(targets);
    }
}
