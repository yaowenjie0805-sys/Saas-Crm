package com.yao.crm.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ApprovalSlaScheduler {

    private final ApprovalSlaService approvalSlaService;

    public ApprovalSlaScheduler(ApprovalSlaService approvalSlaService) {
        this.approvalSlaService = approvalSlaService;
    }

    @Transactional(timeout = 30)
    @Scheduled(fixedDelayString = "${approval.sla.scan-ms:45000}")
    public void runScan() {
        approvalSlaService.scanOverdueAndEscalate();
    }
}
