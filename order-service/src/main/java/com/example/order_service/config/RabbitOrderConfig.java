package com.example.order_service.config;

import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.HashMap;

@Configuration
public class RabbitOrderConfig {
    // 定义交换机名称
    public static final String EXCHANGE_NAME = "order.fanout.exchange1990";

    // 1. 声明 Fanout 交换机
    @Bean
    public FanoutExchange orderFanoutExchange() {
        // durable=true 表示持久化
        return new FanoutExchange(EXCHANGE_NAME, true, false);
    }

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
