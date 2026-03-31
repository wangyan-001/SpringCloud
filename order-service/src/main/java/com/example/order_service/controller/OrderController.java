package com.example.order_service.controller;

import com.example.order_service.FeignClient.UserServiceFeignClient;
import com.example.order_service.OrderService.FanoutProducer;
import com.example.order_service.OrderService.OrderService;
import com.example.order_service.dto.OrderDTO;
import com.example.order_service.dto.UserDTO;
import com.example.order_service.entity.Order;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /*MQ*/
    @Autowired
    private FanoutProducer fanoutProducer;

    @PostMapping("/mq")
    public String createOrderMq() {
        // 发送消息
        fanoutProducer.send();
        return "已发送广播消息";
    }


    @PostMapping("/create")
    public String createOrder(@RequestBody Order order) throws InterruptedException {
        orderService.createOrder(order);
        return "下单成功";
    }

    // Postman 调用这个接口 → 触发 createOrder → 发送 RabbitMQ 广播消息
    @PostMapping("/createMq")
    public String createOrderMq(@RequestBody OrderDTO orderDTO) {
        orderService.createOrderMq(orderDTO);
        return "下单成功，已发送广播消息";
    }

    // 注入Feign客户端
    @Autowired
    private UserServiceFeignClient userServiceFeignClient;

    /**
     * 创建订单（模拟调用用户服务）
     */
    @GetMapping("/orders/create/{userId}")
    public String createOrder(@PathVariable Long userId) {
        // 调用用户服务获取用户信息
        UserDTO user = userServiceFeignClient.getUserById(userId);

        // 模拟创建订单
        String result = String.format("创建订单成功！\n用户信息：%s\n提供服务的端口：%d",
                user.getUsername(), user.getServerPort());
        System.out.println(result);
        return result;
    }
}
