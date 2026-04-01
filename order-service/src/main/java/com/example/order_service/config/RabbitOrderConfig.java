package com.example.order_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.HashMap;

@Configuration
public class RabbitOrderConfig {

    /************************Fanout交换机 Str ********************************/

    // 定义交换机名称
    public static final String EXCHANGE_NAME = "order.fanout.exchange";

    // 1. 声明 Fanout 交换机
    @Bean
    public FanoutExchange orderFanoutExchange() {
        // durable=true 表示持久化
        return new FanoutExchange(EXCHANGE_NAME, true, false);
    }

    /************************Fanout交换机 End ********************************/

    /************************Direct交换机 Str ********************************/

    // ====================== 1. 定义常量（交换机、队列、路由键） ======================
    // Direct 交换机名称
    public static final String DIRECT_EXCHANGE_NAME = "order.direct.exchange";

    // 队列1：订单创建队列（路由键：order.create）
    public static final String ORDER_CREATE_QUEUE = "order.create.queue";
    public static final String ORDER_CREATE_ROUTING_KEY = "order.create";

    // 队列2：订单支付队列（路由键：order.pay）
    public static final String ORDER_PAY_QUEUE = "order.pay.queue";
    public static final String ORDER_PAY_ROUTING_KEY = "order.pay";

    // 队列3：订单取消队列（路由键：order.cancel）
    public static final String ORDER_CANCEL_QUEUE = "order.cancel.queue";
    public static final String ORDER_CANCEL_ROUTING_KEY = "order.cancel";


    // ====================== 2. 声明 Direct 交换机 ======================
    @Bean
    public DirectExchange orderDirectExchange() {
        // durable=true 持久化，autoDelete=false 不自动删除
        return new DirectExchange(DIRECT_EXCHANGE_NAME, true, false);
    }


    // ====================== 3. 声明队列 ======================
    @Bean
    public Queue orderCreateQueue() {
        return QueueBuilder.durable(ORDER_CREATE_QUEUE).build();
    }

    @Bean
    public Queue orderPayQueue() {
        return QueueBuilder.durable(ORDER_PAY_QUEUE).build();
    }

    @Bean
    public Queue orderCancelQueue() {
        return QueueBuilder.durable(ORDER_CANCEL_QUEUE).build();
    }


    // ====================== 4. 队列绑定到交换机（指定路由键） ======================
    @Bean
    public Binding orderCreateBinding(Queue orderCreateQueue, DirectExchange orderDirectExchange) {
        return BindingBuilder.bind(orderCreateQueue)
                .to(orderDirectExchange)
                .with(ORDER_CREATE_ROUTING_KEY);
    }

    @Bean
    public Binding orderPayBinding(Queue orderPayQueue, DirectExchange orderDirectExchange) {
        return BindingBuilder.bind(orderPayQueue)
                .to(orderDirectExchange)
                .with(ORDER_PAY_ROUTING_KEY);
    }

    @Bean
    public Binding orderCancelBinding(Queue orderCancelQueue, DirectExchange orderDirectExchange) {
        return BindingBuilder.bind(orderCancelQueue)
                .to(orderDirectExchange)
                .with(ORDER_CANCEL_ROUTING_KEY);
    }

    /************************Direct交换机 End ********************************/

    // JSON序列化，替代Java序列化

    @Bean
    public MessageConverter messageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();

        // 1. 创建类型映射器，2.4.17版本正确写法
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();

        // 2. 关键：设置空的类映射，彻底不发送__TypeId__
        typeMapper.setIdClassMapping(Collections.emptyMap());
        // 3. 信任所有包，避免反序列化安全限制
        typeMapper.setTrustedPackages("*");

        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
