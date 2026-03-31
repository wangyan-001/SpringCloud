package com.example.product_service.ProductService.impl;

import com.example.product_service.config.RabbitProductConfig;
import com.example.product_service.entity.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    // @Autowired
    // private ProductMapper productMapper;

    @RabbitListener(queues = RabbitProductConfig.QUEUE_NAME)
    @Override
    public void deductStock(OrderCreatedEvent event) {
        System.out.println("【商品服务】开始扣减库存，商品 ID: "+ event.getProductId());
    }
}
