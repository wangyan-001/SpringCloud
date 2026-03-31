package com.example.product_service.config;

import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;

import java.util.HashMap;
import java.util.Map;


@Configuration
public class RabbitProductConfig {

    /*********************************************

    // 广播交换机
    String FANOUT_EXCHANGE = "test_fanout_exchange";
    // 两个队列
    public static final String FANOUT_QUEUE2 = "fanout_queue_2";

    // 交换机（一样）

    @Bean
    public FanoutExchange testFanoutExchange(){
        return new FanoutExchange(FANOUT_EXCHANGE,true,false);
    }

    // 队列2：product 用
    @Bean
    public Queue fanoutQueue2(){
        return new Queue(FANOUT_QUEUE2);
    }

    // 绑定
    @Bean
    public Binding bindingQueue2(){
        return BindingBuilder.bind(fanoutQueue2()).to(testFanoutExchange());
    } */



    // 必须与生产者交换机名称一致
    public static final String EXCHANGE_NAME = "order.fanout.exchange";
    // 用户服务专属队列
    public static final String QUEUE_NAME = "product.queue";

    @Bean
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange(EXCHANGE_NAME, true, false);
    }
    // 1. 声明队列（用户服务用）
    @Bean
    public Queue userQueue() {
        return QueueBuilder.durable(QUEUE_NAME).build();
    }

    // 2. 绑定队列到 Fanout 交换机
    @Bean
    public Binding bindingUserQueue(FanoutExchange orderFanoutExchange, Queue userQueue) {
        // Fanout 绑定不需要 routing key，直接 bind
        return BindingBuilder.bind(userQueue).to(orderFanoutExchange);
    }

    // JSON序列化，替代Java序列化
    @Bean
    public MessageConverter messageConverter() {
        // 1. 创建 JSON 转换器
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();

        // 2. 创建类型映射器，用来控制类型头
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();

        // 3. 关键：信任所有包，同时关闭发送类型 ID（__TypeId__）
        typeMapper.setTrustedPackages("*");

        // 4. 添加类型映射：将生产者的类映射到消费者的本地类
        Map<String, Class<?>> idClassMapping = new HashMap<>();
        idClassMapping.put("com.example.order_service.entity.OrderCreatedEvent",
                com.example.product_service.entity.OrderCreatedEvent.class);
        typeMapper.setIdClassMapping(idClassMapping);

        // 5. 把类型映射器设置给转换器
        converter.setJavaTypeMapper(typeMapper);

        return converter;
    }

}
