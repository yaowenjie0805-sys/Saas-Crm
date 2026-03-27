package com.yao.crm.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yao.crm.config.LeadImportMqConfig;
import com.yao.crm.dto.LeadImportChunkMessage;
import com.yao.crm.entity.Lead;
import com.yao.crm.entity.LeadImportJob;
import com.yao.crm.entity.LeadImportJobChunk;
import com.yao.crm.entity.LeadImportJobItem;
import com.yao.crm.repository.LeadImportJobChunkRepository;
import com.yao.crm.repository.LeadImportJobItemRepository;
import com.yao.crm.repository.LeadImportJobRepository;
import com.yao.crm.repository.LeadRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class LeadImportService {

    private static final Set<String> LEAD_STATUSES = new HashSet<String>(Arrays.asList("NEW", "QUALIFIED", "NURTURING", "CONVERTED", "DISQUALIFIED"));

    private final LeadRepository leadRepository;
    private final LeadImportJobRepository jobRepository;
    private final LeadImportJobItemRepository itemRepository;
    private final LeadImportJobChunkRepository chunkRepository;
    private final LeadAssignmentService leadAssignmentService;
    private final LeadAutomationService leadAutomationService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;
    private final int chunkSize;
    private final int maxRetries;
    private final int maxRows;
    private final long maxFileBytes;
    private final int maxConcurrentPerTenant;
    private final boolean mqPublishEnabled;

    public LeadImportService(LeadRepository leadRepository,
                             LeadImportJobRepository jobRepository,
                             LeadImportJobItemRepository itemRepository,
                             LeadImportJobChunkRepository chunkRepository,
                             LeadAssignmentService leadAssignmentService,
                             LeadAutomationService leadAutomationService,
                             AuditLogService auditLogService,
                             ObjectMapper objectMapper,
                             RabbitTemplate rabbitTemplate,
                             Environment environment,
                             @Value("${lead.import.chunk-size:1000}") int chunkSize,
                             @Value("${lead.import.max-retries:3}") int maxRetries,
                             @Value("${lead.import.max-rows:200000}") int maxRows,
                             @Value("${lead.import.max-file-mb:20}") int maxFileMb,
                             @Value("${lead.import.max-concurrent-per-tenant:2}") int maxConcurrentPerTenant,
                             @Value("${lead.import.mq.publish.enabled:true}") boolean mqPublishEnabled) {
        this.leadRepository = leadRepository;
        this.jobRepository = jobRepository;
        this.itemRepository = itemRepository;
        this.chunkRepository = chunkRepository;
        this.leadAssignmentService = leadAssignmentService;
        this.leadAutomationService = leadAutomationService;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.chunkSize = Math.max(500, Math.min(chunkSize, 2000));
        this.maxRetries = Math.max(1, maxRetries);
        this.maxRows = Math.max(1000, maxRows);
        this.maxFileBytes = Math.max(1, maxFileMb) * 1024L * 1024L;
        this.maxConcurrentPerTenant = Math.max(1, maxConcurrentPerTenant);
        boolean testProfile = Arrays.asList(environment.getActiveProfiles()).contains("test");
        this.mqPublishEnabled = mqPublishEnabled && !testProfile;
    }

    @Transactional(timeout = 30)
    public LeadImportJob createAsyncImportJob(String tenantId, String operator, String requestId, MultipartFile file) {
        validateUpload(file);
        ensureTenantImportCapacity(tenantId);
        LeadImportJob job = new LeadImportJob();
        job.setId(newId("lij"));
        job.setTenantId(tenantId);
        job.setFileName(file.getOriginalFilename());
        job.setStatus("PENDING");
        job.setCreatedBy(operator);
        job.setTotalRows(0);
        job.setProcessedRows(0);
        job.setSuccessCount(0);
        job.setFailCount(0);
        job.setPercent(0);
        job.setCancelRequested(false);
        job.setErrorMessage(null);
        job = jobRepository.save(job);

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
            String header = reader.readLine();
            if (header == null) {
                throw new IllegalArgumentException("lead_import_empty_file");
            }

            String line;
            int lineNo = 1;
            int chunkNo = 0;
            int totalRows = 0;
            List<String> buffer = new ArrayList<String>();
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.trim().isEmpty()) continue;
                totalRows++;
                if (totalRows > maxRows) {
                    throw new IllegalArgumentException("lead_import_rows_exceed_limit");
                }
                buffer.add(lineNo + "\t" + line);
                if (buffer.size() >= chunkSize) {
                    chunkNo++;
                    enqueueChunk(tenantId, job.getId(), chunkNo, buffer, requestId);
                    buffer.clear();
                }
            }
            if (!buffer.isEmpty()) {
                chunkNo++;
                enqueueChunk(tenantId, job.getId(), chunkNo, buffer, requestId);
            }

            job.setTotalRows(totalRows);
            if (chunkNo == 0) {
                job.setStatus("FAILED");
                job.setErrorMessage("lead_import_empty_file");
            } else {
                job.setStatus("RUNNING");
                auditLogService.record(operator, "SYSTEM", "JOB_STARTED", "LEAD_IMPORT_JOB", job.getId(), buildAuditDetails("JOB_STARTED", requestId, "chunks=" + chunkNo), tenantId);
            }
            job.setLastHeartbeatAt(LocalDateTime.now());
            job = jobRepository.save(job);
            auditLogService.record(operator, "SYSTEM", "JOB_CREATED", "LEAD_IMPORT_JOB", job.getId(), buildAuditDetails("JOB_CREATED", requestId, "chunks=" + chunkNo + ", rows=" + totalRows), tenantId);
            return job;
        } catch (Exception ex) {
            job.setStatus("FAILED");
            job.setErrorMessage(normalizeError(ex));
            job.setLastHeartbeatAt(LocalDateTime.now());
            job = jobRepository.save(job);
            auditLogService.record(operator, "SYSTEM", "JOB_FINISHED", "LEAD_IMPORT_JOB", job.getId(), buildAuditDetails("JOB_FINISHED", requestId, "status=FAILED,error=" + normalizeError(ex)), tenantId);
            return job;
        }
    }

    public Map<String, Object> listJobs(String tenantId, String status, int page, int size) {
        int finalPage = Math.max(1, page);
        int finalSize = Math.max(1, Math.min(size, 50));
        Pageable pageable = PageRequest.of(finalPage - 1, finalSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<LeadImportJob> rows = (status == null || status.trim().isEmpty() || "ALL".equalsIgnoreCase(status))
                ? jobRepository.findByTenantId(tenantId, pageable)
                : jobRepository.findByTenantIdAndStatus(tenantId, status.trim().toUpperCase(Locale.ROOT), pageable);

        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (LeadImportJob row : rows.getContent()) items.add(toView(row));

        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("items", items);
        out.put("page", finalPage);
        out.put("size", finalSize);
        out.put("totalPages", Math.max(1, rows.getTotalPages()));
        out.put("total", rows.getTotalElements());
        return out;
    }

    @Transactional
    public LeadImportJob cancelJob(String tenantId, String jobId, String operator, String requestId) {
        Optional<LeadImportJob> optional = jobRepository.findByIdAndTenantId(jobId, tenantId);
        if (!optional.isPresent()) return null;
        LeadImportJob job = optional.get();
        if (!"PENDING".equalsIgnoreCase(job.getStatus()) && !"RUNNING".equalsIgnoreCase(job.getStatus())) {
            throw new IllegalStateException("lead_import_status_transition_invalid");
        }
        job.setCancelRequested(true);
        job.setStatus("CANCELED");
        job.setLastHeartbeatAt(LocalDateTime.now());
        job = jobRepository.save(job);
        auditLogService.record(operator, "SYSTEM", "JOB_CANCELED", "LEAD_IMPORT_JOB", jobId, buildAuditDetails("JOB_CANCELED", requestId, "status=CANCELED"), tenantId);
        return job;
    }

    @Transactional
    public LeadImportJob retryJob(String tenantId, String jobId, String operator, String requestId) {
        Optional<LeadImportJob> optional = jobRepository.findByIdAndTenantId(jobId, tenantId);
        if (!optional.isPresent()) return null;
        LeadImportJob job = optional.get();
        if (!("FAILED".equalsIgnoreCase(job.getStatus()) || "CANCELED".equalsIgnoreCase(job.getStatus()))) {
            throw new IllegalStateException("lead_import_status_transition_invalid");
        }
        ensureTenantImportCapacity(tenantId);
        job.setStatus("RUNNING");
        job.setCancelRequested(false);
        job.setErrorMessage(null);
        job.setLastHeartbeatAt(LocalDateTime.now());
        job = jobRepository.save(job);

        int queued = 0;
        for (LeadImportJobChunk chunk : chunkRepository.findByTenantIdAndJobIdOrderByChunkNoAsc(tenantId, jobId)) {
            if ("PROCESSED".equalsIgnoreCase(chunk.getStatus())) continue;
            chunk.setStatus("PENDING");
            chunkRepository.save(chunk);
            publishChunkMessage(tenantId, jobId, chunk.getChunkNo(), readChunkRows(chunk.getPayloadJson()), 0, requestId, LeadImportMqConfig.ROUTING_KEY);
            queued++;
        }
        if (queued == 0) {
            throw new IllegalStateException("lead_import_retry_no_pending_chunks");
        }

        auditLogService.record(operator, "SYSTEM", "JOB_RETRIED", "LEAD_IMPORT_JOB", jobId, buildAuditDetails("JOB_RETRIED", requestId, "queuedChunks=" + queued), tenantId);
        return job;
    }

    public Map<String, Object> listFailedRows(String tenantId, String jobId, int page, int size) {
        int finalPage = Math.max(1, page);
        int finalSize = Math.max(1, Math.min(size, 200));
        Pageable pageable = PageRequest.of(finalPage - 1, finalSize, Sort.by(Sort.Direction.ASC, "lineNo"));
        Page<LeadImportJobItem> rows = itemRepository.findByTenantIdAndJobIdAndStatusOrderByLineNoAsc(tenantId, jobId, "FAILED", pageable);

        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (LeadImportJobItem row : rows.getContent()) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("lineNo", row.getLineNo());
            item.put("rawLine", row.getRawLine());
            item.put("errorCode", row.getErrorCode());
            item.put("errorMessage", row.getErrorMessage());
            items.add(item);
        }

        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("items", items);
        out.put("page", finalPage);
        out.put("size", finalSize);
        out.put("totalPages", Math.max(1, rows.getTotalPages()));
        out.put("total", rows.getTotalElements());
        return out;
    }

    public Map<String, Object> toView(LeadImportJob job) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("id", job.getId());
        out.put("tenantId", job.getTenantId());
        out.put("fileName", job.getFileName());
        out.put("status", job.getStatus());
        out.put("totalRows", job.getTotalRows());
        out.put("processedRows", job.getProcessedRows());
        out.put("successCount", job.getSuccessCount());
        out.put("failCount", job.getFailCount());
        out.put("percent", job.getPercent());
        out.put("lastHeartbeatAt", job.getLastHeartbeatAt());
        out.put("cancelRequested", job.getCancelRequested());
        out.put("errorMessage", job.getErrorMessage());
        out.put("createdBy", job.getCreatedBy());
        out.put("createdAt", job.getCreatedAt());
        out.put("updatedAt", job.getUpdatedAt());
        out.put("taskStats", buildTaskStats(job));
        Map<String, Object> failureSummary = new LinkedHashMap<String, Object>();
        failureSummary.put("failedRows", job.getFailCount() == null ? 0 : job.getFailCount());
        failureSummary.put("errorMessage", job.getErrorMessage());
        out.put("failureSummary", failureSummary);
        return out;
    }

    @Transactional
    public void consumeLeadImportChunk(LeadImportChunkMessage message) {
        if (message == null || message.getTenantId() == null || message.getJobId() == null || message.getChunkNo() == null) return;
        String tenantId = message.getTenantId();
        String jobId = message.getJobId();
        Integer chunkNo = message.getChunkNo();

        Optional<LeadImportJob> jobOptional = jobRepository.findByIdAndTenantId(jobId, tenantId);
        if (!jobOptional.isPresent()) return;
        LeadImportJob job = jobOptional.get();
        if (Boolean.TRUE.equals(job.getCancelRequested()) || "CANCELED".equalsIgnoreCase(job.getStatus())) {
            markChunkStatus(tenantId, jobId, chunkNo, "CANCELED", "job canceled");
            finalizeIfDone(job);
            return;
        }

        Optional<LeadImportJobChunk> chunkOptional = chunkRepository.findByTenantIdAndJobIdAndChunkNo(tenantId, jobId, chunkNo);
        if (!chunkOptional.isPresent()) return;
        LeadImportJobChunk chunk = chunkOptional.get();
        if ("PROCESSED".equalsIgnoreCase(chunk.getStatus()) || "CANCELED".equalsIgnoreCase(chunk.getStatus()) || "RUNNING".equalsIgnoreCase(chunk.getStatus())) return;
        chunk.setStatus("RUNNING");
        chunkRepository.save(chunk);

        try {
            List<String> rawRows = message.getRows() == null ? Collections.<String>emptyList() : message.getRows();
            int processed = 0;
            int success = 0;
            int failed = 0;
            Set<String> existingKeys = new HashSet<String>();
            for (Lead row : leadRepository.findByTenantId(tenantId)) {
                existingKeys.add(keyOf(row.getName(), row.getCompany(), row.getPhone(), row.getEmail()));
            }
            for (String wrapped : rawRows) {
                processed++;
                int lineNo = parseLineNo(wrapped);
                String line = parseRawLine(wrapped);
                try {
                    Row row = parseRow(splitCsvLine(line));
                    validateRow(row);
                    String dedupe = keyOf(row.name, row.company, row.phone, row.email);
                    if (existingKeys.contains(dedupe)) {
                        throw new IllegalArgumentException("lead_import_duplicate");
                    }
                    existingKeys.add(dedupe);
                    Lead lead = new Lead();
                    lead.setId(newId("ld"));
                    lead.setTenantId(tenantId);
                    lead.setName(row.name);
                    lead.setCompany(row.company);
                    lead.setPhone(row.phone);
                    lead.setEmail(row.email);
                    lead.setSource(row.source);
                    lead.setStatus(row.status);
                    lead.setOwner(row.owner);
                    if (isBlank(lead.getOwner())) {
                        String assigned = leadAssignmentService.assignOwnerForTenant(tenantId);
                        if (!assigned.isEmpty()) lead.setOwner(assigned);
                    }
                    if (isBlank(lead.getOwner())) lead.setOwner(job.getCreatedBy());
                    lead = leadRepository.save(lead);
                    leadAutomationService.onLeadEvent(tenantId, "LEAD_CREATED", lead, job.getCreatedBy());
                    leadAutomationService.onLeadEvent(tenantId, "LEAD_ASSIGNED", lead, job.getCreatedBy());
                    success++;
                } catch (Exception rowEx) {
                    failed++;
                    saveItemFailure(tenantId, jobId, lineNo, line, normalizeError(rowEx), normalizeError(rowEx));
                }
            }

            chunk.setStatus("PROCESSED");
            chunk.setLastError(null);
            chunkRepository.save(chunk);
            updateJobProgress(job, processed, success, failed);
            finalizeIfDone(job);
        } catch (Exception ex) {
            int retry = message.getRetryCount() == null ? 0 : message.getRetryCount();
            retry++;
            chunk.setRetryCount(retry);
            chunk.setStatus("FAILED");
            chunk.setLastError(normalizeError(ex));
            chunkRepository.save(chunk);
            auditLogService.record(job.getCreatedBy(), "SYSTEM", "CHUNK_FAILED", "LEAD_IMPORT_JOB", jobId, buildAuditDetails("CHUNK_FAILED", message.getRequestId(), "chunkNo=" + chunkNo + ",retry=" + retry + ",error=" + normalizeError(ex)), tenantId);
            if (retry <= maxRetries) {
                publishChunkMessage(tenantId, jobId, chunkNo, message.getRows(), retry, message.getRequestId(), LeadImportMqConfig.RETRY_ROUTING_KEY);
            } else {
                job.setStatus("FAILED");
                job.setErrorMessage(normalizeError(ex));
                job.setLastHeartbeatAt(LocalDateTime.now());
                jobRepository.save(job);
                finalizeIfDone(job);
            }
        }
    }

    private void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("lead_import_file_required");
        if (file.getSize() > maxFileBytes) throw new IllegalArgumentException("lead_import_file_too_large");
    }

    private void enqueueChunk(String tenantId, String jobId, int chunkNo, List<String> rows, String requestId) throws Exception {
        List<String> chunkRows = new ArrayList<String>(rows);
        LeadImportJobChunk chunk = new LeadImportJobChunk();
        chunk.setId(newId("ljc"));
        chunk.setTenantId(tenantId);
        chunk.setJobId(jobId);
        chunk.setChunkNo(chunkNo);
        chunk.setStatus("PENDING");
        chunk.setRetryCount(0);
        chunk.setPayloadJson(objectMapper.writeValueAsString(chunkRows));
        chunkRepository.save(chunk);
        publishChunkMessage(tenantId, jobId, chunkNo, chunkRows, 0, requestId, LeadImportMqConfig.ROUTING_KEY);
    }

    private void publishChunkMessage(String tenantId, String jobId, int chunkNo, List<String> rows, int retryCount, String requestId, String routingKey) {
        if (!mqPublishEnabled) {
            return;
        }
        LeadImportChunkMessage message = new LeadImportChunkMessage();
        message.setTenantId(tenantId);
        message.setJobId(jobId);
        message.setChunkNo(chunkNo);
        message.setRows(rows == null ? Collections.<String>emptyList() : rows);
        message.setRetryCount(retryCount);
        message.setRequestId(requestId);
        rabbitTemplate.convertAndSend(LeadImportMqConfig.EXCHANGE, routingKey, message);
    }

    private void updateJobProgress(LeadImportJob job, int processed, int success, int failed) {
        LeadImportJob latest = jobRepository.findById(job.getId()).orElse(job);
        int nextProcessed = (latest.getProcessedRows() == null ? 0 : latest.getProcessedRows()) + processed;
        int nextSuccess = (latest.getSuccessCount() == null ? 0 : latest.getSuccessCount()) + success;
        int nextFailed = (latest.getFailCount() == null ? 0 : latest.getFailCount()) + failed;
        latest.setProcessedRows(nextProcessed);
        latest.setSuccessCount(nextSuccess);
        latest.setFailCount(nextFailed);
        int total = latest.getTotalRows() == null ? 0 : latest.getTotalRows();
        int percent = total <= 0 ? 0 : Math.min(100, (int) Math.round((nextProcessed * 100.0) / total));
        latest.setPercent(percent);
        latest.setLastHeartbeatAt(LocalDateTime.now());
        if (!"RUNNING".equalsIgnoreCase(latest.getStatus())) latest.setStatus("RUNNING");
        jobRepository.save(latest);
    }

    private void finalizeIfDone(LeadImportJob job) {
        long totalChunks = chunkRepository.countByTenantIdAndJobId(job.getTenantId(), job.getId());
        long processedChunks = chunkRepository.countByTenantIdAndJobIdAndStatus(job.getTenantId(), job.getId(), "PROCESSED");
        long canceledChunks = chunkRepository.countByTenantIdAndJobIdAndStatus(job.getTenantId(), job.getId(), "CANCELED");
        long failedChunks = chunkRepository.countByTenantIdAndJobIdAndStatus(job.getTenantId(), job.getId(), "FAILED");
        if (processedChunks + canceledChunks + failedChunks < totalChunks) return;

        LeadImportJob latest = jobRepository.findById(job.getId()).orElse(job);
        latest.setLastHeartbeatAt(LocalDateTime.now());
        latest.setPercent(100);
        if (failedChunks > 0) {
            latest.setStatus("FAILED");
        } else if (Boolean.TRUE.equals(latest.getCancelRequested()) || "CANCELED".equalsIgnoreCase(latest.getStatus())) {
            latest.setStatus("CANCELED");
        } else if ((latest.getFailCount() == null ? 0 : latest.getFailCount()) == 0) {
            latest.setStatus("SUCCESS");
        } else if ((latest.getSuccessCount() == null ? 0 : latest.getSuccessCount()) > 0) {
            latest.setStatus("PARTIAL_SUCCESS");
        } else {
            latest.setStatus("FAILED");
        }
        jobRepository.save(latest);
        auditLogService.record(latest.getCreatedBy(), "SYSTEM", "JOB_FINISHED", "LEAD_IMPORT_JOB", latest.getId(), buildAuditDetails("JOB_FINISHED", "", "status=" + latest.getStatus()), latest.getTenantId());
    }

    private void markChunkStatus(String tenantId, String jobId, Integer chunkNo, String status, String error) {
        Optional<LeadImportJobChunk> optional = chunkRepository.findByTenantIdAndJobIdAndChunkNo(tenantId, jobId, chunkNo);
        if (!optional.isPresent()) return;
        LeadImportJobChunk row = optional.get();
        row.setStatus(status);
        row.setLastError(error);
        chunkRepository.save(row);
    }

    private List<String> readChunkRows(String raw) {
        try {
            if (raw == null || raw.trim().isEmpty()) return Collections.emptyList();
            return objectMapper.readValue(raw, new TypeReference<List<String>>() {});
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    private void saveItemFailure(String tenantId, String jobId, int lineNo, String rawLine, String errorCode, String message) {
        LeadImportJobItem item = new LeadImportJobItem();
        item.setId(newId("lji"));
        item.setTenantId(tenantId);
        item.setJobId(jobId);
        item.setLineNo(lineNo);
        item.setStatus("FAILED");
        item.setRawLine(rawLine);
        item.setErrorCode(errorCode);
        item.setErrorMessage(message);
        itemRepository.save(item);
    }

    private int parseLineNo(String wrapped) {
        if (wrapped == null) return 0;
        int idx = wrapped.indexOf('\t');
        if (idx <= 0) return 0;
        try {
            return Integer.parseInt(wrapped.substring(0, idx));
        } catch (Exception ex) {
            return 0;
        }
    }

    private String parseRawLine(String wrapped) {
        if (wrapped == null) return "";
        int idx = wrapped.indexOf('\t');
        if (idx < 0) return wrapped;
        return wrapped.substring(idx + 1);
    }

    private String[] splitCsvLine(String line) {
        List<String> out = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                inQuote = !inQuote;
                continue;
            }
            if (ch == ',' && !inQuote) {
                out.add(current.toString().trim());
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        out.add(current.toString().trim());
        while (out.size() < 7) out.add("");
        return out.toArray(new String[0]);
    }

    private Row parseRow(String[] cols) {
        Row row = new Row();
        row.name = trim(cols, 0);
        row.company = trim(cols, 1);
        row.phone = trim(cols, 2);
        row.email = trim(cols, 3);
        row.source = trim(cols, 4);
        row.owner = trim(cols, 5);
        String status = trim(cols, 6).toUpperCase(Locale.ROOT);
        row.status = status.isEmpty() ? "NEW" : status;
        return row;
    }

    private String trim(String[] cols, int idx) {
        if (cols == null || idx < 0 || idx >= cols.length || cols[idx] == null) return "";
        return cols[idx].trim();
    }

    private void validateRow(Row row) {
        if (isBlank(row.name)) throw new IllegalArgumentException("lead_name_required");
        if (!isBlank(row.phone) && !row.phone.matches("^[0-9+\\-()\\s]{6,30}$")) {
            throw new IllegalArgumentException("contact_phone_invalid");
        }
        if (!isBlank(row.email) && !row.email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("contact_email_invalid");
        }
        if (!LEAD_STATUSES.contains(row.status)) {
            throw new IllegalArgumentException("invalid_lead_status");
        }
    }

    private String keyOf(String name, String company, String phone, String email) {
        return (normalize(name) + "|" + normalize(company) + "|" + normalize(phone) + "|" + normalize(email)).toLowerCase(Locale.ROOT);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalizeError(Exception ex) {
        if (ex == null || ex.getMessage() == null || ex.getMessage().trim().isEmpty()) return "lead_import_unknown_error";
        return ex.getMessage().trim();
    }

    private void ensureTenantImportCapacity(String tenantId) {
        long activeJobs = jobRepository.countByTenantIdAndStatusIn(
                tenantId,
                Arrays.<String>asList("PENDING", "RUNNING")
        );
        if (activeJobs >= maxConcurrentPerTenant) {
            throw new IllegalStateException("lead_import_concurrent_limit_exceeded");
        }
    }

    private Map<String, Object> buildTaskStats(LeadImportJob job) {
        Map<String, Object> stats = new LinkedHashMap<String, Object>();
        if (job == null || isBlank(job.getTenantId()) || isBlank(job.getId())) {
            stats.put("totalChunks", 0);
            stats.put("pendingChunks", 0);
            stats.put("runningChunks", 0);
            stats.put("processedChunks", 0);
            stats.put("failedChunks", 0);
            stats.put("canceledChunks", 0);
            return stats;
        }
        String tenantId = job.getTenantId();
        String jobId = job.getId();
        long total = chunkRepository.countByTenantIdAndJobId(tenantId, jobId);
        long processed = chunkRepository.countByTenantIdAndJobIdAndStatus(tenantId, jobId, "PROCESSED");
        long failed = chunkRepository.countByTenantIdAndJobIdAndStatus(tenantId, jobId, "FAILED");
        long canceled = chunkRepository.countByTenantIdAndJobIdAndStatus(tenantId, jobId, "CANCELED");
        long running = chunkRepository.countByTenantIdAndJobIdAndStatus(tenantId, jobId, "RUNNING");
        long pending = Math.max(0L, total - processed - failed - canceled - running);
        stats.put("totalChunks", total);
        stats.put("pendingChunks", pending);
        stats.put("runningChunks", running);
        stats.put("processedChunks", processed);
        stats.put("failedChunks", failed);
        stats.put("canceledChunks", canceled);
        return stats;
    }

    private String buildAuditDetails(String event, String requestId, String detail) {
        String rid = requestId == null ? "" : requestId.trim();
        String d = detail == null ? "" : detail;
        return "event=" + event + ";requestId=" + rid + ";detail=" + d;
    }

    private String newId(String prefix) {
        return prefix + "_" + Long.toString(System.currentTimeMillis(), 36) + String.format("%03d", (int) (Math.random() * 1000));
    }

    private static class Row {
        String name;
        String company;
        String phone;
        String email;
        String source;
        String owner;
        String status;
    }
}
