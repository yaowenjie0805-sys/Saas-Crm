package com.yao.crm.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
@ConditionalOnProperty(name = "lead.import.mq.declare.enabled", havingValue = "true", matchIfMissing = true)
public class LeadImportMqConfig {

    public static final String EXCHANGE = "crm.lead.import.exchange";
    public static final String ROUTING_KEY = "crm.lead.import.process";
    public static final String RETRY_ROUTING_KEY = "crm.lead.import.retry";
    public static final String DEAD_ROUTING_KEY = "crm.lead.import.dead";
    public static final String QUEUE = "crm.lead.import.queue";
    public static final String RETRY_QUEUE = "crm.lead.import.retry.queue";
    public static final String DEAD_QUEUE = "crm.lead.import.dead.queue";

    @Bean
    public DirectExchange leadImportExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue leadImportQueue() {
        return QueueBuilder.durable(QUEUE)
                .withArgument("x-dead-letter-exchange", EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DEAD_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue leadImportRetryQueue() {
        return QueueBuilder.durable(RETRY_QUEUE)
                .withArgument("x-message-ttl", 15000)
                .withArgument("x-dead-letter-exchange", EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue leadImportDeadQueue() {
        return QueueBuilder.durable(DEAD_QUEUE).build();
    }

    @Bean
    public Binding leadImportBinding(Queue leadImportQueue, DirectExchange leadImportExchange) {
        return BindingBuilder.bind(leadImportQueue).to(leadImportExchange).with(ROUTING_KEY);
    }

    @Bean
    public Binding leadImportRetryBinding(Queue leadImportRetryQueue, DirectExchange leadImportExchange) {
        return BindingBuilder.bind(leadImportRetryQueue).to(leadImportExchange).with(RETRY_ROUTING_KEY);
    }

    @Bean
    public Binding leadImportDeadBinding(Queue leadImportDeadQueue, DirectExchange leadImportExchange) {
        return BindingBuilder.bind(leadImportDeadQueue).to(leadImportExchange).with(DEAD_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter converter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter);
        rabbitTemplate.setExchange(EXCHANGE);
        return rabbitTemplate;
    }
}
