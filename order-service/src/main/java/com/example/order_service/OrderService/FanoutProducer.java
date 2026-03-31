package com.example.order_service.OrderService;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FanoutProducer {

    // 广播交换机
    String FANOUT_EXCHANGE = "test_fanout_exchange";


    @Autowired
    private RabbitTemplate rabbitTemplate;

    // 发送广播消息
    public void send(){
        String msg = "订单创建成功！广播通知～";
        rabbitTemplate.convertAndSend(
                FANOUT_EXCHANGE,
                "",
                msg
        );
        System.out.println("已发送广播消息");
    }
}
