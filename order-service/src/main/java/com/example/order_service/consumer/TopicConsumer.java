package com.example.order_service.consumer;

import com.example.order_service.config.RabbitOrderConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class TopicConsumer {

    // 只收 order.create
    @RabbitListener(queues = RabbitOrderConfig.QUEUE_CREATE)
    public void create(Object  msg) {
        System.out.println("【创建队列】收到：" + msg);
    }

    // 收所有 order 开头的消息
    @RabbitListener(queues = RabbitOrderConfig.QUEUE_ALL_ORDER)
    public void allOrder(Object  msg) {
        System.out.println("【所有订单队列】收到：" + msg);
    }

    // 收 order.xxx.error
    @RabbitListener(queues = RabbitOrderConfig.QUEUE_ERROR)
    public void error(Object  msg) {
        System.out.println("【错误队列】收到：" + msg);
    }
}
