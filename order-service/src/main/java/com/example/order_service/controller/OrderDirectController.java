package com.example.order_service.controller;

import com.example.order_service.OrderService.OrderMessageService;
import com.example.order_service.dto.OrderDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/order")
public class OrderDirectController {

    // 注入消息发送服务
    @Autowired
    private OrderMessageService orderMessageService;

    /**
     * 测试：发送 订单创建 消息
     */
    @GetMapping("/createDirect")
    public String createOrder() {
        // 1. 构造订单数据（你可以改成从前端接收）
        OrderDTO order = new OrderDTO();
        order.setOrderId("ORDER_1001");
        order.setOrderName("测试订单");
        order.setPrice(99.8);

        // 2. 发送消息
        orderMessageService.sendCreateOrderMessage(order);

        return "订单创建消息发送成功";
    }

    /**
     * 测试：发送 订单支付 消息
     */
    @GetMapping("/pay")
    public String payOrder() {
        OrderDTO order = new OrderDTO();
        order.setOrderId("ORDER_1001");
        order.setOrderName("测试订单");

        orderMessageService.sendPayOrderMessage(order);
        return "订单支付消息发送成功";
    }

    /**
     * 测试：发送 订单取消 消息
     */
    @GetMapping("/cancel")
    public String cancelOrder() {
        OrderDTO order = new OrderDTO();
        order.setOrderId("ORDER_1001");

        orderMessageService.sendCancelOrderMessage(order);
        return "订单取消消息发送成功";
    }
}