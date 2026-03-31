package com.example.order_service.OrderService.impl;

import com.example.order_service.OrderService.OrderService;
import com.example.order_service.config.RabbitOrderConfig;
import com.example.order_service.dto.OrderDTO;
import com.example.order_service.entity.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;


import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OrderServiceImpl extends OrderService {

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RabbitOrderConfig config;

    @Override
    public void createOrderMq(OrderDTO orderDTO) {
        // 1. 执行下单逻辑（保存数据库、生成ID...）
        String orderId = "ORDER_" + System.currentTimeMillis();
        log.info("订单 {} 创建成功", orderId);

        // 2. 构造消息事件
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(orderId);
        event.setUserId(orderDTO.getUserId());
        event.setProductId(orderDTO.getProductId());
        event.setAmount(orderDTO.getAmount());

        // 3. 发送 Fanout 广播消息
        // 注意：Fanout 模式下，Routing key 填 "" 或 null 都可以
        rabbitTemplate.convertAndSend(config.EXCHANGE_NAME, "", event);
        log.info("订单 {} 已广播 Fanout 消息", orderId);
    }
}
