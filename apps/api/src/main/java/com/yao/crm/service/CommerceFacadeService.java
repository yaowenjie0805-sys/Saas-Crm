package com.yao.crm.service;

import com.yao.crm.entity.OrderRecord;
import com.yao.crm.entity.PriceBook;
import com.yao.crm.entity.Quote;
import com.yao.crm.entity.Tenant;
import com.yao.crm.repository.OrderRecordRepository;
import com.yao.crm.repository.PriceBookRepository;
import com.yao.crm.repository.QuoteRepository;
import com.yao.crm.repository.TenantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class CommerceFacadeService {

    private final QuoteRepository quoteRepository;
    private final OrderRecordRepository orderRecordRepository;
    private final PriceBookRepository priceBookRepository;
    private final TenantRepository tenantRepository;

    public CommerceFacadeService(QuoteRepository quoteRepository,
                                 OrderRecordRepository orderRecordRepository,
                                 PriceBookRepository priceBookRepository,
                                 TenantRepository tenantRepository) {
        this.quoteRepository = quoteRepository;
        this.orderRecordRepository = orderRecordRepository;
        this.priceBookRepository = priceBookRepository;
        this.tenantRepository = tenantRepository;
    }

    public String normalizeStatusOrBlank(String rawStatus, Set<String> allowedStatuses) {
        String normalized = rawStatus == null ? "" : rawStatus.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) return "";
        return allowedStatuses.contains(normalized) ? normalized : null;
    }

    public int normalizePageSize(int size) {
        return Math.max(1, Math.min(100, size));
    }

    public Page<PriceBook> findPriceBooks(String tenantId, String normalizedStatus, Pageable pageable) {
        if (normalizedStatus == null || normalizedStatus.isEmpty()) {
            return priceBookRepository.findByTenantId(tenantId, pageable);
        }
        return priceBookRepository.findByTenantIdAndStatus(tenantId, normalizedStatus, pageable);
    }

    public Page<Quote> findQuotes(String tenantId,
                                  String normalizedStatus,
                                  String opportunityId,
                                  Collection<String> owners,
                                  Pageable pageable) {
        boolean scoped = owners != null && !owners.isEmpty();
        boolean hasStatus = normalizedStatus != null && !normalizedStatus.isEmpty();
        boolean hasOpportunity = opportunityId != null && !opportunityId.isEmpty();
        if (scoped) {
            if (!hasOpportunity) {
                return hasStatus
                        ? quoteRepository.findByTenantIdAndStatusAndOwnerIn(tenantId, normalizedStatus, owners, pageable)
                        : quoteRepository.findByTenantIdAndOwnerIn(tenantId, owners, pageable);
            }
            return hasStatus
                    ? quoteRepository.findByTenantIdAndStatusAndOpportunityIdAndOwnerIn(tenantId, normalizedStatus, opportunityId, owners, pageable)
                    : quoteRepository.findByTenantIdAndOpportunityIdAndOwnerIn(tenantId, opportunityId, owners, pageable);
        }
        if (!hasOpportunity) {
            return hasStatus
                    ? quoteRepository.findByTenantIdAndStatus(tenantId, normalizedStatus, pageable)
                    : quoteRepository.findByTenantId(tenantId, pageable);
        }
        return hasStatus
                ? quoteRepository.findByTenantIdAndStatusAndOpportunityId(tenantId, normalizedStatus, opportunityId, pageable)
                : quoteRepository.findByTenantIdAndOpportunityId(tenantId, opportunityId, pageable);
    }

    public Page<OrderRecord> findOrders(String tenantId,
                                        String normalizedStatus,
                                        String opportunityId,
                                        Collection<String> owners,
                                        Pageable pageable) {
        boolean scoped = owners != null && !owners.isEmpty();
        boolean hasStatus = normalizedStatus != null && !normalizedStatus.isEmpty();
        boolean hasOpportunity = opportunityId != null && !opportunityId.isEmpty();
        if (scoped) {
            if (!hasOpportunity) {
                return hasStatus
                        ? orderRecordRepository.findByTenantIdAndStatusAndOwnerIn(tenantId, normalizedStatus, owners, pageable)
                        : orderRecordRepository.findByTenantIdAndOwnerIn(tenantId, owners, pageable);
            }
            return hasStatus
                    ? orderRecordRepository.findByTenantIdAndStatusAndOpportunityIdAndOwnerIn(tenantId, normalizedStatus, opportunityId, owners, pageable)
                    : orderRecordRepository.findByTenantIdAndOpportunityIdAndOwnerIn(tenantId, opportunityId, owners, pageable);
        }
        if (!hasOpportunity) {
            return hasStatus
                    ? orderRecordRepository.findByTenantIdAndStatus(tenantId, normalizedStatus, pageable)
                    : orderRecordRepository.findByTenantId(tenantId, pageable);
        }
        return hasStatus
                ? orderRecordRepository.findByTenantIdAndStatusAndOpportunityId(tenantId, normalizedStatus, opportunityId, pageable)
                : orderRecordRepository.findByTenantIdAndOpportunityId(tenantId, opportunityId, pageable);
    }

    public String resolveApprovalMode(String tenantId) {
        Optional<Tenant> tenantOpt = tenantRepository.findById(tenantId);
        if (!tenantOpt.isPresent()) return "STRICT";
        String mode = tenantOpt.get().getApprovalMode();
        if (mode == null || mode.trim().isEmpty()) return "STRICT";
        String normalized = mode.trim().toUpperCase(Locale.ROOT);
        return "STAGE_GATE".equals(normalized) ? "STAGE_GATE" : "STRICT";
    }

    public String resolveOrderApprovalMode(OrderRecord order, String tenantId) {
        if (order != null && order.getNotes() != null && !order.getNotes().trim().isEmpty()) {
            String notes = order.getNotes().toUpperCase(Locale.ROOT);
            if (notes.contains("[APPROVAL_MODE=STAGE_GATE]")) return "STAGE_GATE";
            if (notes.contains("[APPROVAL_MODE=STRICT]")) return "STRICT";
        }
        return resolveApprovalMode(tenantId);
    }

    public String buildOrderCreationNotes(String quoteNo, String approvalMode) {
        return "Created from quote " + quoteNo + " [approval_mode=" + approvalMode + "]";
    }
}

