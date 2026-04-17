package com.xy.bi.config;


import com.xy.bi.constant.BiMqConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置类
 * @author 25133
 */
@Configuration
public class RabbitMQConfig {

    // 定义交换机名称
    public static final String CODE_EXCHANGE = "code_exchange";

    // 定义队列名称（和消费者中的一致）
    public static final String CODE_QUEUE = "code_queue";

    // 定义路由键
    public static final String CODE_ROUTING_KEY = "code_routing_key";
    // 创建交换机（Direct类型）
    @Bean
    public DirectExchange codeExchange() {
        return new DirectExchange(CODE_EXCHANGE);
    }

    // 创建队列
    @Bean
    public Queue codeQueue() {
        return new Queue(CODE_QUEUE, true); // true表示持久化
    }

    // 绑定队列到交换机
    @Bean
    public Binding codeBinding() {
        return BindingBuilder
                .bind(codeQueue())
                .to(codeExchange())
                .with(CODE_ROUTING_KEY);
    }

    @Bean
    public DirectExchange BiExchange() {
        return new DirectExchange(BiMqConstant.BI_EXCHANGE_NAME);
    }

    // 创建队列
    @Bean
    public Queue BiQueue() {
        return new Queue(BiMqConstant.BI_QUEUE_NAME, true); // true表示持久化
    }

    // 绑定队列到交换机
    @Bean
    public Binding BiBinding() {
        return BindingBuilder
                .bind(BiQueue())
                .to(BiExchange())
                .with(BiMqConstant.BI_ROUTING_KEY);
    }
}