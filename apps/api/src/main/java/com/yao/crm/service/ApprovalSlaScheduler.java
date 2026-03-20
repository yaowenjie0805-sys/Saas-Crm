package com.yao.crm.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ApprovalSlaScheduler {

    private final ApprovalSlaService approvalSlaService;

    public ApprovalSlaScheduler(ApprovalSlaService approvalSlaService) {
        this.approvalSlaService = approvalSlaService;
    }

    @Scheduled(fixedDelayString = "${approval.sla.scan-ms:45000}")
    public void runScan() {
        approvalSlaService.scanOverdueAndEscalate();
    }
}
