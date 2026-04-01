package com.example.order_service.OrderService;

import com.example.order_service.config.RabbitOrderConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TopicMessageService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // 发送：order.create
    public void sendCreate() {
        rabbitTemplate.convertAndSend(
                RabbitOrderConfig.TOPIC_EXCHANGE,
                "order.create",  // 路由键
                "订单创建消息"
        );
    }

    // 发送：order.create.error
    public void sendCreateError() {
        rabbitTemplate.convertAndSend(
                RabbitOrderConfig.TOPIC_EXCHANGE,
                "order.create.error",
                "订单创建失败"
        );
    }

    // 发送：order.pay.error
    public void sendPayError() {
        rabbitTemplate.convertAndSend(
                RabbitOrderConfig.TOPIC_EXCHANGE,
                "order.pay.error",
                "订单支付失败"
        );
    }
}