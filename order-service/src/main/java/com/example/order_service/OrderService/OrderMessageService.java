package com.example.order_service.OrderService;

import com.example.order_service.config.RabbitOrderConfig;
import com.example.order_service.dto.OrderDTO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service // 必须加这个
public class OrderMessageService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // 发送订单创建消息
    public void sendCreateOrderMessage(OrderDTO order) {
        rabbitTemplate.convertAndSend(
                RabbitOrderConfig.DIRECT_EXCHANGE_NAME,
                RabbitOrderConfig.ORDER_CREATE_ROUTING_KEY,
                order
        );
    }

    // 发送订单支付消息
    public void sendPayOrderMessage(OrderDTO order) {
        rabbitTemplate.convertAndSend(
                RabbitOrderConfig.DIRECT_EXCHANGE_NAME,
                RabbitOrderConfig.ORDER_PAY_ROUTING_KEY,
                order
        );
    }

    // 发送订单取消消息
    public void sendCancelOrderMessage(OrderDTO order) {
        rabbitTemplate.convertAndSend(
                RabbitOrderConfig.DIRECT_EXCHANGE_NAME,
                RabbitOrderConfig.ORDER_CANCEL_ROUTING_KEY,
                order
        );
    }
}
