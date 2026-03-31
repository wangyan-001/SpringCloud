package com.example.order_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RefreshScope
public class OrderConfigController {

    // 注入环境对象（直接读Spring环境里的配置）
    @Autowired
    private Environment env;

    @GetMapping("/order/config")
    public String getConfig() {
        // 直接从环境里读，优先级最高
        String timeout = env.getProperty("order.timeout", "2000");
        String paySwitch = env.getProperty("order.pay-switch", "false");
        return "订单超时时间1111：" + timeout + "毫秒，支付开关：" + paySwitch;
    }
}

