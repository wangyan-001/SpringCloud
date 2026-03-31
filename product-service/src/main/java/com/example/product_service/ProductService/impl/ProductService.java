package com.example.product_service.ProductService.impl;

import com.example.product_service.config.RabbitProductConfig;
import com.example.product_service.entity.OrderCreatedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

public interface ProductService {

    /**
     * 模拟扣减库存
     */
    void deductStock(OrderCreatedEvent event);
}