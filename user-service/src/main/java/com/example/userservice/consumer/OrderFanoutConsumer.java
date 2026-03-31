package com.example.userservice.consumer;

import com.example.userservice.config.RabbitUserConfig;
import com.example.userservice.entity.OrderCreatedEvent;
import com.example.userservice.service.UserService;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RabbitListener(queues = RabbitUserConfig.QUEUE_NAME)
@Slf4j
public class OrderFanoutConsumer {

    @Autowired
    private UserService userService;

    // 监听队列，处理消息
    @RabbitHandler
    public void handleOrderEvent(OrderCreatedEvent event) {
        try {
            System.out.println("信息打印：" + event);
            log.info("用户服务接收到订单事件：{}", event.getOrderId());
            // 执行业务：给用户加积分
            userService.addPoints(event.getUserId(), event.getAmount().longValue());
        } catch (Exception e) {
            log.error("处理订单事件失败", e);
        }
    }


}
