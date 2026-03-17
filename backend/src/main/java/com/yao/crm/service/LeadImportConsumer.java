package com.yao.crm.service;

import com.yao.crm.config.LeadImportMqConfig;
import com.yao.crm.dto.LeadImportChunkMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@ConditionalOnProperty(name = "lead.import.listener.enabled", havingValue = "true", matchIfMissing = true)
public class LeadImportConsumer {

    private final LeadImportService leadImportService;

    public LeadImportConsumer(LeadImportService leadImportService) {
        this.leadImportService = leadImportService;
    }

    @RabbitListener(queues = LeadImportMqConfig.QUEUE)
    public void consumeLeadImportChunk(LeadImportChunkMessage message) {
        leadImportService.consumeLeadImportChunk(message);
    }
}
