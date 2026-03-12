package com.denizbitmez.trackingservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "fleet.exchange";
    public static final String ASSIGNED_QUEUE = "courier.assigned.queue";
    public static final String LOCATION_QUEUE = "courier.location.queue";

    @Bean
    public Queue assignedQueue() {
        return new Queue(ASSIGNED_QUEUE);
    }

    @Bean
    public Queue locationQueue() {
        return new Queue(LOCATION_QUEUE);
    }

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public Binding assignedBinding(Queue assignedQueue, DirectExchange exchange) {
        return BindingBuilder.bind(assignedQueue).to(exchange).with("courier.assigned");
    }

    @Bean
    public Binding locationBinding(Queue locationQueue, DirectExchange exchange) {
        return BindingBuilder.bind(locationQueue).to(exchange).with("courier.location");
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
