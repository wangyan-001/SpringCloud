package com.example.order_service.FeignClient;

import com.example.order_service.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

// name：要调用的服务名（Nacos中注册的user-service）
@FeignClient(name = "user-service")
public interface UserServiceFeignClient {

    // 接口路径、请求方式、参数要和用户服务的接口完全一致
    @GetMapping("/users/{id}")
    UserDTO getUserById(@PathVariable("id") Long id);

    /**
     * 远程调用用户服务的扣余额接口
     */
    @PostMapping("/user/deductBalance")
    void deductBalance(@RequestParam("userId") Long userId, @RequestParam("amount") BigDecimal amount);
}
