package com.yao.crm.service;

import com.yao.crm.entity.PushMessage;
import com.yao.crm.repository.PushMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 消息推送服务 - 国内特色。
 * 支持企业微信、钉钉、飞书、短信、邮件与站内信。
 */
@Service
public class MessagePushService {

    private static final Logger log = LoggerFactory.getLogger(MessagePushService.class);

    private final PushMessageRepository pushMessageRepository;
    private final IntegrationWebhookService integrationWebhookService;

    public MessagePushService(PushMessageRepository pushMessageRepository,
                              IntegrationWebhookService integrationWebhookService) {
        this.pushMessageRepository = pushMessageRepository;
        this.integrationWebhookService = integrationWebhookService;
    }

    /**
     * 发送消息。
     */
    public PushMessage sendMessage(String tenantId, String userId, String channel,
                                   String title, String content, String relatedType, String relatedId) {
        PushMessage message = new PushMessage();
        message.setId(UUID.randomUUID().toString());
        message.setTenantId(tenantId);
        message.setUserId(userId);
        message.setChannel(channel);
        message.setTitle(title);
        message.setContent(content);
        message.setRelatedType(relatedType);
        message.setRelatedId(relatedId);
        message.setStatus("PENDING");
        message.setRetryCount(0);
        message.setCreatedAt(LocalDateTime.now());

        boolean success = sendByChannel(tenantId, channel, userId, title, content);

        if (success) {
            message.setStatus("SENT");
            message.setSentAt(LocalDateTime.now());
        } else {
            message.setStatus("FAILED");
            message.setErrorMessage("Failed to send message");
        }

        return pushMessageRepository.save(message);
    }

    /**
     * 按渠道路由发送。
     */
    private boolean sendByChannel(String tenantId, String channel, String userId, String title, String content) {
        String normalized = channel == null ? "" : channel.trim().toUpperCase();
        switch (normalized) {
            case "WECOM":
            case "WECHAT_WORK":
                return sendToWeChatWork(tenantId, userId, title, content);
            case "DINGTALK":
                return sendToDingTalk(tenantId, userId, title, content);
            case "FEISHU":
            case "LARK":
                return sendToFeishu(tenantId, userId, title, content);
            case "SMS":
                return sendSms(userId, content);
            case "EMAIL":
                return sendEmail(userId, title, content);
            case "IN_APP":
                return true;
            default:
                log.warn("Unknown channel: {}", channel);
                return false;
        }
    }

    private boolean sendToWeChatWork(String tenantId, String userId, String title, String content) {
        log.info("Sending WeCom message to user: {}, title: {}", userId, title);
        return integrationWebhookService.sendMessage("WECOM", tenantId, title, content, userId);
    }

    private boolean sendToDingTalk(String tenantId, String userId, String title, String content) {
        log.info("Sending DingTalk message to user: {}, title: {}", userId, title);
        return integrationWebhookService.sendMessage("DINGTALK", tenantId, title, content, userId);
    }

    private boolean sendToFeishu(String tenantId, String userId, String title, String content) {
        log.info("Sending Feishu message to user: {}, title: {}", userId, title);
        return integrationWebhookService.sendMessage("FEISHU", tenantId, title, content, userId);
    }

    /**
     * 短信通道占位。
     */
    private boolean sendSms(String phone, String content) {
        log.info("Sending SMS to: {}", phone);
        return true;
    }

    /**
     * 邮件通道占位。
     */
    private boolean sendEmail(String email, String title, String content) {
        log.info("Sending email to: {}, title: {}", email, title);
        return true;
    }

    public List<PushMessage> sendBatchMessages(String tenantId, List<String> userIds, String channel,
                                               String title, String content, String relatedType, String relatedId) {
        List<PushMessage> results = new ArrayList<PushMessage>();

        for (String userId : userIds) {
            try {
                PushMessage message = sendMessage(tenantId, userId, channel, title, content, relatedType, relatedId);
                results.add(message);
            } catch (Exception e) {
                log.error("Failed to send message to user: {}", userId, e);
            }
        }

        return results;
    }

    /**
     * 重试发送失败消息。
     */
    public void retryFailedMessages(String tenantId) {
        List<PushMessage> failedMessages = pushMessageRepository.findFailedByTenantId(tenantId);

        for (PushMessage message : failedMessages) {
            if (message.getRetryCount() >= 3) {
                continue;
            }

            boolean success = sendByChannel(
                    message.getTenantId(),
                    message.getChannel(),
                    message.getUserId(),
                    message.getTitle(),
                    message.getContent()
            );

            message.setRetryCount(message.getRetryCount() + 1);

            if (success) {
                message.setStatus("SENT");
                message.setSentAt(LocalDateTime.now());
            } else {
                message.setErrorMessage("Retry failed");
            }

            pushMessageRepository.save(message);
        }
    }

    public List<PushMessage> getUserMessages(String tenantId, String userId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return pushMessageRepository.findByUserIdOrderByCreatedAtDesc(tenantId, userId, pageable);
    }

    public void markAsRead(String messageId) {
        pushMessageRepository.findById(messageId).ifPresent(message -> {
            message.setStatus("READ");
            message.setReadAt(LocalDateTime.now());
            pushMessageRepository.save(message);
        });
    }

    public long getUnreadCount(String tenantId, String userId) {
        return pushMessageRepository.countUnreadByUserId(tenantId, userId);
    }

    public PushMessage sendApprovalNotification(String tenantId, String approverId, String taskTitle,
                                                String content, String taskId) {
        return sendMessage(
                tenantId,
                approverId,
                "IN_APP",
                "您有新的审批任务",
                "审批任务: " + taskTitle + "\n" + content,
                "APPROVAL_TASK",
                taskId
        );
    }

    public PushMessage sendTaskReminder(String tenantId, String userId, String taskTitle, String taskId) {
        return sendMessage(
                tenantId,
                userId,
                "IN_APP",
                "任务到期提醒",
                "您有任务即将到期: " + taskTitle,
                "TASK",
                taskId
        );
    }

    public PushMessage sendOpportunityUpdate(String tenantId, String ownerId, String oppName,
                                             String oldStage, String newStage) {
        String content = String.format("商机 %s 已从 [%s] 变更到 [%s]", oppName, oldStage, newStage);

        return sendMessage(
                tenantId,
                ownerId,
                "IN_APP",
                "商机阶段更新",
                content,
                "OPPORTUNITY",
                null
        );
    }
}
