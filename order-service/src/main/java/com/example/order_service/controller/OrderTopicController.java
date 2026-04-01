package com.example.order_service.controller;

import com.example.order_service.OrderService.TopicMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("order/topic")
public class OrderTopicController {

    @Autowired
    private TopicMessageService topicMessageService;

    @GetMapping("/create")
    public String create() {
        topicMessageService.sendCreate();
        return "发送：order.create";
    }

    @GetMapping("/createError")
    public String createError() {
        topicMessageService.sendCreateError();
        return "发送：order.create.error";
    }

    @GetMapping("/payError")
    public String payError() {
        topicMessageService.sendPayError();
        return "发送：order.pay.error";
    }
}
